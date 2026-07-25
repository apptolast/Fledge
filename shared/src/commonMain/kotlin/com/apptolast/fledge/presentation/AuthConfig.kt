package com.apptolast.fledge.presentation

import com.apptolast.customlogin.di.LoginLibraryConfig
import com.apptolast.customlogin.di.PhoneAuthConfig

fun initialFledgeLoginConfig(): LoginLibraryConfig = LoginLibraryConfig(
    googleSignInConfig = null,
    appleSignInConfig = null,
    githubEnabled = false,
    microsoftEnabled = false,
    magicLinkConfig = null,
    phoneEnabled = false,
    phoneAuthConfig = PhoneAuthConfig(enabled = false, defaultCountryCode = "+34"),
    twitterEnabled = false,
    facebookEnabled = false,
)
