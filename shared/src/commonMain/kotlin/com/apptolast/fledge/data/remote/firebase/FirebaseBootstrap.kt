package com.apptolast.fledge.data.remote.firebase

/**
 * Pure bootstrap logic: validates the environment, applies idempotency and captures failures.
 *
 * It never touches the Firebase SDK directly; everything goes through [FirebaseInitializer].
 */
class FirebaseBootstrap(private val environment: FirebaseEnvironment, private val initializer: FirebaseInitializer) {

    private var hasRun: Boolean = false

    // Before the first run nothing has been configured yet, which is also the outcome of an environment
    // without local.properties. Either way isReady is false, so Firestore stays unavailable.
    private var currentState: FirebaseBootstrapState = FirebaseBootstrapState.NotConfigured

    /** Last known outcome. Stable once [run] has been executed. */
    val state: FirebaseBootstrapState
        get() = currentState

    /** Runs the bootstrap at most once per instance and returns the resulting state. */
    fun run(): FirebaseBootstrapState {
        if (hasRun) return currentState
        hasRun = true
        currentState = bootstrap()
        return currentState
    }

    private fun bootstrap(): FirebaseBootstrapState {
        if (!environment.isComplete()) return FirebaseBootstrapState.NotConfigured
        return runCatching {
            if (initializer.isInitialized()) {
                FirebaseBootstrapState.AlreadyInitialized
            } else {
                initializer.initialize(environment)
                FirebaseBootstrapState.Initialized
            }
        }.getOrElse { failure ->
            FirebaseBootstrapState.Failed(failure.message ?: FALLBACK_FAILURE_REASON)
        }
    }

    private companion object {
        const val FALLBACK_FAILURE_REASON = "Firebase initialization failed"
    }
}
