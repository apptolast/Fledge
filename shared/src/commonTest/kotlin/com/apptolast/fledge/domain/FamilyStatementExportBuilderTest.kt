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
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.SettlementId
import com.apptolast.fledge.domain.model.SettlementStatus
import com.apptolast.fledge.domain.model.StatementExportFormat
import com.apptolast.fledge.domain.model.StatementExportScope
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.domain.model.TransactionId
import com.apptolast.fledge.domain.model.VirtualAccountType
import com.apptolast.fledge.domain.service.FamilyStatementExportBuilder
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class FamilyStatementExportBuilderTest {
    private val builder = FamilyStatementExportBuilder()
    private val familyId = FamilyId("family-1")
    private val lucasId = ChildProfileId("child-1")
    private val claraId = ChildProfileId("child-2")
    private val generatedAt = Instant.parse("2026-07-30T10:00:00Z")

    @Test
    fun `FLE-55 AC-01 exports family statement as CSV with ledger and cash-out rows`() {
        // Given
        val transactions = listOf(
            transaction("tx-1", lucasId, 550, "Paga extra, cocina"),
            transaction("tx-2", claraId, -125, "Multa pantalla"),
        )
        val settlements = listOf(settlement("settlement-1", lucasId, 300))

        // When
        val result = builder.build(
            family = family(),
            children = children(),
            transactions = transactions,
            settlements = settlements,
            scope = StatementExportScope.Family,
            format = StatementExportFormat.Csv,
            generatedAt = generatedAt,
        )

        // Then
        assertNotNull(result)
        val csv = result.artifact.bytes.decodeToString()
        assertEquals("text/csv", result.artifact.mimeType)
        assertEquals("EUR", result.currencyCode)
        assertEquals("fledge-familia-garcia-familia-garcia-2026-07-30.csv", result.artifact.fileName)
        assertContains(csv, "date,child,source,account,type,actor,status,concept,amount")
        assertContains(csv, "2026-07-29,Lucas,ledger,Main,Bonus,Parent,,\"Paga extra, cocina\",5.50")
        assertContains(csv, "2026-07-28,Lucas,cash_out,Main,SettlementRequest,Child,Requested,Cromos,-3.00")
        assertEquals(3, result.rowCount)
        assertEquals(125, result.totalCents.value)
    }

    @Test
    fun `FLE-55 AC-02 exports one child statement when child scope is selected`() {
        // Given
        val transactions = listOf(
            transaction("tx-1", lucasId, 550, "Paga extra"),
            transaction("tx-2", claraId, 250, "Paga semanal"),
        )

        // When
        val result = builder.build(
            family = family(),
            children = children(),
            transactions = transactions,
            settlements = emptyList(),
            scope = StatementExportScope.Child(lucasId),
            format = StatementExportFormat.Csv,
            generatedAt = generatedAt,
        )

        // Then
        assertNotNull(result)
        val csv = result.artifact.bytes.decodeToString()
        assertContains(csv, "Lucas")
        assertTrue("Clara" !in csv)
        assertEquals(1, result.rowCount)
        assertEquals("Lucas", result.scopeLabel)
    }

    @Test
    fun `FLE-55 AC-03 exports a PDF artifact for bank-like statement preview`() {
        // Given / When
        val result = builder.build(
            family = family(),
            children = children(),
            transactions = listOf(transaction("tx-1", lucasId, 550, "Paga extra")),
            settlements = emptyList(),
            scope = StatementExportScope.Family,
            format = StatementExportFormat.Pdf,
            generatedAt = generatedAt,
        )

        // Then
        assertNotNull(result)
        assertEquals("application/pdf", result.artifact.mimeType)
        assertEquals("fledge-familia-garcia-familia-garcia-2026-07-30.pdf", result.artifact.fileName)
        assertTrue(result.artifact.bytes.decodeToString().startsWith("%PDF-1.4"))
        assertContains(result.artifact.previewText, "Fledge statement")
        assertContains(result.artifact.previewText, "Rows: 1")
    }

    private fun family(): Family = Family(
        id = familyId,
        name = "Familia Garcia",
        currency = CurrencyCode("EUR"),
        timeZone = TimeZoneId("Europe/Madrid"),
    )

    private fun children(): List<ChildProfile> = listOf(
        ChildProfile(lucasId, "Lucas", 2018, "rocket"),
        ChildProfile(claraId, "Clara", 2016, "star"),
    )

    private fun transaction(id: String, childProfileId: ChildProfileId, amountCents: Long, concept: String) =
        LedgerTransaction(
            id = TransactionId(id),
            familyId = familyId,
            childProfileId = childProfileId,
            accountType = VirtualAccountType.Main,
            type = if (amountCents > 0) LedgerTransactionType.Bonus else LedgerTransactionType.Penalty,
            amountCents = MoneyCents(amountCents),
            concept = LedgerConcept(concept),
            createdBy = LedgerActor.Parent,
            createdAt = Instant.parse("2026-07-29T10:00:00Z"),
        )

    private fun settlement(id: String, childProfileId: ChildProfileId, amountCents: Long): CashOutSettlement =
        CashOutSettlement(
            id = SettlementId(id),
            familyId = familyId,
            childProfileId = childProfileId,
            amountCents = MoneyCents(amountCents),
            concept = LedgerConcept("Cromos"),
            status = SettlementStatus.Requested,
            requestedAt = Instant.parse("2026-07-28T10:00:00Z"),
        )
}
