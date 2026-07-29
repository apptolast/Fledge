package com.apptolast.fledge.domain

import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.TaskAssignment
import com.apptolast.fledge.domain.model.TaskAssignmentDraft
import com.apptolast.fledge.domain.model.TaskAssignmentId
import com.apptolast.fledge.domain.model.TaskRecurrence
import com.apptolast.fledge.domain.model.TaskTemplateId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class TaskAssignmentModelTest {

    @Test
    fun `AC-01 TaskAssignment conserva los campos requeridos`() {
        // Given
        val dueAt = Instant.fromEpochSeconds(1_700_200_000)

        // When
        val assignment = TaskAssignment(
            id = TaskAssignmentId("assignment-1"),
            familyId = FamilyId("family-1"),
            taskTemplateId = TaskTemplateId("template-1"),
            title = "Poner la mesa",
            rewardCents = MoneyCents(50),
            requiresPhoto = false,
            childProfileIds = listOf(ChildProfileId("child-1")),
            recurrence = TaskRecurrence.Daily,
            dueAt = dueAt,
            active = true,
            createdAt = Instant.fromEpochSeconds(1_700_100_000),
            updatedAt = Instant.fromEpochSeconds(1_700_100_000),
        )

        // Then
        assertEquals(TaskAssignmentId("assignment-1"), assignment.id)
        assertEquals(FamilyId("family-1"), assignment.familyId)
        assertEquals(TaskTemplateId("template-1"), assignment.taskTemplateId)
        assertEquals("Poner la mesa", assignment.title)
        assertEquals(MoneyCents(50), assignment.rewardCents)
        assertEquals(false, assignment.requiresPhoto)
        assertEquals(listOf(ChildProfileId("child-1")), assignment.childProfileIds)
        assertEquals(TaskRecurrence.Daily, assignment.recurrence)
        assertEquals(dueAt, assignment.dueAt)
        assertEquals(true, assignment.active)
    }

    @Test
    fun `AC-01 TaskAssignment rechaza datos invalidos`() {
        // Given
        val draft = TaskAssignmentDraft(
            familyId = FamilyId("family-1"),
            taskTemplateId = TaskTemplateId("template-1"),
            title = "Poner la mesa",
            rewardCents = MoneyCents(50),
            requiresPhoto = false,
            childProfileIds = listOf(ChildProfileId("child-1")),
            recurrence = TaskRecurrence.Daily,
            dueAt = Instant.fromEpochSeconds(1_700_200_000),
        )

        // When / Then
        assertFailsWith<IllegalArgumentException> { draft.copy(title = " ") }
        assertFailsWith<IllegalArgumentException> { draft.copy(rewardCents = MoneyCents(-50)) }
        assertFailsWith<IllegalArgumentException> { draft.copy(childProfileIds = emptyList()) }
        assertFailsWith<IllegalArgumentException> {
            draft.copy(recurrence = TaskRecurrence.Custom, customIntervalDays = null)
        }
        assertFailsWith<IllegalArgumentException> {
            draft.copy(recurrence = TaskRecurrence.Custom, customIntervalDays = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            draft.copy(recurrence = TaskRecurrence.Daily, customIntervalDays = 2)
        }
    }

    @Test
    fun `AC-02 las recurrencias soportadas son serializables`() {
        // Given / When
        val recurrenceNames = TaskRecurrence.entries.map { it.name }

        // Then
        assertEquals(listOf("Once", "Daily", "Weekly", "Custom"), recurrenceNames)
    }
}
