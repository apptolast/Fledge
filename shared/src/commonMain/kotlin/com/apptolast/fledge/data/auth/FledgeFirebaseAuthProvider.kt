package com.apptolast.fledge.data.auth

import com.apptolast.customlogin.domain.AuthProvider
import com.apptolast.customlogin.domain.model.AuthError
import com.apptolast.customlogin.domain.model.AuthResult
import com.apptolast.customlogin.domain.model.AuthState
import com.apptolast.customlogin.domain.model.Credentials
import com.apptolast.customlogin.domain.model.IdentityProvider
import com.apptolast.customlogin.domain.model.PhoneAuthResult
import com.apptolast.customlogin.domain.model.SignUpData
import com.apptolast.customlogin.domain.model.UserSession
import com.apptolast.fledge.data.remote.firebase.FirebaseAuthException
import com.apptolast.fledge.data.remote.firebase.FirebaseAuthService
import com.apptolast.fledge.data.remote.firebase.FirebaseIdToken
import com.apptolast.fledge.data.remote.firebase.FirebaseRefreshResponse
import com.apptolast.fledge.data.remote.firebase.FirebaseSignInResponse
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalTime::class)
class FledgeFirebaseAuthProvider(
    private val authService: FirebaseAuthService,
    private val tokenManager: TokenManager,
    private val socialAuthClient: SocialAuthClient,
) : AuthProvider {

    override val id: String = PROVIDER_ID

    private var currentSession: UserSession? = restoreSession()
    private val authState = MutableStateFlow<AuthState>(
        currentSession?.let(AuthState::Authenticated) ?: AuthState.Unauthenticated,
    )

    override suspend fun signIn(credentials: Credentials): AuthResult = when (credentials) {
        is Credentials.EmailPassword -> runAuth {
            saveSession(
                response = authService.signInWithPassword(credentials.email, credentials.password),
                providerId = PASSWORD_PROVIDER_ID,
                fallbackEmail = credentials.email,
            )
        }
        is Credentials.RefreshToken -> refreshSession(credentials.token)
        is Credentials.OAuthToken -> signInWithOAuth(credentials.provider)
    }

    override suspend fun signUp(data: SignUpData): AuthResult = runAuth {
        val displayName = data.displayName?.takeIf { it.isNotBlank() }
        val response = authService.signUp(data.email, data.password)
        if (displayName != null) {
            runCatching { authService.updateProfile(response.idToken, displayName) }
        }
        saveSession(
            response = response,
            providerId = PASSWORD_PROVIDER_ID,
            fallbackEmail = data.email,
            fallbackDisplayName = displayName,
        )
    }

    override suspend fun signOut(): Result<Unit> {
        clearSession()
        return Result.success(Unit)
    }

    override suspend fun sendPasswordResetEmail(email: String): AuthResult = runAuth {
        authService.sendPasswordResetEmail(email)
        AuthResult.PasswordResetSent
    }

    override suspend fun confirmPasswordReset(code: String, newPassword: String): AuthResult = runAuth {
        authService.confirmPasswordReset(code, newPassword)
        AuthResult.PasswordResetSuccess
    }

    override fun observeAuthState(): Flow<AuthState> = authState

    override suspend fun getCurrentSession(): UserSession? = currentSession

    override suspend fun refreshSession(): AuthResult = refreshSession(tokenManager.refreshToken)

    override suspend fun isSignedIn(): Boolean = currentSession != null && tokenManager.isLoggedIn

    override suspend fun getIdToken(forceRefresh: Boolean): String? {
        if (forceRefresh || tokenManager.isAccessTokenExpired()) {
            if (refreshSession() !is AuthResult.Success) return null
        }
        return tokenManager.accessToken
    }

    override suspend fun deleteAccount(): Result<Unit> = runCatching {
        authService.deleteAccount(freshIdToken())
        clearSession()
    }

    override suspend fun updateDisplayName(displayName: String): Result<Unit> = runCatching {
        val token = freshIdToken()
        authService.updateProfile(token, displayName)
        updateCurrentSession { it.copy(displayName = displayName) }
    }

    override suspend fun updateEmail(newEmail: String): Result<Unit> = runCatching {
        val response = authService.updateEmail(freshIdToken(), newEmail)
        saveSession(
            response = response,
            providerId = currentSession?.providerId ?: PASSWORD_PROVIDER_ID,
            fallbackEmail = newEmail,
            fallbackDisplayName = currentSession?.displayName,
        )
    }

    override suspend fun updatePassword(newPassword: String): Result<Unit> = runCatching {
        val response = authService.updatePassword(freshIdToken(), newPassword)
        saveSession(
            response = response,
            providerId = currentSession?.providerId ?: PASSWORD_PROVIDER_ID,
            fallbackEmail = currentSession?.email,
            fallbackDisplayName = currentSession?.displayName,
        )
    }

    override suspend fun sendEmailVerification(): Result<Unit> = runCatching {
        authService.sendEmailVerification(freshIdToken())
    }

    override suspend fun reauthenticate(credentials: Credentials): AuthResult = signIn(credentials)

    override suspend fun sendPhoneOtp(phoneNumber: String): PhoneAuthResult =
        PhoneAuthResult.Failure(AuthError.OperationNotAllowed(DISABLED_PROVIDER_MESSAGE))

    override suspend fun verifyPhoneOtp(verificationId: String, otpCode: String): AuthResult = unsupportedProvider()

    override suspend fun sendMagicLink(email: String, continueUrl: String, iosBundleId: String?): AuthResult =
        unsupportedProvider()

    override suspend fun signInWithMagicLink(email: String, link: String): AuthResult = unsupportedProvider()

    private suspend fun signInWithOAuth(provider: IdentityProvider): AuthResult = runAuth {
        val social = when (provider) {
            IdentityProvider.Google -> socialAuthClient.signInWithGoogle()
            IdentityProvider.Apple -> socialAuthClient.signInWithApple()
            else -> throw SocialAuthUnavailableException(DISABLED_PROVIDER_MESSAGE)
        }
        saveSession(
            response = authService.signInWithIdp(social.providerId, social.idToken, social.rawNonce),
            providerId = social.providerId,
            fallbackDisplayName = social.displayName,
        )
    }

    private suspend fun refreshSession(refreshToken: String?): AuthResult {
        if (refreshToken.isNullOrBlank()) {
            clearSession()
            return AuthResult.Failure(AuthError.SessionExpired("No hay refresh token de Firebase."))
        }
        return try {
            saveSession(
                response = authService.refreshIdToken(refreshToken),
                providerId = currentSession?.providerId ?: PROVIDER_ID,
                fallbackEmail = currentSession?.email,
                fallbackDisplayName = currentSession?.displayName,
            )
        } catch (e: Throwable) {
            if (e is FirebaseAuthException && e.code in SESSION_EXPIRED_CODES) {
                clearSession()
            }
            AuthResult.Failure(e.toAuthError())
        }
    }

    private fun saveSession(
        response: FirebaseSignInResponse,
        providerId: String,
        fallbackEmail: String? = null,
        fallbackDisplayName: String? = null,
    ): AuthResult.Success {
        val expiresIn = response.expiresIn.toLongOrNull() ?: DEFAULT_EXPIRES_IN_SECONDS
        tokenManager.saveTokens(response.idToken, response.refreshToken, expiresIn)
        val session = response.toSession(
            providerId = providerId,
            fallbackEmail = fallbackEmail,
            fallbackDisplayName = fallbackDisplayName,
            expiresIn = expiresIn,
        )
        currentSession = session
        authState.value = AuthState.Authenticated(session)
        return AuthResult.Success(session)
    }

    private fun saveSession(
        response: FirebaseRefreshResponse,
        providerId: String,
        fallbackEmail: String? = null,
        fallbackDisplayName: String? = null,
    ): AuthResult.Success {
        val expiresIn = response.expiresIn.toLongOrNull() ?: DEFAULT_EXPIRES_IN_SECONDS
        tokenManager.saveTokens(response.idToken, response.refreshToken, expiresIn)
        val session = response.toSession(
            providerId = providerId,
            fallbackEmail = fallbackEmail,
            fallbackDisplayName = fallbackDisplayName,
            expiresIn = expiresIn,
        )
        currentSession = session
        authState.value = AuthState.Authenticated(session)
        return AuthResult.Success(session)
    }

    private suspend fun freshIdToken(): String {
        if (tokenManager.isAccessTokenExpired()) {
            val refreshed = refreshSession()
            if (refreshed !is AuthResult.Success) {
                throw IllegalStateException("No hay una sesion activa de Firebase.")
            }
        }
        return tokenManager.accessToken ?: throw IllegalStateException("No hay una sesion activa de Firebase.")
    }

    private fun restoreSession(): UserSession? {
        val token = tokenManager.accessToken ?: return null
        if (tokenManager.isAccessTokenExpired()) return null
        val userId = FirebaseIdToken.claim(token, "user_id")
            ?: FirebaseIdToken.claim(token, "sub")
            ?: return null
        val email = FirebaseIdToken.claim(token, "email")
        return UserSession(
            userId = userId,
            email = email,
            displayName = FirebaseIdToken.claim(token, "name"),
            isEmailVerified = FirebaseIdToken.claim(token, "email_verified").toBoolean(),
            providerId = PROVIDER_ID,
            accessToken = token,
            refreshToken = tokenManager.refreshToken,
            metadata = mapOf("source" to "firebase-rest"),
        )
    }

    private fun updateCurrentSession(update: (UserSession) -> UserSession) {
        val session = currentSession ?: throw IllegalStateException("No hay una sesion activa de Firebase.")
        val updated = update(session)
        currentSession = updated
        authState.value = AuthState.Authenticated(updated)
    }

    private fun clearSession() {
        tokenManager.clearTokens()
        currentSession = null
        authState.value = AuthState.Unauthenticated
    }

    private fun unsupportedProvider(): AuthResult =
        AuthResult.Failure(AuthError.OperationNotAllowed(DISABLED_PROVIDER_MESSAGE))

    private suspend fun runAuth(block: suspend () -> AuthResult): AuthResult = try {
        block()
    } catch (e: Throwable) {
        AuthResult.Failure(e.toAuthError())
    }

    private fun FirebaseSignInResponse.toSession(
        providerId: String,
        fallbackEmail: String?,
        fallbackDisplayName: String?,
        expiresIn: Long,
    ): UserSession {
        val email = email.ifBlank { FirebaseIdToken.claim(idToken, "email").orEmpty() }
            .ifBlank { fallbackEmail }
        return UserSession(
            userId = localId.ifBlank {
                FirebaseIdToken.claim(idToken, "user_id") ?: FirebaseIdToken.claim(idToken, "sub").orEmpty()
            },
            email = email,
            displayName = displayName.ifBlank { fallbackDisplayName.orEmpty() }.ifBlank {
                email?.substringBefore("@").orEmpty()
            },
            isEmailVerified = FirebaseIdToken.claim(idToken, "email_verified").toBoolean(),
            providerId = providerId,
            accessToken = idToken,
            refreshToken = refreshToken,
            expiresAt = Clock.System.now().toEpochMilliseconds() + expiresIn * 1_000L,
            metadata = mapOf("source" to "firebase-rest"),
        )
    }

    private fun FirebaseRefreshResponse.toSession(
        providerId: String,
        fallbackEmail: String?,
        fallbackDisplayName: String?,
        expiresIn: Long,
    ): UserSession {
        val email = FirebaseIdToken.claim(idToken, "email") ?: fallbackEmail
        return UserSession(
            userId = userId.ifBlank {
                FirebaseIdToken.claim(idToken, "user_id") ?: FirebaseIdToken.claim(idToken, "sub").orEmpty()
            },
            email = email,
            displayName = FirebaseIdToken.claim(idToken, "name") ?: fallbackDisplayName ?: email?.substringBefore("@"),
            isEmailVerified = FirebaseIdToken.claim(idToken, "email_verified").toBoolean(),
            providerId = providerId,
            accessToken = idToken,
            refreshToken = refreshToken,
            expiresAt = Clock.System.now().toEpochMilliseconds() + expiresIn * 1_000L,
            metadata = mapOf("source" to "firebase-rest"),
        )
    }

    private fun Throwable.toAuthError(): AuthError = when (this) {
        is SocialAuthCancelledException -> AuthError.OperationNotAllowed("Inicio de sesion cancelado.")
        is SocialAuthUnavailableException -> AuthError.OperationNotAllowed(message ?: DISABLED_PROVIDER_MESSAGE)
        is FirebaseAuthException -> toAuthError()
        else -> AuthError.NetworkError(message ?: "No se pudo contactar con Firebase.", this)
    }

    private fun FirebaseAuthException.toAuthError(): AuthError = when (code) {
        "EMAIL_EXISTS" -> AuthError.EmailAlreadyInUse("Este email ya esta registrado.")
        "EMAIL_NOT_FOUND" -> AuthError.UserNotFound("No existe una cuenta con este email.")
        "INVALID_PASSWORD", "INVALID_LOGIN_CREDENTIALS" -> AuthError.InvalidCredentials("Email o password incorrectos.")
        "USER_DISABLED" -> AuthError.UserDisabled("Esta cuenta esta deshabilitada.")
        "WEAK_PASSWORD" -> AuthError.WeakPassword("La contrasena es demasiado debil.")
        "INVALID_EMAIL", "MISSING_EMAIL" -> AuthError.InvalidEmail("El email no tiene un formato valido.")
        "TOO_MANY_ATTEMPTS_TRY_LATER" -> AuthError.TooManyRequests("Demasiados intentos. Pruebalo mas tarde.")
        "INVALID_OOB_CODE", "EXPIRED_OOB_CODE" -> AuthError.InvalidResetCode(
            "El codigo de recuperacion no es valido o ha caducado.",
        )
        "TOKEN_EXPIRED", "USER_TOKEN_EXPIRED", "INVALID_REFRESH_TOKEN" -> AuthError.SessionExpired(
            "La sesion ha caducado.",
        )
        "OPERATION_NOT_ALLOWED" -> AuthError.OperationNotAllowed("Este proveedor no esta habilitado en Firebase.")
        else -> AuthError.Unknown(message, this)
    }

    companion object {
        const val PROVIDER_ID = "fledge-firebase"
        private const val PASSWORD_PROVIDER_ID = "password"
        private const val DEFAULT_EXPIRES_IN_SECONDS = 3_600L
        private const val DISABLED_PROVIDER_MESSAGE = "Este proveedor de autenticacion no esta disponible."
        private val SESSION_EXPIRED_CODES = setOf("TOKEN_EXPIRED", "USER_TOKEN_EXPIRED", "INVALID_REFRESH_TOKEN")
    }
}
