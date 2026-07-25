package com.apptolast.fledge.presentation

import app.cash.turbine.test
import com.apptolast.fledge.data.repository.InMemoryFamilyFoundationRepository
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.presentation.foundation.familysetup.FamilySetupViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class FamilySetupViewModelTest {

    @Test
    fun `AC-03 family setup exposes StateFlow and creates draft`() = runTest {
        // Given
        val viewModel = FamilySetupViewModel(InMemoryFamilyFoundationRepository())

        // When / Then
        viewModel.uiState.test {
            assertFalse(awaitItem().canSubmit)

            viewModel.updateFamilyName("Familia Garcia")
            viewModel.updateCurrency(CurrencyCode("EUR"))
            viewModel.updateTimeZone(TimeZoneId("Europe/Madrid"))

            val ready = awaitItem()
            assertTrue(ready.canSubmit)

            viewModel.submit()
            val created = awaitItem().createdFamily
            assertEquals("Familia Garcia", created?.name)
            assertEquals(CurrencyCode("EUR"), created?.currency)
            assertEquals(TimeZoneId("Europe/Madrid"), created?.timeZone)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
