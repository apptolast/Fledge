package com.apptolast.fledge.data.auth

import com.apptolast.customlogin.domain.AuthProvider
import com.apptolast.customlogin.domain.model.AuthError
import com.apptolast.customlogin.domain.model.AuthResult
import com.apptolast.customlogin.domain.model.AuthState
import com.apptolast.customlogin.domain.model.Credentials
import com.apptolast.customlogin.domain.model.PhoneAuthResult
import com.apptolast.customlogin.domain.model.SignUpData
import com.apptolast.customlogin.domain.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class InMemoryLoginAuthProvider : AuthProvider {

    override val id: String = PROVIDER_ID

    private val authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    private var currentSession: UserSession? = null

    override suspend fun signIn(credentials: Credentials): AuthResult = when (credentials) {
        is Credentials.EmailPassword -> createSession(
            email = credentials.email,
            displayName = credentials.email.substringBefore("@").takeIf { it.isNotBlank() },
        )
        is Credentials.OAuthToken -> unsupportedProvider()
        is Credentials.RefreshToken -> refreshSession()
    }

    override suspend fun signUp(data: SignUpData): AuthResult = createSession(
        email = data.email,
        displayName = data.displayName,
    )

    override suspend fun signOut(): Result<Unit> {
        currentSession = null
        authState.value = AuthState.Unauthenticated
        return Result.success(Unit)
    }

    override suspend fun sendPasswordResetEmail(email: String): AuthResult = AuthResult.PasswordResetSent

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

    override suspend fun sendEmailVerification(): Result<Unit> = requireSession()

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

    private fun createSession(email: String, displayName: String?): AuthResult {
        val normalizedEmail = email.normalizedEmail()
        val session = UserSession(
            userId = "local-${normalizedEmail.toStableId()}",
            email = normalizedEmail,
            displayName = displayName?.takeIf { it.isNotBlank() },
            isEmailVerified = true,
            providerId = PROVIDER_ID,
            accessToken = "local-${normalizedEmail.toStableId()}",
            metadata = mapOf("source" to "mvp-foundation-scaffold"),
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
            Result.failure(IllegalStateException("No hay una sesión local activa."))
        } else {
            Result.success(Unit)
        }

    private fun unsupportedProvider(): AuthResult =
        AuthResult.Failure(AuthError.OperationNotAllowed(DISABLED_PROVIDER_MESSAGE))

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
