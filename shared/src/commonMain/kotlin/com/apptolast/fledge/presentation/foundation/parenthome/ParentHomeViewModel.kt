package com.apptolast.fledge.presentation.foundation.parenthome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.fledge.domain.model.BalanceCents
import com.apptolast.fledge.domain.model.CashOutSettlement
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FoundationAction
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.SavingsGoalStatus
import com.apptolast.fledge.domain.model.SettlementId
import com.apptolast.fledge.domain.model.SettlementReminder
import com.apptolast.fledge.domain.model.SettlementStatus
import com.apptolast.fledge.domain.model.SetupAction
import com.apptolast.fledge.domain.model.TaskInstance
import com.apptolast.fledge.domain.model.TaskInstanceId
import com.apptolast.fledge.domain.model.TaskInstanceStatus
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import com.apptolast.fledge.domain.repository.LedgerRepository
import com.apptolast.fledge.domain.repository.MoneyFlowRepository
import com.apptolast.fledge.domain.repository.SavingsGoalRepository
import com.apptolast.fledge.domain.repository.TaskInstanceRepository
import com.apptolast.fledge.domain.service.CashOutProcessor
import com.apptolast.fledge.domain.service.SavingsGoalCompletionNotice
import com.apptolast.fledge.domain.service.SavingsGoalCompletionNotifier
import com.apptolast.fledge.domain.service.SettlementReminderPolicy
import com.apptolast.fledge.domain.service.TaskApprovalProcessor
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

data class ParentHomeUiState(
    val familyName: String = "",
    val children: List<ChildProfile> = emptyList(),
    val mainBalances: Map<ChildProfileId, BalanceCents> = emptyMap(),
    val goalBalances: Map<ChildProfileId, BalanceCents> = emptyMap(),
    val activeSavingsGoalChildIds: Set<ChildProfileId> = emptySet(),
    val goalCompletionNotices: List<SavingsGoalCompletionNotice> = emptyList(),
    val pendingSettlements: List<CashOutSettlement> = emptyList(),
    val settlementReminders: List<SettlementReminder> = emptyList(),
    val pendingTaskApprovals: List<TaskInstance> = emptyList(),
    val approvalAmountInputs: Map<TaskInstanceId, String> = emptyMap(),
    val rejectionReasonInputs: Map<TaskInstanceId, String> = emptyMap(),
    val taskApprovalError: ParentTaskApprovalError? = null,
    val currencyCode: String = "EUR",
    val setupActions: List<SetupAction> = defaultSetupActions,
    val syncNotice: FoundationSyncNotice? = FoundationSyncNotice.Loading,
    val operationError: FoundationOperationError? = null,
    val isBusy: Boolean = false,
)

enum class ParentTaskApprovalError {
    InvalidAmount,
    MissingRejectionReason,
}

class ParentHomeViewModel(
    private val repository: FamilyFoundationRepository,
    private val ledgerRepository: LedgerRepository,
    private val moneyFlowRepository: MoneyFlowRepository,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val taskInstanceRepository: TaskInstanceRepository,
    private val cashOutProcessor: CashOutProcessor,
    private val taskApprovalProcessor: TaskApprovalProcessor,
) : ViewModel() {
    private val savingsGoalCompletionNotifier = SavingsGoalCompletionNotifier()
    private val mutableUiState = MutableStateFlow(
        ParentHomeUiState(
            familyName = repository.activeFamily.value?.name.orEmpty(),
            children = repository.children.value,
            currencyCode = repository.activeFamily.value?.currency?.value ?: "EUR",
        ).withBalances().withGoalCompletionNotices().withTaskApprovals(),
    )
    val uiState: StateFlow<ParentHomeUiState> = mutableUiState

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
            repository.children.collect { children ->
                mutableUiState.update {
                    it.copy(children = children).withBalances().withGoalCompletionNotices()
                }
            }
        }
        viewModelScope.launch {
            repository.activeFamily.collect { family ->
                mutableUiState.update {
                    it.copy(
                        familyName = family?.name.orEmpty(),
                        currencyCode = family?.currency?.value ?: "EUR",
                    ).withGoalCompletionNotices().withTaskApprovals()
                }
            }
        }
        viewModelScope.launch {
            ledgerRepository.transactions.collect {
                mutableUiState.update { it.withBalances().withGoalCompletionNotices() }
            }
        }
        viewModelScope.launch {
            savingsGoalRepository.goals.collect {
                mutableUiState.update { it.withGoalCompletionNotices() }
            }
        }
        viewModelScope.launch {
            moneyFlowRepository.settlements.collect { settlements ->
                mutableUiState.update { it.withSettlements(settlements) }
            }
        }
        viewModelScope.launch {
            taskInstanceRepository.instances.collect {
                mutableUiState.update { state -> state.withTaskApprovals() }
            }
        }
    }

    suspend fun requestProtectedAction(action: FoundationAction): Boolean = runOperation {
        repository.requireParentalGate(action)
    }

    suspend fun markSettlementPaid(settlementId: SettlementId): Boolean = runOperation {
        cashOutProcessor.markPaidByParent(settlementId, Clock.System.now())
        mutableUiState.update { it.withSettlements(moneyFlowRepository.settlements.value) }
    }

    fun updateApprovalAmount(instanceId: TaskInstanceId, input: String) {
        mutableUiState.update { state ->
            state.copy(
                approvalAmountInputs = state.approvalAmountInputs + (instanceId to input),
                taskApprovalError = null,
                operationError = null,
            )
        }
    }

    fun updateRejectionReason(instanceId: TaskInstanceId, input: String) {
        mutableUiState.update { state ->
            state.copy(
                rejectionReasonInputs = state.rejectionReasonInputs + (instanceId to input),
                taskApprovalError = null,
                operationError = null,
            )
        }
    }

    suspend fun approveTask(instanceId: TaskInstanceId): Boolean {
        val state = mutableUiState.value
        val defaultInput = state.pendingTaskApprovals
            .firstOrNull { it.id == instanceId }
            ?.rewardCents
            ?.value
            ?.let(::formatInputCents)
        val amountCents = parseAmountCents(state.approvalAmountInputs[instanceId] ?: defaultInput.orEmpty())
        if (amountCents == null || amountCents <= 0L) {
            mutableUiState.update {
                it.copy(taskApprovalError = ParentTaskApprovalError.InvalidAmount, operationError = null)
            }
            return false
        }

        return runOperation {
            taskApprovalProcessor.approve(
                instanceId = instanceId,
                approvedRewardCents = MoneyCents(amountCents),
                reviewedAt = Clock.System.now(),
            )
            mutableUiState.update { it.withBalances().withGoalCompletionNotices().withTaskApprovals() }
        }
    }

    suspend fun rejectTask(instanceId: TaskInstanceId): Boolean {
        val reason = mutableUiState.value.rejectionReasonInputs[instanceId].orEmpty().trim()
        if (reason.isBlank()) {
            mutableUiState.update {
                it.copy(taskApprovalError = ParentTaskApprovalError.MissingRejectionReason, operationError = null)
            }
            return false
        }

        return runOperation {
            taskApprovalProcessor.reject(
                instanceId = instanceId,
                reason = reason,
                reviewedAt = Clock.System.now(),
            )
            mutableUiState.update { it.withTaskApprovals() }
        }
    }

    private fun ParentHomeUiState.withBalances(): ParentHomeUiState = copy(
        mainBalances = children.associate { child ->
            child.id to ledgerRepository.balancesFor(child.id).main
        },
        goalBalances = children.associate { child ->
            child.id to ledgerRepository.balancesFor(child.id).goal
        },
    )

    private fun ParentHomeUiState.withGoalCompletionNotices(): ParentHomeUiState {
        val goals = savingsGoalRepository.goals.value
        return copy(
            activeSavingsGoalChildIds = goals
                .filter { it.status == SavingsGoalStatus.Active }
                .map { it.childProfileId }
                .toSet(),
            goalCompletionNotices = savingsGoalCompletionNotifier.noticesForParent(
                children = children,
                goals = goals,
                goalBalances = goalBalances,
                now = Clock.System.now(),
            ),
        )
    }

    private fun ParentHomeUiState.withSettlements(settlements: List<CashOutSettlement>): ParentHomeUiState {
        val pending = settlements.filter { it.status != SettlementStatus.ConfirmedByChild }
        return copy(
            pendingSettlements = pending,
            settlementReminders = SettlementReminderPolicy.remindersFor(pending, Clock.System.now()),
        )
    }

    private fun ParentHomeUiState.withTaskApprovals(): ParentHomeUiState {
        val familyId = repository.activeFamily.value?.id
        val pending = familyId?.let { taskInstanceRepository.instancesForFamily(it) }
            .orEmpty()
            .filter { it.status == TaskInstanceStatus.Submitted }
            .sortedWith(compareByDescending<TaskInstance> { it.submittedAt ?: it.updatedAt }.thenBy { it.id.value })
        val pendingIds = pending.map { it.id }.toSet()
        val seededAmountInputs = pending.associate { instance ->
            instance.id to (approvalAmountInputs[instance.id] ?: formatInputCents(instance.rewardCents.value))
        }
        return copy(
            pendingTaskApprovals = pending,
            approvalAmountInputs = seededAmountInputs,
            rejectionReasonInputs = rejectionReasonInputs.filterKeys { it in pendingIds },
            taskApprovalError = taskApprovalError,
        )
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

private fun ParentHomeUiState.hasKnownData(): Boolean = children.isNotEmpty() ||
    goalCompletionNotices.isNotEmpty() ||
    pendingSettlements.isNotEmpty() ||
    pendingTaskApprovals.isNotEmpty() ||
    mainBalances.isNotEmpty()

private fun formatInputCents(value: Long): String {
    val whole = value / 100
    val cents = (value % 100).toString().padStart(2, '0')
    return "$whole,$cents"
}

private val defaultSetupActions = listOf(
    SetupAction(id = "add-child", label = "Anadir hijo"),
    SetupAction(id = "pair-device", label = "Emparejar dispositivo"),
    SetupAction(id = "parental-gate", label = "Configurar parental gate"),
)
