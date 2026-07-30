package com.apptolast.fledge.presentation.foundation.guest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.GuestContributionDraft
import com.apptolast.fledge.domain.model.GuestContributionKind
import com.apptolast.fledge.domain.model.GuestSponsorAccess
import com.apptolast.fledge.domain.model.LedgerConcept
import com.apptolast.fledge.domain.model.LedgerTransaction
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.repository.GuestSponsorRepository
import com.apptolast.fledge.domain.repository.RepositorySyncStatus
import com.apptolast.fledge.presentation.foundation.FoundationOperationError
import com.apptolast.fledge.presentation.foundation.FoundationSyncNotice
import com.apptolast.fledge.presentation.foundation.manualadjustment.parseAmountCents
import com.apptolast.fledge.presentation.foundation.toFoundationOperationError
import com.apptolast.fledge.presentation.foundation.toFoundationSyncNotice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GuestHomeUiState(
    val access: GuestSponsorAccess? = null,
    val selectedChildProfileId: ChildProfileId? = null,
    val kind: GuestContributionKind = GuestContributionKind.Gift,
    val amountInput: String = "",
    val concept: String = "",
    val error: GuestHomeError? = null,
    val savedTransaction: LedgerTransaction? = null,
    val syncNotice: FoundationSyncNotice? = FoundationSyncNotice.Loading,
    val operationError: FoundationOperationError? = null,
    val isSaving: Boolean = false,
) {
    val selectedChild: ChildProfile?
        get() = access?.children?.firstOrNull { it.id == selectedChildProfileId }

    val canSubmit: Boolean
        get() = !isSaving &&
            access != null &&
            selectedChildProfileId != null &&
            amountInput.isNotBlank() &&
            concept.isNotBlank()
}

enum class GuestHomeError {
    MissingAccess,
    MissingChild,
    MissingConcept,
    InvalidAmount,
}

class GuestHomeViewModel(private val guestSponsorRepository: GuestSponsorRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        guestSponsorRepository.activeGuestAccess.value.toGuestHomeUiState(
            syncStatus = guestSponsorRepository.syncStatus.value,
        ),
    )

    val uiState: StateFlow<GuestHomeUiState> = mutableUiState

    init {
        viewModelScope.launch {
            combine(
                guestSponsorRepository.activeGuestAccess,
                guestSponsorRepository.syncStatus,
            ) { access, syncStatus ->
                access to syncStatus
            }.collect { (access, syncStatus) ->
                mutableUiState.update { state ->
                    val selectedChildProfileId = state.selectedChildProfileId
                        ?.takeIf { selected -> access?.children.orEmpty().any { it.id == selected } }
                        ?: access?.children?.firstOrNull()?.id
                    state.copy(
                        access = access,
                        selectedChildProfileId = selectedChildProfileId,
                        syncNotice = listOf(syncStatus).toFoundationSyncNotice(access != null),
                    )
                }
            }
        }
    }

    fun selectChild(childProfileId: ChildProfileId) {
        mutableUiState.update {
            it.copy(
                selectedChildProfileId = childProfileId,
                error = null,
                savedTransaction = null,
                operationError = null,
            )
        }
    }

    fun selectKind(kind: GuestContributionKind) {
        mutableUiState.update { it.copy(kind = kind, error = null, savedTransaction = null, operationError = null) }
    }

    fun updateAmount(input: String) {
        mutableUiState.update {
            it.copy(amountInput = input, error = null, savedTransaction = null, operationError = null)
        }
    }

    fun updateConcept(input: String) {
        mutableUiState.update { it.copy(concept = input, error = null, savedTransaction = null, operationError = null) }
    }

    suspend fun submit(): Boolean {
        val state = mutableUiState.value
        val access = state.access
        val childProfileId = state.selectedChildProfileId
        val amountCents = parseAmountCents(state.amountInput)
        val concept = state.concept.trim()
        when {
            access == null -> {
                mutableUiState.update { it.copy(error = GuestHomeError.MissingAccess, operationError = null) }
                return false
            }
            childProfileId == null -> {
                mutableUiState.update { it.copy(error = GuestHomeError.MissingChild, operationError = null) }
                return false
            }
            concept.isBlank() -> {
                mutableUiState.update { it.copy(error = GuestHomeError.MissingConcept, operationError = null) }
                return false
            }
            amountCents == null || amountCents <= 0L -> {
                mutableUiState.update { it.copy(error = GuestHomeError.InvalidAmount, operationError = null) }
                return false
            }
        }

        mutableUiState.update { it.copy(isSaving = true, operationError = null) }
        return runCatching {
            guestSponsorRepository.contribute(
                GuestContributionDraft(
                    familyId = access.familyId,
                    childProfileId = childProfileId,
                    kind = state.kind,
                    amountCents = MoneyCents(amountCents),
                    concept = LedgerConcept(concept),
                ),
            )
        }.fold(
            onSuccess = { transaction ->
                mutableUiState.update {
                    it.copy(
                        amountInput = "",
                        concept = "",
                        error = null,
                        savedTransaction = transaction,
                        isSaving = false,
                    )
                }
                true
            },
            onFailure = { error ->
                mutableUiState.update {
                    it.copy(isSaving = false, operationError = error.toFoundationOperationError())
                }
                false
            },
        )
    }
}

private fun GuestSponsorAccess?.toGuestHomeUiState(syncStatus: RepositorySyncStatus): GuestHomeUiState =
    GuestHomeUiState(
        access = this,
        selectedChildProfileId = this?.children?.firstOrNull()?.id,
        syncNotice = listOf(syncStatus).toFoundationSyncNotice(this != null),
    )
