package com.apptolast.fledge.di

import com.apptolast.fledge.data.auth.IosSocialAuthClient
import com.apptolast.fledge.data.auth.SocialAuthClient
import com.apptolast.fledge.data.remote.firebase.FirebaseEnvironment
import com.apptolast.fledge.data.remote.firebase.FirebaseInitializer
import com.apptolast.fledge.data.remote.firebase.FirestoreProvider
import com.apptolast.fledge.data.remote.firebase.GitLiveFirestoreProvider
import com.apptolast.fledge.data.remote.firebase.IosFirebaseInitializer
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

actual val platformModule: Module = module {
    single<Settings> { NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults) }
    single<SocialAuthClient> { IosSocialAuthClient() }
    single<FirebaseInitializer> { IosFirebaseInitializer() }
    single<FirestoreProvider> { GitLiveFirestoreProvider(get(), get<FirebaseEnvironment>().databaseId) }
}
