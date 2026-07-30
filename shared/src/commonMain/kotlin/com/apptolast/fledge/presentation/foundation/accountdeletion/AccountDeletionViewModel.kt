package com.apptolast.fledge.presentation.foundation.accountdeletion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.fledge.domain.model.AccountDeletionState
import com.apptolast.fledge.domain.model.AccountDeletionStatus
import com.apptolast.fledge.domain.repository.AccountDeletionRepository
import com.apptolast.fledge.presentation.foundation.FoundationOperationError
import com.apptolast.fledge.presentation.foundation.toFoundationOperationError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val ACCOUNT_DELETION_CONFIRMATION_PHRASE = "ELIMINAR"

data class AccountDeletionUiState(
    val deletionState: AccountDeletionState = AccountDeletionState(),
    val confirmationInput: String = "",
    val isBusy: Boolean = false,
    val operationError: FoundationOperationError? = null,
) {
    val isSubmitted: Boolean
        get() = deletionState.status == AccountDeletionStatus.Requested ||
            deletionState.status == AccountDeletionStatus.Deleting

    val canSubmit: Boolean
        get() = !isBusy &&
            deletionState.canRequest &&
            confirmationInput == ACCOUNT_DELETION_CONFIRMATION_PHRASE
}

class AccountDeletionViewModel(private val repository: AccountDeletionRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        AccountDeletionUiState(deletionState = repository.deletionState.value),
    )
    val uiState: StateFlow<AccountDeletionUiState> = mutableUiState

    init {
        viewModelScope.launch {
            repository.deletionState.collect { deletionState ->
                mutableUiState.update { it.copy(deletionState = deletionState) }
            }
        }
    }

    fun updateConfirmationInput(input: String) {
        mutableUiState.update {
            it.copy(
                confirmationInput = input,
                operationError = null,
            )
        }
    }

    suspend fun submit(): Boolean {
        if (!uiState.value.canSubmit) return false

        mutableUiState.update { it.copy(isBusy = true, operationError = null) }
        return runCatching { repository.requestAccountDeletion() }
            .onSuccess {
                mutableUiState.update { state ->
                    state.copy(
                        deletionState = repository.deletionState.value,
                        isBusy = false,
                        operationError = null,
                    )
                }
            }
            .onFailure { error ->
                mutableUiState.update { state ->
                    state.copy(
                        isBusy = false,
                        operationError = error.toFoundationOperationError(),
                    )
                }
            }
            .isSuccess
    }
}
