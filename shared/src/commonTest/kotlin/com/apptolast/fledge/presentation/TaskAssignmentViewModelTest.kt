package com.apptolast.fledge.presentation

import com.apptolast.fledge.data.repository.InMemoryFamilyFoundationRepository
import com.apptolast.fledge.data.repository.InMemoryTaskAssignmentRepository
import com.apptolast.fledge.data.repository.InMemoryTaskTemplateRepository
import com.apptolast.fledge.domain.model.ChildPin
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.TaskRecurrence
import com.apptolast.fledge.domain.model.TaskTemplateDraft
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.presentation.foundation.taskassignment.TaskAssignmentError
import com.apptolast.fledge.presentation.foundation.taskassignment.TaskAssignmentViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class TaskAssignmentViewModelTest {

    @Test
    fun `AC-06 el formulario crea asignacion desde una plantilla sugerida`() = runTest {
        // Given
        val familyRepository = InMemoryFamilyFoundationRepository()
        val templateRepository = InMemoryTaskTemplateRepository()
        val assignmentRepository = InMemoryTaskAssignmentRepository()
        val family = familyRepository.createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid"))
        familyRepository.recordVirtualMoneyConsent()
        val lucas = familyRepository.addChildProfile(family.id, "Lucas", 2017, "rocket", ChildPin("1234"))
        val mia = familyRepository.addChildProfile(family.id, "Mia", 2019, "star", ChildPin("4321"))
        val template = templateRepository.saveTemplate(
            TaskTemplateDraft(
                familyId = family.id,
                title = "Poner la mesa",
                description = "Preparar platos, vasos y cubiertos.",
                iconKey = "utensils",
                defaultValueCents = MoneyCents(50),
                requiresPhoto = false,
            ),
            createdAt = Instant.fromEpochSeconds(1_700_100_000),
        )
        val viewModel = TaskAssignmentViewModel(familyRepository, templateRepository, assignmentRepository)

        // When
        viewModel.load()
        viewModel.selectTemplate(template.id)
        viewModel.updateTitle("Poner la mesa grande")
        viewModel.updateReward("0,75")
        viewModel.setRequiresPhoto(true)
        viewModel.toggleChild(lucas.id)
        viewModel.toggleChild(mia.id)
        viewModel.selectRecurrence(TaskRecurrence.Daily)
        viewModel.updateDueAt(Instant.fromEpochSeconds(1_700_200_000))
        assertTrue(viewModel.submit())

        // Then
        val assignment = checkNotNull(viewModel.uiState.value.savedAssignment)
        assertEquals(template.id, assignment.taskTemplateId)
        assertEquals("Poner la mesa grande", assignment.title)
        assertEquals(MoneyCents(75), assignment.rewardCents)
        assertEquals(true, assignment.requiresPhoto)
        assertEquals(listOf(lucas.id, mia.id), assignment.childProfileIds)
        assertEquals(TaskRecurrence.Daily, assignment.recurrence)
        assertEquals(Instant.fromEpochSeconds(1_700_200_000), assignment.dueAt)
        assertEquals(listOf(assignment), assignmentRepository.assignments.value)
    }

    @Test
    fun `AC-06 el formulario exige al menos un hijo seleccionado`() = runTest {
        // Given
        val familyRepository = InMemoryFamilyFoundationRepository()
        val templateRepository = InMemoryTaskTemplateRepository()
        val assignmentRepository = InMemoryTaskAssignmentRepository()
        val family = familyRepository.createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid"))
        val template = templateRepository.saveTemplate(
            TaskTemplateDraft(
                familyId = family.id,
                title = "Poner la mesa",
                description = "Preparar platos, vasos y cubiertos.",
                iconKey = "utensils",
                defaultValueCents = MoneyCents(50),
                requiresPhoto = false,
            ),
            createdAt = Instant.fromEpochSeconds(1_700_100_000),
        )
        val viewModel = TaskAssignmentViewModel(familyRepository, templateRepository, assignmentRepository)

        // When
        viewModel.load()
        viewModel.selectTemplate(template.id)

        // Then
        assertEquals(false, viewModel.submit())
        assertEquals(TaskAssignmentError.MissingChildren, viewModel.uiState.value.error)
    }
}
