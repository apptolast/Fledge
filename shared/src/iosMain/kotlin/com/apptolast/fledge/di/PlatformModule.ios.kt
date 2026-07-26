package com.apptolast.fledge.di

import com.apptolast.fledge.data.auth.IosSocialAuthClient
import com.apptolast.fledge.data.auth.SocialAuthClient
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

actual val platformModule: Module = module {
    single<Settings> { NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults) }
    single<SocialAuthClient> { IosSocialAuthClient() }
}
