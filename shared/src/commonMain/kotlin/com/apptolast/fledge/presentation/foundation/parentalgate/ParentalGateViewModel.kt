package com.apptolast.fledge.presentation.foundation.parentalgate

import androidx.lifecycle.ViewModel
import com.apptolast.fledge.domain.model.FoundationAction
import com.apptolast.fledge.domain.model.ParentalGateRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class ParentalGateUiState(
    val pendingRequest: ParentalGateRequest? = null,
    val resumedAction: FoundationAction? = null,
)

class ParentalGateViewModel : ViewModel() {
    private val mutableUiState = MutableStateFlow(ParentalGateUiState())
    val uiState: StateFlow<ParentalGateUiState> = mutableUiState

    fun requireGate(request: ParentalGateRequest) {
        mutableUiState.update { it.copy(pendingRequest = request, resumedAction = null) }
    }

    fun confirmGate() {
        mutableUiState.update {
            it.copy(
                pendingRequest = null,
                resumedAction = it.pendingRequest?.action,
            )
        }
    }
}
