package com.apptolast.fledge.data.remote.firebase

import android.content.Context
import com.apptolast.fledge.shared.BuildKonfig
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.apps
import dev.gitlive.firebase.initialize

actual val firebaseApplicationId: String = BuildKonfig.FIREBASE_APP_ID_ANDROID

/**
 * Android adapter over `Firebase.initialize`.
 *
 * Uses explicit [FirebaseOptions] so the project needs no `google-services.json` and no
 * `com.google.gms.google-services` plugin.
 */
class AndroidFirebaseInitializer(context: Context) : FirebaseInitializer {

    // Koin registers the Activity as the android context; Firebase outlives it, so keep the app context.
    private val context: Context = context.applicationContext

    /** Asks the real SDK, so idempotency also covers apps created by `FirebaseInitProvider`. */
    override fun isInitialized(): Boolean = Firebase.apps(context).isNotEmpty()

    override fun initialize(environment: FirebaseEnvironment) {
        Firebase.initialize(
            context = context,
            options = FirebaseOptions(
                applicationId = environment.applicationId,
                apiKey = environment.apiKey,
                projectId = environment.projectId,
                gcmSenderId = environment.gcmSenderId,
                storageBucket = environment.storageBucket.takeIf { it.isNotBlank() },
            ),
        )
    }
}
