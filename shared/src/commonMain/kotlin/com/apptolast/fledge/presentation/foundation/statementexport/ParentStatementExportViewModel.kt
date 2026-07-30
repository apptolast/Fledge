package com.apptolast.fledge.presentation.foundation.statementexport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.fledge.domain.model.CashOutSettlement
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.Family
import com.apptolast.fledge.domain.model.LedgerTransaction
import com.apptolast.fledge.domain.model.StatementExportFormat
import com.apptolast.fledge.domain.model.StatementExportResult
import com.apptolast.fledge.domain.model.StatementExportScope
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import com.apptolast.fledge.domain.repository.LedgerRepository
import com.apptolast.fledge.domain.repository.MoneyFlowRepository
import com.apptolast.fledge.domain.service.FamilyStatementExportBuilder
import com.apptolast.fledge.presentation.foundation.FoundationSyncNotice
import com.apptolast.fledge.presentation.foundation.toFoundationSyncNotice
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ParentStatementExportUiState(
    val children: List<ChildProfile> = emptyList(),
    val selectedChildProfileId: ChildProfileId? = null,
    val format: StatementExportFormat = StatementExportFormat.Csv,
    val export: StatementExportResult? = null,
    val syncNotice: FoundationSyncNotice? = FoundationSyncNotice.Loading,
)

class ParentStatementExportViewModel(
    private val familyRepository: FamilyFoundationRepository,
    private val ledgerRepository: LedgerRepository,
    private val moneyFlowRepository: MoneyFlowRepository,
    private val builder: FamilyStatementExportBuilder,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        ParentStatementExportUiState(
            children = familyRepository.children.value,
            export = currentExport(format = StatementExportFormat.Csv, selectedChildProfileId = null),
        ),
    )
    val uiState: StateFlow<ParentStatementExportUiState> = mutableUiState

    init {
        viewModelScope.launch {
            combine(
                familyRepository.syncStatus,
                ledgerRepository.syncStatus,
                moneyFlowRepository.syncStatus,
            ) { foundationStatus, ledgerStatus, moneyStatus ->
                listOf(foundationStatus, ledgerStatus, moneyStatus)
            }.collect { statuses ->
                mutableUiState.update { state ->
                    state.copy(syncNotice = statuses.toFoundationSyncNotice(state.export != null))
                }
            }
        }

        viewModelScope.launch {
            val familyData = combine(familyRepository.activeFamily, familyRepository.children) { family, children ->
                StatementFamilyData(family = family, children = children)
            }
            val activityData = combine(
                ledgerRepository.transactions,
                moneyFlowRepository.settlements,
            ) { transactions, settlements ->
                StatementActivityData(transactions = transactions, settlements = settlements)
            }

            combine(familyData, activityData) { familyData, activity ->
                familyData to activity
            }.collect { (familyData, activity) ->
                mutableUiState.update { state ->
                    val selectedChildId = state.selectedChildProfileId
                        ?.takeIf { childId -> familyData.children.any { it.id == childId } }
                    state.copy(
                        children = familyData.children,
                        selectedChildProfileId = selectedChildId,
                        export = buildExport(
                            family = familyData.family,
                            children = familyData.children,
                            transactions = activity.transactions,
                            settlements = activity.settlements,
                            format = state.format,
                            selectedChildProfileId = selectedChildId,
                        ),
                    )
                }
            }
        }
    }

    fun selectFamilyScope() {
        mutableUiState.update { state ->
            state.copy(
                selectedChildProfileId = null,
                export = currentExport(format = state.format, selectedChildProfileId = null),
            )
        }
    }

    fun selectChildScope(childProfileId: ChildProfileId) {
        mutableUiState.update { state ->
            state.copy(
                selectedChildProfileId = childProfileId,
                export = currentExport(format = state.format, selectedChildProfileId = childProfileId),
            )
        }
    }

    fun selectFormat(format: StatementExportFormat) {
        mutableUiState.update { state ->
            state.copy(
                format = format,
                export = currentExport(format = format, selectedChildProfileId = state.selectedChildProfileId),
            )
        }
    }

    private fun currentExport(
        format: StatementExportFormat,
        selectedChildProfileId: ChildProfileId?,
    ): StatementExportResult? = buildExport(
        family = familyRepository.activeFamily.value,
        children = familyRepository.children.value,
        transactions = ledgerRepository.transactions.value,
        settlements = moneyFlowRepository.settlements.value,
        format = format,
        selectedChildProfileId = selectedChildProfileId,
    )

    private fun buildExport(
        family: Family?,
        children: List<ChildProfile>,
        transactions: List<LedgerTransaction>,
        settlements: List<CashOutSettlement>,
        format: StatementExportFormat,
        selectedChildProfileId: ChildProfileId?,
    ): StatementExportResult? = builder.build(
        family = family,
        children = children,
        transactions = transactions,
        settlements = settlements,
        scope = selectedChildProfileId?.let(StatementExportScope::Child) ?: StatementExportScope.Family,
        format = format,
        generatedAt = Clock.System.now(),
    )
}

private data class StatementFamilyData(val family: Family?, val children: List<ChildProfile>)

private data class StatementActivityData(
    val transactions: List<LedgerTransaction>,
    val settlements: List<CashOutSettlement>,
)
