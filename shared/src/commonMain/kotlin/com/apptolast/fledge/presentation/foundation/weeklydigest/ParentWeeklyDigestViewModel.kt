package com.apptolast.fledge.presentation.foundation.weeklydigest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.fledge.domain.model.CashOutSettlement
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.Family
import com.apptolast.fledge.domain.model.LedgerTransaction
import com.apptolast.fledge.domain.model.SavingsGoal
import com.apptolast.fledge.domain.model.TaskInstance
import com.apptolast.fledge.domain.model.WeeklyParentDigest
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import com.apptolast.fledge.domain.repository.LedgerRepository
import com.apptolast.fledge.domain.repository.MoneyFlowRepository
import com.apptolast.fledge.domain.repository.SavingsGoalRepository
import com.apptolast.fledge.domain.repository.TaskInstanceRepository
import com.apptolast.fledge.domain.service.WeeklyParentDigestCalculator
import com.apptolast.fledge.presentation.foundation.FoundationSyncNotice
import com.apptolast.fledge.presentation.foundation.toFoundationSyncNotice
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ParentWeeklyDigestUiState(
    val digest: WeeklyParentDigest? = null,
    val syncNotice: FoundationSyncNotice? = FoundationSyncNotice.Loading,
)

class ParentWeeklyDigestViewModel(
    private val familyRepository: FamilyFoundationRepository,
    private val taskInstanceRepository: TaskInstanceRepository,
    private val ledgerRepository: LedgerRepository,
    private val moneyFlowRepository: MoneyFlowRepository,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val calculator: WeeklyParentDigestCalculator,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ParentWeeklyDigestUiState(digest = currentDigest()))
    val uiState: StateFlow<ParentWeeklyDigestUiState> = mutableUiState

    init {
        viewModelScope.launch {
            combine(
                familyRepository.syncStatus,
                taskInstanceRepository.syncStatus,
                ledgerRepository.syncStatus,
                moneyFlowRepository.syncStatus,
                savingsGoalRepository.syncStatus,
            ) { foundationStatus, taskStatus, ledgerStatus, moneyStatus, savingsStatus ->
                listOf(foundationStatus, taskStatus, ledgerStatus, moneyStatus, savingsStatus)
            }.collect { statuses ->
                mutableUiState.update { state ->
                    state.copy(syncNotice = statuses.toFoundationSyncNotice(state.digest != null))
                }
            }
        }

        viewModelScope.launch {
            val familyData = combine(familyRepository.activeFamily, familyRepository.children) { family, children ->
                DigestFamilyData(family = family, children = children)
            }
            val activityData = combine(
                taskInstanceRepository.instances,
                ledgerRepository.transactions,
                moneyFlowRepository.settlements,
                savingsGoalRepository.goals,
            ) { tasks, transactions, settlements, goals ->
                DigestActivityData(
                    tasks = tasks,
                    transactions = transactions,
                    settlements = settlements,
                    goals = goals,
                )
            }

            combine(familyData, activityData) { family, activity ->
                calculator.calculate(
                    family = family.family,
                    children = family.children,
                    taskInstances = activity.tasks,
                    transactions = activity.transactions,
                    settlements = activity.settlements,
                    goals = activity.goals,
                    now = Clock.System.now(),
                )
            }.collect { digest ->
                mutableUiState.update { it.copy(digest = digest) }
            }
        }
    }

    private fun currentDigest(): WeeklyParentDigest? = calculator.calculate(
        family = familyRepository.activeFamily.value,
        children = familyRepository.children.value,
        taskInstances = taskInstanceRepository.instances.value,
        transactions = ledgerRepository.transactions.value,
        settlements = moneyFlowRepository.settlements.value,
        goals = savingsGoalRepository.goals.value,
        now = Clock.System.now(),
    )
}

private data class DigestFamilyData(val family: Family?, val children: List<ChildProfile>)

private data class DigestActivityData(
    val tasks: List<TaskInstance>,
    val transactions: List<LedgerTransaction>,
    val settlements: List<CashOutSettlement>,
    val goals: List<SavingsGoal>,
)
