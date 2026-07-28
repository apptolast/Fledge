package com.apptolast.fledge.di

import com.apptolast.customlogin.domain.AuthProvider
import com.apptolast.customlogin.presentation.screens.login.LoginViewModel
import com.apptolast.customlogin.presentation.screens.register.RegisterViewModel
import com.apptolast.fledge.data.auth.FledgeFirebaseAuthProvider
import com.apptolast.fledge.data.auth.SocialAuthClient
import com.apptolast.fledge.data.auth.SocialAuthUnavailableException
import com.apptolast.fledge.data.auth.SocialSignInResult
import com.apptolast.fledge.data.remote.firebase.FakeFirebaseInitializer
import com.apptolast.fledge.data.remote.firebase.FirebaseBootstrap
import com.apptolast.fledge.data.remote.firebase.FirebaseEnvironment
import com.apptolast.fledge.data.remote.firebase.FirebaseInitializer
import com.apptolast.fledge.data.remote.firebase.FirestoreProvider
import com.apptolast.fledge.data.repository.InMemoryFamilyFoundationRepository
import com.apptolast.fledge.data.repository.InMemoryLedgerRepository
import com.apptolast.fledge.data.repository.InMemoryMoneyFlowRepository
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
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
import kotlin.test.assertIs
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
                    },
                ),
            )
        }

        // When / Then
        assertEquals(FledgeFirebaseAuthProvider.PROVIDER_ID, application.koin.get<AuthProvider>().id)
        assertNotNull(application.koin.get<LoginViewModel>())
        assertNotNull(application.koin.get<RegisterViewModel>())
    }

    @Test
    fun `FLE-78 koin graph resolves firestore provider without initializing firebase`() {
        // Given the production graph with a test platform module providing Firebase fakes
        val initializer = FakeFirebaseInitializer(initialized = false)
        val application = koinApplication {
            modules(
                fledgeModules(
                    module {
                        single<Settings> { TestSettings() }
                        single<SocialAuthClient> { FakeSocialAuthClient() }
                        single<FirebaseInitializer> { initializer }
                        single<FirestoreProvider> {
                            FakeFirestoreProvider(databaseId = get<FirebaseEnvironment>().databaseId)
                        }
                    },
                ),
            )
        }

        // When
        val environment = application.koin.get<FirebaseEnvironment>()
        val provider = application.koin.get<FirestoreProvider>()

        // Then
        assertNotNull(provider)
        assertEquals(environment.databaseId, provider.databaseId)
        // koinApplication does not create eager instances, so building the graph must not touch Firebase.
        assertEquals(0, initializer.initializeCalls)
        assertNotNull(application.koin.get<FirebaseBootstrap>())
    }

    @Test
    fun `FLE-78 repositories remain in memory after firestore integration`() {
        // Given the production data module alone: it declares no FirestoreProvider at all
        val application = koinApplication {
            modules(dataModule)
        }

        // When
        val familyFoundation = application.koin.get<FamilyFoundationRepository>()
        val ledger = application.koin.get<LedgerRepository>()
        val moneyFlow = application.koin.get<MoneyFlowRepository>()

        // Then every repository is still in memory, and none of them needed a FirestoreProvider to resolve
        assertIs<InMemoryFamilyFoundationRepository>(familyFoundation)
        assertIs<InMemoryLedgerRepository>(ledger)
        assertIs<InMemoryMoneyFlowRepository>(moneyFlow)
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

private class FakeFirestoreProvider(override val databaseId: String, override val isAvailable: Boolean = false) :
    FirestoreProvider
