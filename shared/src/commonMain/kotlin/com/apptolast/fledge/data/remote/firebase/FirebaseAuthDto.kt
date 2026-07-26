package com.apptolast.fledge.data.remote.firebase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FirebaseSignInRequest(
    val email: String,
    val password: String,
    val returnSecureToken: Boolean,
)

@Serializable
data class FirebaseSignInWithIdpRequest(
    val postBody: String,
    val requestUri: String,
    val returnSecureToken: Boolean,
    val returnIdpCredential: Boolean,
)

@Serializable
data class FirebaseSignInResponse(
    val idToken: String,
    val refreshToken: String,
    val expiresIn: String = "3600",
    val localId: String = "",
    val email: String = "",
    val displayName: String = "",
    val registered: Boolean? = null,
)

@Serializable
data class FirebaseRefreshResponse(
    @SerialName("id_token") val idToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: String = "3600",
    @SerialName("user_id") val userId: String = "",
)

@Serializable
data class FirebasePasswordResetRequest(
    val requestType: String,
    val email: String,
)

@Serializable
data class FirebaseConfirmPasswordResetRequest(
    val oobCode: String,
    val newPassword: String,
)

@Serializable
data class FirebaseSendEmailVerificationRequest(
    val requestType: String,
    val idToken: String,
)

@Serializable
data class FirebaseDeleteAccountRequest(
    val idToken: String,
)

@Serializable
data class FirebaseUpdateProfileRequest(
    val idToken: String,
    val displayName: String,
)

@Serializable
data class FirebaseUpdateEmailRequest(
    val idToken: String,
    val email: String,
    val returnSecureToken: Boolean,
)

@Serializable
data class FirebaseUpdatePasswordRequest(
    val idToken: String,
    val password: String,
    val returnSecureToken: Boolean,
)

@Serializable
data class FirebaseErrorResponse(
    val error: FirebaseErrorBody? = null,
)

@Serializable
data class FirebaseErrorBody(
    val code: Int? = null,
    val message: String = "",
)
