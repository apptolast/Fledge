package com.apptolast.fledge.presentation.foundation.parenthome

import com.apptolast.fledge.domain.model.ChildProfileId

enum class ParentHomePrimaryAction {
    AddChild,
    CreateTask,
}

enum class ParentHomeEmptyStateKind {
    FirstRun,
    TaskApprovals,
    SavingsGoal,
    Settlements,
}

enum class ParentHomeEmptyStateAction {
    AddChild,
    CreateTask,
    CreateSavingsGoal,
}

data class ParentHomeEmptyState(
    val kind: ParentHomeEmptyStateKind,
    val action: ParentHomeEmptyStateAction? = null,
    val childProfileId: ChildProfileId? = null,
)

val ParentHomeUiState.primaryAction: ParentHomePrimaryAction
    get() = if (children.isEmpty()) ParentHomePrimaryAction.AddChild else ParentHomePrimaryAction.CreateTask

val ParentHomeUiState.actionableEmptyStates: List<ParentHomeEmptyState>
    get() {
        if (children.isEmpty()) {
            return listOf(
                ParentHomeEmptyState(
                    kind = ParentHomeEmptyStateKind.FirstRun,
                    action = ParentHomeEmptyStateAction.AddChild,
                ),
                ParentHomeEmptyState(kind = ParentHomeEmptyStateKind.TaskApprovals),
                ParentHomeEmptyState(kind = ParentHomeEmptyStateKind.Settlements),
            )
        }

        val states = mutableListOf<ParentHomeEmptyState>()
        if (pendingTaskApprovals.isEmpty()) {
            states += ParentHomeEmptyState(
                kind = ParentHomeEmptyStateKind.TaskApprovals,
                action = ParentHomeEmptyStateAction.CreateTask,
            )
        }

        children.firstOrNull { it.id !in activeSavingsGoalChildIds }?.let { child ->
            states += ParentHomeEmptyState(
                kind = ParentHomeEmptyStateKind.SavingsGoal,
                action = ParentHomeEmptyStateAction.CreateSavingsGoal,
                childProfileId = child.id,
            )
        }

        if (pendingSettlements.isEmpty()) {
            states += ParentHomeEmptyState(kind = ParentHomeEmptyStateKind.Settlements)
        }

        return states
    }
