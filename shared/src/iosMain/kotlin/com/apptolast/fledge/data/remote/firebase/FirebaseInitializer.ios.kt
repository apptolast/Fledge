package com.apptolast.fledge.data.remote.firebase

import com.apptolast.fledge.shared.BuildKonfig
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.apps
import dev.gitlive.firebase.initialize

actual val firebaseApplicationId: String = BuildKonfig.FIREBASE_APP_ID_IOS

/**
 * iOS adapter over `Firebase.initialize`.
 *
 * Uses explicit [FirebaseOptions] so the project needs no `GoogleService-Info.plist`. On iOS
 * `applicationId` and `gcmSenderId` are mandatory: `FIROptions(applicationId, gcmSenderId)` is the only
 * constructor GitLive uses, so an incomplete environment must never reach here (see [FirebaseBootstrap]).
 */
class IosFirebaseInitializer : FirebaseInitializer {

    /** Asks the real SDK so a `FIRApp` configured elsewhere is honoured instead of configured twice. */
    override fun isInitialized(): Boolean = Firebase.apps().isNotEmpty()

    override fun initialize(environment: FirebaseEnvironment) {
        Firebase.initialize(
            context = null,
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
