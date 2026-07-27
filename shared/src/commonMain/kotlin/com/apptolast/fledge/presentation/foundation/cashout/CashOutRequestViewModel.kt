package com.apptolast.fledge.presentation.foundation.cashout

import androidx.lifecycle.ViewModel
import com.apptolast.fledge.domain.model.CashOutSettlement
import com.apptolast.fledge.domain.model.CashOutSettlementDraft
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.LedgerConcept
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.VirtualAccountType
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import com.apptolast.fledge.domain.repository.LedgerRepository
import com.apptolast.fledge.domain.service.CashOutProcessor
import com.apptolast.fledge.presentation.foundation.manualadjustment.parseAmountCents
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class CashOutRequestUiState(
    val child: ChildProfile? = null,
    val mainBalanceCents: Long = 0,
    val currencyCode: String = "EUR",
    val amountInput: String = "",
    val concept: String = "",
    val error: CashOutRequestError? = null,
    val savedSettlement: CashOutSettlement? = null,
) {
    val canSubmit: Boolean
        get() = child != null && amountInput.isNotBlank() && concept.isNotBlank()
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
    val uiState: StateFlow<CashOutRequestUiState> = mutableUiState

    fun load(childProfileId: ChildProfileId) {
        val child = familyRepository.children.value.firstOrNull { it.id == childProfileId }
        mutableUiState.update { state ->
            state.copy(
                child = child,
                mainBalanceCents = ledgerRepository.balanceFor(childProfileId, VirtualAccountType.Main).value,
                currencyCode = familyRepository.activeFamily.value?.currency?.value ?: "EUR",
                error = if (child == null) CashOutRequestError.MissingChild else null,
            )
        }
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
        val amountCents = parseAmountCents(state.amountInput)

        when {
            family == null -> {
                mutableUiState.update { it.copy(error = CashOutRequestError.MissingFamily) }
                return false
            }
            child == null -> {
                mutableUiState.update { it.copy(error = CashOutRequestError.MissingChild) }
                return false
            }
            concept.isBlank() -> {
                mutableUiState.update { it.copy(error = CashOutRequestError.MissingConcept) }
                return false
            }
            amountCents == null || amountCents <= 0L -> {
                mutableUiState.update { it.copy(error = CashOutRequestError.InvalidAmount) }
                return false
            }
            amountCents > state.mainBalanceCents -> {
                mutableUiState.update { it.copy(error = CashOutRequestError.InsufficientBalance) }
                return false
            }
        }

        val settlement = cashOutProcessor.requestCashOut(
            draft = CashOutSettlementDraft(
                familyId = family.id,
                childProfileId = child.id,
                amountCents = MoneyCents(amountCents),
                concept = LedgerConcept(concept),
            ),
            requestedAt = Clock.System.now(),
        )
        mutableUiState.update { it.copy(error = null, savedSettlement = settlement) }
        return true
    }
}
