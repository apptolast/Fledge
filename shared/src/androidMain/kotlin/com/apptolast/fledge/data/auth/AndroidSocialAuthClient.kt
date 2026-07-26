package com.apptolast.fledge.data.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.apptolast.fledge.shared.BuildKonfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.lang.ref.WeakReference

object SocialAuthActivityHolder {
    private var activityRef: WeakReference<Activity>? = null

    fun attach(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    fun detach(activity: Activity) {
        if (activityRef?.get() === activity) {
            activityRef = null
        }
    }

    fun requireActivity(): Activity = activityRef?.get()
        ?: throw SocialAuthUnavailableException("No hay una pantalla activa para iniciar sesion.")
}

class AndroidSocialAuthClient(
    appContext: Context,
) : SocialAuthClient {
    private val credentialManager = CredentialManager.create(appContext)
    private val webClientId: String = BuildKonfig.GOOGLE_WEB_CLIENT_ID

    override val isGoogleAvailable: Boolean = webClientId.isNotBlank()
    override val isAppleAvailable: Boolean = false

    override suspend fun signInWithGoogle(): SocialSignInResult {
        if (!isGoogleAvailable) {
            throw SocialAuthUnavailableException("Google Sign-In no esta configurado.")
        }
        val option = GetSignInWithGoogleOption.Builder(webClientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val response = try {
            credentialManager.getCredential(context = SocialAuthActivityHolder.requireActivity(), request = request)
        } catch (_: GetCredentialCancellationException) {
            throw SocialAuthCancelledException()
        } catch (e: GetCredentialException) {
            throw SocialAuthUnavailableException(e.message ?: "No se pudo iniciar sesion con Google.")
        }

        val credential = response.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val google = GoogleIdTokenCredential.createFrom(credential.data)
            return SocialSignInResult(
                providerId = "google.com",
                idToken = google.idToken,
                displayName = google.displayName,
            )
        }
        throw SocialAuthUnavailableException("No se obtuvo una credencial de Google valida.")
    }

    override suspend fun signInWithApple(): SocialSignInResult =
        throw SocialAuthUnavailableException("Sign in with Apple solo esta disponible en iOS.")
}
