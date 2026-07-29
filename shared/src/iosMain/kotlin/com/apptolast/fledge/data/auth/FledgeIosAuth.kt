package com.apptolast.fledge.data.auth

import com.apptolast.customlogin.provider.AppleSignInProviderIOS

/**
 * iOS-side wiring that BaseLogin's Apple provider needs.
 *
 * Symmetric to `FledgeAndroidAuth`, and for the same reason: `:shared` declares BaseLogin as
 * `implementation`, so its symbols are not exported into the `Shared` framework and Swift cannot
 * reach `AppleSignInProviderIOS` directly. Exporting the whole library would drag its entire public
 * API into the framework just to set one handler.
 *
 * The Swift side owns the `ASAuthorizationController` flow; this only forwards the result.
 */
object FledgeIosAuth {

    /**
     * Registers the Swift closure that performs Apple Sign-In.
     *
     * The closure must call [completion] with BaseLogin's packed string, or with `null` if the user
     * cancelled:
     *
     * ```
     * idToken|||rawNonce|||<rawNonce>|||displayName|||<name>
     * ```
     *
     * The `displayName` segment is optional and must be appended **only** when Apple actually
     * returns a name — which happens on the very first authorisation of each user and never again.
     */
    fun registerAppleSignInHandler(handler: (completion: (String?) -> Unit) -> Unit) {
        AppleSignInProviderIOS.signInHandler = { _, completion -> handler(completion) }
    }
}
