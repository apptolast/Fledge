package com.apptolast.fledge.presentation

import com.apptolast.fledge.data.repository.InMemoryFamilyFoundationRepository
import com.apptolast.fledge.data.repository.InMemoryLedgerRepository
import com.apptolast.fledge.data.repository.InMemoryMoneyFlowRepository
import com.apptolast.fledge.domain.model.CashOutSettlementDraft
import com.apptolast.fledge.domain.model.ChildPin
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.LedgerActor
import com.apptolast.fledge.domain.model.LedgerConcept
import com.apptolast.fledge.domain.model.LedgerTransactionDraft
import com.apptolast.fledge.domain.model.LedgerTransactionType
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.StatementExportFormat
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.domain.model.VirtualAccountType
import com.apptolast.fledge.domain.service.FamilyStatementExportBuilder
import com.apptolast.fledge.presentation.foundation.statementexport.ParentStatementExportViewModel
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class ParentStatementExportViewModelTest {

    @Test
    fun `FLE-55 AC-04 parent statement export resolves family child scope and format`() = runTest {
        // Given
        val familyRepository = InMemoryFamilyFoundationRepository()
        val ledgerRepository = InMemoryLedgerRepository()
        val moneyFlowRepository = InMemoryMoneyFlowRepository()
        val now = Instant.parse("2026-07-30T10:00:00Z")
        val family = familyRepository.createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid"))
        familyRepository.recordVirtualMoneyConsent()
        val lucas = familyRepository.addChildProfile(
            family.id,
            displayName = "Lucas",
            birthYear = 2018,
            avatarKey = "rocket",
            pin = ChildPin("1234"),
        )
        val clara = familyRepository.addChildProfile(
            family.id,
            displayName = "Clara",
            birthYear = 2016,
            avatarKey = "star",
            pin = ChildPin("5678"),
        )
        ledgerRepository.appendTransaction(
            LedgerTransactionDraft(
                familyId = family.id,
                childProfileId = lucas.id,
                accountType = VirtualAccountType.Main,
                type = LedgerTransactionType.Bonus,
                amountCents = MoneyCents(550),
                concept = LedgerConcept("Paga extra"),
                createdBy = LedgerActor.Parent,
            ),
            createdAt = now,
        )
        ledgerRepository.appendTransaction(
            LedgerTransactionDraft(
                familyId = family.id,
                childProfileId = clara.id,
                accountType = VirtualAccountType.Main,
                type = LedgerTransactionType.Allowance,
                amountCents = MoneyCents(250),
                concept = LedgerConcept("Paga semanal"),
                createdBy = LedgerActor.Parent,
            ),
            createdAt = now,
        )
        moneyFlowRepository.createSettlementRequest(
            CashOutSettlementDraft(
                familyId = family.id,
                childProfileId = lucas.id,
                amountCents = MoneyCents(300),
                concept = LedgerConcept("Cromos"),
            ),
            requestedAt = now,
        )

        // When
        val viewModel = ParentStatementExportViewModel(
            familyRepository = familyRepository,
            ledgerRepository = ledgerRepository,
            moneyFlowRepository = moneyFlowRepository,
            builder = FamilyStatementExportBuilder(),
        )

        // Then
        val familyExport = assertNotNull(viewModel.uiState.value.export)
        assertEquals(StatementExportFormat.Csv, familyExport.format)
        assertEquals(3, familyExport.rowCount)
        assertEquals(500, familyExport.totalCents.value)
        assertContains(familyExport.artifact.previewText, "Clara")

        // When
        viewModel.selectChildScope(lucas.id)
        viewModel.selectFormat(StatementExportFormat.Pdf)

        // Then
        val childExport = assertNotNull(viewModel.uiState.value.export)
        assertEquals(StatementExportFormat.Pdf, childExport.format)
        assertEquals("Lucas", childExport.scopeLabel)
        assertEquals(2, childExport.rowCount)
        assertEquals("application/pdf", childExport.artifact.mimeType)
        assertTrue("Clara" !in childExport.artifact.previewText)
    }

    @Test
    fun `FLE-55 AC-05 parent statement export stays empty without active family`() = runTest {
        // Given / When
        val viewModel = ParentStatementExportViewModel(
            familyRepository = InMemoryFamilyFoundationRepository(),
            ledgerRepository = InMemoryLedgerRepository(),
            moneyFlowRepository = InMemoryMoneyFlowRepository(),
            builder = FamilyStatementExportBuilder(),
        )

        // Then
        assertNull(viewModel.uiState.value.export)
    }
}
