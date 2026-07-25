package com.apptolast.fledge.di

import com.apptolast.customlogin.di.loginModules
import com.apptolast.fledge.data.repository.InMemoryFamilyFoundationRepository
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import com.apptolast.fledge.navigation.FoundationRouteDecider
import com.apptolast.fledge.presentation.initialFledgeLoginConfig
import com.apptolast.fledge.presentation.foundation.childpin.ChildPinViewModel
import com.apptolast.fledge.presentation.foundation.familysetup.FamilySetupViewModel
import com.apptolast.fledge.presentation.foundation.pairing.PairingViewModel
import com.apptolast.fledge.presentation.foundation.parentalgate.ParentalGateViewModel
import com.apptolast.fledge.presentation.foundation.parenthome.ParentHomeViewModel
import com.apptolast.fledge.presentation.foundation.roles.RoleSelectorViewModel
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools

val dataModule = module {
    single { InMemoryFamilyFoundationRepository() } bind FamilyFoundationRepository::class
}

val presentationModule = module {
    single { FoundationRouteDecider() }
    viewModelOf(::RoleSelectorViewModel)
    viewModelOf(::FamilySetupViewModel)
    viewModelOf(::ChildPinViewModel)
    viewModelOf(::PairingViewModel)
    viewModelOf(::ParentalGateViewModel)
    viewModelOf(::ParentHomeViewModel)
}

expect val platformModule: Module

fun initFledgeKoin(appDeclaration: KoinAppDeclaration? = null) {
    val modules = loginModules(initialFledgeLoginConfig()) + listOf(
        dataModule,
        presentationModule,
        platformModule,
    )

    if (KoinPlatformTools.defaultContext().getOrNull() != null) {
        loadKoinModules(modules)
    } else {
        startKoin {
            appDeclaration?.invoke(this)
            modules(modules)
        }
    }
}
