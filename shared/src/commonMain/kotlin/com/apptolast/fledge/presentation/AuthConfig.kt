package com.apptolast.fledge.presentation

import com.apptolast.customlogin.config.AppleSignInConfig
import com.apptolast.customlogin.config.GoogleSignInConfig
import com.apptolast.customlogin.di.LoginLibraryConfig
import com.apptolast.customlogin.di.PhoneAuthConfig

enum class FledgeLoginPlatform {
    Android,
    Ios,
    Unsupported,
}

expect fun initialFledgeLoginConfig(): LoginLibraryConfig

fun fledgeLoginConfigFor(
    platform: FledgeLoginPlatform,
    googleWebClientId: String = "",
): LoginLibraryConfig = LoginLibraryConfig(
    googleSignInConfig = if (platform == FledgeLoginPlatform.Android && googleWebClientId.isNotBlank()) {
        GoogleSignInConfig(webClientId = googleWebClientId)
    } else {
        null
    },
    appleSignInConfig = if (platform == FledgeLoginPlatform.Ios) AppleSignInConfig() else null,
    githubEnabled = false,
    microsoftEnabled = false,
    magicLinkConfig = null,
    phoneEnabled = false,
    phoneAuthConfig = PhoneAuthConfig(enabled = false, defaultCountryCode = "+34"),
    twitterEnabled = false,
    facebookEnabled = false,
)
