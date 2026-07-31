package com.apptolast.fledge.domain.service

import com.apptolast.fledge.domain.model.ChildAchievementBadgeId
import com.apptolast.fledge.domain.model.ChildAchievementBadgeProgress
import com.apptolast.fledge.domain.model.ChildAchievementSummary
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FIRST_APPROVED_TASK_TARGET
import com.apptolast.fledge.domain.model.THREE_APPROVED_TASKS_TARGET
import com.apptolast.fledge.domain.model.THREE_DAY_STREAK_TARGET
import com.apptolast.fledge.domain.model.TaskInstance
import com.apptolast.fledge.domain.model.TaskInstanceStatus
import com.apptolast.fledge.domain.model.TimeZoneId
import kotlin.time.Instant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class ChildAchievementCalculator {

    fun calculate(
        childProfileId: ChildProfileId,
        taskInstances: List<TaskInstance>,
        timeZoneId: TimeZoneId,
        now: Instant,
    ): ChildAchievementSummary {
        val zone = TimeZone.of(timeZoneId.value)
        val approvedTasks = taskInstances
            .filter { it.childProfileId == childProfileId && it.status == TaskInstanceStatus.Approved }
            .filter { it.reviewedAt != null }
        val approvedDates = approvedTasks
            .mapNotNull { it.reviewedAt?.toLocalDateTime(zone)?.date }
            .distinct()
            .sorted()
        val approvedTaskCount = approvedDates.size
        val bestStreakDays = approvedDates.bestConsecutiveStreak()
        val currentStreakDays = approvedDates.currentStreakEndingAt(now.toLocalDateTime(zone).date)

        return ChildAchievementSummary(
            currentStreakDays = currentStreakDays,
            bestStreakDays = bestStreakDays,
            approvedTaskCount = approvedTaskCount,
            badges = listOf(
                ChildAchievementBadgeProgress(
                    id = ChildAchievementBadgeId.FirstApprovedTask,
                    progress = approvedTaskCount.coerceAtMost(FIRST_APPROVED_TASK_TARGET),
                    target = FIRST_APPROVED_TASK_TARGET,
                ),
                ChildAchievementBadgeProgress(
                    id = ChildAchievementBadgeId.ThreeApprovedTasks,
                    progress = approvedTaskCount.coerceAtMost(THREE_APPROVED_TASKS_TARGET),
                    target = THREE_APPROVED_TASKS_TARGET,
                ),
                ChildAchievementBadgeProgress(
                    id = ChildAchievementBadgeId.ThreeDayStreak,
                    progress = bestStreakDays.coerceAtMost(THREE_DAY_STREAK_TARGET),
                    target = THREE_DAY_STREAK_TARGET,
                ),
            ),
        )
    }

    private fun List<LocalDate>.bestConsecutiveStreak(): Int {
        if (isEmpty()) return 0

        var best = 1
        var current = 1
        zipWithNext().forEach { (previous, next) ->
            if (previous.nextDay() == next) {
                current += 1
            } else {
                current = 1
            }
            best = maxOf(best, current)
        }
        return best
    }

    private fun List<LocalDate>.currentStreakEndingAt(today: LocalDate): Int {
        val dates = toSet()
        val anchor = when {
            today in dates -> today
            today.previousDay() in dates -> today.previousDay()
            else -> return 0
        }

        var currentDate = anchor
        var streak = 0
        while (currentDate in dates) {
            streak += 1
            currentDate = currentDate.previousDay()
        }
        return streak
    }

    private fun LocalDate.nextDay(): LocalDate = plus(DatePeriod(days = 1))

    private fun LocalDate.previousDay(): LocalDate = plus(DatePeriod(days = -1))
}
