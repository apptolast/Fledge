package com.apptolast.fledge.domain.service

import com.apptolast.fledge.domain.model.BalanceCents
import com.apptolast.fledge.domain.model.CashOutSettlement
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.Family
import com.apptolast.fledge.domain.model.LedgerTransaction
import com.apptolast.fledge.domain.model.LedgerTransactionType
import com.apptolast.fledge.domain.model.SavingsGoal
import com.apptolast.fledge.domain.model.SavingsGoalStatus
import com.apptolast.fledge.domain.model.SettlementStatus
import com.apptolast.fledge.domain.model.TaskInstance
import com.apptolast.fledge.domain.model.TaskInstanceStatus
import com.apptolast.fledge.domain.model.VirtualAccountType
import com.apptolast.fledge.domain.model.WeeklyChildDigest
import com.apptolast.fledge.domain.model.WeeklyParentDigest
import com.apptolast.fledge.domain.model.WeeklyParentDigestPeriod
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class WeeklyParentDigestCalculator {
    fun calculate(
        family: Family?,
        children: List<ChildProfile>,
        taskInstances: List<TaskInstance>,
        transactions: List<LedgerTransaction>,
        settlements: List<CashOutSettlement>,
        goals: List<SavingsGoal>,
        now: Instant,
    ): WeeklyParentDigest? {
        family ?: return null

        val period = WeeklyParentDigestPeriod(startAt = now.minus(7.days), endAt = now)
        val familyTasks = taskInstances.filter { it.familyId == family.id }
        val familyTransactions = transactions.filter { it.familyId == family.id }
        val familySettlements = settlements.filter { it.familyId == family.id }
        val familyGoals = goals.filter { it.familyId == family.id }
        val childDigests = children
            .sortedBy { it.displayName.lowercase() }
            .map { child ->
                val childTasks = familyTasks.filter { it.childProfileId == child.id }
                val childTransactions = familyTransactions.filter { it.childProfileId == child.id }
                val childSettlements = familySettlements.filter { it.childProfileId == child.id }
                val childGoals = familyGoals.filter { it.childProfileId == child.id }

                WeeklyChildDigest(
                    childProfileId = child.id,
                    childName = child.displayName,
                    submittedTaskCount = childTasks.submittedWithin(period),
                    approvedTaskCount = childTasks.approvedWithin(period),
                    rejectedTaskCount = childTasks.rejectedWithin(period),
                    pendingTaskApprovalCount = childTasks.count { it.status == TaskInstanceStatus.Submitted },
                    savedCents = BalanceCents(childTransactions.savedWithin(period)),
                    withdrawnFromGoalsCents = BalanceCents(childTransactions.withdrawnFromGoalsWithin(period)),
                    requestedCashOutCents = BalanceCents(childSettlements.requestedWithin(period)),
                    pendingDebtCents = BalanceCents(childSettlements.pendingDebt()),
                    pendingSettlementCount = childSettlements.count { it.status != SettlementStatus.ConfirmedByChild },
                    activeGoalCount = childGoals.count { it.status == SavingsGoalStatus.Active },
                    goalBalanceCents = BalanceCents(childTransactions.goalBalance()),
                )
            }

        return WeeklyParentDigest(
            familyId = family.id,
            familyName = family.name,
            currency = family.currency,
            period = period,
            children = childDigests,
            submittedTaskCount = childDigests.sumOf { it.submittedTaskCount },
            approvedTaskCount = childDigests.sumOf { it.approvedTaskCount },
            rejectedTaskCount = childDigests.sumOf { it.rejectedTaskCount },
            pendingTaskApprovalCount = childDigests.sumOf { it.pendingTaskApprovalCount },
            savedCents = BalanceCents(childDigests.sumOf { it.savedCents.value }),
            withdrawnFromGoalsCents = BalanceCents(childDigests.sumOf { it.withdrawnFromGoalsCents.value }),
            requestedCashOutCents = BalanceCents(childDigests.sumOf { it.requestedCashOutCents.value }),
            pendingDebtCents = BalanceCents(childDigests.sumOf { it.pendingDebtCents.value }),
            pendingSettlementCount = childDigests.sumOf { it.pendingSettlementCount },
        )
    }

    private fun List<TaskInstance>.submittedWithin(period: WeeklyParentDigestPeriod): Int =
        count { it.submittedAt?.isWithin(period) == true }

    private fun List<TaskInstance>.approvedWithin(period: WeeklyParentDigestPeriod): Int =
        count { it.status == TaskInstanceStatus.Approved && it.reviewedAt?.isWithin(period) == true }

    private fun List<TaskInstance>.rejectedWithin(period: WeeklyParentDigestPeriod): Int =
        count { it.status == TaskInstanceStatus.Rejected && it.reviewedAt?.isWithin(period) == true }

    private fun List<LedgerTransaction>.savedWithin(period: WeeklyParentDigestPeriod): Long = filter { transaction ->
        transaction.createdAt.isWithin(period) &&
            transaction.accountType != VirtualAccountType.Main &&
            transaction.type != LedgerTransactionType.Reversal &&
            transaction.amountCents.value > 0
    }.sumOf { it.amountCents.value }

    private fun List<LedgerTransaction>.withdrawnFromGoalsWithin(period: WeeklyParentDigestPeriod): Long =
        filter { transaction ->
            transaction.createdAt.isWithin(period) &&
                transaction.accountType != VirtualAccountType.Main &&
                transaction.type == LedgerTransactionType.GoalTransfer &&
                transaction.amountCents.value < 0
        }.sumOf { it.amountCents.value.absoluteValue }

    private fun List<LedgerTransaction>.goalBalance(): Long =
        filter { it.accountType != VirtualAccountType.Main }.sumOf { it.amountCents.value }

    private fun List<CashOutSettlement>.requestedWithin(period: WeeklyParentDigestPeriod): Long =
        filter { it.requestedAt.isWithin(period) }.sumOf { it.amountCents.value }

    private fun List<CashOutSettlement>.pendingDebt(): Long =
        filter { it.status != SettlementStatus.ConfirmedByChild }.sumOf { it.amountCents.value }

    private fun Instant.isWithin(period: WeeklyParentDigestPeriod): Boolean =
        this >= period.startAt && this <= period.endAt
}
