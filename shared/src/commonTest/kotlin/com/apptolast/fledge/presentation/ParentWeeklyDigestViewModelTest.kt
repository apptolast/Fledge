package com.apptolast.fledge.presentation

import com.apptolast.fledge.data.repository.InMemoryFamilyFoundationRepository
import com.apptolast.fledge.data.repository.InMemoryLedgerRepository
import com.apptolast.fledge.data.repository.InMemoryMoneyFlowRepository
import com.apptolast.fledge.data.repository.InMemorySavingsGoalRepository
import com.apptolast.fledge.data.repository.InMemoryTaskInstanceRepository
import com.apptolast.fledge.domain.model.CashOutSettlementDraft
import com.apptolast.fledge.domain.model.ChildPin
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.LedgerActor
import com.apptolast.fledge.domain.model.LedgerConcept
import com.apptolast.fledge.domain.model.LedgerTransactionDraft
import com.apptolast.fledge.domain.model.LedgerTransactionType
import com.apptolast.fledge.domain.model.LedgerTransferGroupId
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.SavingsGoalDraft
import com.apptolast.fledge.domain.model.TaskAssignmentId
import com.apptolast.fledge.domain.model.TaskInstance
import com.apptolast.fledge.domain.model.TaskInstanceId
import com.apptolast.fledge.domain.model.TaskInstanceStatus
import com.apptolast.fledge.domain.model.TaskTemplateId
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.domain.model.VirtualAccountType
import com.apptolast.fledge.domain.service.WeeklyParentDigestCalculator
import com.apptolast.fledge.presentation.foundation.weeklydigest.ParentWeeklyDigestViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Clock
import kotlinx.coroutines.test.runTest

class ParentWeeklyDigestViewModelTest {

    @Test
    fun `FLE-54 AC-02 parent weekly digest resolves repository activity`() = runTest {
        // Given
        val familyRepository = InMemoryFamilyFoundationRepository()
        val ledgerRepository = InMemoryLedgerRepository()
        val moneyFlowRepository = InMemoryMoneyFlowRepository()
        val savingsGoalRepository = InMemorySavingsGoalRepository()
        val now = Clock.System.now()
        val family = familyRepository.createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid"))
        familyRepository.recordVirtualMoneyConsent()
        val child = familyRepository.addChildProfile(
            family.id,
            displayName = "Lucas",
            birthYear = 2018,
            avatarKey = "star",
            pin = ChildPin("1234"),
        )
        val taskRepository = InMemoryTaskInstanceRepository(
            listOf(
                TaskInstance(
                    id = TaskInstanceId("task-1"),
                    familyId = family.id,
                    taskAssignmentId = TaskAssignmentId("assignment-1"),
                    taskTemplateId = TaskTemplateId("template-1"),
                    childProfileId = child.id,
                    title = "Poner la mesa",
                    rewardCents = MoneyCents(50),
                    requiresPhoto = false,
                    status = TaskInstanceStatus.Submitted,
                    dueAt = now,
                    periodKey = "20260730",
                    createdAt = now,
                    updatedAt = now,
                    submittedAt = now,
                ),
            ),
        )
        savingsGoalRepository.saveGoal(
            SavingsGoalDraft(
                familyId = family.id,
                childProfileId = child.id,
                title = "Bici nueva",
                targetCents = MoneyCents(4_000),
                iconKey = "bike",
            ),
            createdAt = now,
        )
        ledgerRepository.appendTransaction(
            LedgerTransactionDraft(
                familyId = family.id,
                childProfileId = child.id,
                accountType = VirtualAccountType.Goal,
                type = LedgerTransactionType.GoalTransfer,
                amountCents = MoneyCents(500),
                concept = LedgerConcept("Ahorro bici"),
                createdBy = LedgerActor.Child,
                transferGroupId = LedgerTransferGroupId("transfer-1"),
            ),
            createdAt = now,
        )
        moneyFlowRepository.createSettlementRequest(
            CashOutSettlementDraft(
                familyId = family.id,
                childProfileId = child.id,
                amountCents = MoneyCents(300),
                concept = LedgerConcept("Cromos"),
            ),
            requestedAt = now,
        )

        // When
        val viewModel = ParentWeeklyDigestViewModel(
            familyRepository = familyRepository,
            taskInstanceRepository = taskRepository,
            ledgerRepository = ledgerRepository,
            moneyFlowRepository = moneyFlowRepository,
            savingsGoalRepository = savingsGoalRepository,
            calculator = WeeklyParentDigestCalculator(),
        )

        // Then
        val digest = assertNotNull(viewModel.uiState.value.digest)
        assertEquals("Familia Garcia", digest.familyName)
        assertEquals(1, digest.submittedTaskCount)
        assertEquals(1, digest.pendingTaskApprovalCount)
        assertEquals(500, digest.savedCents.value)
        assertEquals(300, digest.requestedCashOutCents.value)
        assertEquals(300, digest.pendingDebtCents.value)
        assertEquals(1, digest.children.single().activeGoalCount)
    }
}
