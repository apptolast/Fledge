package com.apptolast.fledge.presentation.foundation.childpin

import androidx.lifecycle.ViewModel
import com.apptolast.fledge.domain.model.ChildPin
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class ChildPinResetUiState(
    val newPin: String = "",
    val saved: Boolean = false,
    val error: ChildPinResetError? = null,
) {
    val canSubmit: Boolean = newPin.matches(Regex("\\d{4}"))
}

enum class ChildPinResetError {
    InvalidPin,
}

class ChildPinResetViewModel(private val repository: FamilyFoundationRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ChildPinResetUiState())
    val uiState: StateFlow<ChildPinResetUiState> = mutableUiState

    fun updatePin(value: String) {
        mutableUiState.update {
            it.copy(
                newPin = value.filter(Char::isDigit).take(4),
                error = null,
                saved = false,
            )
        }
    }

    suspend fun submit(childProfileId: ChildProfileId): Boolean {
        val state = mutableUiState.value
        if (!state.canSubmit) {
            mutableUiState.update { it.copy(error = ChildPinResetError.InvalidPin) }
            return false
        }

        repository.setChildPin(childProfileId, ChildPin(state.newPin))
        mutableUiState.update { it.copy(saved = true, error = null) }
        return true
    }
}
