package com.apptolast.fledge.presentation

import com.apptolast.fledge.data.repository.InMemoryFamilyFoundationRepository
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.presentation.foundation.match.ParentMatchError
import com.apptolast.fledge.presentation.foundation.match.ParentMatchViewModel
import com.apptolast.fledge.presentation.foundation.match.parseMatchBasisPoints
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ParentMatchViewModelTest {

    @Test
    fun `FLE-51 parent saves configurable match settings`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()
        repository.createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid"))
        val viewModel = ParentMatchViewModel(repository)

        // When
        viewModel.updateEnabled(true)
        viewModel.updateMatchRate("75,50")
        viewModel.updateMaxMatch("4,25")
        val saved = viewModel.submit()

        // Then
        assertTrue(saved)
        val settings = repository.activeFamily.value?.matchSettings
        assertEquals(true, settings?.enabled)
        assertEquals(7_550, settings?.matchBasisPoints)
        assertEquals(425L, settings?.maxMatchCents)
        assertEquals("75,50", viewModel.uiState.value.matchRateInput)
        assertEquals("4,25", viewModel.uiState.value.maxMatchInput)
    }

    @Test
    fun `FLE-51 enabled match requires positive rate and cap`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()
        repository.createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid"))
        val viewModel = ParentMatchViewModel(repository)

        // When
        viewModel.updateEnabled(true)
        viewModel.updateMatchRate("0,00")
        val invalidRateSaved = viewModel.submit()
        viewModel.updateMatchRate("100,00")
        viewModel.updateMaxMatch("0")
        val invalidCapSaved = viewModel.submit()

        // Then
        assertFalse(invalidRateSaved)
        assertFalse(invalidCapSaved)
        assertEquals(ParentMatchError.InvalidCap, viewModel.uiState.value.error)
        assertEquals(false, repository.activeFamily.value?.matchSettings?.enabled)
    }

    @Test
    fun `FLE-51 match rate parser accepts localized decimals and caps at one hundred percent`() {
        // Given / When / Then
        assertEquals(7_550, parseMatchBasisPoints("75,5"))
        assertEquals(7_550, parseMatchBasisPoints("75.50"))
        assertEquals(10_000, parseMatchBasisPoints("100,00"))
        assertEquals(null, parseMatchBasisPoints("100,01"))
    }
}
