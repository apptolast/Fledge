package com.apptolast.fledge.domain

import com.apptolast.fledge.data.repository.InMemoryLedgerRepository
import com.apptolast.fledge.data.repository.InMemoryMoneyFlowRepository
import com.apptolast.fledge.domain.model.AllowanceDay
import com.apptolast.fledge.domain.model.AllowanceFrequency
import com.apptolast.fledge.domain.model.AllowanceRuleDraft
import com.apptolast.fledge.domain.model.BalanceCents
import com.apptolast.fledge.domain.model.CashOutSettlementDraft
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.LedgerActor
import com.apptolast.fledge.domain.model.LedgerConcept
import com.apptolast.fledge.domain.model.LedgerTransactionDraft
import com.apptolast.fledge.domain.model.LedgerTransactionType
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.SettlementStatus
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.domain.model.VirtualAccountType
import com.apptolast.fledge.domain.service.AllowanceProcessor
import com.apptolast.fledge.domain.service.AllowanceSchedule
import com.apptolast.fledge.domain.service.CashOutProcessor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class MoneyFlowProcessorTest {

    @Test
    fun `FLE-22 due allowance appends ledger transaction and advances next run date`() = runTest {
        // Given
        val moneyFlowRepository = InMemoryMoneyFlowRepository()
        val ledgerRepository = InMemoryLedgerRepository()
        val processor = AllowanceProcessor(moneyFlowRepository, ledgerRepository)
        val firstRunAt = Instant.parse("2026-02-28T00:00:00Z")
        val rule = moneyFlowRepository.saveAllowanceRule(
            draft = sampleAllowanceRuleDraft(
                frequency = AllowanceFrequency.Monthly,
                day = AllowanceDay(31),
            ),
            nextRunAt = firstRunAt,
            createdAt = Instant.parse("2026-02-01T10:00:00Z"),
        )

        // When
        val transactions = processor.runDueAllowances(firstRunAt)

        // Then
        assertEquals(1, transactions.size)
        assertEquals(LedgerTransactionType.Allowance, transactions.single().type)
        assertEquals(MoneyCents(500), transactions.single().amountCents)
        assertEquals(BalanceCents(500), ledgerRepository.balanceFor(rule.childProfileId, VirtualAccountType.Main))
        assertEquals(
            AllowanceSchedule.nextRunAtAfter(
                previousRunAt = firstRunAt,
                frequency = AllowanceFrequency.Monthly,
                day = AllowanceDay(31),
                timeZone = TimeZoneId("Europe/Madrid"),
            ),
            moneyFlowRepository.allowanceRuleById(rule.id)?.nextRunAt,
        )
    }

    @Test
    fun `FLE-23 cash-out is not discounted until child confirmation`() = runTest {
        // Given
        val moneyFlowRepository = InMemoryMoneyFlowRepository()
        val ledgerRepository = InMemoryLedgerRepository()
        val processor = CashOutProcessor(moneyFlowRepository, ledgerRepository)
        ledgerRepository.appendTransaction(sampleLedgerDraft(amountCents = MoneyCents(1_000)))
        val requestedAt = Instant.parse("2026-07-26T10:00:00Z")

        // When
        val settlement = processor.requestCashOut(
            draft = sampleSettlementDraft(amountCents = MoneyCents(400)),
            requestedAt = requestedAt,
        )

        // Then
        assertEquals(SettlementStatus.Requested, settlement.status)
        assertEquals(BalanceCents(1_000), ledgerRepository.balanceFor(settlement.childProfileId, VirtualAccountType.Main))

        val paid = processor.markPaidByParent(settlement.id, requestedAt + 1.days)
        assertEquals(SettlementStatus.PaidByParent, paid.status)
        assertEquals(BalanceCents(1_000), ledgerRepository.balanceFor(settlement.childProfileId, VirtualAccountType.Main))

        val confirmed = processor.confirmByChild(settlement.id, requestedAt + 2.days)
        assertEquals(SettlementStatus.ConfirmedByChild, confirmed.status)
        assertEquals(BalanceCents(600), ledgerRepository.balanceFor(settlement.childProfileId, VirtualAccountType.Main))
        assertEquals(LedgerTransactionType.Settlement, ledgerRepository.transactions.value.last().type)
        assertEquals(MoneyCents(-400), ledgerRepository.transactions.value.last().amountCents)
    }

    @Test
    fun `FLE-23 cash-out cannot exceed current main balance`() = runTest {
        // Given
        val moneyFlowRepository = InMemoryMoneyFlowRepository()
        val ledgerRepository = InMemoryLedgerRepository()
        val processor = CashOutProcessor(moneyFlowRepository, ledgerRepository)
        ledgerRepository.appendTransaction(sampleLedgerDraft(amountCents = MoneyCents(300)))

        // When / Then
        assertFailsWith<IllegalArgumentException> {
            processor.requestCashOut(
                draft = sampleSettlementDraft(amountCents = MoneyCents(400)),
                requestedAt = Instant.parse("2026-07-26T10:00:00Z"),
            )
        }
        assertEquals(emptyList(), moneyFlowRepository.settlements.value)
        assertEquals(BalanceCents(300), ledgerRepository.balanceFor(ChildProfileId("child-1"), VirtualAccountType.Main))
    }

    private fun sampleAllowanceRuleDraft(
        frequency: AllowanceFrequency = AllowanceFrequency.Weekly,
        day: AllowanceDay = AllowanceDay(1),
    ): AllowanceRuleDraft =
        AllowanceRuleDraft(
            familyId = FamilyId("family-1"),
            childProfileId = ChildProfileId("child-1"),
            frequency = frequency,
            day = day,
            amountCents = MoneyCents(500),
            concept = LedgerConcept("Paga"),
            timeZone = TimeZoneId("Europe/Madrid"),
        )

    private fun sampleSettlementDraft(amountCents: MoneyCents): CashOutSettlementDraft =
        CashOutSettlementDraft(
            familyId = FamilyId("family-1"),
            childProfileId = ChildProfileId("child-1"),
            amountCents = amountCents,
            concept = LedgerConcept("Retirada"),
        )

    private fun sampleLedgerDraft(amountCents: MoneyCents): LedgerTransactionDraft =
        LedgerTransactionDraft(
            familyId = FamilyId("family-1"),
            childProfileId = ChildProfileId("child-1"),
            accountType = VirtualAccountType.Main,
            type = LedgerTransactionType.Bonus,
            amountCents = amountCents,
            concept = LedgerConcept("Saldo inicial"),
            createdBy = LedgerActor.Parent,
        )
}
