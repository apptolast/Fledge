package com.apptolast.fledge.data.auth

import com.apptolast.customlogin.domain.AuthProvider
import com.apptolast.customlogin.domain.model.AuthError
import com.apptolast.customlogin.domain.model.AuthResult
import com.apptolast.customlogin.domain.model.AuthState
import com.apptolast.customlogin.domain.model.Credentials
import com.apptolast.customlogin.domain.model.PhoneAuthResult
import com.apptolast.customlogin.domain.model.SignUpData
import com.apptolast.customlogin.domain.model.UserSession
import com.apptolast.fledge.domain.security.Sha256
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class InMemoryLoginAuthProvider : AuthProvider {

    override val id: String = PROVIDER_ID

    private val authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    private val accountsByEmail = mutableMapOf<String, ParentAccount>()
    private var currentSession: UserSession? = null

    override suspend fun signIn(credentials: Credentials): AuthResult = when (credentials) {
        is Credentials.EmailPassword -> signInWithPassword(credentials)
        is Credentials.OAuthToken -> unsupportedProvider()
        is Credentials.RefreshToken -> refreshSession()
    }

    override suspend fun signUp(data: SignUpData): AuthResult {
        val normalizedEmail = data.email.normalizedEmail()
        if (accountsByEmail.containsKey(normalizedEmail)) {
            return AuthResult.Failure(AuthError.EmailAlreadyInUse())
        }

        val account = ParentAccount(
            userId = "parent-${normalizedEmail.toStableId()}",
            email = normalizedEmail,
            displayName = data.displayName?.takeIf { it.isNotBlank() },
            passwordHash = hashPassword(normalizedEmail, data.password),
            emailVerified = false,
        )
        accountsByEmail[normalizedEmail] = account
        return authenticate(account)
    }

    override suspend fun signOut(): Result<Unit> {
        currentSession = null
        authState.value = AuthState.Unauthenticated
        return Result.success(Unit)
    }

    override suspend fun sendPasswordResetEmail(email: String): AuthResult =
        if (accountsByEmail.containsKey(email.normalizedEmail())) {
            AuthResult.PasswordResetSent
        } else {
            AuthResult.Failure(AuthError.UserNotFound())
        }

    override suspend fun confirmPasswordReset(code: String, newPassword: String): AuthResult =
        AuthResult.PasswordResetSuccess

    override fun observeAuthState(): Flow<AuthState> = authState

    override suspend fun getCurrentSession(): UserSession? = currentSession

    override suspend fun refreshSession(): AuthResult =
        currentSession?.let(AuthResult::Success)
            ?: AuthResult.Failure(AuthError.SessionExpired("No hay una sesión local activa."))

    override suspend fun isSignedIn(): Boolean = currentSession != null

    override suspend fun getIdToken(forceRefresh: Boolean): String? = currentSession?.accessToken

    override suspend fun deleteAccount(): Result<Unit> = signOut()

    override suspend fun updateDisplayName(displayName: String): Result<Unit> =
        updateCurrentSession { it.copy(displayName = displayName) }

    override suspend fun updateEmail(newEmail: String): Result<Unit> =
        updateCurrentSession { it.copy(email = newEmail.normalizedEmail()) }

    override suspend fun updatePassword(newPassword: String): Result<Unit> = requireSession()

    override suspend fun sendEmailVerification(): Result<Unit> {
        val session = currentSession ?: return noActiveSession()
        val email = session.email ?: return noActiveSession()
        val account = accountsByEmail[email] ?: return noActiveSession()
        accountsByEmail[email] = account.copy(emailVerificationSent = true)
        return Result.success(Unit)
    }

    override suspend fun reauthenticate(credentials: Credentials): AuthResult = signIn(credentials)

    override suspend fun sendPhoneOtp(phoneNumber: String): PhoneAuthResult =
        PhoneAuthResult.Failure(AuthError.OperationNotAllowed(DISABLED_PROVIDER_MESSAGE))

    override suspend fun verifyPhoneOtp(verificationId: String, otpCode: String): AuthResult = unsupportedProvider()

    override suspend fun sendMagicLink(
        email: String,
        continueUrl: String,
        iosBundleId: String?,
    ): AuthResult = unsupportedProvider()

    override suspend fun signInWithMagicLink(email: String, link: String): AuthResult = unsupportedProvider()

    private fun signInWithPassword(credentials: Credentials.EmailPassword): AuthResult {
        val normalizedEmail = credentials.email.normalizedEmail()
        val account = accountsByEmail[normalizedEmail]
            ?: return AuthResult.Failure(AuthError.UserNotFound())
        return if (account.passwordHash == hashPassword(normalizedEmail, credentials.password)) {
            authenticate(account)
        } else {
            AuthResult.Failure(AuthError.InvalidCredentials())
        }
    }

    private fun authenticate(account: ParentAccount): AuthResult {
        val session = UserSession(
            userId = account.userId,
            email = account.email,
            displayName = account.displayName,
            isEmailVerified = account.emailVerified,
            providerId = PROVIDER_ID,
            accessToken = "local-${account.userId}",
            metadata = mapOf(
                "source" to "mvp-foundation-scaffold",
                "emailVerification" to if (account.emailVerified) "verified" else "pending",
            ),
        )
        currentSession = session
        authState.value = AuthState.Authenticated(session)
        return AuthResult.Success(session)
    }

    private fun updateCurrentSession(update: (UserSession) -> UserSession): Result<Unit> {
        val updatedSession = currentSession?.let(update)
            ?: return Result.failure(IllegalStateException("No hay una sesión local activa."))
        currentSession = updatedSession
        authState.value = AuthState.Authenticated(updatedSession)
        return Result.success(Unit)
    }

    private fun requireSession(): Result<Unit> =
        if (currentSession == null) {
            noActiveSession()
        } else {
            Result.success(Unit)
        }

    private fun noActiveSession(): Result<Unit> =
        Result.failure(IllegalStateException("No hay una sesión local activa."))

    private fun unsupportedProvider(): AuthResult =
        AuthResult.Failure(AuthError.OperationNotAllowed(DISABLED_PROVIDER_MESSAGE))

    private fun hashPassword(email: String, password: String): String =
        Sha256.hashHex("fledge-parent-password:$email:$password")

    private fun String.normalizedEmail(): String = trim().lowercase()

    private fun String.toStableId(): String =
        map { char ->
            when {
                char in 'a'..'z' -> char
                char in '0'..'9' -> char
                else -> '-'
            }
        }.joinToString(separator = "").trim('-').ifBlank { "user" }

    companion object {
        const val PROVIDER_ID = "fledge-in-memory"
        private const val DISABLED_PROVIDER_MESSAGE =
            "Este proveedor de autenticación todavía no está configurado."
    }
}

private data class ParentAccount(
    val userId: String,
    val email: String,
    val displayName: String?,
    val passwordHash: String,
    val emailVerified: Boolean,
    val emailVerificationSent: Boolean = false,
)
