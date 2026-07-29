package com.apptolast.fledge.data.auth

import android.app.Activity
import com.apptolast.customlogin.CustomLoginAndroid

/**
 * Android-side wiring that BaseLogin's social layer needs.
 *
 * Exists so that `androidApp` does not have to depend on BaseLogin directly: `:shared` declares the
 * library as `implementation`, and the entry point has no business knowing which auth library sits
 * underneath.
 *
 * Both calls are mandatory. Without [attach], Google Sign-In crashes on an uninitialised
 * `lateinit var appContext` the first time the user taps the button — it is Credential Manager that
 * needs the application context.
 */
object FledgeAndroidAuth {

    /** Call from `Activity.onCreate`. */
    fun attach(activity: Activity) {
        CustomLoginAndroid.initialize(activity.applicationContext, activity)
    }

    /** Call from `Activity.onDestroy`, so the library does not hold on to a destroyed activity. */
    fun detach(activity: Activity) {
        CustomLoginAndroid.detachActivity(activity)
    }
}
