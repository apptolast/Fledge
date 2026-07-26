package com.apptolast.fledge.data

import app.cash.turbine.test
import com.apptolast.fledge.data.repository.InMemoryMoneyFlowRepository
import com.apptolast.fledge.domain.model.AllowanceDay
import com.apptolast.fledge.domain.model.AllowanceFrequency
import com.apptolast.fledge.domain.model.AllowanceRuleDraft
import com.apptolast.fledge.domain.model.CashOutSettlementDraft
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.LedgerConcept
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.SettlementStatus
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.domain.model.TransactionId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class InMemoryMoneyFlowRepositoryTest {

    @Test
    fun `FLE-22 saves allowance rule with next run date`() = runTest {
        // Given
        val repository = InMemoryMoneyFlowRepository()
        val nextRunAt = Instant.parse("2026-07-27T00:00:00Z")

        // When / Then
        repository.allowanceRules.test {
            assertEquals(emptyList(), awaitItem())
            val rule = repository.saveAllowanceRule(
                draft = sampleAllowanceRuleDraft(),
                nextRunAt = nextRunAt,
                createdAt = Instant.parse("2026-07-26T10:00:00Z"),
            )

            assertEquals(listOf(rule), awaitItem())
            assertEquals(listOf(rule), repository.dueAllowanceRules(nextRunAt))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `FLE-23 settlement transitions are strict`() = runTest {
        // Given
        val repository = InMemoryMoneyFlowRepository()
        val requestedAt = Instant.parse("2026-07-26T10:00:00Z")

        // When
        val settlement = repository.createSettlementRequest(
            draft = sampleSettlementDraft(),
            requestedAt = requestedAt,
        )

        // Then
        assertEquals(SettlementStatus.Requested, settlement.status)
        assertFailsWith<IllegalArgumentException> {
            repository.markSettlementConfirmedByChild(
                settlementId = settlement.id,
                transactionId = TransactionId("transaction-1"),
                confirmedAt = requestedAt + 1.days,
            )
        }

        val paid = repository.markSettlementPaidByParent(settlement.id, requestedAt + 1.days)
        assertEquals(SettlementStatus.PaidByParent, paid.status)

        val confirmed = repository.markSettlementConfirmedByChild(
            settlementId = settlement.id,
            transactionId = TransactionId("transaction-1"),
            confirmedAt = requestedAt + 2.days,
        )
        assertEquals(SettlementStatus.ConfirmedByChild, confirmed.status)
        assertEquals(TransactionId("transaction-1"), confirmed.settlementTransactionId)
    }

    private fun sampleAllowanceRuleDraft(): AllowanceRuleDraft =
        AllowanceRuleDraft(
            familyId = FamilyId("family-1"),
            childProfileId = ChildProfileId("child-1"),
            frequency = AllowanceFrequency.Weekly,
            day = AllowanceDay(1),
            amountCents = MoneyCents(500),
            concept = LedgerConcept("Paga semanal"),
            timeZone = TimeZoneId("Europe/Madrid"),
        )

    private fun sampleSettlementDraft(): CashOutSettlementDraft =
        CashOutSettlementDraft(
            familyId = FamilyId("family-1"),
            childProfileId = ChildProfileId("child-1"),
            amountCents = MoneyCents(300),
            concept = LedgerConcept("Retirada para cromos"),
        )
}
