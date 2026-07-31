package com.apptolast.fledge.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ChildAchievementBadgeId {
    FirstApprovedTask,
    ThreeApprovedTasks,
    ThreeDayStreak,
}

@Serializable
data class ChildAchievementBadgeProgress(val id: ChildAchievementBadgeId, val progress: Int, val target: Int) {
    init {
        require(progress >= 0) { "Badge progress cannot be negative." }
        require(target > 0) { "Badge target must be positive." }
    }

    val unlocked: Boolean
        get() = progress >= target
}

@Serializable
data class ChildAchievementSummary(
    val currentStreakDays: Int,
    val bestStreakDays: Int,
    val approvedTaskCount: Int,
    val badges: List<ChildAchievementBadgeProgress>,
) {
    init {
        require(currentStreakDays >= 0) { "Current streak cannot be negative." }
        require(bestStreakDays >= 0) { "Best streak cannot be negative." }
        require(approvedTaskCount >= 0) { "Approved task count cannot be negative." }
    }

    val hasApprovedActivity: Boolean
        get() = approvedTaskCount > 0

    companion object {
        fun empty(): ChildAchievementSummary = ChildAchievementSummary(
            currentStreakDays = 0,
            bestStreakDays = 0,
            approvedTaskCount = 0,
            badges = listOf(
                ChildAchievementBadgeProgress(
                    id = ChildAchievementBadgeId.FirstApprovedTask,
                    progress = 0,
                    target = FIRST_APPROVED_TASK_TARGET,
                ),
                ChildAchievementBadgeProgress(
                    id = ChildAchievementBadgeId.ThreeApprovedTasks,
                    progress = 0,
                    target = THREE_APPROVED_TASKS_TARGET,
                ),
                ChildAchievementBadgeProgress(
                    id = ChildAchievementBadgeId.ThreeDayStreak,
                    progress = 0,
                    target = THREE_DAY_STREAK_TARGET,
                ),
            ),
        )
    }
}

const val FIRST_APPROVED_TASK_TARGET = 1
const val THREE_APPROVED_TASKS_TARGET = 3
const val THREE_DAY_STREAK_TARGET = 3
