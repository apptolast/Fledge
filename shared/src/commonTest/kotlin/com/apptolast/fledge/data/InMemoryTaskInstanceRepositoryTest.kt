package com.apptolast.fledge.data

import com.apptolast.fledge.data.repository.InMemoryTaskInstanceRepository
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.TaskAssignmentId
import com.apptolast.fledge.domain.model.TaskInstance
import com.apptolast.fledge.domain.model.TaskInstanceId
import com.apptolast.fledge.domain.model.TaskInstanceStatus
import com.apptolast.fledge.domain.model.TaskTemplateId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class InMemoryTaskInstanceRepositoryTest {

    @Test
    fun `FLE-29 repository returns instances by family and child`() = runTest {
        // Given
        val familyId = FamilyId("family-1")
        val childId = ChildProfileId("child-1")
        val repository = InMemoryTaskInstanceRepository(
            listOf(
                taskInstance(
                    id = "late",
                    familyId = familyId,
                    childProfileId = childId,
                    dueAt = Instant.fromEpochSeconds(20),
                ),
                taskInstance(
                    id = "early",
                    familyId = familyId,
                    childProfileId = childId,
                    dueAt = Instant.fromEpochSeconds(10),
                ),
                taskInstance(id = "other-child", familyId = familyId, childProfileId = ChildProfileId("child-2")),
                taskInstance(
                    id = "other-family",
                    familyId = FamilyId("family-2"),
                    childProfileId = childId,
                    dueAt = Instant.fromEpochSeconds(30),
                ),
            ),
        )

        // When
        val familyInstances = repository.instancesForFamily(familyId)
        val childInstances = repository.instancesForChild(childId)

        // Then
        assertEquals(listOf("early", "other-child", "late"), familyInstances.map { it.id.value })
        assertEquals(listOf("early", "late", "other-family"), childInstances.map { it.id.value })
    }

    @Test
    fun `FLE-30 repository submits pending task for review`() = runTest {
        // Given
        val submittedAt = Instant.fromEpochSeconds(1_700_300_000)
        val childId = ChildProfileId("child-1")
        val repository = InMemoryTaskInstanceRepository(
            listOf(taskInstance(id = "task-1", childProfileId = childId)),
        )

        // When
        val submitted = repository.submitForReview(
            instanceId = TaskInstanceId("task-1"),
            childProfileId = childId,
            photoEvidenceUri = null,
            submittedAt = submittedAt,
        )

        // Then
        assertEquals(TaskInstanceStatus.Submitted, submitted.status)
        assertEquals(submittedAt, submitted.submittedAt)
        assertEquals(null, submitted.photoEvidenceUri)
        assertEquals(50, submitted.rewardCents.value)
        assertEquals(
            TaskInstanceStatus.Submitted,
            repository.instances.value.single { it.id.value == "task-1" }.status,
        )
    }

    @Test
    fun `FLE-30 repository requires photo evidence when configured`() = runTest {
        // Given
        val childId = ChildProfileId("child-1")
        val repository = InMemoryTaskInstanceRepository(
            listOf(taskInstance(id = "task-1", childProfileId = childId, requiresPhoto = true)),
        )

        // When / Then
        assertFailsWith<IllegalArgumentException> {
            repository.submitForReview(
                instanceId = TaskInstanceId("task-1"),
                childProfileId = childId,
                photoEvidenceUri = null,
                submittedAt = Instant.fromEpochSeconds(1_700_300_000),
            )
        }
        assertEquals(TaskInstanceStatus.Pending, repository.instances.value.single().status)
    }

    @Test
    fun `FLE-30 repository allows resubmitting rejected task`() = runTest {
        // Given
        val childId = ChildProfileId("child-1")
        val repository = InMemoryTaskInstanceRepository(
            listOf(
                taskInstance(
                    id = "task-1",
                    childProfileId = childId,
                    requiresPhoto = true,
                    status = TaskInstanceStatus.Rejected,
                ),
            ),
        )

        // When
        val submitted = repository.submitForReview(
            instanceId = TaskInstanceId("task-1"),
            childProfileId = childId,
            photoEvidenceUri = "local://retry-photo",
            submittedAt = Instant.fromEpochSeconds(1_700_300_000),
        )

        // Then
        assertEquals(TaskInstanceStatus.Submitted, submitted.status)
        assertEquals("local://retry-photo", submitted.photoEvidenceUri)
    }

    private fun taskInstance(
        id: String,
        familyId: FamilyId = FamilyId("family-1"),
        childProfileId: ChildProfileId,
        dueAt: Instant = Instant.fromEpochSeconds(10),
        requiresPhoto: Boolean = false,
        status: TaskInstanceStatus = TaskInstanceStatus.Pending,
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
        createdAt = Instant.fromEpochSeconds(1),
        updatedAt = Instant.fromEpochSeconds(1),
    )
}
