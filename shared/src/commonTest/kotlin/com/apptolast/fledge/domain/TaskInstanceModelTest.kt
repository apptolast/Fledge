package com.apptolast.fledge.domain

import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.TaskAssignmentId
import com.apptolast.fledge.domain.model.TaskInstance
import com.apptolast.fledge.domain.model.TaskInstanceId
import com.apptolast.fledge.domain.model.TaskInstanceStatus
import com.apptolast.fledge.domain.model.TaskTemplateId
import com.apptolast.fledge.domain.model.TransactionId
import com.apptolast.fledge.domain.model.approvedByParent
import com.apptolast.fledge.domain.model.rejectedByParent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class TaskInstanceModelTest {

    @Test
    fun `FLE-29 TaskInstance conserva snapshot requerido`() {
        // Given
        val now = Instant.fromEpochSeconds(1_700_000_000)

        // When
        val instance = TaskInstance(
            id = TaskInstanceId("task-assignment-1-child-1-20260729"),
            familyId = FamilyId("family-1"),
            taskAssignmentId = TaskAssignmentId("assignment-1"),
            taskTemplateId = TaskTemplateId("template-1"),
            childProfileId = ChildProfileId("child-1"),
            title = "Poner la mesa",
            rewardCents = MoneyCents(50),
            requiresPhoto = true,
            status = TaskInstanceStatus.Pending,
            dueAt = now,
            periodKey = "20260729",
            createdAt = now,
            updatedAt = now,
        )

        // Then
        assertEquals("family-1", instance.familyId.value)
        assertEquals("assignment-1", instance.taskAssignmentId.value)
        assertEquals("template-1", instance.taskTemplateId.value)
        assertEquals("child-1", instance.childProfileId.value)
        assertEquals("Poner la mesa", instance.title)
        assertEquals(50, instance.rewardCents.value)
        assertEquals(true, instance.requiresPhoto)
        assertEquals(TaskInstanceStatus.Pending, instance.status)
        assertEquals("20260729", instance.periodKey)
    }

    @Test
    fun `FLE-29 TaskInstance rechaza datos invalidos`() {
        // Given
        val now = Instant.fromEpochSeconds(1_700_000_000)

        // When / Then
        assertFailsWith<IllegalArgumentException> {
            TaskInstance(
                id = TaskInstanceId("task-1"),
                familyId = FamilyId("family-1"),
                taskAssignmentId = TaskAssignmentId("assignment-1"),
                taskTemplateId = TaskTemplateId("template-1"),
                childProfileId = ChildProfileId("child-1"),
                title = " ",
                rewardCents = MoneyCents(50),
                requiresPhoto = false,
                status = TaskInstanceStatus.Pending,
                dueAt = now,
                periodKey = "20260729",
                createdAt = now,
                updatedAt = now,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            TaskInstance(
                id = TaskInstanceId("task-1"),
                familyId = FamilyId("family-1"),
                taskAssignmentId = TaskAssignmentId("assignment-1"),
                taskTemplateId = TaskTemplateId("template-1"),
                childProfileId = ChildProfileId("child-1"),
                title = "Poner la mesa",
                rewardCents = MoneyCents(-50),
                requiresPhoto = false,
                status = TaskInstanceStatus.Pending,
                dueAt = now,
                periodKey = "20260729",
                createdAt = now,
                updatedAt = now,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            TaskInstance(
                id = TaskInstanceId("task-1"),
                familyId = FamilyId("family-1"),
                taskAssignmentId = TaskAssignmentId("assignment-1"),
                taskTemplateId = TaskTemplateId("template-1"),
                childProfileId = ChildProfileId("child-1"),
                title = "Poner la mesa",
                rewardCents = MoneyCents(50),
                requiresPhoto = false,
                status = TaskInstanceStatus.Pending,
                dueAt = now,
                periodKey = " ",
                createdAt = now,
                updatedAt = now,
            )
        }
    }

    @Test
    fun `FLE-29 TaskInstanceStatus conserva nombres estables`() {
        // Then
        assertEquals("Pending", TaskInstanceStatus.Pending.name)
        assertEquals("Submitted", TaskInstanceStatus.Submitted.name)
        assertEquals("Approved", TaskInstanceStatus.Approved.name)
        assertEquals("Rejected", TaskInstanceStatus.Rejected.name)
        assertEquals("Expired", TaskInstanceStatus.Expired.name)
    }

    @Test
    fun `FLE-30 TaskInstance modela evidencia de envio`() {
        // Given
        val now = Instant.fromEpochSeconds(1_700_000_000)

        // When
        val instance = TaskInstance(
            id = TaskInstanceId("task-assignment-1-child-1-20260729"),
            familyId = FamilyId("family-1"),
            taskAssignmentId = TaskAssignmentId("assignment-1"),
            taskTemplateId = TaskTemplateId("template-1"),
            childProfileId = ChildProfileId("child-1"),
            title = "Poner la mesa",
            rewardCents = MoneyCents(50),
            requiresPhoto = true,
            status = TaskInstanceStatus.Submitted,
            dueAt = now,
            periodKey = "20260729",
            createdAt = now,
            updatedAt = now,
            submittedAt = now,
            photoEvidenceUri = "local://task-photo-1",
        )

        // Then
        assertEquals(TaskInstanceStatus.Submitted, instance.status)
        assertEquals(now, instance.submittedAt)
        assertEquals("local://task-photo-1", instance.photoEvidenceUri)
    }

    @Test
    fun `FLE-30 TaskInstance rechaza envio con foto obligatoria sin evidencia`() {
        // Given
        val now = Instant.fromEpochSeconds(1_700_000_000)

        // When / Then
        assertFailsWith<IllegalArgumentException> {
            TaskInstance(
                id = TaskInstanceId("task-assignment-1-child-1-20260729"),
                familyId = FamilyId("family-1"),
                taskAssignmentId = TaskAssignmentId("assignment-1"),
                taskTemplateId = TaskTemplateId("template-1"),
                childProfileId = ChildProfileId("child-1"),
                title = "Poner la mesa",
                rewardCents = MoneyCents(50),
                requiresPhoto = true,
                status = TaskInstanceStatus.Submitted,
                dueAt = now,
                periodKey = "20260729",
                createdAt = now,
                updatedAt = now,
                submittedAt = now,
                photoEvidenceUri = null,
            )
        }
    }

    @Test
    fun `FLE-31 TaskInstance modela aprobacion parental con importe final y transaccion`() {
        // Given
        val submittedAt = Instant.fromEpochSeconds(1_700_300_000)
        val reviewedAt = Instant.fromEpochSeconds(1_700_300_600)
        val instance = taskInstance(
            status = TaskInstanceStatus.Submitted,
            updatedAt = submittedAt,
            submittedAt = submittedAt,
        )

        // When
        val approved = instance.approvedByParent(
            approvedRewardCents = MoneyCents(75),
            transactionId = TransactionId("tx-task-1"),
            reviewedAt = reviewedAt,
        )

        // Then
        assertEquals(TaskInstanceStatus.Approved, approved.status)
        assertEquals(reviewedAt, approved.reviewedAt)
        assertEquals(reviewedAt, approved.updatedAt)
        assertEquals(MoneyCents(75), approved.approvedRewardCents)
        assertEquals(TransactionId("tx-task-1"), approved.approvalTransactionId)
        assertEquals(null, approved.rejectionReason)
        assertEquals(submittedAt, approved.submittedAt)
    }

    @Test
    fun `FLE-31 TaskInstance modela rechazo parental con motivo obligatorio`() {
        // Given
        val submittedAt = Instant.fromEpochSeconds(1_700_300_000)
        val reviewedAt = Instant.fromEpochSeconds(1_700_300_600)
        val instance = taskInstance(
            status = TaskInstanceStatus.Submitted,
            updatedAt = submittedAt,
            submittedAt = submittedAt,
        )

        // When
        val rejected = instance.rejectedByParent(
            reason = "  Falta recoger los vasos.  ",
            reviewedAt = reviewedAt,
        )

        // Then
        assertEquals(TaskInstanceStatus.Rejected, rejected.status)
        assertEquals(reviewedAt, rejected.reviewedAt)
        assertEquals(reviewedAt, rejected.updatedAt)
        assertEquals("Falta recoger los vasos.", rejected.rejectionReason)
        assertEquals(null, rejected.approvedRewardCents)
        assertEquals(null, rejected.approvalTransactionId)
    }

    @Test
    fun `FLE-31 TaskInstance no permite aprobar ni rechazar estados no enviados`() {
        // Given
        val reviewedAt = Instant.fromEpochSeconds(1_700_300_600)
        val pending = taskInstance(status = TaskInstanceStatus.Pending)

        // When / Then
        assertFailsWith<IllegalArgumentException> {
            pending.approvedByParent(
                approvedRewardCents = MoneyCents(50),
                transactionId = TransactionId("tx-task-1"),
                reviewedAt = reviewedAt,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            pending.rejectedByParent(
                reason = "No esta terminada.",
                reviewedAt = reviewedAt,
            )
        }
    }

    private fun taskInstance(
        status: TaskInstanceStatus = TaskInstanceStatus.Pending,
        updatedAt: Instant = Instant.fromEpochSeconds(1_700_100_000),
        submittedAt: Instant? = null,
    ): TaskInstance = TaskInstance(
        id = TaskInstanceId("task-assignment-1-child-1-20260729"),
        familyId = FamilyId("family-1"),
        taskAssignmentId = TaskAssignmentId("assignment-1"),
        taskTemplateId = TaskTemplateId("template-1"),
        childProfileId = ChildProfileId("child-1"),
        title = "Poner la mesa",
        rewardCents = MoneyCents(50),
        requiresPhoto = false,
        status = status,
        dueAt = Instant.fromEpochSeconds(1_700_200_000),
        periodKey = "20260729",
        createdAt = Instant.fromEpochSeconds(1_700_100_000),
        updatedAt = updatedAt,
        submittedAt = submittedAt,
    )
}
