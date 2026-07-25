package com.apptolast.fledge.presentation

import app.cash.turbine.test
import com.apptolast.fledge.data.repository.InMemoryFamilyFoundationRepository
import com.apptolast.fledge.domain.model.ChildPin
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.FoundationAction
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.presentation.foundation.childpin.ChildPinViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ChildPinViewModelTest {

    @Test
    fun `AC-06 reset pin requires parental gate`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()
        val family = repository.createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid"))
        val child = repository.addChildProfile(family.id, "Lucas", age = 9, avatarKey = "rocket")
        repository.setChildPin(child.id, ChildPin("1234"))
        val viewModel = ChildPinViewModel(repository)

        // When / Then
        viewModel.uiState.test {
            assertEquals(null, awaitItem().parentalGateRequest)
            viewModel.requestPinReset(child.id)
            val state = awaitItem()
            assertTrue(state.requiresParentalGate)
            assertEquals(FoundationAction.ResetChildPin(child.id), state.parentalGateRequest?.action)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
