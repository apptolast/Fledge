package com.apptolast.fledge.presentation.foundation.cashout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.fledge.domain.model.CashOutSettlement
import com.apptolast.fledge.domain.model.CashOutSettlementDraft
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.LedgerConcept
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.VirtualAccountType
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import com.apptolast.fledge.domain.repository.LedgerRepository
import com.apptolast.fledge.domain.repository.RepositorySyncStatus
import com.apptolast.fledge.domain.service.CashOutProcessor
import com.apptolast.fledge.presentation.foundation.FoundationOperationError
import com.apptolast.fledge.presentation.foundation.FoundationSyncNotice
import com.apptolast.fledge.presentation.foundation.manualadjustment.parseAmountCents
import com.apptolast.fledge.presentation.foundation.toFoundationOperationError
import com.apptolast.fledge.presentation.foundation.toFoundationSyncNotice
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CashOutRequestUiState(
    val child: ChildProfile? = null,
    val mainBalanceCents: Long = 0,
    val currencyCode: String = "EUR",
    val amountInput: String = "",
    val concept: String = "",
    val error: CashOutRequestError? = null,
    val savedSettlement: CashOutSettlement? = null,
    val syncNotice: FoundationSyncNotice? = FoundationSyncNotice.Loading,
    val operationError: FoundationOperationError? = null,
    val isSaving: Boolean = false,
) {
    val canSubmit: Boolean
        get() = !isSaving && child != null && amountInput.isNotBlank() && concept.isNotBlank()
}

enum class CashOutRequestError {
    MissingFamily,
    MissingChild,
    MissingConcept,
    InvalidAmount,
    InsufficientBalance,
}

class CashOutRequestViewModel(
    private val familyRepository: FamilyFoundationRepository,
    private val ledgerRepository: LedgerRepository,
    private val cashOutProcessor: CashOutProcessor,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(CashOutRequestUiState())
    private var loadedChildProfileId: ChildProfileId? = null
    val uiState: StateFlow<CashOutRequestUiState> = mutableUiState

    init {
        viewModelScope.launch {
            combine(familyRepository.syncStatus, ledgerRepository.syncStatus) { familyStatus, ledgerStatus ->
                listOf(familyStatus, ledgerStatus)
            }.collect { statuses ->
                mutableUiState.update { state ->
                    state.copy(syncNotice = statuses.toFoundationSyncNotice(state.child != null))
                }
                refreshSelectedChild()
            }
        }
        viewModelScope.launch {
            familyRepository.children.collect {
                refreshSelectedChild()
            }
        }
        viewModelScope.launch {
            ledgerRepository.transactions.collect {
                refreshSelectedChild()
            }
        }
        viewModelScope.launch {
            familyRepository.activeFamily.collect {
                refreshSelectedChild()
            }
        }
    }

    fun load(childProfileId: ChildProfileId) {
        loadedChildProfileId = childProfileId
        refreshSelectedChild()
    }

    fun updateAmount(input: String) {
        mutableUiState.update { it.copy(amountInput = input, error = null, operationError = null) }
    }

    fun updateConcept(input: String) {
        mutableUiState.update { it.copy(concept = input, error = null, operationError = null) }
    }

    suspend fun submit(): Boolean {
        val state = mutableUiState.value
        val family = familyRepository.activeFamily.value
        val child = state.child
        val concept = state.concept.trim()
        val amountCents = parseAmountCents(state.amountInput)

        when {
            family == null -> {
                mutableUiState.update { it.copy(error = CashOutRequestError.MissingFamily, operationError = null) }
                return false
            }
            child == null -> {
                mutableUiState.update { it.copy(error = CashOutRequestError.MissingChild, operationError = null) }
                return false
            }
            concept.isBlank() -> {
                mutableUiState.update { it.copy(error = CashOutRequestError.MissingConcept, operationError = null) }
                return false
            }
            amountCents == null || amountCents <= 0L -> {
                mutableUiState.update { it.copy(error = CashOutRequestError.InvalidAmount, operationError = null) }
                return false
            }
            amountCents > state.mainBalanceCents -> {
                mutableUiState.update {
                    it.copy(error = CashOutRequestError.InsufficientBalance, operationError = null)
                }
                return false
            }
        }

        mutableUiState.update { it.copy(isSaving = true, operationError = null) }
        return runCatching {
            cashOutProcessor.requestCashOut(
                draft = CashOutSettlementDraft(
                    familyId = family.id,
                    childProfileId = child.id,
                    amountCents = MoneyCents(amountCents),
                    concept = LedgerConcept(concept),
                ),
                requestedAt = Clock.System.now(),
            )
        }.fold(
            onSuccess = { settlement ->
                mutableUiState.update { it.copy(error = null, savedSettlement = settlement, isSaving = false) }
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

    private fun refreshSelectedChild() {
        val childProfileId = loadedChildProfileId ?: return
        val child = familyRepository.children.value.firstOrNull { it.id == childProfileId }
        val shouldShowMissingChild = child == null && familyRepository.syncStatus.value != RepositorySyncStatus.Loading
        mutableUiState.update { state ->
            state.copy(
                child = child,
                mainBalanceCents = ledgerRepository.balanceFor(childProfileId, VirtualAccountType.Main).value,
                currencyCode = familyRepository.activeFamily.value?.currency?.value ?: "EUR",
                error = when {
                    shouldShowMissingChild -> CashOutRequestError.MissingChild
                    state.error == CashOutRequestError.MissingChild -> null
                    else -> state.error
                },
            )
        }
    }
}
