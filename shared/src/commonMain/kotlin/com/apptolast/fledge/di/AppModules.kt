package com.apptolast.fledge.di

import com.apptolast.customlogin.di.loginConfigModule
import com.apptolast.customlogin.di.loginDataModule
import com.apptolast.customlogin.di.loginPresentationModule
import com.apptolast.customlogin.domain.AuthProvider
import com.apptolast.fledge.data.remote.firebase.FirebaseBootstrap
import com.apptolast.fledge.data.remote.firebase.firebaseApplicationId
import com.apptolast.fledge.data.remote.firebase.firebaseEnvironmentOf
import com.apptolast.fledge.data.repository.FirestoreFamilyFoundationRepository
import com.apptolast.fledge.data.repository.FirestoreLedgerRepository
import com.apptolast.fledge.data.repository.FirestoreMoneyFlowRepository
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import com.apptolast.fledge.domain.repository.LedgerRepository
import com.apptolast.fledge.domain.repository.MoneyFlowRepository
import com.apptolast.fledge.domain.service.AllowanceProcessor
import com.apptolast.fledge.domain.service.CashOutProcessor
import com.apptolast.fledge.navigation.FoundationRouteDecider
import com.apptolast.fledge.presentation.foundation.allowance.AllowanceRuleViewModel
import com.apptolast.fledge.presentation.foundation.cashout.CashOutRequestViewModel
import com.apptolast.fledge.presentation.foundation.childhome.ChildHomeViewModel
import com.apptolast.fledge.presentation.foundation.childpin.ChildPinResetViewModel
import com.apptolast.fledge.presentation.foundation.childpin.ChildPinViewModel
import com.apptolast.fledge.presentation.foundation.childsetup.ChildProfileSetupViewModel
import com.apptolast.fledge.presentation.foundation.familysetup.FamilySetupViewModel
import com.apptolast.fledge.presentation.foundation.manualadjustment.ManualAdjustmentViewModel
import com.apptolast.fledge.presentation.foundation.pairing.PairingViewModel
import com.apptolast.fledge.presentation.foundation.parentalgate.ParentalGateViewModel
import com.apptolast.fledge.presentation.foundation.parenthome.ParentHomeViewModel
import com.apptolast.fledge.presentation.foundation.roles.RoleSelectorViewModel
import com.apptolast.fledge.presentation.foundation.virtualconsent.VirtualMoneyConsentViewModel
import com.apptolast.fledge.presentation.initialFledgeLoginConfig
import com.apptolast.fledge.shared.BuildKonfig
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools

val dataModule = module {
    single { FirestoreFamilyFoundationRepository(get(), get()) } bind FamilyFoundationRepository::class
    single { FirestoreLedgerRepository(get(), get()) } bind LedgerRepository::class
    single { FirestoreMoneyFlowRepository(get(), get()) } bind MoneyFlowRepository::class
    single { AllowanceProcessor(get(), get()) }
    single { CashOutProcessor(get(), get()) }
    // Firebase SDK bootstrap (FLE-78). The FirebaseInitializer and the FirestoreProvider come from
    // platformModule, so tests can substitute them without overriding the production graph.
    single {
        firebaseEnvironmentOf(
            apiKey = BuildKonfig.FIREBASE_API_KEY,
            projectId = BuildKonfig.FIREBASE_PROJECT_ID,
            applicationId = firebaseApplicationId,
            gcmSenderId = BuildKonfig.FIREBASE_GCM_SENDER_ID,
            storageBucket = BuildKonfig.FIREBASE_STORAGE_BUCKET,
            databaseId = BuildKonfig.FIRESTORE_DATABASE_ID,
        )
    }
    // Deliberately NOT createdAtStart: koinApplication { } creates eager instances by default in Koin
    // 4.2.x, so an eager bootstrap would touch Firebase just by building the graph. initFledgeKoin runs
    // it explicitly instead.
    single { FirebaseBootstrap(get(), get()) }
}

val presentationModule = module {
    single { FoundationRouteDecider() }
    viewModelOf(::RoleSelectorViewModel)
    viewModelOf(::FamilySetupViewModel)
    viewModelOf(::VirtualMoneyConsentViewModel)
    viewModelOf(::ChildProfileSetupViewModel)
    viewModelOf(::ChildPinViewModel)
    viewModelOf(::ChildPinResetViewModel)
    viewModelOf(::ChildHomeViewModel)
    viewModelOf(::AllowanceRuleViewModel)
    viewModelOf(::CashOutRequestViewModel)
    viewModelOf(::ManualAdjustmentViewModel)
    viewModelOf(::PairingViewModel)
    viewModelOf(::ParentalGateViewModel)
    viewModelOf(::ParentHomeViewModel)
}

expect val platformModule: Module

internal fun fledgeModules(platform: Module, authProvider: AuthProvider? = null): List<Module> = listOf(
    loginConfigModule(initialFledgeLoginConfig()),
    // Auth comes from BaseLogin: loginDataModule() registers its FirebaseAuthGateway and the
    // FirebaseAuthProvider built on top of it. Fledge used to pass its own REST provider here, a
    // stopgap from when the iOS SPM integration had been reverted and there was no native Firebase.
    loginDataModule(authProvider),
    dataModule,
    loginPresentationModule,
    presentationModule,
    platform,
)

fun initFledgeKoin(appDeclaration: KoinAppDeclaration? = null) {
    val modules = fledgeModules(platformModule)

    if (KoinPlatformTools.defaultContext().getOrNull() != null) {
        loadKoinModules(modules)
    } else {
        startKoin {
            appDeclaration?.invoke(this)
            modules(modules)
        }
    }

    // Firebase is bootstrapped here, outside the Koin eager-instance machinery, so that both branches
    // behave the same and building a graph in tests never touches the SDK. FirebaseBootstrap.run() is
    // idempotent and the single is cached, so this stays at exactly one initialization per process.
    KoinPlatformTools.defaultContext().get().get<FirebaseBootstrap>().run()
}
