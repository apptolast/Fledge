package com.apptolast.fledge.presentation.foundation.childpin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.fledge.domain.model.ChildPin
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.ChildSession
import com.apptolast.fledge.domain.model.FoundationAction
import com.apptolast.fledge.domain.model.ParentalGateRequest
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChildPinUiState(
    val enteredPin: String = "",
    val timeoutMinutes: Int = 15,
    val childSession: ChildSession? = null,
    val parentalGateRequest: ParentalGateRequest? = null,
    val error: ChildPinError? = null,
) {
    val requiresParentalGate: Boolean = parentalGateRequest != null
    val canSubmit: Boolean = enteredPin.length == 4
}

enum class ChildPinError {
    InvalidPin,
}

class ChildPinViewModel(
    private val repository: FamilyFoundationRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ChildPinUiState())
    val uiState: StateFlow<ChildPinUiState> = mutableUiState

    init {
        viewModelScope.launch {
            repository.childPinPolicy.collect { policy ->
                mutableUiState.update { it.copy(timeoutMinutes = policy.timeoutMinutes) }
            }
        }
    }

    fun updatePin(value: String) {
        mutableUiState.update {
            it.copy(
                enteredPin = value.filter(Char::isDigit).take(4),
                error = null,
            )
        }
    }

    suspend fun unlock(childProfileId: ChildProfileId): Boolean {
        val state = mutableUiState.value
        if (!state.canSubmit) {
            mutableUiState.update { it.copy(error = ChildPinError.InvalidPin) }
            return false
        }

        val session = repository.validateChildPin(childProfileId, ChildPin(state.enteredPin))
        mutableUiState.update {
            it.copy(
                childSession = session,
                error = if (session == null) ChildPinError.InvalidPin else null,
            )
        }
        return session != null
    }

    suspend fun requestPinReset(childProfileId: ChildProfileId) {
        val request = repository.requireParentalGate(FoundationAction.ResetChildPin(childProfileId))
        mutableUiState.update { it.copy(parentalGateRequest = request) }
    }
}
