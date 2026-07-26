package com.apptolast.fledge.presentation

import app.cash.turbine.test
import com.apptolast.fledge.data.repository.InMemoryFamilyFoundationRepository
import com.apptolast.fledge.domain.model.ChildPin
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.presentation.foundation.pairing.PairingViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class PairingViewModelTest {

    @Test
    fun `AC-07 pairing exposes short lived code for child profile`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()
        val family = repository.createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid"))
        repository.recordVirtualMoneyConsent()
        val child = repository.addChildProfile(family.id, "Lucas", birthYear = 2017, avatarKey = "rocket", pin = ChildPin("1234"))
        val viewModel = PairingViewModel(repository)

        // When / Then
        viewModel.uiState.test {
            assertEquals(null, awaitItem().pairingSession)
            viewModel.startPairing(child.id)
            val session = awaitItem().pairingSession
            assertNotNull(session)
            assertEquals(child.id, session.childProfileId)
            assertEquals(6, session.code.value.length)
            viewModel.updateDeviceLabel("Tablet salon")
            awaitItem()
            viewModel.registerDevice(defaultDeviceLabel = "Dispositivo infantil")
            val paired = awaitItem().pairedDevice
            assertNotNull(paired)
            assertEquals(child.id, paired.childProfileId)
            assertTrue(paired.lastSeenAt >= paired.pairedAt)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
