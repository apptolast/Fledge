package com.apptolast.fledge.presentation.foundation.childhome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.fledge.domain.model.CashOutSettlement
import com.apptolast.fledge.domain.model.ChildLedgerBalances
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FoundationAction
import com.apptolast.fledge.domain.model.LedgerTransaction
import com.apptolast.fledge.domain.model.SavingsGoal
import com.apptolast.fledge.domain.model.SettlementId
import com.apptolast.fledge.domain.model.SettlementReminder
import com.apptolast.fledge.domain.model.TaskInstance
import com.apptolast.fledge.domain.model.TaskInstanceId
import com.apptolast.fledge.domain.model.TaskInstanceStatus
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import com.apptolast.fledge.domain.repository.LedgerRepository
import com.apptolast.fledge.domain.repository.MoneyFlowRepository
import com.apptolast.fledge.domain.repository.SavingsGoalRepository
import com.apptolast.fledge.domain.repository.TaskInstanceRepository
import com.apptolast.fledge.domain.service.CashOutProcessor
import com.apptolast.fledge.domain.service.SettlementReminderPolicy
import com.apptolast.fledge.presentation.foundation.FoundationOperationError
import com.apptolast.fledge.presentation.foundation.FoundationSyncNotice
import com.apptolast.fledge.presentation.foundation.toFoundationOperationError
import com.apptolast.fledge.presentation.foundation.toFoundationSyncNotice
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChildHomeUiState(
    val childProfileId: ChildProfileId? = null,
    val balances: ChildLedgerBalances? = null,
    val ledgerTransactions: List<LedgerTransaction> = emptyList(),
    val settlements: List<CashOutSettlement> = emptyList(),
    val settlementReminders: List<SettlementReminder> = emptyList(),
    val taskInstances: List<TaskInstance> = emptyList(),
    val activeSavingsGoal: SavingsGoal? = null,
    val selectedPhotoEvidenceByTaskId: Map<TaskInstanceId, String> = emptyMap(),
    val currencyCode: String = "EUR",
    val syncNotice: FoundationSyncNotice? = FoundationSyncNotice.Loading,
    val operationError: FoundationOperationError? = null,
    val taskSubmissionError: ChildTaskSubmissionError? = null,
    val isBusy: Boolean = false,
)

enum class ChildTaskSubmissionError {
    MissingPhotoEvidence,
    SubmitFailed,
}

class ChildHomeViewModel(
    private val repository: FamilyFoundationRepository,
    private val ledgerRepository: LedgerRepository,
    private val moneyFlowRepository: MoneyFlowRepository,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val taskInstanceRepository: TaskInstanceRepository,
    private val cashOutProcessor: CashOutProcessor,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ChildHomeUiState())
    val uiState: StateFlow<ChildHomeUiState> = mutableUiState

    init {
        viewModelScope.launch {
            combine(
                repository.syncStatus,
                ledgerRepository.syncStatus,
                moneyFlowRepository.syncStatus,
                savingsGoalRepository.syncStatus,
                taskInstanceRepository.syncStatus,
            ) { foundationStatus, ledgerStatus, moneyStatus, savingsGoalStatus, taskStatus ->
                listOf(foundationStatus, ledgerStatus, moneyStatus, savingsGoalStatus, taskStatus)
            }.collect { statuses ->
                mutableUiState.update { state ->
                    state.copy(syncNotice = statuses.toFoundationSyncNotice(state.hasKnownData()))
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
                refreshMoneyState()
            }
        }
        viewModelScope.launch {
            moneyFlowRepository.settlements.collect {
                refreshMoneyState()
            }
        }
        viewModelScope.launch {
            savingsGoalRepository.goals.collect {
                refreshGoalState()
            }
        }
        viewModelScope.launch {
            taskInstanceRepository.instances.collect {
                refreshTaskState()
            }
        }
    }

    fun load(childProfileId: ChildProfileId) {
        mutableUiState.update { it.copy(childProfileId = childProfileId) }
        refreshMoneyState()
        refreshGoalState()
        refreshTaskState()
    }

    fun attachPhotoEvidence(instanceId: TaskInstanceId, photoEvidenceUri: String) {
        val normalized = photoEvidenceUri.trim()
        mutableUiState.update { state ->
            val updatedEvidence = if (normalized.isBlank()) {
                state.selectedPhotoEvidenceByTaskId - instanceId
            } else {
                state.selectedPhotoEvidenceByTaskId + (instanceId to normalized)
            }
            state.copy(
                selectedPhotoEvidenceByTaskId = updatedEvidence,
                taskSubmissionError = null,
                operationError = null,
            )
        }
    }

    suspend fun requestProtectedAction(action: FoundationAction): Boolean = runOperation {
        repository.requireParentalGate(action)
    }

    suspend fun confirmSettlement(settlementId: SettlementId): Boolean = runOperation {
        cashOutProcessor.confirmByChild(settlementId, Clock.System.now())
        refreshMoneyState()
    }

    suspend fun submitTask(instanceId: TaskInstanceId): Boolean {
        val state = mutableUiState.value
        val childProfileId = state.childProfileId ?: return false
        val instance = state.taskInstances.firstOrNull { it.id == instanceId } ?: return false
        val photoEvidenceUri = state.selectedPhotoEvidenceByTaskId[instanceId]
        if (instance.requiresPhoto && photoEvidenceUri.isNullOrBlank()) {
            mutableUiState.update {
                it.copy(
                    taskSubmissionError = ChildTaskSubmissionError.MissingPhotoEvidence,
                    operationError = null,
                )
            }
            return false
        }

        mutableUiState.update {
            it.copy(isBusy = true, taskSubmissionError = null, operationError = null)
        }
        return runCatching {
            taskInstanceRepository.submitForReview(
                instanceId = instanceId,
                childProfileId = childProfileId,
                photoEvidenceUri = photoEvidenceUri,
                submittedAt = Clock.System.now(),
            )
            refreshTaskState()
        }.onSuccess {
            mutableUiState.update {
                it.copy(
                    isBusy = false,
                    taskSubmissionError = null,
                    selectedPhotoEvidenceByTaskId = it.selectedPhotoEvidenceByTaskId - instanceId,
                )
            }
        }.onFailure {
            mutableUiState.update {
                it.copy(
                    isBusy = false,
                    taskSubmissionError = ChildTaskSubmissionError.SubmitFailed,
                )
            }
        }.isSuccess
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

    private fun refreshTaskState() {
        val childProfileId = mutableUiState.value.childProfileId ?: return
        mutableUiState.update {
            it.copy(taskInstances = taskInstanceRepository.instancesForChild(childProfileId).sortedForChildHome())
        }
    }

    private fun refreshGoalState() {
        val childProfileId = mutableUiState.value.childProfileId ?: return
        mutableUiState.update {
            it.copy(activeSavingsGoal = savingsGoalRepository.activeGoalForChild(childProfileId))
        }
    }

    private suspend fun runOperation(block: suspend () -> Unit): Boolean {
        mutableUiState.update { it.copy(isBusy = true, operationError = null) }
        return runCatching { block() }
            .onSuccess {
                mutableUiState.update { state -> state.copy(isBusy = false, operationError = null) }
            }
            .onFailure { error ->
                mutableUiState.update { state ->
                    state.copy(isBusy = false, operationError = error.toFoundationOperationError())
                }
            }
            .isSuccess
    }
}

private fun ChildHomeUiState.hasKnownData(): Boolean = balances != null ||
    ledgerTransactions.isNotEmpty() ||
    settlements.isNotEmpty() ||
    taskInstances.isNotEmpty() ||
    activeSavingsGoal != null

private fun List<TaskInstance>.sortedForChildHome(): List<TaskInstance> =
    sortedWith(compareBy<TaskInstance> { it.status.childHomeSortOrder }.thenBy { it.dueAt }.thenBy { it.id.value })

private val TaskInstanceStatus.childHomeSortOrder: Int
    get() = when (this) {
        TaskInstanceStatus.Pending -> 0
        TaskInstanceStatus.Rejected -> 1
        TaskInstanceStatus.Submitted -> 2
        TaskInstanceStatus.Approved -> 3
        TaskInstanceStatus.Expired -> 4
    }
