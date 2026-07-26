package com.apptolast.fledge.presentation.foundation.childhome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.fledge.domain.model.ChildLedgerBalances
import com.apptolast.fledge.domain.model.CashOutSettlement
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FoundationAction
import com.apptolast.fledge.domain.model.LedgerTransaction
import com.apptolast.fledge.domain.model.SettlementReminder
import com.apptolast.fledge.domain.model.SettlementId
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import com.apptolast.fledge.domain.repository.LedgerRepository
import com.apptolast.fledge.domain.repository.MoneyFlowRepository
import com.apptolast.fledge.domain.service.CashOutProcessor
import com.apptolast.fledge.domain.service.SettlementReminderPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

data class ChildHomeUiState(
    val childProfileId: ChildProfileId? = null,
    val balances: ChildLedgerBalances? = null,
    val ledgerTransactions: List<LedgerTransaction> = emptyList(),
    val settlements: List<CashOutSettlement> = emptyList(),
    val settlementReminders: List<SettlementReminder> = emptyList(),
    val currencyCode: String = "EUR",
)

class ChildHomeViewModel(
    private val repository: FamilyFoundationRepository,
    private val ledgerRepository: LedgerRepository,
    private val moneyFlowRepository: MoneyFlowRepository,
    private val cashOutProcessor: CashOutProcessor,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ChildHomeUiState())
    val uiState: StateFlow<ChildHomeUiState> = mutableUiState

    init {
        viewModelScope.launch {
            repository.activeFamily.collect { family ->
                mutableUiState.update { it.copy(currencyCode = family?.currency?.value ?: "EUR") }
            }
        }
        viewModelScope.launch {
            ledgerRepository.transactions.collect {
                refreshMoneyState()
            }
        }
        viewModelScope.launch {
            moneyFlowRepository.settlements.collect {
                refreshMoneyState()
            }
        }
    }

    fun load(childProfileId: ChildProfileId) {
        mutableUiState.update { it.copy(childProfileId = childProfileId) }
        refreshMoneyState()
    }

    suspend fun requestProtectedAction(action: FoundationAction) {
        repository.requireParentalGate(action)
    }

    suspend fun confirmSettlement(settlementId: SettlementId): Boolean {
        cashOutProcessor.confirmByChild(settlementId, Clock.System.now())
        refreshMoneyState()
        return true
    }

    private fun refreshMoneyState() {
        val childProfileId = mutableUiState.value.childProfileId ?: return
        val settlements = moneyFlowRepository.settlementsFor(childProfileId)
        mutableUiState.update {
            it.copy(
                balances = ledgerRepository.balancesFor(childProfileId),
                ledgerTransactions = ledgerRepository.transactionsFor(childProfileId),
                settlements = settlements,
                settlementReminders = SettlementReminderPolicy.remindersFor(settlements, Clock.System.now()),
            )
        }
    }
}
