package com.apptolast.fledge.data.remote.firebase

/**
 * Platform port over the native Firebase SDK initialization.
 *
 * No GitLive type crosses this boundary, so commonTest can substitute a hand-written fake.
 */
interface FirebaseInitializer {

    /** True when a Firebase app already exists in this process. */
    fun isInitialized(): Boolean

    /** Initializes the default Firebase app with explicit options. */
    fun initialize(environment: FirebaseEnvironment)
}
