package com.apptolast.fledge.presentation.foundation.parentalgate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.fledge.domain.model.FoundationAction
import com.apptolast.fledge.domain.model.ParentalGateRequest
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ParentalGateUiState(
    val challenge: String = "7 + 5",
    val answer: String = "",
    val pendingRequest: ParentalGateRequest? = null,
    val resumedAction: FoundationAction? = null,
    val error: ParentalGateError? = null,
    val confirmed: Boolean = false,
)

enum class ParentalGateError {
    WrongAnswer,
    MissingRequest,
}

class ParentalGateViewModel(
    private val repository: FamilyFoundationRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        ParentalGateUiState(pendingRequest = repository.parentalGateRequest.value),
    )
    val uiState: StateFlow<ParentalGateUiState> = mutableUiState

    init {
        viewModelScope.launch {
            repository.parentalGateRequest.collect { request ->
                mutableUiState.update {
                    if (request == null && it.confirmed) {
                        it.copy(pendingRequest = null)
                    } else {
                        it.copy(
                            pendingRequest = request,
                            resumedAction = null,
                            confirmed = false,
                        )
                    }
                }
            }
        }
    }

    fun requireGate(request: ParentalGateRequest) {
        mutableUiState.update { it.copy(pendingRequest = request, resumedAction = null) }
    }

    fun updateAnswer(value: String) {
        mutableUiState.update {
            it.copy(
                answer = value.filter(Char::isDigit).take(2),
                error = null,
            )
        }
    }

    suspend fun confirmGate(): FoundationAction? {
        val state = mutableUiState.value
        val pendingRequest = state.pendingRequest ?: repository.parentalGateRequest.value
        if (pendingRequest == null) {
            mutableUiState.update { it.copy(error = ParentalGateError.MissingRequest) }
            return null
        }
        if (state.answer != EXPECTED_ANSWER) {
            mutableUiState.update { it.copy(error = ParentalGateError.WrongAnswer) }
            return null
        }

        val action = repository.confirmParentalGate()
        mutableUiState.update {
            it.copy(
                pendingRequest = null,
                resumedAction = action,
                confirmed = action != null,
                error = null,
            )
        }
        return action
    }

    private companion object {
        const val EXPECTED_ANSWER = "12"
    }
}
