package com.apptolast.fledge.domain

import com.apptolast.fledge.domain.model.BalanceCents
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.LedgerActor
import com.apptolast.fledge.domain.model.LedgerConcept
import com.apptolast.fledge.domain.model.LedgerTransaction
import com.apptolast.fledge.domain.model.LedgerTransactionType
import com.apptolast.fledge.domain.model.LedgerTransferGroupId
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.SavingsGoal
import com.apptolast.fledge.domain.model.SavingsGoalId
import com.apptolast.fledge.domain.model.TransactionId
import com.apptolast.fledge.domain.model.VirtualAccountType
import com.apptolast.fledge.domain.service.SavingsGoalProjectionCalculator
import com.apptolast.fledge.domain.service.SavingsGoalProjectionStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class SavingsGoalProjectionCalculatorTest {
    private val calculator = SavingsGoalProjectionCalculator()
    private val familyId = FamilyId("family-1")
    private val childProfileId = ChildProfileId("child-1")
    private val goalId = SavingsGoalId("goal-1")
    private val createdAt = Instant.fromEpochSeconds(1_700_000_000)

    @Test
    fun `FLE-39 AC-01 calculates progress and remaining amount for active goal`() {
        // Given
        val goal = goal(targetCents = 4_000)

        // When
        val projection = calculator.project(
            goal = goal,
            currentGoalBalance = BalanceCents(1_230),
            transactions = emptyList(),
            now = createdAt.plus(5.days),
        )

        // Then
        assertEquals(30, projection.progressPercent)
        assertEquals(BalanceCents(2_770), projection.remainingCents)
        assertEquals(SavingsGoalProjectionStatus.NeedsContribution, projection.status)
    }

    @Test
    fun `FLE-39 AC-02 estimates completion date from net daily goal pace`() {
        // Given
        val now = createdAt.plus(10.days)
        val goal = goal(targetCents = 4_000)
        val transactions = listOf(
            transaction(
                id = "goal-credit-1",
                amountCents = 1_000,
                createdAt = createdAt.plus(6.days),
            ),
            transaction(
                id = "goal-credit-2",
                amountCents = 500,
                createdAt = createdAt.plus(8.days),
            ),
        )

        // When
        val projection = calculator.project(
            goal = goal,
            currentGoalBalance = BalanceCents(1_500),
            transactions = transactions,
            now = now,
        )

        // Then
        assertEquals(SavingsGoalProjectionStatus.OnTrack, projection.status)
        assertEquals(BalanceCents(375), projection.dailyPaceCents)
        assertEquals(7, projection.estimatedDaysRemaining)
        assertEquals(now.plus(7.days), projection.estimatedCompletionAt)
    }

    @Test
    fun `FLE-39 AC-03 omits estimated date when the goal has no positive pace`() {
        // Given
        val now = createdAt.plus(10.days)
        val goal = goal(targetCents = 4_000)
        val transactions = listOf(
            transaction(
                id = "goal-credit",
                amountCents = 700,
                createdAt = createdAt.plus(6.days),
            ),
            transaction(
                id = "goal-debit",
                amountCents = -700,
                createdAt = createdAt.plus(8.days),
            ),
        )

        // When
        val projection = calculator.project(
            goal = goal,
            currentGoalBalance = BalanceCents(0),
            transactions = transactions,
            now = now,
        )

        // Then
        assertEquals(SavingsGoalProjectionStatus.NeedsContribution, projection.status)
        assertEquals(null, projection.dailyPaceCents)
        assertEquals(null, projection.estimatedDaysRemaining)
        assertEquals(null, projection.estimatedCompletionAt)
    }

    private fun goal(targetCents: Long): SavingsGoal = SavingsGoal(
        id = goalId,
        familyId = familyId,
        childProfileId = childProfileId,
        title = "Bici nueva",
        targetCents = MoneyCents(targetCents),
        iconKey = "bike",
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    private fun transaction(id: String, amountCents: Long, createdAt: Instant): LedgerTransaction = LedgerTransaction(
        id = TransactionId(id),
        familyId = familyId,
        childProfileId = childProfileId,
        accountType = VirtualAccountType.Goal,
        type = LedgerTransactionType.GoalTransfer,
        amountCents = MoneyCents(amountCents),
        concept = LedgerConcept("Movimiento a objetivo"),
        createdBy = LedgerActor.Child,
        createdAt = createdAt,
        transferGroupId = LedgerTransferGroupId("transfer-$id"),
    )
}
