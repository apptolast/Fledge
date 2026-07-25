package com.apptolast.fledge.presentation.foundation.childpin

import androidx.lifecycle.ViewModel
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FoundationAction
import com.apptolast.fledge.domain.model.ParentalGateRequest
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class ChildPinUiState(
    val enteredPin: String = "",
    val parentalGateRequest: ParentalGateRequest? = null,
) {
    val requiresParentalGate: Boolean = parentalGateRequest != null
    val canSubmit: Boolean = enteredPin.length == 4
}

class ChildPinViewModel(
    private val repository: FamilyFoundationRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ChildPinUiState())
    val uiState: StateFlow<ChildPinUiState> = mutableUiState

    fun updatePin(value: String) {
        mutableUiState.update { it.copy(enteredPin = value.filter(Char::isDigit).take(4)) }
    }

    suspend fun requestPinReset(childProfileId: ChildProfileId) {
        val request = repository.requireParentalGate(FoundationAction.ResetChildPin(childProfileId))
        mutableUiState.update { it.copy(parentalGateRequest = request) }
    }
}
