package com.apptolast.fledge.presentation

import app.cash.turbine.test
import com.apptolast.fledge.data.repository.InMemoryFamilyFoundationRepository
import com.apptolast.fledge.data.repository.InMemoryLedgerRepository
import com.apptolast.fledge.data.repository.InMemoryMoneyFlowRepository
import com.apptolast.fledge.data.repository.InMemorySavingsGoalRepository
import com.apptolast.fledge.data.repository.InMemoryTaskInstanceRepository
import com.apptolast.fledge.domain.model.BalanceCents
import com.apptolast.fledge.domain.model.ChildPin
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.FamilyId
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
import com.apptolast.fledge.domain.service.CashOutProcessor
import com.apptolast.fledge.domain.service.TaskApprovalProcessor
import com.apptolast.fledge.presentation.foundation.parenthome.ParentHomeViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class ParentHomeViewModelTest {

    @Test
    fun `AC-10 parent home exposes stable lists with unique ids`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()
        val ledgerRepository = InMemoryLedgerRepository()
        val moneyFlowRepository = InMemoryMoneyFlowRepository()
        val savingsGoalRepository = InMemorySavingsGoalRepository()
        val taskInstanceRepository = InMemoryTaskInstanceRepository()
        val family = repository.createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid"))
        repository.recordVirtualMoneyConsent()
        val child = repository.addChildProfile(
            family.id,
            "Lucas",
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
        val viewModel = ParentHomeViewModel(
            repository,
            ledgerRepository,
            moneyFlowRepository,
            savingsGoalRepository,
            taskInstanceRepository,
            CashOutProcessor(moneyFlowRepository, ledgerRepository),
            TaskApprovalProcessor(taskInstanceRepository, ledgerRepository),
        )

        // When / Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.children.size)
            assertEquals(BalanceCents(500), state.mainBalances[child.id])
            assertTrue(state.setupActions.isNotEmpty())
            assertEquals(state.setupActions.size, state.setupActions.map { it.id }.toSet().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `FLE-31 parent home lists submitted tasks with editable default approval amount`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()
        val ledgerRepository = InMemoryLedgerRepository()
        val moneyFlowRepository = InMemoryMoneyFlowRepository()
        val savingsGoalRepository = InMemorySavingsGoalRepository()
        val family = repository.createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid"))
        repository.recordVirtualMoneyConsent()
        val child = repository.addChildProfile(
            family.id,
            "Lucas",
            birthYear = 2017,
            avatarKey = "rocket",
            pin = ChildPin("1234"),
        )
        val submittedAt = Instant.parse("2026-07-29T10:00:00Z")
        val taskInstanceRepository = InMemoryTaskInstanceRepository(
            listOf(
                taskInstance(
                    id = "task-1",
                    familyId = family.id,
                    childProfileId = child.id,
                    status = TaskInstanceStatus.Submitted,
                    submittedAt = submittedAt,
                ),
                taskInstance(
                    id = "task-2",
                    familyId = family.id,
                    childProfileId = child.id,
                    status = TaskInstanceStatus.Pending,
                ),
            ),
        )
        val viewModel = ParentHomeViewModel(
            repository,
            ledgerRepository,
            moneyFlowRepository,
            savingsGoalRepository,
            taskInstanceRepository,
            CashOutProcessor(moneyFlowRepository, ledgerRepository),
            TaskApprovalProcessor(taskInstanceRepository, ledgerRepository),
        )

        // When / Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(listOf("task-1"), state.pendingTaskApprovals.map { it.id.value })
            assertEquals("0,50", state.approvalAmountInputs[TaskInstanceId("task-1")])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `FLE-31 parent home approves a task with adjusted amount`() = runTest {
        // Given
        val fixture = parentHomeWithSubmittedTask()

        // When
        fixture.viewModel.updateApprovalAmount(TaskInstanceId("task-1"), "0,75")
        val approved = fixture.viewModel.approveTask(TaskInstanceId("task-1"))

        // Then
        assertTrue(approved)
        assertEquals(BalanceCents(75), fixture.ledgerRepository.balanceFor(fixture.childId, VirtualAccountType.Main))
        assertEquals(
            TaskInstanceStatus.Approved,
            fixture.taskInstanceRepository.instanceById(TaskInstanceId("task-1"))?.status,
        )
        assertEquals(emptyList(), fixture.viewModel.uiState.value.pendingTaskApprovals)
    }

    @Test
    fun `FLE-31 parent home rejects a task only after a reason is entered`() = runTest {
        // Given
        val fixture = parentHomeWithSubmittedTask()

        // When
        val rejectedWithoutReason = fixture.viewModel.rejectTask(TaskInstanceId("task-1"))
        fixture.viewModel.updateRejectionReason(TaskInstanceId("task-1"), "Falta recoger los vasos.")
        val rejectedWithReason = fixture.viewModel.rejectTask(TaskInstanceId("task-1"))

        // Then
        assertEquals(false, rejectedWithoutReason)
        assertEquals(emptyList(), fixture.ledgerRepository.transactions.value)
        assertTrue(rejectedWithReason)
        assertEquals(
            TaskInstanceStatus.Rejected,
            fixture.taskInstanceRepository.instanceById(TaskInstanceId("task-1"))?.status,
        )
        assertEquals(
            "Falta recoger los vasos.",
            fixture.taskInstanceRepository.instanceById(TaskInstanceId("task-1"))?.rejectionReason,
        )
    }

    @Test
    fun `FLE-40 AC-03 parent home exposes completed goal notices for children`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()
        val ledgerRepository = InMemoryLedgerRepository()
        val moneyFlowRepository = InMemoryMoneyFlowRepository()
        val savingsGoalRepository = InMemorySavingsGoalRepository()
        val taskInstanceRepository = InMemoryTaskInstanceRepository()
        val family = repository.createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid"))
        repository.recordVirtualMoneyConsent()
        val child = repository.addChildProfile(
            family.id,
            "Lucas",
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
            createdAt = Instant.parse("2026-07-01T08:00:00Z"),
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
        val viewModel = ParentHomeViewModel(
            repository,
            ledgerRepository,
            moneyFlowRepository,
            savingsGoalRepository,
            taskInstanceRepository,
            CashOutProcessor(moneyFlowRepository, ledgerRepository),
            TaskApprovalProcessor(taskInstanceRepository, ledgerRepository),
        )

        // When / Then
        val notice = viewModel.uiState.value.goalCompletionNotices.single()
        assertEquals(goal.id, notice.goalId)
        assertEquals(child.id, notice.childProfileId)
        assertEquals("Lucas", notice.childName)
        assertEquals("Bici nueva", notice.goalTitle)
        assertEquals(BalanceCents(4_200), notice.currentCents)
        assertEquals(MoneyCents(4_000), notice.targetCents)
    }

    private suspend fun parentHomeWithSubmittedTask(): ParentHomeFixture {
        val repository = InMemoryFamilyFoundationRepository()
        val ledgerRepository = InMemoryLedgerRepository()
        val moneyFlowRepository = InMemoryMoneyFlowRepository()
        val family = repository.createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid"))
        repository.recordVirtualMoneyConsent()
        val child = repository.addChildProfile(
            family.id,
            "Lucas",
            birthYear = 2017,
            avatarKey = "rocket",
            pin = ChildPin("1234"),
        )
        val taskInstanceRepository = InMemoryTaskInstanceRepository(
            listOf(
                taskInstance(
                    id = "task-1",
                    familyId = family.id,
                    childProfileId = child.id,
                    status = TaskInstanceStatus.Submitted,
                    submittedAt = Instant.parse("2026-07-29T10:00:00Z"),
                ),
            ),
        )
        val viewModel = ParentHomeViewModel(
            repository,
            ledgerRepository,
            moneyFlowRepository,
            InMemorySavingsGoalRepository(),
            taskInstanceRepository,
            CashOutProcessor(moneyFlowRepository, ledgerRepository),
            TaskApprovalProcessor(taskInstanceRepository, ledgerRepository),
        )

        return ParentHomeFixture(
            viewModel = viewModel,
            ledgerRepository = ledgerRepository,
            taskInstanceRepository = taskInstanceRepository,
            childId = child.id,
        )
    }

    private data class ParentHomeFixture(
        val viewModel: ParentHomeViewModel,
        val ledgerRepository: InMemoryLedgerRepository,
        val taskInstanceRepository: InMemoryTaskInstanceRepository,
        val childId: ChildProfileId,
    )

    private fun taskInstance(
        id: String,
        familyId: FamilyId,
        childProfileId: ChildProfileId,
        status: TaskInstanceStatus,
        submittedAt: Instant? = null,
    ): TaskInstance = TaskInstance(
        id = TaskInstanceId(id),
        familyId = familyId,
        taskAssignmentId = TaskAssignmentId("assignment-1"),
        taskTemplateId = TaskTemplateId("template-1"),
        childProfileId = childProfileId,
        title = "Poner la mesa",
        rewardCents = MoneyCents(50),
        requiresPhoto = false,
        status = status,
        dueAt = Instant.parse("2026-07-29T20:00:00Z"),
        periodKey = "20260729",
        createdAt = Instant.parse("2026-07-29T08:00:00Z"),
        updatedAt = submittedAt ?: Instant.parse("2026-07-29T08:00:00Z"),
        submittedAt = submittedAt,
    )
}
