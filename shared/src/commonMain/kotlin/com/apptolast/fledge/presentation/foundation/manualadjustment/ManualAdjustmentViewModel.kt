package com.apptolast.fledge.presentation.foundation.manualadjustment

import androidx.lifecycle.ViewModel
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class ManualAdjustmentUiState(
    val child: ChildProfile? = null,
    val kind: ManualAdjustmentKind = ManualAdjustmentKind.Bonus,
    val amountInput: String = "",
    val concept: String = "",
    val error: ManualAdjustmentError? = null,
    val savedTransaction: LedgerTransaction? = null,
) {
    val canSubmit: Boolean
        get() = child != null && amountInput.isNotBlank() && concept.isNotBlank()
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
    val uiState: StateFlow<ManualAdjustmentUiState> = mutableUiState

    fun load(childProfileId: ChildProfileId) {
        val child = familyRepository.children.value.firstOrNull { it.id == childProfileId }
        mutableUiState.update { state ->
            state.copy(
                child = child,
                error = if (child == null) ManualAdjustmentError.MissingChild else null,
            )
        }
    }

    fun selectKind(kind: ManualAdjustmentKind) {
        mutableUiState.update { it.copy(kind = kind, error = null) }
    }

    fun updateAmount(input: String) {
        mutableUiState.update { it.copy(amountInput = input, error = null) }
    }

    fun updateConcept(input: String) {
        mutableUiState.update { it.copy(concept = input, error = null) }
    }

    suspend fun submit(): Boolean {
        val state = mutableUiState.value
        val family = familyRepository.activeFamily.value
        val child = state.child
        val concept = state.concept.trim()
        val absoluteAmountCents = parseAmountCents(state.amountInput)

        when {
            family == null -> {
                mutableUiState.update { it.copy(error = ManualAdjustmentError.MissingFamily) }
                return false
            }
            child == null -> {
                mutableUiState.update { it.copy(error = ManualAdjustmentError.MissingChild) }
                return false
            }
            concept.isBlank() -> {
                mutableUiState.update { it.copy(error = ManualAdjustmentError.MissingConcept) }
                return false
            }
            absoluteAmountCents == null || absoluteAmountCents <= 0L -> {
                mutableUiState.update { it.copy(error = ManualAdjustmentError.InvalidAmount) }
                return false
            }
        }

        val signedAmount = when (state.kind) {
            ManualAdjustmentKind.Bonus,
            ManualAdjustmentKind.Gift,
            -> absoluteAmountCents
            ManualAdjustmentKind.Penalty -> -absoluteAmountCents
        }
        val transaction = ledgerRepository.appendTransaction(
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
        mutableUiState.update { it.copy(error = null, savedTransaction = transaction) }
        return true
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
