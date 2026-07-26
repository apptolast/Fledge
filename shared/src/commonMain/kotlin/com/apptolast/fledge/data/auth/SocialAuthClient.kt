package com.apptolast.fledge.data.auth

data class SocialSignInResult(
    val providerId: String,
    val idToken: String,
    val rawNonce: String? = null,
    val displayName: String? = null,
)

interface SocialAuthClient {
    val isGoogleAvailable: Boolean
    val isAppleAvailable: Boolean

    suspend fun signInWithGoogle(): SocialSignInResult
    suspend fun signInWithApple(): SocialSignInResult
}

class SocialAuthUnavailableException(message: String) : IllegalStateException(message)

class SocialAuthCancelledException : Exception("Social sign-in cancelled by the user")
