package com.apptolast.fledge.data.auth

import com.apptolast.fledge.data.auth.IosAppleAuthBridge.signInHandler
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

typealias IosAppleCompletion = (String?) -> Unit

object IosAppleAuthBridge {
    var signInHandler: ((IosAppleCompletion) -> Unit)? = null
}

class IosSocialAuthClient : SocialAuthClient {
    override val isGoogleAvailable: Boolean = false
    override val isAppleAvailable: Boolean get() = signInHandler != null

    override suspend fun signInWithGoogle(): SocialSignInResult =
        throw SocialAuthUnavailableException("Google Sign-In solo esta disponible en Android.")

    override suspend fun signInWithApple(): SocialSignInResult {
        val handler = signInHandler
            ?: throw SocialAuthUnavailableException("Sign in with Apple no esta disponible.")
        val payload = suspendCancellableCoroutine<String?> { continuation ->
            handler { result -> continuation.resume(result) }
        } ?: throw SocialAuthCancelledException()

        val parts = payload.split(SEPARATOR)
        val idToken = parts.getOrNull(0).orEmpty()
        val rawNonce = parts.getOrNull(1).orEmpty()
        val displayName = parts.getOrNull(2)?.takeIf { it.isNotBlank() }
        if (idToken.isBlank() || rawNonce.isBlank()) {
            throw SocialAuthUnavailableException("Apple no devolvio una credencial valida.")
        }
        return SocialSignInResult(
            providerId = "apple.com",
            idToken = idToken,
            rawNonce = rawNonce,
            displayName = displayName,
        )
    }

    private companion object {
        const val SEPARATOR = "|||"
    }
}
