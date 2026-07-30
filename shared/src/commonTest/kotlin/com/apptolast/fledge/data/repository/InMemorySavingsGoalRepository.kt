package com.apptolast.fledge.data.repository

import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.SavingsGoal
import com.apptolast.fledge.domain.model.SavingsGoalDraft
import com.apptolast.fledge.domain.model.SavingsGoalId
import com.apptolast.fledge.domain.model.toSavingsGoal
import com.apptolast.fledge.domain.repository.RepositorySyncStatus
import com.apptolast.fledge.domain.repository.SavingsGoalRepository
import com.apptolast.fledge.domain.repository.sortedForSavingsGoals
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class InMemorySavingsGoalRepository(initialGoals: List<SavingsGoal> = emptyList()) : SavingsGoalRepository {
    private var goalCounter = 1
    private val mutableSyncStatus = MutableStateFlow<RepositorySyncStatus>(RepositorySyncStatus.Synced)
    private val mutableGoals = MutableStateFlow(initialGoals.sortedForSavingsGoals())

    override val syncStatus: StateFlow<RepositorySyncStatus> = mutableSyncStatus
    override val goals: StateFlow<List<SavingsGoal>> = mutableGoals

    override suspend fun saveGoal(draft: SavingsGoalDraft, createdAt: Instant): SavingsGoal {
        val goal = draft.toSavingsGoal(
            id = SavingsGoalId("goal-${goalCounter++}"),
            createdAt = createdAt,
        )
        upsert(goal)
        return goal
    }

    override fun goalsForChild(childProfileId: ChildProfileId): List<SavingsGoal> =
        goals.value.filter { it.childProfileId == childProfileId }.sortedForSavingsGoals()

    private fun upsert(goal: SavingsGoal) {
        mutableGoals.value = (goals.value.filterNot { it.id == goal.id } + goal).sortedForSavingsGoals()
    }
}
