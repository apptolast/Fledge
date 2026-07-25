package com.apptolast.fledge.presentation

import app.cash.turbine.test
import com.apptolast.fledge.data.repository.InMemoryFamilyFoundationRepository
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.presentation.foundation.pairing.PairingViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest

class PairingViewModelTest {

    @Test
    fun `AC-07 pairing exposes short lived code for child profile`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()
        val family = repository.createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid"))
        val child = repository.addChildProfile(family.id, "Lucas", age = 9, avatarKey = "rocket")
        val viewModel = PairingViewModel(repository)

        // When / Then
        viewModel.uiState.test {
            assertEquals(null, awaitItem().pairingSession)
            viewModel.startPairing(child.id)
            val session = awaitItem().pairingSession
            assertNotNull(session)
            assertEquals(child.id, session.childProfileId)
            assertEquals(6, session.code.value.length)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
