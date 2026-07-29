package com.apptolast.fledge.presentation

import com.apptolast.fledge.data.repository.InMemoryFamilyFoundationRepository
import com.apptolast.fledge.data.repository.InMemoryLedgerRepository
import com.apptolast.fledge.data.repository.InMemoryMoneyFlowRepository
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
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.TaskAssignmentId
import com.apptolast.fledge.domain.model.TaskInstance
import com.apptolast.fledge.domain.model.TaskInstanceId
import com.apptolast.fledge.domain.model.TaskInstanceStatus
import com.apptolast.fledge.domain.model.TaskTemplateId
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.domain.model.VirtualAccountType
import com.apptolast.fledge.domain.service.CashOutProcessor
import com.apptolast.fledge.presentation.foundation.childhome.ChildHomeViewModel
import com.apptolast.fledge.presentation.foundation.childhome.ChildTaskSubmissionError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class ChildHomeViewModelTest {

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
    fun `FLE-34 child home returns rejected task to pending before retrying`() = runTest {
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
                    id = "retry-task",
                    familyId = family.id,
                    childProfileId = child.id,
                    status = TaskInstanceStatus.Rejected,
                    dueAt = Instant.fromEpochSeconds(1_900_000_000),
                    submittedAt = Instant.fromEpochSeconds(1_700_300_000),
                    reviewedAt = Instant.fromEpochSeconds(1_700_300_600),
                    rejectionReason = "Falta ver toda la mesa.",
                ),
            ),
        )
        val viewModel = ChildHomeViewModel(
            foundationRepository,
            ledgerRepository,
            moneyFlowRepository,
            taskInstanceRepository,
            CashOutProcessor(moneyFlowRepository, ledgerRepository),
        )
        viewModel.load(child.id)

        // When
        val retried = viewModel.retryTask(TaskInstanceId("retry-task"))

        // Then
        val instance = viewModel.uiState.value.taskInstances.single { it.id.value == "retry-task" }
        assertEquals(true, retried)
        assertEquals(TaskInstanceStatus.Pending, instance.status)
        assertNull(instance.rejectionReason)
        assertNull(instance.submittedAt)
    }

    private fun taskInstance(
        id: String,
        familyId: FamilyId,
        childProfileId: ChildProfileId,
        requiresPhoto: Boolean = false,
        status: TaskInstanceStatus = TaskInstanceStatus.Pending,
        dueAt: Instant = Instant.fromEpochSeconds(1_700_200_000),
        submittedAt: Instant? = null,
        reviewedAt: Instant? = null,
        rejectionReason: String? = null,
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
        dueAt = dueAt,
        periodKey = "20260729",
        createdAt = Instant.fromEpochSeconds(1_700_100_000),
        updatedAt = Instant.fromEpochSeconds(1_700_100_000),
        submittedAt = submittedAt,
        reviewedAt = reviewedAt,
        rejectionReason = rejectionReason,
    )
}
