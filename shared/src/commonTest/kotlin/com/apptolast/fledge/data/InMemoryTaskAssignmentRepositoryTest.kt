package com.apptolast.fledge.data

import com.apptolast.fledge.data.repository.InMemoryTaskAssignmentRepository
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.TaskAssignmentDraft
import com.apptolast.fledge.domain.model.TaskRecurrence
import com.apptolast.fledge.domain.model.TaskTemplateId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class InMemoryTaskAssignmentRepositoryTest {

    @Test
    fun `AC-03 el padre asigna una tarea a varios hijos`() = runTest {
        // Given
        val repository = InMemoryTaskAssignmentRepository()
        val familyId = FamilyId("family-1")

        // When
        val assignment = repository.saveAssignment(
            TaskAssignmentDraft(
                familyId = familyId,
                taskTemplateId = TaskTemplateId("template-1"),
                title = "Poner la mesa",
                rewardCents = MoneyCents(50),
                requiresPhoto = false,
                childProfileIds = listOf(ChildProfileId("child-1"), ChildProfileId("child-2")),
                recurrence = TaskRecurrence.Weekly,
                dueAt = Instant.fromEpochSeconds(1_700_200_000),
            ),
            createdAt = Instant.fromEpochSeconds(1_700_100_000),
        )

        // Then
        assertEquals(listOf(ChildProfileId("child-1"), ChildProfileId("child-2")), assignment.childProfileIds)
        assertEquals(listOf(assignment), repository.assignmentsForFamily(familyId))
    }

    @Test
    fun `AC-04 el repositorio expone asignaciones activas por hijo`() = runTest {
        // Given
        val repository = InMemoryTaskAssignmentRepository()
        val familyId = FamilyId("family-1")
        val childId = ChildProfileId("child-1")
        val matching = repository.saveAssignment(
            TaskAssignmentDraft(
                familyId = familyId,
                taskTemplateId = TaskTemplateId("template-1"),
                title = "Poner la mesa",
                rewardCents = MoneyCents(50),
                requiresPhoto = false,
                childProfileIds = listOf(childId),
                recurrence = TaskRecurrence.Once,
                dueAt = Instant.fromEpochSeconds(1_700_200_000),
            ),
            createdAt = Instant.fromEpochSeconds(1_700_100_000),
        )
        repository.saveAssignment(
            TaskAssignmentDraft(
                familyId = familyId,
                taskTemplateId = TaskTemplateId("template-2"),
                title = "Hacer la cama",
                rewardCents = MoneyCents(75),
                requiresPhoto = true,
                childProfileIds = listOf(ChildProfileId("child-2")),
                recurrence = TaskRecurrence.Daily,
                dueAt = Instant.fromEpochSeconds(1_700_300_000),
            ),
            createdAt = Instant.fromEpochSeconds(1_700_100_001),
        )

        // When
        val activeForChild = repository.activeAssignmentsForChild(childId)

        // Then
        assertEquals(listOf(matching), activeForChild)
    }
}
