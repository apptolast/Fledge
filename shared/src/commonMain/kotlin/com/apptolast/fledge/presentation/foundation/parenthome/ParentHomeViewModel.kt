package com.apptolast.fledge.presentation.foundation.parenthome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.fledge.domain.model.BalanceCents
import com.apptolast.fledge.domain.model.CashOutSettlement
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FoundationAction
import com.apptolast.fledge.domain.model.SettlementId
import com.apptolast.fledge.domain.model.SettlementReminder
import com.apptolast.fledge.domain.model.SettlementStatus
import com.apptolast.fledge.domain.model.SetupAction
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

data class ParentHomeUiState(
    val children: List<ChildProfile> = emptyList(),
    val mainBalances: Map<ChildProfileId, BalanceCents> = emptyMap(),
    val goalBalances: Map<ChildProfileId, BalanceCents> = emptyMap(),
    val pendingSettlements: List<CashOutSettlement> = emptyList(),
    val settlementReminders: List<SettlementReminder> = emptyList(),
    val currencyCode: String = "EUR",
    val setupActions: List<SetupAction> = defaultSetupActions,
)

class ParentHomeViewModel(
    private val repository: FamilyFoundationRepository,
    private val ledgerRepository: LedgerRepository,
    private val moneyFlowRepository: MoneyFlowRepository,
    private val cashOutProcessor: CashOutProcessor,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        ParentHomeUiState(
            children = repository.children.value,
            currencyCode = repository.activeFamily.value?.currency?.value ?: "EUR",
        ).withBalances()
    )
    val uiState: StateFlow<ParentHomeUiState> = mutableUiState

    init {
        viewModelScope.launch {
            repository.children.collect { children ->
                mutableUiState.update {
                    it.copy(children = children).withBalances()
                }
            }
        }
        viewModelScope.launch {
            repository.activeFamily.collect { family ->
                mutableUiState.update { it.copy(currencyCode = family?.currency?.value ?: "EUR") }
            }
        }
        viewModelScope.launch {
            ledgerRepository.transactions.collect {
                mutableUiState.update { it.withBalances() }
            }
        }
        viewModelScope.launch {
            moneyFlowRepository.settlements.collect { settlements ->
                mutableUiState.update { it.withSettlements(settlements) }
            }
        }
    }

    suspend fun requestProtectedAction(action: FoundationAction) {
        repository.requireParentalGate(action)
    }

    suspend fun markSettlementPaid(settlementId: SettlementId) {
        cashOutProcessor.markPaidByParent(settlementId, Clock.System.now())
        mutableUiState.update { it.withSettlements(moneyFlowRepository.settlements.value) }
    }

    private fun ParentHomeUiState.withBalances(): ParentHomeUiState =
        copy(
            mainBalances = children.associate { child ->
                child.id to ledgerRepository.balancesFor(child.id).main
            },
            goalBalances = children.associate { child ->
                child.id to ledgerRepository.balancesFor(child.id).goal
            },
        )

    private fun ParentHomeUiState.withSettlements(
        settlements: List<CashOutSettlement>,
    ): ParentHomeUiState {
        val pending = settlements.filter { it.status != SettlementStatus.ConfirmedByChild }
        return copy(
            pendingSettlements = pending,
            settlementReminders = SettlementReminderPolicy.remindersFor(pending, Clock.System.now()),
        )
    }
}

private val defaultSetupActions = listOf(
    SetupAction(id = "add-child", label = "Anadir hijo"),
    SetupAction(id = "pair-device", label = "Emparejar dispositivo"),
    SetupAction(id = "parental-gate", label = "Configurar parental gate"),
)
