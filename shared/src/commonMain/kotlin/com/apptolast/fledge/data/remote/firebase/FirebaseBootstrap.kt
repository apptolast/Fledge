package com.apptolast.fledge.data.remote.firebase

/**
 * Pure bootstrap logic: validates the environment, applies idempotency and captures failures.
 *
 * It never touches the Firebase SDK directly; everything goes through [FirebaseInitializer].
 */
class FirebaseBootstrap(private val environment: FirebaseEnvironment, private val initializer: FirebaseInitializer) {

    /** Last known outcome. Stable once [run] has been executed. */
    val state: FirebaseBootstrapState
        get() = TODO("FLE-78 T3b: expose the current bootstrap state")

    /** Runs the bootstrap at most once per instance and returns the resulting state. */
    fun run(): FirebaseBootstrapState = TODO("FLE-78 T3b: validate, initialize once and capture failures")
}
