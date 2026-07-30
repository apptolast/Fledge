package com.apptolast.fledge.domain

import com.apptolast.fledge.domain.model.CashOutSettlement
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.Family
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.LedgerActor
import com.apptolast.fledge.domain.model.LedgerConcept
import com.apptolast.fledge.domain.model.LedgerTransaction
import com.apptolast.fledge.domain.model.LedgerTransactionType
import com.apptolast.fledge.domain.model.LedgerTransferGroupId
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.SavingsGoal
import com.apptolast.fledge.domain.model.SavingsGoalId
import com.apptolast.fledge.domain.model.SettlementId
import com.apptolast.fledge.domain.model.SettlementStatus
import com.apptolast.fledge.domain.model.TaskAssignmentId
import com.apptolast.fledge.domain.model.TaskInstance
import com.apptolast.fledge.domain.model.TaskInstanceId
import com.apptolast.fledge.domain.model.TaskInstanceStatus
import com.apptolast.fledge.domain.model.TaskTemplateId
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.domain.model.TransactionId
import com.apptolast.fledge.domain.model.VirtualAccountType
import com.apptolast.fledge.domain.service.WeeklyParentDigestCalculator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class WeeklyParentDigestCalculatorTest {
    private val calculator = WeeklyParentDigestCalculator()
    private val familyId = FamilyId("family-1")
    private val childId = ChildProfileId("child-1")
    private val siblingId = ChildProfileId("child-2")
    private val now = Instant.parse("2026-07-30T12:00:00Z")

    @Test
    fun `FLE-54 AC-01 summarizes weekly tasks savings cash-outs and pending debt`() {
        // Given
        val family = family()
        val children = listOf(child(childId, "Lucas"), child(siblingId, "Clara"))
        val tasks = listOf(
            task("submitted-now", childId, TaskInstanceStatus.Submitted, submittedAt = "2026-07-29T10:00:00Z"),
            task(
                "approved-now",
                childId,
                TaskInstanceStatus.Approved,
                submittedAt = "2026-07-25T10:00:00Z",
                reviewedAt = "2026-07-28T10:00:00Z",
            ),
            task(
                "rejected-now",
                siblingId,
                TaskInstanceStatus.Rejected,
                submittedAt = "2026-07-24T10:00:00Z",
                reviewedAt = "2026-07-27T10:00:00Z",
            ),
            task("submitted-old", childId, TaskInstanceStatus.Submitted, submittedAt = "2026-07-20T10:00:00Z"),
            task(
                "approved-old",
                childId,
                TaskInstanceStatus.Approved,
                submittedAt = "2026-07-18T10:00:00Z",
                reviewedAt = "2026-07-20T10:00:00Z",
            ),
        )
        val transactions = listOf(
            transaction("save-goal", childId, VirtualAccountType.Goal, 1_000, "2026-07-29T09:00:00Z"),
            transaction("save-give", siblingId, VirtualAccountType.Give, 200, "2026-07-29T09:00:00Z"),
            transaction("withdraw-goal", childId, VirtualAccountType.Goal, -250, "2026-07-29T11:00:00Z"),
            transaction("old-save", childId, VirtualAccountType.Goal, 500, "2026-07-20T11:00:00Z"),
            transaction("main-bonus", childId, VirtualAccountType.Main, 900, "2026-07-29T12:00:00Z"),
        )
        val settlements = listOf(
            settlement("requested-now", childId, 700, SettlementStatus.Requested, "2026-07-29T15:00:00Z"),
            settlement(
                "paid-old",
                childId,
                300,
                SettlementStatus.PaidByParent,
                "2026-07-20T15:00:00Z",
            ),
            settlement(
                "confirmed-now",
                siblingId,
                400,
                SettlementStatus.ConfirmedByChild,
                "2026-07-28T15:00:00Z",
            ),
        )
        val goals = listOf(goal(childId), goal(siblingId))

        // When
        val digest = calculator.calculate(
            family = family,
            children = children,
            taskInstances = tasks,
            transactions = transactions,
            settlements = settlements,
            goals = goals,
            now = now,
        )

        // Then
        assertNotNull(digest)
        assertEquals(Instant.parse("2026-07-23T12:00:00Z"), digest.period.startAt)
        assertEquals(3, digest.submittedTaskCount)
        assertEquals(1, digest.approvedTaskCount)
        assertEquals(1, digest.rejectedTaskCount)
        assertEquals(2, digest.pendingTaskApprovalCount)
        assertEquals(1_200, digest.savedCents.value)
        assertEquals(250, digest.withdrawnFromGoalsCents.value)
        assertEquals(1_100, digest.requestedCashOutCents.value)
        assertEquals(1_000, digest.pendingDebtCents.value)
        assertEquals(2, digest.pendingSettlementCount)
        assertTrue(digest.hasActivity)

        val lucas = digest.children.first { it.childProfileId == childId }
        assertEquals(2, lucas.submittedTaskCount)
        assertEquals(2, lucas.pendingTaskApprovalCount)
        assertEquals(1_000, lucas.savedCents.value)
        assertEquals(250, lucas.withdrawnFromGoalsCents.value)
        assertEquals(700, lucas.requestedCashOutCents.value)
        assertEquals(1_000, lucas.pendingDebtCents.value)
        assertEquals(1_250, lucas.goalBalanceCents.value)
    }

    @Test
    fun `FLE-54 AC-04 returns null when parent has no active family`() {
        // Given / When
        val digest = calculator.calculate(
            family = null,
            children = emptyList(),
            taskInstances = emptyList(),
            transactions = emptyList(),
            settlements = emptyList(),
            goals = emptyList(),
            now = now,
        )

        // Then
        assertNull(digest)
    }

    private fun family(): Family = Family(
        id = familyId,
        name = "Familia Garcia",
        currency = CurrencyCode("EUR"),
        timeZone = TimeZoneId("Europe/Madrid"),
    )

    private fun child(id: ChildProfileId, name: String): ChildProfile = ChildProfile(
        id = id,
        displayName = name,
        birthYear = 2018,
        avatarKey = "star",
    )

    private fun task(
        id: String,
        childProfileId: ChildProfileId,
        status: TaskInstanceStatus,
        submittedAt: String? = null,
        reviewedAt: String? = null,
    ): TaskInstance = TaskInstance(
        id = TaskInstanceId(id),
        familyId = familyId,
        taskAssignmentId = TaskAssignmentId("assignment-$id"),
        taskTemplateId = TaskTemplateId("template-$id"),
        childProfileId = childProfileId,
        title = "Poner la mesa",
        rewardCents = MoneyCents(50),
        requiresPhoto = false,
        status = status,
        dueAt = Instant.parse("2026-07-30T20:00:00Z"),
        periodKey = id,
        createdAt = Instant.parse("2026-07-20T08:00:00Z"),
        updatedAt = reviewedAt?.let(Instant::parse) ?: submittedAt?.let(Instant::parse)
            ?: Instant.parse("2026-07-20T08:00:00Z"),
        submittedAt = submittedAt?.let(Instant::parse),
        reviewedAt = reviewedAt?.let(Instant::parse),
        approvedRewardCents = if (status == TaskInstanceStatus.Approved) MoneyCents(50) else null,
        approvalTransactionId = if (status == TaskInstanceStatus.Approved) TransactionId("tx-$id") else null,
        rejectionReason = if (status == TaskInstanceStatus.Rejected) "Falta terminar." else null,
    )

    private fun transaction(
        id: String,
        childProfileId: ChildProfileId,
        accountType: VirtualAccountType,
        amountCents: Long,
        createdAt: String,
    ): LedgerTransaction = LedgerTransaction(
        id = TransactionId(id),
        familyId = familyId,
        childProfileId = childProfileId,
        accountType = accountType,
        type = if (accountType == VirtualAccountType.Main) {
            LedgerTransactionType.Bonus
        } else {
            LedgerTransactionType.GoalTransfer
        },
        amountCents = MoneyCents(amountCents),
        concept = LedgerConcept("Movimiento"),
        createdBy = LedgerActor.Child,
        createdAt = Instant.parse(createdAt),
        transferGroupId = if (accountType == VirtualAccountType.Main) null else LedgerTransferGroupId("transfer-$id"),
    )

    private fun settlement(
        id: String,
        childProfileId: ChildProfileId,
        amountCents: Long,
        status: SettlementStatus,
        requestedAt: String,
    ): CashOutSettlement = CashOutSettlement(
        id = SettlementId(id),
        familyId = familyId,
        childProfileId = childProfileId,
        amountCents = MoneyCents(amountCents),
        concept = LedgerConcept("Cash out"),
        status = status,
        requestedAt = Instant.parse(requestedAt),
        paidByParentAt = if (status != SettlementStatus.Requested) Instant.parse("2026-07-29T16:00:00Z") else null,
        confirmedByChildAt = if (status == SettlementStatus.ConfirmedByChild) {
            Instant.parse("2026-07-29T17:00:00Z")
        } else {
            null
        },
        settlementTransactionId = if (status ==
            SettlementStatus.ConfirmedByChild
        ) {
            TransactionId("settle-$id")
        } else {
            null
        },
    )

    private fun goal(childProfileId: ChildProfileId): SavingsGoal = SavingsGoal(
        id = SavingsGoalId("goal-${childProfileId.value}"),
        familyId = familyId,
        childProfileId = childProfileId,
        title = "Bici nueva",
        targetCents = MoneyCents(4_000),
        iconKey = "bike",
        createdAt = Instant.parse("2026-07-01T08:00:00Z"),
        updatedAt = Instant.parse("2026-07-01T08:00:00Z"),
    )
}
