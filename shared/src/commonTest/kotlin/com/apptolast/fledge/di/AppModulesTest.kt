package com.apptolast.fledge.di

import com.apptolast.customlogin.data.FirebaseAuthProvider
import com.apptolast.customlogin.domain.AuthProvider
import com.apptolast.customlogin.domain.model.AuthError
import com.apptolast.customlogin.domain.model.AuthResult
import com.apptolast.customlogin.domain.model.AuthState
import com.apptolast.customlogin.domain.model.Credentials
import com.apptolast.customlogin.domain.model.PhoneAuthResult
import com.apptolast.customlogin.domain.model.SignUpData
import com.apptolast.customlogin.domain.model.UserSession
import com.apptolast.customlogin.presentation.screens.login.LoginViewModel
import com.apptolast.customlogin.presentation.screens.register.RegisterViewModel
import com.apptolast.fledge.data.remote.firebase.FakeFirebaseInitializer
import com.apptolast.fledge.data.remote.firebase.FirebaseBootstrap
import com.apptolast.fledge.data.remote.firebase.FirebaseEnvironment
import com.apptolast.fledge.data.remote.firebase.FirebaseInitializer
import com.apptolast.fledge.data.remote.firebase.FirestoreProvider
import com.apptolast.fledge.data.repository.FirestoreAccountDeletionRepository
import com.apptolast.fledge.data.repository.FirestoreFamilyFoundationRepository
import com.apptolast.fledge.data.repository.FirestoreLedgerRepository
import com.apptolast.fledge.data.repository.FirestoreMoneyFlowRepository
import com.apptolast.fledge.data.repository.FirestorePushRegistrationRepository
import com.apptolast.fledge.data.repository.FirestoreSavingsGoalRepository
import com.apptolast.fledge.data.repository.FirestoreTaskAssignmentRepository
import com.apptolast.fledge.data.repository.FirestoreTaskInstanceRepository
import com.apptolast.fledge.data.repository.FirestoreTaskTemplateRepository
import com.apptolast.fledge.domain.repository.AccountDeletionRepository
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import com.apptolast.fledge.domain.repository.LedgerRepository
import com.apptolast.fledge.domain.repository.MoneyFlowRepository
import com.apptolast.fledge.domain.repository.PushRegistrationRepository
import com.apptolast.fledge.domain.repository.SavingsGoalRepository
import com.apptolast.fledge.domain.repository.TaskAssignmentRepository
import com.apptolast.fledge.domain.repository.TaskInstanceRepository
import com.apptolast.fledge.domain.repository.TaskTemplateRepository
import com.apptolast.fledge.domain.service.AllowanceProcessor
import com.apptolast.fledge.domain.service.CashOutProcessor
import com.apptolast.fledge.domain.service.SavingsGoalDepositProcessor
import com.apptolast.fledge.domain.service.SavingsGoalWithdrawalProcessor
import com.apptolast.fledge.navigation.FoundationRouteDecider
import com.apptolast.fledge.presentation.foundation.accountdeletion.AccountDeletionViewModel
import com.apptolast.fledge.presentation.foundation.interest.ParentInterestViewModel
import com.apptolast.fledge.presentation.foundation.roles.RoleSelectorViewModel
import com.apptolast.fledge.presentation.foundation.savingsgoal.SavingsGoalDepositViewModel
import com.apptolast.fledge.presentation.foundation.savingsgoal.SavingsGoalSetupViewModel
import com.apptolast.fledge.presentation.foundation.savingsgoal.SavingsGoalWithdrawalViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class AppModulesTest {

    @Test
    fun `AC-09 presentation module resolves role selector graph`() {
        // Given
        val application = koinApplication {
            modules(fledgeModules(testPlatformModule(), FakeAuthProvider()))
        }

        // When / Then
        assertNotNull(application.koin.get<FoundationRouteDecider>())
        assertNotNull(application.koin.get<AccountDeletionRepository>())
        assertNotNull(application.koin.get<AccountDeletionViewModel>())
        assertNotNull(application.koin.get<RoleSelectorViewModel>())
        assertNotNull(application.koin.get<LedgerRepository>())
        assertNotNull(application.koin.get<MoneyFlowRepository>())
        assertNotNull(application.koin.get<PushRegistrationRepository>())
        assertNotNull(application.koin.get<SavingsGoalRepository>())
        assertNotNull(application.koin.get<TaskAssignmentRepository>())
        assertNotNull(application.koin.get<TaskInstanceRepository>())
        assertNotNull(application.koin.get<TaskTemplateRepository>())
        assertNotNull(application.koin.get<AllowanceProcessor>())
        assertNotNull(application.koin.get<CashOutProcessor>())
        assertNotNull(application.koin.get<SavingsGoalDepositProcessor>())
        assertNotNull(application.koin.get<SavingsGoalWithdrawalProcessor>())
        assertNotNull(application.koin.get<ParentInterestViewModel>())
        assertNotNull(application.koin.get<SavingsGoalSetupViewModel>())
        assertNotNull(application.koin.get<SavingsGoalDepositViewModel>())
        assertNotNull(application.koin.get<SavingsGoalWithdrawalViewModel>())
    }

    @Test
    fun `FLE-88 BaseLogin graph resolves its own Firebase provider`() {
        // Given the production graph: auth now comes from the library, not from a Fledge REST provider
        val application = koinApplication {
            modules(fledgeModules(module { }))
        }

        // When / Then
        assertEquals(FirebaseAuthProvider.PROVIDER_ID, application.koin.get<AuthProvider>().id)
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
                    testPlatformModule(initializer),
                    FakeAuthProvider(),
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
    fun `FLE-77 production repositories use firestore after sdk integration`() {
        // Given the production graph with test platform bindings
        val application = koinApplication {
            modules(fledgeModules(testPlatformModule(), FakeAuthProvider()))
        }

        // When
        val familyFoundation = application.koin.get<FamilyFoundationRepository>()
        val accountDeletion = application.koin.get<AccountDeletionRepository>()
        val ledger = application.koin.get<LedgerRepository>()
        val moneyFlow = application.koin.get<MoneyFlowRepository>()
        val pushRegistrations = application.koin.get<PushRegistrationRepository>()
        val savingsGoals = application.koin.get<SavingsGoalRepository>()
        val taskAssignments = application.koin.get<TaskAssignmentRepository>()
        val taskInstances = application.koin.get<TaskInstanceRepository>()
        val taskTemplates = application.koin.get<TaskTemplateRepository>()

        // Then production bindings now point to Firestore-backed repositories
        assertIs<FirestoreAccountDeletionRepository>(accountDeletion)
        assertIs<FirestoreFamilyFoundationRepository>(familyFoundation)
        assertIs<FirestoreLedgerRepository>(ledger)
        assertIs<FirestoreMoneyFlowRepository>(moneyFlow)
        assertIs<FirestorePushRegistrationRepository>(pushRegistrations)
        assertIs<FirestoreSavingsGoalRepository>(savingsGoals)
        assertIs<FirestoreTaskAssignmentRepository>(taskAssignments)
        assertIs<FirestoreTaskInstanceRepository>(taskInstances)
        assertIs<FirestoreTaskTemplateRepository>(taskTemplates)
    }
}

private fun testPlatformModule(initializer: FirebaseInitializer = FakeFirebaseInitializer(initialized = false)) =
    module {
        single<FirebaseInitializer> { initializer }
        single<FirestoreProvider> {
            FakeFirestoreProvider(databaseId = get<FirebaseEnvironment>().databaseId)
        }
    }

private class FakeFirestoreProvider(override val databaseId: String, override val isAvailable: Boolean = false) :
    FirestoreProvider

private class FakeAuthProvider : AuthProvider {
    override val id: String = "fake-auth"

    private val failure = AuthError.Unknown("Fake auth provider does not sign users in.")

    override suspend fun signIn(credentials: Credentials): AuthResult = AuthResult.Failure(failure)
    override suspend fun signUp(data: SignUpData): AuthResult = AuthResult.Failure(failure)
    override suspend fun signOut(): Result<Unit> = Result.success(Unit)
    override suspend fun sendPasswordResetEmail(email: String): AuthResult = AuthResult.Failure(failure)
    override suspend fun confirmPasswordReset(code: String, newPassword: String): AuthResult =
        AuthResult.Failure(failure)
    override fun observeAuthState(): Flow<AuthState> = flowOf(AuthState.Unauthenticated)
    override suspend fun getCurrentSession(): UserSession? = null
    override suspend fun refreshSession(): AuthResult = AuthResult.Failure(failure)
    override suspend fun isSignedIn(): Boolean = false
    override suspend fun getIdToken(forceRefresh: Boolean): String? = null
    override suspend fun deleteAccount(): Result<Unit> = Result.success(Unit)
    override suspend fun updateDisplayName(displayName: String): Result<Unit> = Result.success(Unit)
    override suspend fun updateEmail(newEmail: String): Result<Unit> = Result.success(Unit)
    override suspend fun updatePassword(newPassword: String): Result<Unit> = Result.success(Unit)
    override suspend fun sendEmailVerification(): Result<Unit> = Result.success(Unit)
    override suspend fun reauthenticate(credentials: Credentials): AuthResult = AuthResult.Failure(failure)
    override suspend fun sendPhoneOtp(phoneNumber: String): PhoneAuthResult = PhoneAuthResult.Failure(failure)
    override suspend fun verifyPhoneOtp(verificationId: String, otpCode: String): AuthResult =
        AuthResult.Failure(failure)
    override suspend fun sendMagicLink(email: String, continueUrl: String, iosBundleId: String?): AuthResult =
        AuthResult.Failure(failure)

    override suspend fun signInWithMagicLink(email: String, link: String): AuthResult = AuthResult.Failure(failure)
}
