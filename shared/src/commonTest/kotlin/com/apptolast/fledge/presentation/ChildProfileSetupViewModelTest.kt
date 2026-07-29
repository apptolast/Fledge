package com.apptolast.fledge.presentation

import com.apptolast.fledge.data.repository.InMemoryFamilyFoundationRepository
import com.apptolast.fledge.data.repository.InMemoryTaskTemplateRepository
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.presentation.foundation.childsetup.ChildProfileSetupError
import com.apptolast.fledge.presentation.foundation.childsetup.ChildProfileSetupViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ChildProfileSetupViewModelTest {

    @Test
    fun `FLE-11 child setup creates profile with birth year and pin hash`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()
        val taskTemplates = InMemoryTaskTemplateRepository()
        repository.createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid"))
        repository.recordVirtualMoneyConsent()
        val viewModel = ChildProfileSetupViewModel(repository, taskTemplates)

        // When
        viewModel.updateDisplayName("Lucas")
        viewModel.updateBirthYear("2017")
        viewModel.updateAvatarKey("rocket")
        viewModel.updatePin("1234")
        assertTrue(viewModel.uiState.value.canSubmit)
        assertTrue(viewModel.submit())

        // Then
        val child = viewModel.uiState.value.createdChild
        assertEquals("Lucas", child?.displayName)
        assertEquals(2017, child?.birthYear)
        assertNotNull(child?.pinHash)
    }

    @Test
    fun `FLE-16 child setup blocks submit without virtual money consent`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()
        val taskTemplates = InMemoryTaskTemplateRepository()
        repository.createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid"))
        val viewModel = ChildProfileSetupViewModel(repository, taskTemplates)

        // When
        viewModel.updateDisplayName("Lucas")
        viewModel.updateBirthYear("2017")
        viewModel.updatePin("1234")

        // Then
        assertTrue(viewModel.uiState.value.canSubmit)
        assertEquals(false, viewModel.submit())
        assertEquals(ChildProfileSetupError.MissingVirtualMoneyConsent, viewModel.uiState.value.error)
    }

    @Test
    fun `AC-03 el primer hijo siembra sugerencias iniciales`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()
        val taskTemplates = InMemoryTaskTemplateRepository()
        val family = repository.createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid"))
        repository.recordVirtualMoneyConsent()
        val viewModel = ChildProfileSetupViewModel(repository, taskTemplates)

        // When
        viewModel.updateDisplayName("Lucas")
        viewModel.updateBirthYear("2017")
        viewModel.updatePin("1234")
        assertTrue(viewModel.submit())

        // Then
        assertEquals(3, taskTemplates.templatesForFamily(family.id).size)
    }

    @Test
    fun `AC-03 el segundo hijo no vuelve a sembrar automaticamente`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()
        val taskTemplates = InMemoryTaskTemplateRepository()
        val family = repository.createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid"))
        repository.recordVirtualMoneyConsent()
        val firstViewModel = ChildProfileSetupViewModel(repository, taskTemplates)
        firstViewModel.updateDisplayName("Lucas")
        firstViewModel.updateBirthYear("2017")
        firstViewModel.updatePin("1234")
        assertTrue(firstViewModel.submit())

        // When
        val secondViewModel = ChildProfileSetupViewModel(repository, taskTemplates)
        secondViewModel.updateDisplayName("Mia")
        secondViewModel.updateBirthYear("2019")
        secondViewModel.updatePin("4321")
        assertTrue(secondViewModel.submit())

        // Then
        assertEquals(2, repository.children.value.size)
        assertEquals(3, taskTemplates.templatesForFamily(family.id).size)
    }
}
