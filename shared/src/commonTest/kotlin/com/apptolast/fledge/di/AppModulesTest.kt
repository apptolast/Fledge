package com.apptolast.fledge.di

import com.apptolast.customlogin.domain.AuthProvider
import com.apptolast.customlogin.presentation.screens.register.RegisterViewModel
import com.apptolast.fledge.data.auth.InMemoryLoginAuthProvider
import com.apptolast.fledge.navigation.FoundationRouteDecider
import com.apptolast.fledge.presentation.foundation.roles.RoleSelectorViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class AppModulesTest {

    @Test
    fun `AC-09 presentation module resolves role selector graph`() {
        // Given
        val application = koinApplication {
            modules(dataModule, presentationModule)
        }

        // When / Then
        assertNotNull(application.koin.get<FoundationRouteDecider>())
        assertNotNull(application.koin.get<RoleSelectorViewModel>())
    }

    @Test
    fun `AC-02 BaseLogin register graph resolves without Firebase initialization`() {
        // Given
        val application = koinApplication {
            modules(fledgeModules(module {}))
        }

        // When / Then
        assertEquals(InMemoryLoginAuthProvider.PROVIDER_ID, application.koin.get<AuthProvider>().id)
        assertNotNull(application.koin.get<RegisterViewModel>())
    }
}
