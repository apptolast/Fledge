package com.apptolast.fledge.di

import com.apptolast.fledge.navigation.FoundationRouteDecider
import com.apptolast.fledge.presentation.foundation.roles.RoleSelectorViewModel
import kotlin.test.Test
import kotlin.test.assertNotNull
import org.koin.dsl.koinApplication

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
}
