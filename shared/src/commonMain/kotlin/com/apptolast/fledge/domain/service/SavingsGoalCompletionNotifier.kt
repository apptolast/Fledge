package com.apptolast.fledge.domain.service

import com.apptolast.fledge.domain.model.BalanceCents
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.SavingsGoal
import com.apptolast.fledge.domain.model.SavingsGoalId
import com.apptolast.fledge.domain.model.SavingsGoalStatus
import com.apptolast.fledge.domain.repository.sortedForSavingsGoals
import kotlin.time.Instant

data class SavingsGoalCompletionNotice(
    val goalId: SavingsGoalId,
    val familyId: FamilyId,
    val childProfileId: ChildProfileId,
    val childName: String,
    val goalTitle: String,
    val currentCents: BalanceCents,
    val targetCents: MoneyCents,
    val completedAt: Instant,
)

class SavingsGoalCompletionNotifier {
    fun noticeForChild(
        child: ChildProfile,
        goal: SavingsGoal?,
        goalBalance: BalanceCents,
        now: Instant,
    ): SavingsGoalCompletionNotice? {
        if (goal == null || goal.childProfileId != child.id || goal.status != SavingsGoalStatus.Active) {
            return null
        }
        if (goalBalance.value < goal.targetCents.value) {
            return null
        }
        return SavingsGoalCompletionNotice(
            goalId = goal.id,
            familyId = goal.familyId,
            childProfileId = child.id,
            childName = child.displayName,
            goalTitle = goal.title,
            currentCents = goalBalance,
            targetCents = goal.targetCents,
            completedAt = now,
        )
    }

    fun noticesForParent(
        children: List<ChildProfile>,
        goals: List<SavingsGoal>,
        goalBalances: Map<ChildProfileId, BalanceCents>,
        now: Instant,
    ): List<SavingsGoalCompletionNotice> = children.mapNotNull { child ->
        val activeGoal = goals
            .filter { it.childProfileId == child.id && it.status == SavingsGoalStatus.Active }
            .sortedForSavingsGoals()
            .firstOrNull()
        noticeForChild(
            child = child,
            goal = activeGoal,
            goalBalance = goalBalances[child.id] ?: BalanceCents(0),
            now = now,
        )
    }.sortedWith(compareBy<SavingsGoalCompletionNotice> { it.childName.lowercase() }.thenBy { it.goalId.value })
}
