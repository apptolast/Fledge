package com.apptolast.fledge.domain

import com.apptolast.fledge.domain.model.BalanceCents
import com.apptolast.fledge.domain.service.CompoundInterestExplanationLevel
import com.apptolast.fledge.domain.service.CompoundInterestProjectionCalculator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CompoundInterestProjectionCalculatorTest {
    private val calculator = CompoundInterestProjectionCalculator()

    @Test
    fun `FLE-49 AC-04 projection compounds monthly and rounds down to whole cents`() {
        // Given
        val balance = BalanceCents(10_000)

        // When
        val projection = calculator.project(
            mainBalance = balance,
            annualRateBasisPoints = 1_200,
            birthYear = 2012,
            currentYear = 2026,
        )

        // Then
        assertEquals(BalanceCents(10_000), projection?.currentCents)
        assertEquals(BalanceCents(11_266), projection?.oneYearCents)
        assertEquals(BalanceCents(14_292), projection?.threeYearsCents)
        assertEquals(BalanceCents(1_266), projection?.oneYearGainCents)
        assertEquals(BalanceCents(4_292), projection?.threeYearsGainCents)
    }

    @Test
    fun `FLE-49 AC-03 projection selects age appropriate explanation`() {
        // Given
        val balance = BalanceCents(2_000)

        // When
        val younger = calculator.project(
            mainBalance = balance,
            annualRateBasisPoints = 333,
            birthYear = 2018,
            currentYear = 2026,
        )
        val older = calculator.project(
            mainBalance = balance,
            annualRateBasisPoints = 333,
            birthYear = 2012,
            currentYear = 2026,
        )

        // Then
        assertEquals(CompoundInterestExplanationLevel.Younger, younger?.explanationLevel)
        assertEquals(CompoundInterestExplanationLevel.Older, older?.explanationLevel)
    }

    @Test
    fun `FLE-49 AC-02 projection is absent without balance or interest rate`() {
        // Given / When / Then
        assertNull(
            calculator.project(
                mainBalance = BalanceCents(0),
                annualRateBasisPoints = 333,
                birthYear = 2018,
                currentYear = 2026,
            ),
        )
        assertNull(
            calculator.project(
                mainBalance = BalanceCents(2_000),
                annualRateBasisPoints = 0,
                birthYear = 2018,
                currentYear = 2026,
            ),
        )
    }
}
