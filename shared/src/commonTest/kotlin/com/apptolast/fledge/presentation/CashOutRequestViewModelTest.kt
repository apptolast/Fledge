package com.apptolast.fledge.presentation

import com.apptolast.fledge.data.repository.InMemoryFamilyFoundationRepository
import com.apptolast.fledge.data.repository.InMemoryLedgerRepository
import com.apptolast.fledge.data.repository.InMemoryMoneyFlowRepository
import com.apptolast.fledge.domain.model.BalanceCents
import com.apptolast.fledge.domain.model.ChildPin
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.LedgerActor
import com.apptolast.fledge.domain.model.LedgerConcept
import com.apptolast.fledge.domain.model.LedgerTransactionDraft
import com.apptolast.fledge.domain.model.LedgerTransactionType
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.SettlementStatus
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.domain.model.VirtualAccountType
import com.apptolast.fledge.domain.service.CashOutProcessor
import com.apptolast.fledge.presentation.foundation.cashout.CashOutRequestError
import com.apptolast.fledge.presentation.foundation.cashout.CashOutRequestViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class CashOutRequestViewModelTest {

    @Test
    fun `FLE-23 child requests cash-out without discounting balance`() = runTest {
        // Given
        val foundationRepository = InMemoryFamilyFoundationRepository()
        val ledgerRepository = InMemoryLedgerRepository()
        val moneyFlowRepository = InMemoryMoneyFlowRepository()
        val child = foundationRepository.createChild()
        ledgerRepository.appendTransaction(sampleLedgerDraft(amountCents = MoneyCents(1_000)))
        val viewModel = CashOutRequestViewModel(
            foundationRepository,
            ledgerRepository,
            CashOutProcessor(moneyFlowRepository, ledgerRepository),
        )

        // When
        viewModel.load(child.id)
        viewModel.updateAmount("4,00")
        viewModel.updateConcept("Cromos")

        // Then
        assertTrue(viewModel.submit())
        val settlement = checkNotNull(viewModel.uiState.value.savedSettlement)
        assertEquals(SettlementStatus.Requested, settlement.status)
        assertEquals(MoneyCents(400), settlement.amountCents)
        assertEquals(BalanceCents(1_000), ledgerRepository.balanceFor(child.id, VirtualAccountType.Main))
    }

    @Test
    fun `FLE-23 child cannot request more than available balance`() = runTest {
        // Given
        val foundationRepository = InMemoryFamilyFoundationRepository()
        val ledgerRepository = InMemoryLedgerRepository()
        val moneyFlowRepository = InMemoryMoneyFlowRepository()
        val child = foundationRepository.createChild()
        ledgerRepository.appendTransaction(sampleLedgerDraft(amountCents = MoneyCents(300)))
        val viewModel = CashOutRequestViewModel(
            foundationRepository,
            ledgerRepository,
            CashOutProcessor(moneyFlowRepository, ledgerRepository),
        )

        // When
        viewModel.load(child.id)
        viewModel.updateAmount("4,00")
        viewModel.updateConcept("Cromos")

        // Then
        assertFalse(viewModel.submit())
        assertEquals(CashOutRequestError.InsufficientBalance, viewModel.uiState.value.error)
        assertEquals(emptyList(), moneyFlowRepository.settlements.value)
    }

    private suspend fun InMemoryFamilyFoundationRepository.createChild() =
        createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid")).let { family ->
            recordVirtualMoneyConsent()
            addChildProfile(
                familyId = family.id,
                displayName = "Lucas",
                birthYear = 2017,
                avatarKey = "rocket",
                pin = ChildPin("1234"),
            )
        }

    private fun sampleLedgerDraft(amountCents: MoneyCents): LedgerTransactionDraft =
        LedgerTransactionDraft(
            familyId = com.apptolast.fledge.domain.model.FamilyId("family-1"),
            childProfileId = com.apptolast.fledge.domain.model.ChildProfileId("child-1"),
            accountType = VirtualAccountType.Main,
            type = LedgerTransactionType.Bonus,
            amountCents = amountCents,
            concept = LedgerConcept("Saldo inicial"),
            createdBy = LedgerActor.Parent,
        )
}
