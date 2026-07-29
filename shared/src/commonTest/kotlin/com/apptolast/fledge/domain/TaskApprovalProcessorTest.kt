package com.apptolast.fledge.domain

import com.apptolast.fledge.data.repository.InMemoryLedgerRepository
import com.apptolast.fledge.data.repository.InMemoryTaskInstanceRepository
import com.apptolast.fledge.domain.model.BalanceCents
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.LedgerTransactionType
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.TaskAssignmentId
import com.apptolast.fledge.domain.model.TaskInstance
import com.apptolast.fledge.domain.model.TaskInstanceId
import com.apptolast.fledge.domain.model.TaskInstanceStatus
import com.apptolast.fledge.domain.model.TaskTemplateId
import com.apptolast.fledge.domain.model.VirtualAccountType
import com.apptolast.fledge.domain.service.TaskApprovalProcessor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class TaskApprovalProcessorTest {

    @Test
    fun `FLE-31 approval appends task reward and stores transaction id`() = runTest {
        // Given
        val submittedAt = Instant.parse("2026-07-29T10:00:00Z")
        val reviewedAt = Instant.parse("2026-07-29T10:05:00Z")
        val taskRepository = InMemoryTaskInstanceRepository(
            listOf(taskInstance(status = TaskInstanceStatus.Submitted, submittedAt = submittedAt)),
        )
        val ledgerRepository = InMemoryLedgerRepository()
        val processor = TaskApprovalProcessor(taskRepository, ledgerRepository)

        // When
        val approved = processor.approve(
            instanceId = TaskInstanceId("task-1"),
            approvedRewardCents = MoneyCents(75),
            reviewedAt = reviewedAt,
        )

        // Then
        val transaction = ledgerRepository.transactions.value.single()
        assertEquals(LedgerTransactionType.TaskReward, transaction.type)
        assertEquals(VirtualAccountType.Main, transaction.accountType)
        assertEquals(MoneyCents(75), transaction.amountCents)
        assertEquals("Tarea aprobada: Poner la mesa", transaction.concept.value)
        assertEquals(BalanceCents(75), ledgerRepository.balanceFor(ChildProfileId("child-1"), VirtualAccountType.Main))
        assertEquals(TaskInstanceStatus.Approved, approved.status)
        assertEquals(transaction.id, approved.approvalTransactionId)
        assertEquals(reviewedAt, approved.reviewedAt)
    }

    @Test
    fun `FLE-31 rejection keeps ledger unchanged and stores reason`() = runTest {
        // Given
        val submittedAt = Instant.parse("2026-07-29T10:00:00Z")
        val reviewedAt = Instant.parse("2026-07-29T10:05:00Z")
        val taskRepository = InMemoryTaskInstanceRepository(
            listOf(taskInstance(status = TaskInstanceStatus.Submitted, submittedAt = submittedAt)),
        )
        val ledgerRepository = InMemoryLedgerRepository()
        val processor = TaskApprovalProcessor(taskRepository, ledgerRepository)

        // When
        val rejected = processor.reject(
            instanceId = TaskInstanceId("task-1"),
            reason = "Falta recoger los vasos.",
            reviewedAt = reviewedAt,
        )

        // Then
        assertEquals(emptyList(), ledgerRepository.transactions.value)
        assertEquals(TaskInstanceStatus.Rejected, rejected.status)
        assertEquals("Falta recoger los vasos.", rejected.rejectionReason)
        assertEquals(reviewedAt, rejected.reviewedAt)
    }

    @Test
    fun `FLE-31 approval of non submitted task fails without ledger side effect`() = runTest {
        // Given
        val taskRepository = InMemoryTaskInstanceRepository(
            listOf(taskInstance(status = TaskInstanceStatus.Pending)),
        )
        val ledgerRepository = InMemoryLedgerRepository()
        val processor = TaskApprovalProcessor(taskRepository, ledgerRepository)

        // When / Then
        assertFailsWith<IllegalArgumentException> {
            processor.approve(
                instanceId = TaskInstanceId("task-1"),
                approvedRewardCents = MoneyCents(50),
                reviewedAt = Instant.parse("2026-07-29T10:05:00Z"),
            )
        }
        assertEquals(emptyList(), ledgerRepository.transactions.value)
    }

    private fun taskInstance(status: TaskInstanceStatus, submittedAt: Instant? = null): TaskInstance = TaskInstance(
        id = TaskInstanceId("task-1"),
        familyId = FamilyId("family-1"),
        taskAssignmentId = TaskAssignmentId("assignment-1"),
        taskTemplateId = TaskTemplateId("template-1"),
        childProfileId = ChildProfileId("child-1"),
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
