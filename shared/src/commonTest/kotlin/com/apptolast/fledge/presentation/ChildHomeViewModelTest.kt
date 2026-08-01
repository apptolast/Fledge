package com.apptolast.fledge.presentation

import com.apptolast.fledge.data.repository.InMemoryFamilyFoundationRepository
import com.apptolast.fledge.data.repository.InMemoryLedgerRepository
import com.apptolast.fledge.data.repository.InMemoryMoneyFlowRepository
import com.apptolast.fledge.data.repository.InMemorySavingsGoalRepository
import com.apptolast.fledge.data.repository.InMemoryTaskInstanceRepository
import com.apptolast.fledge.domain.model.BalanceCents
import com.apptolast.fledge.domain.model.ChildAchievementBadgeId
import com.apptolast.fledge.domain.model.ChildPin
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.InterestSettingsDraft
import com.apptolast.fledge.domain.model.LedgerActor
import com.apptolast.fledge.domain.model.LedgerConcept
import com.apptolast.fledge.domain.model.LedgerTransactionDraft
import com.apptolast.fledge.domain.model.LedgerTransactionType
import com.apptolast.fledge.domain.model.LedgerTransferGroupId
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.MoneyPotType
import com.apptolast.fledge.domain.model.SavingsGoalDraft
import com.apptolast.fledge.domain.model.TaskAssignmentId
import com.apptolast.fledge.domain.model.TaskInstance
import com.apptolast.fledge.domain.model.TaskInstanceId
import com.apptolast.fledge.domain.model.TaskInstanceStatus
import com.apptolast.fledge.domain.model.TaskTemplateId
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.domain.model.VirtualAccountType
import com.apptolast.fledge.domain.service.CashOutProcessor
import com.apptolast.fledge.domain.service.CompoundInterestExplanationLevel
import com.apptolast.fledge.presentation.foundation.childhome.ChildHomeEmptyStateAction
import com.apptolast.fledge.presentation.foundation.childhome.ChildHomeEmptyStateKind
import com.apptolast.fledge.presentation.foundation.childhome.ChildHomeViewModel
import com.apptolast.fledge.presentation.foundation.childhome.ChildTaskSubmissionError
import com.apptolast.fledge.presentation.foundation.childhome.actionableEmptyStates
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class ChildHomeViewModelTest {

    @Test
    fun `FLE-96 AC-01 child home greets the active child profile`() = runTest {
        // Given
        val foundationRepository = InMemoryFamilyFoundationRepository()
        val ledgerRepository = InMemoryLedgerRepository()
        val moneyFlowRepository = InMemoryMoneyFlowRepository()
        val savingsGoalRepository = InMemorySavingsGoalRepository()
        val taskInstanceRepository = InMemoryTaskInstanceRepository()
        val family = foundationRepository.createFamily(
            "Familia Garcia",
            CurrencyCode("EUR"),
            TimeZoneId("Europe/Madrid"),
        )
        foundationRepository.recordVirtualMoneyConsent()
        val child = foundationRepository.addChildProfile(
            familyId = family.id,
            displayName = "Elisa",
            birthYear = 2018,
            avatarKey = "star",
            pin = ChildPin("1234"),
        )
        val viewModel = ChildHomeViewModel(
            foundationRepository,
            ledgerRepository,
            moneyFlowRepository,
            savingsGoalRepository,
            taskInstanceRepository,
            CashOutProcessor(moneyFlowRepository, ledgerRepository),
        )

        // When
        viewModel.load(child.id)

        // Then
        assertEquals("Elisa", viewModel.uiState.value.childDisplayName)
    }

    @Test
    fun `FLE-43 AC-04 AC-05 child empty home asks for adult help and explains ledger`() = runTest {
        // Given
        val foundationRepository = InMemoryFamilyFoundationRepository()
        val ledgerRepository = InMemoryLedgerRepository()
        val moneyFlowRepository = InMemoryMoneyFlowRepository()
        val savingsGoalRepository = InMemorySavingsGoalRepository()
        val taskInstanceRepository = InMemoryTaskInstanceRepository()
        val family = foundationRepository.createFamily(
            "Familia Garcia",
            CurrencyCode("EUR"),
            TimeZoneId("Europe/Madrid"),
        )
        foundationRepository.recordVirtualMoneyConsent()
        val child = foundationRepository.addChildProfile(
            familyId = family.id,
            displayName = "Lucia",
            birthYear = 2018,
            avatarKey = "star",
            pin = ChildPin("1234"),
        )
        val viewModel = ChildHomeViewModel(
            foundationRepository,
            ledgerRepository,
            moneyFlowRepository,
            savingsGoalRepository,
            taskInstanceRepository,
            CashOutProcessor(moneyFlowRepository, ledgerRepository),
        )

        // When
        viewModel.load(child.id)
        val state = viewModel.uiState.value
        val emptyStatesByKind = state.actionableEmptyStates.associateBy { it.kind }

        // Then
        assertEquals(BalanceCents(0), state.balances?.main)
        assertEquals(ChildHomeEmptyStateAction.OpenParentZone, emptyStatesByKind[ChildHomeEmptyStateKind.Tasks]?.action)
        assertEquals(
            ChildHomeEmptyStateAction.OpenParentZone,
            emptyStatesByKind[ChildHomeEmptyStateKind.SavingsGoal]?.action,
        )
        assertTrue(ChildHomeEmptyStateKind.Ledger in emptyStatesByKind)
        assertEquals(null, emptyStatesByKind[ChildHomeEmptyStateKind.Ledger]?.action)
        assertEquals(emptyList(), state.ledgerTransactions)
    }

    @Test
    fun `FLE-21 child ledger shows original and reversal entries`() = runTest {
        // Given
        val foundationRepository = InMemoryFamilyFoundationRepository()
        val ledgerRepository = InMemoryLedgerRepository()
        val moneyFlowRepository = InMemoryMoneyFlowRepository()
        val taskInstanceRepository = InMemoryTaskInstanceRepository()
        val family = foundationRepository.createFamily(
            "Familia Garcia",
            CurrencyCode("EUR"),
            TimeZoneId("Europe/Madrid"),
        )
        foundationRepository.recordVirtualMoneyConsent()
        val child = foundationRepository.addChildProfile(
            familyId = family.id,
            displayName = "Lucas",
            birthYear = 2017,
            avatarKey = "rocket",
            pin = ChildPin("1234"),
        )
        val original = ledgerRepository.appendTransaction(
            LedgerTransactionDraft(
                familyId = family.id,
                childProfileId = child.id,
                accountType = VirtualAccountType.Main,
                type = LedgerTransactionType.Bonus,
                amountCents = MoneyCents(500),
                concept = LedgerConcept("Paga extra"),
                createdBy = LedgerActor.Parent,
            ),
        )
        ledgerRepository.reverseTransaction(
            transactionId = original.id,
            concept = LedgerConcept("Correccion"),
            createdBy = LedgerActor.Parent,
        )
        val viewModel = ChildHomeViewModel(
            foundationRepository,
            ledgerRepository,
            moneyFlowRepository,
            InMemorySavingsGoalRepository(),
            taskInstanceRepository,
            CashOutProcessor(moneyFlowRepository, ledgerRepository),
        )

        // When
        viewModel.load(child.id)

        // Then
        val state = viewModel.uiState.value
        assertEquals(BalanceCents(0), state.balances?.main)
        assertEquals(2, state.ledgerTransactions.size)
        assertEquals(original.id, state.ledgerTransactions[1].reversesTransactionId)
    }

    @Test
    fun `FLE-30 child home lists and submits actionable task without changing ledger`() = runTest {
        // Given
        val foundationRepository = InMemoryFamilyFoundationRepository()
        val ledgerRepository = InMemoryLedgerRepository()
        val moneyFlowRepository = InMemoryMoneyFlowRepository()
        val family = foundationRepository.createFamily(
            "Familia Garcia",
            CurrencyCode("EUR"),
            TimeZoneId("Europe/Madrid"),
        )
        foundationRepository.recordVirtualMoneyConsent()
        val child = foundationRepository.addChildProfile(
            familyId = family.id,
            displayName = "Lucas",
            birthYear = 2017,
            avatarKey = "rocket",
            pin = ChildPin("1234"),
        )
        ledgerRepository.appendTransaction(
            LedgerTransactionDraft(
                familyId = family.id,
                childProfileId = child.id,
                accountType = VirtualAccountType.Main,
                type = LedgerTransactionType.Bonus,
                amountCents = MoneyCents(500),
                concept = LedgerConcept("Paga extra"),
                createdBy = LedgerActor.Parent,
            ),
        )
        val taskInstanceRepository = InMemoryTaskInstanceRepository(
            listOf(
                taskInstance(id = "pending", familyId = family.id, childProfileId = child.id),
                taskInstance(
                    id = "waiting",
                    familyId = family.id,
                    childProfileId = child.id,
                    status = TaskInstanceStatus.Submitted,
                    submittedAt = Instant.fromEpochSeconds(1_700_300_000),
                ),
                taskInstance(
                    id = "approved",
                    familyId = family.id,
                    childProfileId = child.id,
                    status = TaskInstanceStatus.Approved,
                    submittedAt = Instant.fromEpochSeconds(1_700_300_000),
                    reviewedAt = Instant.fromEpochSeconds(1_700_400_000),
                ),
            ),
        )
        val viewModel = ChildHomeViewModel(
            foundationRepository,
            ledgerRepository,
            moneyFlowRepository,
            InMemorySavingsGoalRepository(),
            taskInstanceRepository,
            CashOutProcessor(moneyFlowRepository, ledgerRepository),
        )
        viewModel.load(child.id)

        // When
        val submitted = viewModel.submitTask(TaskInstanceId("pending"))

        // Then
        val state = viewModel.uiState.value
        assertEquals(true, submitted)
        assertEquals(BalanceCents(500), state.balances?.main)
        assertEquals(1, state.ledgerTransactions.size)
        assertEquals(listOf("pending", "waiting", "approved"), state.taskInstances.map { it.id.value })
        assertEquals(TaskInstanceStatus.Submitted, state.taskInstances.first { it.id.value == "pending" }.status)
    }

    @Test
    fun `FLE-30 child home requires photo before submitting photo task`() = runTest {
        // Given
        val foundationRepository = InMemoryFamilyFoundationRepository()
        val ledgerRepository = InMemoryLedgerRepository()
        val moneyFlowRepository = InMemoryMoneyFlowRepository()
        val family = foundationRepository.createFamily(
            "Familia Garcia",
            CurrencyCode("EUR"),
            TimeZoneId("Europe/Madrid"),
        )
        foundationRepository.recordVirtualMoneyConsent()
        val child = foundationRepository.addChildProfile(
            familyId = family.id,
            displayName = "Lucas",
            birthYear = 2017,
            avatarKey = "rocket",
            pin = ChildPin("1234"),
        )
        val taskInstanceRepository = InMemoryTaskInstanceRepository(
            listOf(
                taskInstance(
                    id = "photo-task",
                    familyId = family.id,
                    childProfileId = child.id,
                    requiresPhoto = true,
                ),
            ),
        )
        val viewModel = ChildHomeViewModel(
            foundationRepository,
            ledgerRepository,
            moneyFlowRepository,
            InMemorySavingsGoalRepository(),
            taskInstanceRepository,
            CashOutProcessor(moneyFlowRepository, ledgerRepository),
        )
        viewModel.load(child.id)

        // When
        val submittedWithoutPhoto = viewModel.submitTask(TaskInstanceId("photo-task"))
        viewModel.attachPhotoEvidence(TaskInstanceId("photo-task"), "local://photo-task")
        val submittedWithPhoto = viewModel.submitTask(TaskInstanceId("photo-task"))

        // Then
        val state = viewModel.uiState.value
        assertEquals(false, submittedWithoutPhoto)
        assertEquals(true, submittedWithPhoto)
        assertEquals(null, state.taskSubmissionError)
        assertEquals(
            TaskInstanceStatus.Submitted,
            state.taskInstances.single { it.id.value == "photo-task" }.status,
        )
        assertEquals(
            "local://photo-task",
            state.taskInstances.single { it.id.value == "photo-task" }.photoEvidenceUri,
        )
    }

    @Test
    fun `FLE-30 child home reports missing photo as recoverable error`() = runTest {
        // Given
        val foundationRepository = InMemoryFamilyFoundationRepository()
        val ledgerRepository = InMemoryLedgerRepository()
        val moneyFlowRepository = InMemoryMoneyFlowRepository()
        val family = foundationRepository.createFamily(
            "Familia Garcia",
            CurrencyCode("EUR"),
            TimeZoneId("Europe/Madrid"),
        )
        foundationRepository.recordVirtualMoneyConsent()
        val child = foundationRepository.addChildProfile(
            familyId = family.id,
            displayName = "Lucas",
            birthYear = 2017,
            avatarKey = "rocket",
            pin = ChildPin("1234"),
        )
        val taskInstanceRepository = InMemoryTaskInstanceRepository(
            listOf(
                taskInstance(id = "photo-task", familyId = family.id, childProfileId = child.id, requiresPhoto = true),
            ),
        )
        val viewModel = ChildHomeViewModel(
            foundationRepository,
            ledgerRepository,
            moneyFlowRepository,
            InMemorySavingsGoalRepository(),
            taskInstanceRepository,
            CashOutProcessor(moneyFlowRepository, ledgerRepository),
        )
        viewModel.load(child.id)

        // When
        val submitted = viewModel.submitTask(TaskInstanceId("photo-task"))

        // Then
        assertEquals(false, submitted)
        assertEquals(ChildTaskSubmissionError.MissingPhotoEvidence, viewModel.uiState.value.taskSubmissionError)
        assertEquals(TaskInstanceStatus.Pending, taskInstanceRepository.instances.value.single().status)
    }

    @Test
    fun `FLE-36 child home exposes active savings goal with goal balance`() = runTest {
        // Given
        val foundationRepository = InMemoryFamilyFoundationRepository()
        val ledgerRepository = InMemoryLedgerRepository()
        val moneyFlowRepository = InMemoryMoneyFlowRepository()
        val savingsGoalRepository = InMemorySavingsGoalRepository()
        val taskInstanceRepository = InMemoryTaskInstanceRepository()
        val family = foundationRepository.createFamily(
            "Familia Garcia",
            CurrencyCode("EUR"),
            TimeZoneId("Europe/Madrid"),
        )
        foundationRepository.recordVirtualMoneyConsent()
        val child = foundationRepository.addChildProfile(
            familyId = family.id,
            displayName = "Lucas",
            birthYear = 2017,
            avatarKey = "rocket",
            pin = ChildPin("1234"),
        )
        ledgerRepository.appendTransaction(
            LedgerTransactionDraft(
                familyId = family.id,
                childProfileId = child.id,
                accountType = VirtualAccountType.Goal,
                type = LedgerTransactionType.GoalTransfer,
                amountCents = MoneyCents(1_230),
                concept = LedgerConcept("Ahorro bici"),
                createdBy = LedgerActor.Child,
                transferGroupId = LedgerTransferGroupId("transfer-1"),
            ),
        )
        val goal = savingsGoalRepository.saveGoal(
            SavingsGoalDraft(
                familyId = family.id,
                childProfileId = child.id,
                title = "Bici nueva",
                targetCents = MoneyCents(4_000),
                iconKey = "bike",
            ),
            createdAt = Instant.fromEpochSeconds(1_700_100_000),
        )
        val viewModel = ChildHomeViewModel(
            foundationRepository,
            ledgerRepository,
            moneyFlowRepository,
            savingsGoalRepository,
            taskInstanceRepository,
            CashOutProcessor(moneyFlowRepository, ledgerRepository),
        )

        // When
        viewModel.load(child.id)

        // Then
        val state = viewModel.uiState.value
        assertEquals(goal, state.activeSavingsGoal)
        assertEquals(BalanceCents(1_230), state.balances?.goal)
    }

    @Test
    fun `FLE-50 child home exposes Give goal and Give pot balance`() = runTest {
        // Given
        val foundationRepository = InMemoryFamilyFoundationRepository()
        val ledgerRepository = InMemoryLedgerRepository()
        val moneyFlowRepository = InMemoryMoneyFlowRepository()
        val savingsGoalRepository = InMemorySavingsGoalRepository()
        val taskInstanceRepository = InMemoryTaskInstanceRepository()
        val family = foundationRepository.createFamily(
            "Familia Garcia",
            CurrencyCode("EUR"),
            TimeZoneId("Europe/Madrid"),
        )
        foundationRepository.recordVirtualMoneyConsent()
        val child = foundationRepository.addChildProfile(
            familyId = family.id,
            displayName = "Lucas",
            birthYear = 2017,
            avatarKey = "rocket",
            pin = ChildPin("1234"),
        )
        ledgerRepository.appendTransaction(
            LedgerTransactionDraft(
                familyId = family.id,
                childProfileId = child.id,
                accountType = VirtualAccountType.Give,
                type = LedgerTransactionType.GoalTransfer,
                amountCents = MoneyCents(1_230),
                concept = LedgerConcept("Donar al refugio"),
                createdBy = LedgerActor.Child,
                transferGroupId = LedgerTransferGroupId("transfer-1"),
            ),
        )
        val goal = savingsGoalRepository.saveGoal(
            SavingsGoalDraft(
                familyId = family.id,
                childProfileId = child.id,
                title = "Donar al refugio",
                targetCents = MoneyCents(2_000),
                potType = MoneyPotType.Give,
                iconKey = "heart",
            ),
            createdAt = Instant.fromEpochSeconds(1_700_100_000),
        )
        val viewModel = ChildHomeViewModel(
            foundationRepository,
            ledgerRepository,
            moneyFlowRepository,
            savingsGoalRepository,
            taskInstanceRepository,
            CashOutProcessor(moneyFlowRepository, ledgerRepository),
        )

        // When
        viewModel.load(child.id)

        // Then
        val state = viewModel.uiState.value
        assertEquals(goal, state.activeSavingsGoal)
        assertEquals(BalanceCents(0), state.balances?.goal)
        assertEquals(BalanceCents(1_230), state.balances?.give)
        assertEquals(BalanceCents(770), state.activeSavingsGoalProjection?.remainingCents)
    }

    @Test
    fun `FLE-39 child home exposes progress projection for active goal`() = runTest {
        // Given
        val foundationRepository = InMemoryFamilyFoundationRepository()
        val ledgerRepository = InMemoryLedgerRepository()
        val moneyFlowRepository = InMemoryMoneyFlowRepository()
        val savingsGoalRepository = InMemorySavingsGoalRepository()
        val taskInstanceRepository = InMemoryTaskInstanceRepository()
        val now = Clock.System.now()
        val family = foundationRepository.createFamily(
            "Familia Garcia",
            CurrencyCode("EUR"),
            TimeZoneId("Europe/Madrid"),
        )
        foundationRepository.recordVirtualMoneyConsent()
        val child = foundationRepository.addChildProfile(
            familyId = family.id,
            displayName = "Lucas",
            birthYear = 2017,
            avatarKey = "rocket",
            pin = ChildPin("1234"),
        )
        ledgerRepository.appendTransaction(
            LedgerTransactionDraft(
                familyId = family.id,
                childProfileId = child.id,
                accountType = VirtualAccountType.Goal,
                type = LedgerTransactionType.GoalTransfer,
                amountCents = MoneyCents(1_200),
                concept = LedgerConcept("Ahorro bici"),
                createdBy = LedgerActor.Child,
                transferGroupId = LedgerTransferGroupId("transfer-1"),
            ),
            createdAt = now.minus(4.days),
        )
        savingsGoalRepository.saveGoal(
            SavingsGoalDraft(
                familyId = family.id,
                childProfileId = child.id,
                title = "Bici nueva",
                targetCents = MoneyCents(3_600),
                iconKey = "bike",
            ),
            createdAt = now.minus(8.days),
        )
        val viewModel = ChildHomeViewModel(
            foundationRepository,
            ledgerRepository,
            moneyFlowRepository,
            savingsGoalRepository,
            taskInstanceRepository,
            CashOutProcessor(moneyFlowRepository, ledgerRepository),
        )

        // When
        viewModel.load(child.id)

        // Then
        val projection = viewModel.uiState.value.activeSavingsGoalProjection
        assertEquals(33, projection?.progressPercent)
        assertEquals(BalanceCents(2_400), projection?.remainingCents)
        assertEquals(BalanceCents(300), projection?.dailyPaceCents)
        assertEquals(8, projection?.estimatedDaysRemaining)
    }

    @Test
    fun `FLE-40 AC-02 child home exposes celebration notice for completed active goal`() = runTest {
        // Given
        val foundationRepository = InMemoryFamilyFoundationRepository()
        val ledgerRepository = InMemoryLedgerRepository()
        val moneyFlowRepository = InMemoryMoneyFlowRepository()
        val savingsGoalRepository = InMemorySavingsGoalRepository()
        val taskInstanceRepository = InMemoryTaskInstanceRepository()
        val family = foundationRepository.createFamily(
            "Familia Garcia",
            CurrencyCode("EUR"),
            TimeZoneId("Europe/Madrid"),
        )
        foundationRepository.recordVirtualMoneyConsent()
        val child = foundationRepository.addChildProfile(
            familyId = family.id,
            displayName = "Lucas",
            birthYear = 2017,
            avatarKey = "rocket",
            pin = ChildPin("1234"),
        )
        val goal = savingsGoalRepository.saveGoal(
            SavingsGoalDraft(
                familyId = family.id,
                childProfileId = child.id,
                title = "Bici nueva",
                targetCents = MoneyCents(4_000),
                iconKey = "bike",
            ),
            createdAt = Clock.System.now().minus(2.days),
        )
        ledgerRepository.appendTransaction(
            LedgerTransactionDraft(
                familyId = family.id,
                childProfileId = child.id,
                accountType = VirtualAccountType.Goal,
                type = LedgerTransactionType.GoalTransfer,
                amountCents = MoneyCents(4_200),
                concept = LedgerConcept("Ahorro bici"),
                createdBy = LedgerActor.Child,
                transferGroupId = LedgerTransferGroupId("transfer-1"),
            ),
        )
        val viewModel = ChildHomeViewModel(
            foundationRepository,
            ledgerRepository,
            moneyFlowRepository,
            savingsGoalRepository,
            taskInstanceRepository,
            CashOutProcessor(moneyFlowRepository, ledgerRepository),
        )

        // When
        viewModel.load(child.id)

        // Then
        val notice = viewModel.uiState.value.activeSavingsGoalCompletionNotice
        assertEquals(goal.id, notice?.goalId)
        assertEquals(child.id, notice?.childProfileId)
        assertEquals("Lucas", notice?.childName)
        assertEquals("Bici nueva", notice?.goalTitle)
        assertEquals(BalanceCents(4_200), notice?.currentCents)
        assertEquals(MoneyCents(4_000), notice?.targetCents)
    }

    @Test
    fun `FLE-49 AC-01 child home exposes compound interest projection when interest is enabled`() = runTest {
        // Given
        val foundationRepository = InMemoryFamilyFoundationRepository()
        val ledgerRepository = InMemoryLedgerRepository()
        val moneyFlowRepository = InMemoryMoneyFlowRepository()
        val savingsGoalRepository = InMemorySavingsGoalRepository()
        val taskInstanceRepository = InMemoryTaskInstanceRepository()
        val family = foundationRepository.createFamily(
            "Familia Garcia",
            CurrencyCode("EUR"),
            TimeZoneId("Europe/Madrid"),
        )
        foundationRepository.recordVirtualMoneyConsent()
        val child = foundationRepository.addChildProfile(
            familyId = family.id,
            displayName = "Lucas",
            birthYear = 2012,
            avatarKey = "rocket",
            pin = ChildPin("1234"),
        )
        foundationRepository.updateInterestSettings(
            InterestSettingsDraft(
                enabled = true,
                annualRateBasisPoints = 1_200,
                postingDayOfMonth = 1,
            ),
        )
        ledgerRepository.appendTransaction(
            LedgerTransactionDraft(
                familyId = family.id,
                childProfileId = child.id,
                accountType = VirtualAccountType.Main,
                type = LedgerTransactionType.Bonus,
                amountCents = MoneyCents(10_000),
                concept = LedgerConcept("Paga extra"),
                createdBy = LedgerActor.Parent,
            ),
        )
        val viewModel = ChildHomeViewModel(
            foundationRepository,
            ledgerRepository,
            moneyFlowRepository,
            savingsGoalRepository,
            taskInstanceRepository,
            CashOutProcessor(moneyFlowRepository, ledgerRepository),
        )

        // When
        viewModel.load(child.id)

        // Then
        val projection = viewModel.uiState.value.compoundInterestProjection
        assertEquals(BalanceCents(10_000), projection?.currentCents)
        assertEquals(BalanceCents(11_266), projection?.oneYearCents)
        assertEquals(BalanceCents(14_292), projection?.threeYearsCents)
        assertEquals(CompoundInterestExplanationLevel.Older, projection?.explanationLevel)
    }

    @Test
    fun `FLE-49 AC-02 child home hides compound interest projection when disabled or balance is zero`() = runTest {
        // Given
        val foundationRepository = InMemoryFamilyFoundationRepository()
        val ledgerRepository = InMemoryLedgerRepository()
        val moneyFlowRepository = InMemoryMoneyFlowRepository()
        val savingsGoalRepository = InMemorySavingsGoalRepository()
        val taskInstanceRepository = InMemoryTaskInstanceRepository()
        val family = foundationRepository.createFamily(
            "Familia Garcia",
            CurrencyCode("EUR"),
            TimeZoneId("Europe/Madrid"),
        )
        foundationRepository.recordVirtualMoneyConsent()
        val child = foundationRepository.addChildProfile(
            familyId = family.id,
            displayName = "Lucia",
            birthYear = 2018,
            avatarKey = "star",
            pin = ChildPin("1234"),
        )
        foundationRepository.updateInterestSettings(
            InterestSettingsDraft(
                enabled = true,
                annualRateBasisPoints = 333,
                postingDayOfMonth = 1,
            ),
        )
        val zeroBalanceViewModel = ChildHomeViewModel(
            foundationRepository,
            ledgerRepository,
            moneyFlowRepository,
            savingsGoalRepository,
            taskInstanceRepository,
            CashOutProcessor(moneyFlowRepository, ledgerRepository),
        )

        // When
        zeroBalanceViewModel.load(child.id)

        // Then
        assertEquals(null, zeroBalanceViewModel.uiState.value.compoundInterestProjection)

        // Given
        val disabledRepository = InMemoryFamilyFoundationRepository()
        val disabledLedgerRepository = InMemoryLedgerRepository()
        val disabledMoneyFlowRepository = InMemoryMoneyFlowRepository()
        val disabledSavingsGoalRepository = InMemorySavingsGoalRepository()
        val disabledTaskInstanceRepository = InMemoryTaskInstanceRepository()
        val disabledFamily = disabledRepository.createFamily(
            "Familia Garcia",
            CurrencyCode("EUR"),
            TimeZoneId("Europe/Madrid"),
        )
        disabledRepository.recordVirtualMoneyConsent()
        val disabledChild = disabledRepository.addChildProfile(
            familyId = disabledFamily.id,
            displayName = "Lucia",
            birthYear = 2018,
            avatarKey = "star",
            pin = ChildPin("1234"),
        )
        disabledLedgerRepository.appendTransaction(
            LedgerTransactionDraft(
                familyId = disabledFamily.id,
                childProfileId = disabledChild.id,
                accountType = VirtualAccountType.Main,
                type = LedgerTransactionType.Bonus,
                amountCents = MoneyCents(2_000),
                concept = LedgerConcept("Paga extra"),
                createdBy = LedgerActor.Parent,
            ),
        )
        val disabledInterestViewModel = ChildHomeViewModel(
            disabledRepository,
            disabledLedgerRepository,
            disabledMoneyFlowRepository,
            disabledSavingsGoalRepository,
            disabledTaskInstanceRepository,
            CashOutProcessor(disabledMoneyFlowRepository, disabledLedgerRepository),
        )

        // When
        disabledInterestViewModel.load(disabledChild.id)

        // Then
        assertEquals(null, disabledInterestViewModel.uiState.value.compoundInterestProjection)
    }

    @Test
    fun `FLE-57 AC-03 child home exposes achievement summary before task list`() = runTest {
        // Given
        val foundationRepository = InMemoryFamilyFoundationRepository()
        val ledgerRepository = InMemoryLedgerRepository()
        val moneyFlowRepository = InMemoryMoneyFlowRepository()
        val savingsGoalRepository = InMemorySavingsGoalRepository()
        val family = foundationRepository.createFamily(
            "Familia Garcia",
            CurrencyCode("EUR"),
            TimeZoneId("Europe/Madrid"),
        )
        foundationRepository.recordVirtualMoneyConsent()
        val child = foundationRepository.addChildProfile(
            familyId = family.id,
            displayName = "Lucas",
            birthYear = 2017,
            avatarKey = "rocket",
            pin = ChildPin("1234"),
        )
        val now = Clock.System.now()
        val taskInstanceRepository = InMemoryTaskInstanceRepository(
            listOf(
                taskInstance(
                    id = "approved-1",
                    familyId = family.id,
                    childProfileId = child.id,
                    status = TaskInstanceStatus.Approved,
                    submittedAt = now.minus(2.days),
                    reviewedAt = now.minus(2.days),
                ),
                taskInstance(
                    id = "approved-2",
                    familyId = family.id,
                    childProfileId = child.id,
                    status = TaskInstanceStatus.Approved,
                    submittedAt = now.minus(1.days),
                    reviewedAt = now.minus(1.days),
                ),
                taskInstance(
                    id = "approved-3",
                    familyId = family.id,
                    childProfileId = child.id,
                    status = TaskInstanceStatus.Approved,
                    submittedAt = now,
                    reviewedAt = now,
                ),
            ),
        )
        val viewModel = ChildHomeViewModel(
            foundationRepository,
            ledgerRepository,
            moneyFlowRepository,
            savingsGoalRepository,
            taskInstanceRepository,
            CashOutProcessor(moneyFlowRepository, ledgerRepository),
        )

        // When
        viewModel.load(child.id)

        // Then
        val summary = viewModel.uiState.value.achievementSummary
        assertEquals(3, summary.currentStreakDays)
        assertEquals(3, summary.bestStreakDays)
        assertEquals(3, summary.approvedTaskCount)
        assertEquals(
            true,
            summary.badges.first { it.id == ChildAchievementBadgeId.ThreeApprovedTasks }.unlocked,
        )
    }

    private fun taskInstance(
        id: String,
        familyId: FamilyId,
        childProfileId: ChildProfileId,
        requiresPhoto: Boolean = false,
        status: TaskInstanceStatus = TaskInstanceStatus.Pending,
        submittedAt: Instant? = null,
        reviewedAt: Instant? = null,
    ): TaskInstance = TaskInstance(
        id = TaskInstanceId(id),
        familyId = familyId,
        taskAssignmentId = TaskAssignmentId("assignment-1"),
        taskTemplateId = TaskTemplateId("template-1"),
        childProfileId = childProfileId,
        title = "Poner la mesa",
        rewardCents = MoneyCents(50),
        requiresPhoto = requiresPhoto,
        status = status,
        dueAt = Instant.fromEpochSeconds(1_700_200_000),
        periodKey = "20260729",
        createdAt = Instant.fromEpochSeconds(1_700_100_000),
        updatedAt = Instant.fromEpochSeconds(1_700_100_000),
        submittedAt = submittedAt,
        reviewedAt = reviewedAt,
    )
}
