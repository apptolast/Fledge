package com.apptolast.fledge.domain.service

import com.apptolast.fledge.domain.model.BalanceCents
import com.apptolast.fledge.domain.model.LedgerTransaction
import com.apptolast.fledge.domain.model.SavingsGoal
import com.apptolast.fledge.domain.model.VirtualAccountType
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

data class SavingsGoalProjection(
    val progressPercent: Int,
    val remainingCents: BalanceCents,
    val dailyPaceCents: BalanceCents?,
    val estimatedDaysRemaining: Int?,
    val estimatedCompletionAt: Instant?,
    val status: SavingsGoalProjectionStatus,
)

enum class SavingsGoalProjectionStatus {
    OnTrack,
    NeedsContribution,
    Completed,
}

class SavingsGoalProjectionCalculator {
    fun project(
        goal: SavingsGoal,
        currentGoalBalance: BalanceCents,
        transactions: List<LedgerTransaction>,
        now: Instant,
    ): SavingsGoalProjection {
        val targetCents = goal.targetCents.value
        val currentCents = currentGoalBalance.value.coerceAtLeast(0)
        val remainingCents = (targetCents - currentCents).coerceAtLeast(0)
        val progressPercent = ((currentCents * 100) / targetCents).coerceIn(0, 100).toInt()
        val goalMovements = transactions
            .asSequence()
            .filter { it.familyId == goal.familyId }
            .filter { it.childProfileId == goal.childProfileId }
            .filter { it.accountType == VirtualAccountType.Goal }
            .filter { it.createdAt >= goal.createdAt && it.createdAt <= now }
            .toList()
        val netMovementCents = goalMovements.sumOf { it.amountCents.value }
        val firstMovementAt = goalMovements.minOfOrNull { it.createdAt } ?: goal.createdAt
        val elapsedDays = maxOf(1L, (now - firstMovementAt).inWholeDays)
        val dailyPaceCents = netMovementCents / elapsedDays

        if (remainingCents == 0L) {
            return SavingsGoalProjection(
                progressPercent = progressPercent,
                remainingCents = BalanceCents(0),
                dailyPaceCents = dailyPaceCents.takeIf { it > 0 }?.let(::BalanceCents),
                estimatedDaysRemaining = 0,
                estimatedCompletionAt = now,
                status = SavingsGoalProjectionStatus.Completed,
            )
        }

        if (dailyPaceCents <= 0L) {
            return SavingsGoalProjection(
                progressPercent = progressPercent,
                remainingCents = BalanceCents(remainingCents),
                dailyPaceCents = null,
                estimatedDaysRemaining = null,
                estimatedCompletionAt = null,
                status = SavingsGoalProjectionStatus.NeedsContribution,
            )
        }

        val estimatedDaysRemaining = ceilDiv(remainingCents, dailyPaceCents).coerceAtLeast(1L).toInt()
        return SavingsGoalProjection(
            progressPercent = progressPercent,
            remainingCents = BalanceCents(remainingCents),
            dailyPaceCents = BalanceCents(dailyPaceCents),
            estimatedDaysRemaining = estimatedDaysRemaining,
            estimatedCompletionAt = now.plus(estimatedDaysRemaining.days),
            status = SavingsGoalProjectionStatus.OnTrack,
        )
    }
}

private fun ceilDiv(value: Long, divisor: Long): Long = (value + divisor - 1) / divisor
