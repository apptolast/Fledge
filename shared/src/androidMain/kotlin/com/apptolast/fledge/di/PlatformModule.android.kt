package com.apptolast.fledge.di

import com.apptolast.fledge.data.remote.firebase.AndroidFirebaseInitializer
import com.apptolast.fledge.data.remote.firebase.FirebaseEnvironment
import com.apptolast.fledge.data.remote.firebase.FirebaseInitializer
import com.apptolast.fledge.data.remote.firebase.FirestoreProvider
import com.apptolast.fledge.data.remote.firebase.GitLiveFirestoreProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<FirebaseInitializer> { AndroidFirebaseInitializer(androidContext()) }
    single<FirestoreProvider> { GitLiveFirestoreProvider(get(), get<FirebaseEnvironment>().databaseId) }
}
