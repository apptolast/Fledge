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

    private fun taskInstance(
        id: String,
        familyId: FamilyId,
        childProfileId: ChildProfileId,
        dueAt: Instant = Instant.fromEpochSeconds(10),
    ): TaskInstance = TaskInstance(
        id = TaskInstanceId(id),
        familyId = familyId,
        taskAssignmentId = TaskAssignmentId("assignment-1"),
        taskTemplateId = TaskTemplateId("template-1"),
        childProfileId = childProfileId,
        title = "Poner la mesa",
        rewardCents = MoneyCents(50),
        requiresPhoto = false,
        status = TaskInstanceStatus.Pending,
        dueAt = dueAt,
        periodKey = "20260729",
        createdAt = Instant.fromEpochSeconds(1),
        updatedAt = Instant.fromEpochSeconds(1),
    )
}
