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
    /** True when every mandatory field is present, so Firebase can actually be initialized. */
    fun isComplete(): Boolean = TODO("FLE-78 T3b: validate mandatory Firebase configuration fields")
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
): FirebaseEnvironment = TODO("FLE-78 T3b: derive the effective Firebase environment")
