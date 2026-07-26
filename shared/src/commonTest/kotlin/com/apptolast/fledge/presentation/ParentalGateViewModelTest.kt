package com.apptolast.fledge.presentation

import com.apptolast.fledge.data.repository.InMemoryFamilyFoundationRepository
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FoundationAction
import com.apptolast.fledge.presentation.foundation.parentalgate.ParentalGateError
import com.apptolast.fledge.presentation.foundation.parentalgate.ParentalGateViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class ParentalGateViewModelTest {

    @Test
    fun `AC-08 protected flow resumes after parental gate`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()
        val pendingAction = FoundationAction.ResetChildPin(ChildProfileId("child-1"))
        repository.requireParentalGate(pendingAction)
        val viewModel = ParentalGateViewModel(repository)

        // When / Then
        assertEquals(null, viewModel.uiState.value.resumedAction)
        assertEquals(pendingAction, viewModel.uiState.value.pendingRequest?.action)

        viewModel.updateAnswer("11")
        assertEquals("11", viewModel.uiState.value.answer)
        assertEquals(null, viewModel.confirmGate())
        assertEquals(ParentalGateError.WrongAnswer, viewModel.uiState.value.error)

        viewModel.updateAnswer("12")
        assertEquals("12", viewModel.uiState.value.answer)
        assertEquals(pendingAction, viewModel.confirmGate())
        assertEquals(pendingAction, viewModel.uiState.value.resumedAction)
    }
}
