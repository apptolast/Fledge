package com.apptolast.fledge.domain.service

import com.apptolast.fledge.domain.model.CashOutSettlement
import com.apptolast.fledge.domain.model.SettlementReminder
import com.apptolast.fledge.domain.model.SettlementReminderAudience
import com.apptolast.fledge.domain.model.SettlementReminderLevel
import com.apptolast.fledge.domain.model.SettlementStatus
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

object SettlementReminderPolicy {
    fun remindersFor(settlements: List<CashOutSettlement>, now: Instant): List<SettlementReminder> =
        settlements.mapNotNull { settlement -> reminderFor(settlement, now) }

    private fun reminderFor(settlement: CashOutSettlement, now: Instant): SettlementReminder? {
        val reference = when (settlement.status) {
            SettlementStatus.Requested -> settlement.requestedAt
            SettlementStatus.PaidByParent -> settlement.paidByParentAt
            SettlementStatus.ConfirmedByChild -> null
        } ?: return null
        val elapsed = now - reference
        val level = when {
            elapsed >= 14.days -> SettlementReminderLevel.FourteenDays
            elapsed >= 7.days -> SettlementReminderLevel.SevenDays
            else -> return null
        }
        val audience = when (settlement.status) {
            SettlementStatus.Requested -> SettlementReminderAudience.Parent
            SettlementStatus.PaidByParent -> SettlementReminderAudience.Child
            SettlementStatus.ConfirmedByChild -> return null
        }
        return SettlementReminder(
            settlementId = settlement.id,
            childProfileId = settlement.childProfileId,
            audience = audience,
            level = level,
            dueAt = reference + when (level) {
                SettlementReminderLevel.SevenDays -> 7.days
                SettlementReminderLevel.FourteenDays -> 14.days
            },
        )
    }
}
