package com.apptolast.fledge.presentation

import com.apptolast.customlogin.di.LoginLibraryConfig
import com.apptolast.fledge.shared.BuildKonfig

actual fun initialFledgeLoginConfig(): LoginLibraryConfig =
    fledgeLoginConfigFor(
        platform = FledgeLoginPlatform.Android,
        googleWebClientId = BuildKonfig.GOOGLE_WEB_CLIENT_ID,
    )
