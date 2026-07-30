package com.apptolast.fledge.domain.service

import com.apptolast.fledge.domain.model.BalanceCents

data class CompoundInterestProjection(
    val annualRateBasisPoints: Int,
    val currentCents: BalanceCents,
    val oneYearCents: BalanceCents,
    val threeYearsCents: BalanceCents,
    val oneYearGainCents: BalanceCents,
    val threeYearsGainCents: BalanceCents,
    val explanationLevel: CompoundInterestExplanationLevel,
)

enum class CompoundInterestExplanationLevel {
    Younger,
    Older,
}

class CompoundInterestProjectionCalculator {
    fun project(
        mainBalance: BalanceCents,
        annualRateBasisPoints: Int,
        birthYear: Int,
        currentYear: Int,
    ): CompoundInterestProjection? {
        val principalCents = mainBalance.value.coerceAtLeast(0)
        if (principalCents == 0L || annualRateBasisPoints <= 0) return null

        val oneYearCents = compoundMonthly(principalCents, annualRateBasisPoints, months = 12)
        val threeYearsCents = compoundMonthly(principalCents, annualRateBasisPoints, months = 36)
        val age = (currentYear - birthYear).coerceAtLeast(0)
        return CompoundInterestProjection(
            annualRateBasisPoints = annualRateBasisPoints,
            currentCents = BalanceCents(principalCents),
            oneYearCents = BalanceCents(oneYearCents),
            threeYearsCents = BalanceCents(threeYearsCents),
            oneYearGainCents = BalanceCents(oneYearCents - principalCents),
            threeYearsGainCents = BalanceCents(threeYearsCents - principalCents),
            explanationLevel = if (age <= YOUNGER_CHILD_MAX_AGE) {
                CompoundInterestExplanationLevel.Younger
            } else {
                CompoundInterestExplanationLevel.Older
            },
        )
    }

    private fun compoundMonthly(principalCents: Long, annualRateBasisPoints: Int, months: Int): Long {
        var balanceCents = principalCents
        repeat(months) {
            balanceCents += (balanceCents * annualRateBasisPoints) / 10_000 / 12
        }
        return balanceCents
    }
}

private const val YOUNGER_CHILD_MAX_AGE = 9
