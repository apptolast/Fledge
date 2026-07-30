package com.apptolast.fledge.presentation.foundation.savingsgoal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.fledge.domain.model.ChildLedgerBalances
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.LedgerActor
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.SavingsGoal
import com.apptolast.fledge.domain.model.SavingsGoalId
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import com.apptolast.fledge.domain.repository.LedgerRepository
import com.apptolast.fledge.domain.repository.LedgerTransferPair
import com.apptolast.fledge.domain.repository.RepositorySyncStatus
import com.apptolast.fledge.domain.repository.SavingsGoalRepository
import com.apptolast.fledge.domain.service.SavingsGoalWithdrawalProcessor
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

data class SavingsGoalWithdrawalUiState(
    val childProfileId: ChildProfileId? = null,
    val goalId: SavingsGoalId? = null,
    val goal: SavingsGoal? = null,
    val balances: ChildLedgerBalances? = null,
    val amountInput: String = DEFAULT_GOAL_WITHDRAWAL_AMOUNT,
    val opportunityCostConfirmed: Boolean = false,
    val currencyCode: String = "EUR",
    val error: SavingsGoalWithdrawalError? = null,
    val savedTransfer: LedgerTransferPair? = null,
    val syncNotice: FoundationSyncNotice? = FoundationSyncNotice.Loading,
    val operationError: FoundationOperationError? = null,
    val isSaving: Boolean = false,
) {
    val canAttemptSubmit: Boolean
        get() = !isSaving && goal != null && childProfileId != null && amountInput.isNotBlank()

    val canSubmit: Boolean
        get() = canAttemptSubmit && opportunityCostConfirmed
}

enum class SavingsGoalWithdrawalError {
    MissingGoal,
    InvalidAmount,
    InsufficientGoalBalance,
    MissingConfirmation,
}

class SavingsGoalWithdrawalViewModel(
    private val familyRepository: FamilyFoundationRepository,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val ledgerRepository: LedgerRepository,
    private val processor: SavingsGoalWithdrawalProcessor,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SavingsGoalWithdrawalUiState())
    val uiState: StateFlow<SavingsGoalWithdrawalUiState> = mutableUiState

    init {
        viewModelScope.launch {
            combine(
                familyRepository.syncStatus,
                savingsGoalRepository.syncStatus,
                ledgerRepository.syncStatus,
            ) { familyStatus, savingsGoalStatus, ledgerStatus ->
                listOf(familyStatus, savingsGoalStatus, ledgerStatus)
            }.collect { statuses ->
                mutableUiState.update { state ->
                    state.copy(
                        syncNotice = statuses.toFoundationSyncNotice(state.goal != null || state.balances != null),
                    )
                }
                refreshGoal()
                refreshBalances()
            }
        }
        viewModelScope.launch {
            familyRepository.activeFamily.collect { family ->
                mutableUiState.update { it.copy(currencyCode = family?.currency?.value ?: "EUR") }
            }
        }
        viewModelScope.launch {
            savingsGoalRepository.goals.collect { refreshGoal() }
        }
        viewModelScope.launch {
            ledgerRepository.transactions.collect { refreshBalances() }
        }
    }

    fun load(childProfileId: ChildProfileId, goalId: SavingsGoalId) {
        mutableUiState.update {
            it.copy(
                childProfileId = childProfileId,
                goalId = goalId,
                error = null,
                operationError = null,
                savedTransfer = null,
            )
        }
        refreshGoal()
        refreshBalances()
    }

    fun updateAmount(input: String) {
        mutableUiState.update {
            it.copy(
                amountInput = input,
                error = null,
                operationError = null,
                savedTransfer = null,
            )
        }
    }

    fun selectQuickAmount(cents: Long) {
        if (cents <= 0L) return
        updateAmount(formatWithdrawalAmountInput(cents))
    }

    fun confirmOpportunityCost(confirmed: Boolean) {
        mutableUiState.update {
            it.copy(
                opportunityCostConfirmed = confirmed,
                error = if (confirmed && it.error == SavingsGoalWithdrawalError.MissingConfirmation) null else it.error,
                operationError = null,
            )
        }
    }

    suspend fun submit(): Boolean {
        val state = mutableUiState.value
        val childProfileId = state.childProfileId
        val goal = state.goal
        val amountCents = parseAmountCents(state.amountInput)

        when {
            childProfileId == null || goal == null -> {
                mutableUiState.update { it.copy(error = SavingsGoalWithdrawalError.MissingGoal, operationError = null) }
                return false
            }
            amountCents == null || amountCents <= 0L -> {
                mutableUiState.update {
                    it.copy(error = SavingsGoalWithdrawalError.InvalidAmount, operationError = null)
                }
                return false
            }
            state.balances.balanceFor(goal.accountType) < amountCents -> {
                mutableUiState.update {
                    it.copy(error = SavingsGoalWithdrawalError.InsufficientGoalBalance, operationError = null)
                }
                return false
            }
            !state.opportunityCostConfirmed -> {
                mutableUiState.update {
                    it.copy(error = SavingsGoalWithdrawalError.MissingConfirmation, operationError = null)
                }
                return false
            }
        }

        mutableUiState.update { it.copy(isSaving = true, operationError = null) }
        return runCatching {
            processor.withdrawFromGoal(
                goalId = goal.id,
                childProfileId = childProfileId,
                amountCents = MoneyCents(amountCents),
                createdBy = LedgerActor.Child,
            )
        }.fold(
            onSuccess = { transfer ->
                refreshBalances()
                mutableUiState.update {
                    it.copy(
                        error = null,
                        savedTransfer = transfer,
                        isSaving = false,
                    )
                }
                true
            },
            onFailure = { error ->
                mutableUiState.update {
                    it.copy(
                        isSaving = false,
                        operationError = error.toFoundationOperationError(),
                    )
                }
                false
            },
        )
    }

    private fun refreshGoal() {
        val goalId = mutableUiState.value.goalId ?: return
        val childProfileId = mutableUiState.value.childProfileId ?: return
        val goal = savingsGoalRepository.goals.value.firstOrNull {
            it.id == goalId &&
                it.childProfileId == childProfileId
        }
        val shouldShowMissingGoal =
            goal == null && savingsGoalRepository.syncStatus.value != RepositorySyncStatus.Loading
        mutableUiState.update { state ->
            state.copy(
                goal = goal,
                error = when {
                    shouldShowMissingGoal -> SavingsGoalWithdrawalError.MissingGoal
                    state.error == SavingsGoalWithdrawalError.MissingGoal -> null
                    else -> state.error
                },
            )
        }
    }

    private fun refreshBalances() {
        val childProfileId = mutableUiState.value.childProfileId ?: return
        mutableUiState.update {
            it.copy(balances = ledgerRepository.balancesFor(childProfileId))
        }
    }
}

const val DEFAULT_GOAL_WITHDRAWAL_AMOUNT = "2,00"

private fun formatWithdrawalAmountInput(cents: Long): String {
    val whole = cents / 100
    val fractional = (cents % 100).toString().padStart(2, '0')
    return "$whole,$fractional"
}
