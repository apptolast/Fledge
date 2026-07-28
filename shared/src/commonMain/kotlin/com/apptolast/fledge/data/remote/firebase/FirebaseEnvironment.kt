package com.apptolast.fledge.data.remote.firebase

/**
 * Effective client configuration used to bootstrap the native Firebase SDK.
 *
 * Every field is a public client value (see FLE-78 spec, "Seguridad"): no server secret belongs here.
 */
data class FirebaseEnvironment(
    val apiKey: String,
    val projectId: String,
    val applicationId: String,
    val gcmSenderId: String,
    val storageBucket: String,
    val databaseId: String,
) {
    /**
     * True when every mandatory field is present, so Firebase can actually be initialized.
     *
     * [storageBucket] is optional: Firebase accepts null there, and Fledge does not use Storage yet.
     */
    fun isComplete(): Boolean = apiKey.isNotBlank() &&
        projectId.isNotBlank() &&
        applicationId.isNotBlank() &&
        gcmSenderId.isNotBlank() &&
        databaseId.isNotBlank()
}

/**
 * Pure factory for the effective environment.
 *
 * Kept parameterized on purpose (instead of reading BuildKonfig inside) so the derivation is
 * deterministic and testable from commonTest.
 */
fun firebaseEnvironmentOf(
    apiKey: String,
    projectId: String,
    applicationId: String,
    gcmSenderId: String,
    storageBucket: String,
    databaseId: String,
): FirebaseEnvironment = FirebaseEnvironment(
    apiKey = apiKey,
    projectId = projectId,
    applicationId = applicationId,
    gcmSenderId = gcmSenderId,
    storageBucket = storageBucket,
    // Used literally: "debug" and "(default)" are both valid Firestore database ids.
    databaseId = databaseId,
)
