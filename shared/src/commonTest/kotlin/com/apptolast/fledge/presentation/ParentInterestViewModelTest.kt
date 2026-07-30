package com.apptolast.fledge.presentation

import com.apptolast.fledge.data.repository.InMemoryFamilyFoundationRepository
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.presentation.foundation.interest.ParentInterestError
import com.apptolast.fledge.presentation.foundation.interest.ParentInterestViewModel
import com.apptolast.fledge.presentation.foundation.interest.parseAnnualRateBasisPoints
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ParentInterestViewModelTest {

    @Test
    fun `FLE-48 parent saves configurable interest settings`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()
        repository.createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid"))
        val viewModel = ParentInterestViewModel(repository)

        // When
        viewModel.updateEnabled(true)
        viewModel.updateAnnualRate("2,50")
        viewModel.updatePostingDay("5")
        val saved = viewModel.submit()

        // Then
        assertTrue(saved)
        val settings = repository.activeFamily.value?.interestSettings
        assertEquals(true, settings?.enabled)
        assertEquals(250, settings?.annualRateBasisPoints)
        assertEquals(5, settings?.postingDayOfMonth)
        assertEquals("2,50", viewModel.uiState.value.annualRateInput)
    }

    @Test
    fun `FLE-48 enabled interest requires a positive supported annual rate`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()
        repository.createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid"))
        val viewModel = ParentInterestViewModel(repository)

        // When
        viewModel.updateEnabled(true)
        viewModel.updateAnnualRate("0,00")
        val saved = viewModel.submit()

        // Then
        assertFalse(saved)
        assertEquals(ParentInterestError.InvalidRate, viewModel.uiState.value.error)
        assertEquals(false, repository.activeFamily.value?.interestSettings?.enabled)
    }

    @Test
    fun `FLE-48 interest rate parser accepts localized decimals and caps at fifty percent`() {
        // Given / When / Then
        assertEquals(250, parseAnnualRateBasisPoints("2,5"))
        assertEquals(250, parseAnnualRateBasisPoints("2.50"))
        assertEquals(5_000, parseAnnualRateBasisPoints("50,00"))
        assertEquals(null, parseAnnualRateBasisPoints("50,01"))
    }
}
