package com.apptolast.fledge.presentation

import app.cash.turbine.test
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FoundationAction
import com.apptolast.fledge.domain.model.ParentalGateRequest
import com.apptolast.fledge.presentation.foundation.parentalgate.ParentalGateViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class ParentalGateViewModelTest {

    @Test
    fun `AC-08 protected flow resumes after parental gate`() = runTest {
        // Given
        val pendingAction = FoundationAction.ResetChildPin(ChildProfileId("child-1"))
        val request = ParentalGateRequest(action = pendingAction)
        val viewModel = ParentalGateViewModel()

        // When / Then
        viewModel.uiState.test {
            assertEquals(null, awaitItem().resumedAction)
            viewModel.requireGate(request)
            assertEquals(pendingAction, awaitItem().pendingRequest?.action)
            viewModel.confirmGate()
            assertEquals(pendingAction, awaitItem().resumedAction)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
