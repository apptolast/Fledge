package com.apptolast.fledge.di

import com.apptolast.customlogin.domain.AuthProvider
import com.apptolast.customlogin.presentation.screens.login.LoginViewModel
import com.apptolast.customlogin.presentation.screens.register.RegisterViewModel
import com.apptolast.fledge.data.auth.FledgeFirebaseAuthProvider
import com.apptolast.fledge.data.auth.SocialAuthClient
import com.apptolast.fledge.data.auth.SocialAuthUnavailableException
import com.apptolast.fledge.data.auth.SocialSignInResult
import com.apptolast.fledge.domain.repository.LedgerRepository
import com.apptolast.fledge.domain.repository.MoneyFlowRepository
import com.apptolast.fledge.domain.service.AllowanceProcessor
import com.apptolast.fledge.domain.service.CashOutProcessor
import com.apptolast.fledge.navigation.FoundationRouteDecider
import com.apptolast.fledge.presentation.foundation.roles.RoleSelectorViewModel
import com.apptolast.fledge.testing.TestSettings
import com.russhwolf.settings.Settings
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
        assertNotNull(application.koin.get<LedgerRepository>())
        assertNotNull(application.koin.get<MoneyFlowRepository>())
        assertNotNull(application.koin.get<AllowanceProcessor>())
        assertNotNull(application.koin.get<CashOutProcessor>())
    }

    @Test
    fun `FLE-8 BaseLogin graph resolves with the real Firebase provider`() {
        // Given
        val application = koinApplication {
            modules(
                fledgeModules(
                    module {
                        single<Settings> { TestSettings() }
                        single<SocialAuthClient> { FakeSocialAuthClient() }
                    }
                )
            )
        }

        // When / Then
        assertEquals(FledgeFirebaseAuthProvider.PROVIDER_ID, application.koin.get<AuthProvider>().id)
        assertNotNull(application.koin.get<LoginViewModel>())
        assertNotNull(application.koin.get<RegisterViewModel>())
    }
}

private class FakeSocialAuthClient : SocialAuthClient {
    override val isGoogleAvailable: Boolean = false
    override val isAppleAvailable: Boolean = false

    override suspend fun signInWithGoogle(): SocialSignInResult =
        throw SocialAuthUnavailableException("Google no esta disponible.")

    override suspend fun signInWithApple(): SocialSignInResult =
        throw SocialAuthUnavailableException("Apple no esta disponible.")
}
