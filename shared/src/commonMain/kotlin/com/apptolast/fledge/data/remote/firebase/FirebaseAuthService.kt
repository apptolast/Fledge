package com.apptolast.fledge.data.remote.firebase

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

class FirebaseAuthService(private val client: HttpClient, private val json: Json) {
    suspend fun signInWithPassword(email: String, password: String): FirebaseSignInResponse =
        client.post("${FirebaseConfig.IDENTITY_TOOLKIT}/accounts:signInWithPassword") {
            url { parameters.append("key", FirebaseConfig.apiKey) }
            contentType(ContentType.Application.Json)
            setBody(FirebaseSignInRequest(email, password, returnSecureToken = true))
        }.decodeOrThrow()

    suspend fun signUp(email: String, password: String): FirebaseSignInResponse =
        client.post("${FirebaseConfig.IDENTITY_TOOLKIT}/accounts:signUp") {
            url { parameters.append("key", FirebaseConfig.apiKey) }
            contentType(ContentType.Application.Json)
            setBody(FirebaseSignInRequest(email, password, returnSecureToken = true))
        }.decodeOrThrow()

    suspend fun signInWithIdp(providerId: String, idToken: String, rawNonce: String? = null): FirebaseSignInResponse {
        val postBody = buildString {
            append("id_token=").append(idToken)
            append("&providerId=").append(providerId)
            if (!rawNonce.isNullOrBlank()) {
                append("&nonce=").append(rawNonce)
            }
        }
        return client.post("${FirebaseConfig.IDENTITY_TOOLKIT}/accounts:signInWithIdp") {
            url { parameters.append("key", FirebaseConfig.apiKey) }
            contentType(ContentType.Application.Json)
            setBody(
                FirebaseSignInWithIdpRequest(
                    postBody = postBody,
                    requestUri = "http://localhost",
                    returnSecureToken = true,
                    returnIdpCredential = true,
                ),
            )
        }.decodeOrThrow()
    }

    suspend fun sendPasswordResetEmail(email: String) {
        client.post("${FirebaseConfig.IDENTITY_TOOLKIT}/accounts:sendOobCode") {
            url { parameters.append("key", FirebaseConfig.apiKey) }
            contentType(ContentType.Application.Json)
            setBody(FirebasePasswordResetRequest(requestType = REQUEST_PASSWORD_RESET, email = email))
        }.decodeUnitOrThrow()
    }

    suspend fun confirmPasswordReset(code: String, newPassword: String) {
        client.post("${FirebaseConfig.IDENTITY_TOOLKIT}/accounts:resetPassword") {
            url { parameters.append("key", FirebaseConfig.apiKey) }
            contentType(ContentType.Application.Json)
            setBody(FirebaseConfirmPasswordResetRequest(oobCode = code, newPassword = newPassword))
        }.decodeUnitOrThrow()
    }

    suspend fun sendEmailVerification(idToken: String) {
        client.post("${FirebaseConfig.IDENTITY_TOOLKIT}/accounts:sendOobCode") {
            url { parameters.append("key", FirebaseConfig.apiKey) }
            contentType(ContentType.Application.Json)
            setBody(FirebaseSendEmailVerificationRequest(requestType = REQUEST_VERIFY_EMAIL, idToken = idToken))
        }.decodeUnitOrThrow()
    }

    suspend fun updateProfile(idToken: String, displayName: String) {
        client.post("${FirebaseConfig.IDENTITY_TOOLKIT}/accounts:update") {
            url { parameters.append("key", FirebaseConfig.apiKey) }
            contentType(ContentType.Application.Json)
            setBody(FirebaseUpdateProfileRequest(idToken = idToken, displayName = displayName))
        }.decodeUnitOrThrow()
    }

    suspend fun updateEmail(idToken: String, email: String): FirebaseSignInResponse =
        client.post("${FirebaseConfig.IDENTITY_TOOLKIT}/accounts:update") {
            url { parameters.append("key", FirebaseConfig.apiKey) }
            contentType(ContentType.Application.Json)
            setBody(FirebaseUpdateEmailRequest(idToken = idToken, email = email, returnSecureToken = true))
        }.decodeOrThrow()

    suspend fun updatePassword(idToken: String, password: String): FirebaseSignInResponse =
        client.post("${FirebaseConfig.IDENTITY_TOOLKIT}/accounts:update") {
            url { parameters.append("key", FirebaseConfig.apiKey) }
            contentType(ContentType.Application.Json)
            setBody(FirebaseUpdatePasswordRequest(idToken = idToken, password = password, returnSecureToken = true))
        }.decodeOrThrow()

    suspend fun deleteAccount(idToken: String) {
        client.post("${FirebaseConfig.IDENTITY_TOOLKIT}/accounts:delete") {
            url { parameters.append("key", FirebaseConfig.apiKey) }
            contentType(ContentType.Application.Json)
            setBody(FirebaseDeleteAccountRequest(idToken))
        }.decodeUnitOrThrow()
    }

    suspend fun refreshIdToken(refreshToken: String): FirebaseRefreshResponse = client.submitForm(
        url = "${FirebaseConfig.SECURE_TOKEN}/token",
        formParameters = Parameters.build {
            append("grant_type", "refresh_token")
            append("refresh_token", refreshToken)
        },
    ) {
        url { parameters.append("key", FirebaseConfig.apiKey) }
    }.decodeOrThrow()

    private suspend inline fun <reified T> HttpResponse.decodeOrThrow(): T {
        if (status.isSuccess()) return body()
        throw firebaseException(bodyAsText())
    }

    private suspend fun HttpResponse.decodeUnitOrThrow() {
        if (!status.isSuccess()) {
            throw firebaseException(bodyAsText())
        }
    }

    private fun firebaseException(body: String): FirebaseAuthException {
        val error = runCatching {
            json.decodeFromString<FirebaseErrorResponse>(body).error
        }.getOrNull()
        val message = error?.message?.takeIf { it.isNotBlank() } ?: body
        return FirebaseAuthException(code = message.substringBefore(" : "), message = message)
    }

    private companion object {
        const val REQUEST_PASSWORD_RESET = "PASSWORD_RESET"
        const val REQUEST_VERIFY_EMAIL = "VERIFY_EMAIL"
    }
}

class FirebaseAuthException(val code: String, override val message: String) : Exception(message)
