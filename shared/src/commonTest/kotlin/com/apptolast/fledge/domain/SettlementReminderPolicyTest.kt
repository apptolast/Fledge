package com.apptolast.fledge.domain

import com.apptolast.fledge.domain.model.CashOutSettlement
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.LedgerConcept
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.SettlementId
import com.apptolast.fledge.domain.model.SettlementReminderAudience
import com.apptolast.fledge.domain.model.SettlementReminderLevel
import com.apptolast.fledge.domain.model.SettlementStatus
import com.apptolast.fledge.domain.model.TransactionId
import com.apptolast.fledge.domain.service.SettlementReminderPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class SettlementReminderPolicyTest {

    @Test
    fun `FLE-24 requested settlement reminds parent after seven days`() {
        // Given
        val requestedAt = Instant.parse("2026-07-01T10:00:00Z")

        // When
        val reminders = SettlementReminderPolicy.remindersFor(
            settlements = listOf(sampleSettlement(requestedAt = requestedAt)),
            now = requestedAt + 7.days,
        )

        // Then
        assertEquals(1, reminders.size)
        assertEquals(SettlementReminderAudience.Parent, reminders.single().audience)
        assertEquals(SettlementReminderLevel.SevenDays, reminders.single().level)
    }

    @Test
    fun `FLE-24 paid settlement reminds child after fourteen days`() {
        // Given
        val requestedAt = Instant.parse("2026-07-01T10:00:00Z")
        val paidAt = requestedAt + 1.days

        // When
        val reminders = SettlementReminderPolicy.remindersFor(
            settlements = listOf(
                sampleSettlement(
                    requestedAt = requestedAt,
                    status = SettlementStatus.PaidByParent,
                    paidByParentAt = paidAt,
                ),
            ),
            now = paidAt + 14.days,
        )

        // Then
        assertEquals(1, reminders.size)
        assertEquals(SettlementReminderAudience.Child, reminders.single().audience)
        assertEquals(SettlementReminderLevel.FourteenDays, reminders.single().level)
    }

    @Test
    fun `FLE-24 confirmed settlement does not produce reminders`() {
        // Given
        val requestedAt = Instant.parse("2026-07-01T10:00:00Z")

        // When
        val reminders = SettlementReminderPolicy.remindersFor(
            settlements = listOf(
                sampleSettlement(
                    requestedAt = requestedAt,
                    status = SettlementStatus.ConfirmedByChild,
                    paidByParentAt = requestedAt + 1.days,
                    confirmedByChildAt = requestedAt + 2.days,
                    settlementTransactionId = TransactionId("transaction-1"),
                ),
            ),
            now = requestedAt + 30.days,
        )

        // Then
        assertTrue(reminders.isEmpty())
    }

    private fun sampleSettlement(
        requestedAt: Instant,
        status: SettlementStatus = SettlementStatus.Requested,
        paidByParentAt: Instant? = null,
        confirmedByChildAt: Instant? = null,
        settlementTransactionId: TransactionId? = null,
    ): CashOutSettlement = CashOutSettlement(
        id = SettlementId("settlement-1"),
        familyId = FamilyId("family-1"),
        childProfileId = ChildProfileId("child-1"),
        amountCents = MoneyCents(300),
        concept = LedgerConcept("Retirada"),
        status = status,
        requestedAt = requestedAt,
        paidByParentAt = paidByParentAt,
        confirmedByChildAt = confirmedByChildAt,
        settlementTransactionId = settlementTransactionId,
    )
}
