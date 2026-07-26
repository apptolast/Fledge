package com.apptolast.fledge.presentation

import com.apptolast.fledge.data.repository.InMemoryFamilyFoundationRepository
import com.apptolast.fledge.presentation.foundation.virtualconsent.VirtualMoneyConsentViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class VirtualMoneyConsentViewModelTest {

    @Test
    fun `FLE-16 consent must be accepted before first child setup`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()
        val viewModel = VirtualMoneyConsentViewModel(repository)

        // When / Then
        assertFalse(viewModel.submit())
        assertEquals(null, repository.virtualMoneyConsent.value)

        viewModel.updateAcceptedTerms(true)
        assertTrue(viewModel.submit())

        assertNotNull(viewModel.uiState.value.recordedConsent)
        assertEquals(repository.virtualMoneyConsent.value, viewModel.uiState.value.recordedConsent)
    }
}
