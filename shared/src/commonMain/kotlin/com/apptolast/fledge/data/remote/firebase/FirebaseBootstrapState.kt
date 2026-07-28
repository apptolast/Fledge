package com.apptolast.fledge.data.remote.firebase

/** Outcome of the Firebase bootstrap for the current process. */
sealed interface FirebaseBootstrapState {

    /** True when the Firebase app is available and Firestore can be used. */
    val isReady: Boolean
        get() = this is Initialized || this is AlreadyInitialized

    /** The configuration is incomplete, so initialization was skipped on purpose. */
    data object NotConfigured : FirebaseBootstrapState

    /** A Firebase app already existed in this process, so nothing was initialized again. */
    data object AlreadyInitialized : FirebaseBootstrapState

    /** This bootstrap initialized the Firebase app. */
    data object Initialized : FirebaseBootstrapState

    /** Initialization was attempted and failed. */
    data class Failed(val reason: String) : FirebaseBootstrapState
}
