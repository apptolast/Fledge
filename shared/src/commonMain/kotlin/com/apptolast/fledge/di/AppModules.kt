package com.apptolast.fledge.di

import com.apptolast.customlogin.data.AuthRepositoryImpl
import com.apptolast.customlogin.di.loginConfigModule
import com.apptolast.customlogin.di.loginPresentationModule
import com.apptolast.customlogin.domain.AuthProvider
import com.apptolast.customlogin.domain.AuthRepository
import com.apptolast.fledge.data.auth.FledgeFirebaseAuthProvider
import com.apptolast.fledge.data.auth.TokenManager
import com.apptolast.fledge.data.remote.firebase.FirebaseAuthService
import com.apptolast.fledge.data.remote.firebase.createFirebaseAuthHttpClient
import com.apptolast.fledge.data.repository.InMemoryFamilyFoundationRepository
import com.apptolast.fledge.data.repository.InMemoryLedgerRepository
import com.apptolast.fledge.data.repository.InMemoryMoneyFlowRepository
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
import kotlinx.serialization.json.Json
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools

val dataModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
    single { TokenManager(get()) }
    single { createFirebaseAuthHttpClient(get()) }
    single { FirebaseAuthService(get(), get()) }
    single<AuthProvider> { FledgeFirebaseAuthProvider(get(), get(), get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single { InMemoryFamilyFoundationRepository() } bind FamilyFoundationRepository::class
    single { InMemoryLedgerRepository() } bind LedgerRepository::class
    single { InMemoryMoneyFlowRepository() } bind MoneyFlowRepository::class
    single { AllowanceProcessor(get(), get()) }
    single { CashOutProcessor(get(), get()) }
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

internal fun fledgeModules(platform: Module): List<Module> = listOf(
    loginConfigModule(initialFledgeLoginConfig()),
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
}
