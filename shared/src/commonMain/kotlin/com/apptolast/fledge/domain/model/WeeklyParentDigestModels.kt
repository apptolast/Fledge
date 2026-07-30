package com.apptolast.fledge.domain.model

import kotlin.time.Instant

data class WeeklyParentDigestPeriod(val startAt: Instant, val endAt: Instant)

data class WeeklyParentDigest(
    val familyId: FamilyId,
    val familyName: String,
    val currency: CurrencyCode,
    val period: WeeklyParentDigestPeriod,
    val children: List<WeeklyChildDigest>,
    val submittedTaskCount: Int,
    val approvedTaskCount: Int,
    val rejectedTaskCount: Int,
    val pendingTaskApprovalCount: Int,
    val savedCents: BalanceCents,
    val withdrawnFromGoalsCents: BalanceCents,
    val requestedCashOutCents: BalanceCents,
    val pendingDebtCents: BalanceCents,
    val pendingSettlementCount: Int,
) {
    val hasActivity: Boolean
        get() = submittedTaskCount > 0 ||
            approvedTaskCount > 0 ||
            rejectedTaskCount > 0 ||
            pendingTaskApprovalCount > 0 ||
            savedCents.value > 0 ||
            withdrawnFromGoalsCents.value > 0 ||
            requestedCashOutCents.value > 0 ||
            pendingDebtCents.value > 0
}

data class WeeklyChildDigest(
    val childProfileId: ChildProfileId,
    val childName: String,
    val submittedTaskCount: Int,
    val approvedTaskCount: Int,
    val rejectedTaskCount: Int,
    val pendingTaskApprovalCount: Int,
    val savedCents: BalanceCents,
    val withdrawnFromGoalsCents: BalanceCents,
    val requestedCashOutCents: BalanceCents,
    val pendingDebtCents: BalanceCents,
    val pendingSettlementCount: Int,
    val activeGoalCount: Int,
    val goalBalanceCents: BalanceCents,
) {
    val hasActivity: Boolean
        get() = submittedTaskCount > 0 ||
            approvedTaskCount > 0 ||
            rejectedTaskCount > 0 ||
            pendingTaskApprovalCount > 0 ||
            savedCents.value > 0 ||
            withdrawnFromGoalsCents.value > 0 ||
            requestedCashOutCents.value > 0 ||
            pendingDebtCents.value > 0 ||
            activeGoalCount > 0 ||
            goalBalanceCents.value > 0
}
