package com.apptolast.fledge.presentation.foundation.manualadjustment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.LedgerActor
import com.apptolast.fledge.domain.model.LedgerConcept
import com.apptolast.fledge.domain.model.LedgerTransaction
import com.apptolast.fledge.domain.model.LedgerTransactionDraft
import com.apptolast.fledge.domain.model.LedgerTransactionType
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.VirtualAccountType
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import com.apptolast.fledge.domain.repository.LedgerRepository
import com.apptolast.fledge.domain.repository.RepositorySyncStatus
import com.apptolast.fledge.presentation.foundation.FoundationOperationError
import com.apptolast.fledge.presentation.foundation.FoundationSyncNotice
import com.apptolast.fledge.presentation.foundation.toFoundationOperationError
import com.apptolast.fledge.presentation.foundation.toFoundationSyncNotice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ManualAdjustmentUiState(
    val child: ChildProfile? = null,
    val kind: ManualAdjustmentKind = ManualAdjustmentKind.Bonus,
    val amountInput: String = "",
    val concept: String = "",
    val error: ManualAdjustmentError? = null,
    val savedTransaction: LedgerTransaction? = null,
    val syncNotice: FoundationSyncNotice? = FoundationSyncNotice.Loading,
    val operationError: FoundationOperationError? = null,
    val isSaving: Boolean = false,
) {
    val canSubmit: Boolean
        get() = !isSaving && child != null && amountInput.isNotBlank() && concept.isNotBlank()
}

enum class ManualAdjustmentKind {
    Bonus,
    Penalty,
    Gift,
}

enum class ManualAdjustmentError {
    MissingFamily,
    MissingChild,
    MissingConcept,
    InvalidAmount,
}

class ManualAdjustmentViewModel(
    private val familyRepository: FamilyFoundationRepository,
    private val ledgerRepository: LedgerRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ManualAdjustmentUiState())
    private var loadedChildProfileId: ChildProfileId? = null
    val uiState: StateFlow<ManualAdjustmentUiState> = mutableUiState

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
    }

    fun load(childProfileId: ChildProfileId) {
        loadedChildProfileId = childProfileId
        refreshSelectedChild()
    }

    fun selectKind(kind: ManualAdjustmentKind) {
        mutableUiState.update { it.copy(kind = kind, error = null, operationError = null) }
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
        val absoluteAmountCents = parseAmountCents(state.amountInput)

        when {
            family == null -> {
                mutableUiState.update { it.copy(error = ManualAdjustmentError.MissingFamily, operationError = null) }
                return false
            }
            child == null -> {
                mutableUiState.update { it.copy(error = ManualAdjustmentError.MissingChild, operationError = null) }
                return false
            }
            concept.isBlank() -> {
                mutableUiState.update { it.copy(error = ManualAdjustmentError.MissingConcept, operationError = null) }
                return false
            }
            absoluteAmountCents == null || absoluteAmountCents <= 0L -> {
                mutableUiState.update { it.copy(error = ManualAdjustmentError.InvalidAmount, operationError = null) }
                return false
            }
        }

        val signedAmount = when (state.kind) {
            ManualAdjustmentKind.Bonus,
            ManualAdjustmentKind.Gift,
            -> absoluteAmountCents
            ManualAdjustmentKind.Penalty -> -absoluteAmountCents
        }
        mutableUiState.update { it.copy(isSaving = true, operationError = null) }
        return runCatching {
            ledgerRepository.appendTransaction(
                LedgerTransactionDraft(
                    familyId = family.id,
                    childProfileId = child.id,
                    accountType = VirtualAccountType.Main,
                    type = state.kind.toTransactionType(),
                    amountCents = MoneyCents(signedAmount),
                    concept = LedgerConcept(concept),
                    createdBy = LedgerActor.Parent,
                ),
            )
        }.fold(
            onSuccess = { transaction ->
                mutableUiState.update {
                    it.copy(error = null, savedTransaction = transaction, isSaving = false)
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

    private fun refreshSelectedChild() {
        val childProfileId = loadedChildProfileId ?: return
        val child = familyRepository.children.value.firstOrNull { it.id == childProfileId }
        val shouldShowMissingChild = child == null && familyRepository.syncStatus.value != RepositorySyncStatus.Loading
        mutableUiState.update { state ->
            state.copy(
                child = child,
                syncNotice = listOf(
                    familyRepository.syncStatus.value,
                    ledgerRepository.syncStatus.value,
                ).toFoundationSyncNotice(child != null),
                error = when {
                    shouldShowMissingChild -> ManualAdjustmentError.MissingChild
                    state.error == ManualAdjustmentError.MissingChild -> null
                    else -> state.error
                },
            )
        }
    }
}

internal fun parseAmountCents(input: String): Long? {
    val normalized = input.trim().replace(',', '.')
    if (normalized.isBlank() || normalized.startsWith("-")) return null

    val parts = normalized.split(".")
    if (parts.size > 2) return null

    val whole = parts.getOrNull(0)?.takeIf { it.isNotBlank() && it.all(Char::isDigit) } ?: return null
    val cents = parts.getOrNull(1)?.let { fractional ->
        if (fractional.length > 2 || !fractional.all(Char::isDigit)) return null
        fractional.padEnd(2, '0')
    } ?: "00"

    val wholeCents = whole.toLongOrNull()?.let { value ->
        value.takeIf { it <= Long.MAX_VALUE / 100 }?.times(100)
    } ?: return null

    return wholeCents + cents.toLong()
}

private fun ManualAdjustmentKind.toTransactionType(): LedgerTransactionType = when (this) {
    ManualAdjustmentKind.Bonus -> LedgerTransactionType.Bonus
    ManualAdjustmentKind.Penalty -> LedgerTransactionType.Penalty
    ManualAdjustmentKind.Gift -> LedgerTransactionType.Gift
}
