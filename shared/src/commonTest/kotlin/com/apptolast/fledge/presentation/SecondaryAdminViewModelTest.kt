package com.apptolast.fledge.presentation

import com.apptolast.fledge.data.repository.InMemoryFamilyFoundationRepository
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.presentation.foundation.admin.SecondaryAdminError
import com.apptolast.fledge.presentation.foundation.admin.SecondaryAdminViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class SecondaryAdminViewModelTest {

    @Test
    fun `FLE-52 owner invites a secondary admin with normalized email`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()
        repository.createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid"))
        val viewModel = SecondaryAdminViewModel(repository)
        runCurrent()

        // When
        viewModel.updateEmail(" Cristina@Example.com ")
        val saved = viewModel.invite()

        // Then
        assertTrue(saved)
        assertEquals(listOf("cristina@example.com"), repository.activeFamily.value?.adminEmails)
        assertEquals("cristina@example.com", viewModel.uiState.value.savedInvite?.email)
        assertEquals("", viewModel.uiState.value.emailInput)
    }

    @Test
    fun `FLE-52 secondary admin invite requires a valid email`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()
        repository.createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid"))
        val viewModel = SecondaryAdminViewModel(repository)
        runCurrent()

        // When
        viewModel.updateEmail("not-email")
        val saved = viewModel.invite()

        // Then
        assertFalse(saved)
        assertEquals(SecondaryAdminError.InvalidEmail, viewModel.uiState.value.error)
        assertEquals(emptyList(), repository.activeFamily.value?.adminEmails)
    }
}
