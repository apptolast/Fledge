package com.apptolast.fledge.presentation

import com.apptolast.customlogin.di.LoginLibraryConfig

actual fun initialFledgeLoginConfig(): LoginLibraryConfig = fledgeLoginConfigFor(platform = FledgeLoginPlatform.Ios)
