package com.apptolast.fledge.domain.repository

import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.SavingsGoal
import com.apptolast.fledge.domain.model.SavingsGoalDraft
import com.apptolast.fledge.domain.model.SavingsGoalStatus
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.StateFlow

interface SavingsGoalRepository {
    val syncStatus: StateFlow<RepositorySyncStatus>
    val goals: StateFlow<List<SavingsGoal>>

    suspend fun saveGoal(draft: SavingsGoalDraft, createdAt: Instant = Clock.System.now()): SavingsGoal

    fun goalsForChild(childProfileId: ChildProfileId): List<SavingsGoal>

    fun activeGoalForChild(childProfileId: ChildProfileId): SavingsGoal? = goalsForChild(childProfileId)
        .filter { it.status == SavingsGoalStatus.Active }
        .sortedForSavingsGoals()
        .firstOrNull()
}

fun List<SavingsGoal>.sortedForSavingsGoals(): List<SavingsGoal> = sortedWith(
    compareBy<SavingsGoal> { it.status != SavingsGoalStatus.Active }
        .thenBy { it.createdAt }
        .thenBy { it.id.value },
)
