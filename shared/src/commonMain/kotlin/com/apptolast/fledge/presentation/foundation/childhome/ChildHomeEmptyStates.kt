package com.apptolast.fledge.presentation.foundation.childhome

import com.apptolast.fledge.domain.model.SettlementStatus

enum class ChildHomeEmptyStateKind {
    Tasks,
    SavingsGoal,
    Settlements,
    Ledger,
}

enum class ChildHomeEmptyStateAction {
    OpenParentZone,
}

data class ChildHomeEmptyState(val kind: ChildHomeEmptyStateKind, val action: ChildHomeEmptyStateAction? = null)

val ChildHomeUiState.actionableEmptyStates: List<ChildHomeEmptyState>
    get() = buildList {
        if (taskInstances.isEmpty()) {
            add(
                ChildHomeEmptyState(
                    kind = ChildHomeEmptyStateKind.Tasks,
                    action = ChildHomeEmptyStateAction.OpenParentZone,
                ),
            )
        }
        if (activeSavingsGoal == null) {
            add(
                ChildHomeEmptyState(
                    kind = ChildHomeEmptyStateKind.SavingsGoal,
                    action = ChildHomeEmptyStateAction.OpenParentZone,
                ),
            )
        }
        if (settlements.none { it.status != SettlementStatus.ConfirmedByChild }) {
            add(ChildHomeEmptyState(kind = ChildHomeEmptyStateKind.Settlements))
        }
        if (ledgerTransactions.isEmpty()) {
            add(ChildHomeEmptyState(kind = ChildHomeEmptyStateKind.Ledger))
        }
    }
