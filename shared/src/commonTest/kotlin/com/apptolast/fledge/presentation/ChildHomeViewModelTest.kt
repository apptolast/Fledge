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
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.domain.model.VirtualAccountType
import com.apptolast.fledge.domain.service.CashOutProcessor
import com.apptolast.fledge.presentation.foundation.childhome.ChildHomeViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class ChildHomeViewModelTest {

    @Test
    fun `FLE-21 child ledger shows original and reversal entries`() = runTest {
        // Given
        val foundationRepository = InMemoryFamilyFoundationRepository()
        val ledgerRepository = InMemoryLedgerRepository()
        val moneyFlowRepository = InMemoryMoneyFlowRepository()
        val family = foundationRepository.createFamily(
            "Familia Garcia",
            CurrencyCode("EUR"),
            TimeZoneId("Europe/Madrid"),
        )
        foundationRepository.recordVirtualMoneyConsent()
        val child = foundationRepository.addChildProfile(
            familyId = family.id,
            displayName = "Lucas",
            birthYear = 2017,
            avatarKey = "rocket",
            pin = ChildPin("1234"),
        )
        val original = ledgerRepository.appendTransaction(
            LedgerTransactionDraft(
                familyId = family.id,
                childProfileId = child.id,
                accountType = VirtualAccountType.Main,
                type = LedgerTransactionType.Bonus,
                amountCents = MoneyCents(500),
                concept = LedgerConcept("Paga extra"),
                createdBy = LedgerActor.Parent,
            ),
        )
        ledgerRepository.reverseTransaction(
            transactionId = original.id,
            concept = LedgerConcept("Correccion"),
            createdBy = LedgerActor.Parent,
        )
        val viewModel = ChildHomeViewModel(
            foundationRepository,
            ledgerRepository,
            moneyFlowRepository,
            CashOutProcessor(moneyFlowRepository, ledgerRepository),
        )

        // When
        viewModel.load(child.id)

        // Then
        val state = viewModel.uiState.value
        assertEquals(BalanceCents(0), state.balances?.main)
        assertEquals(2, state.ledgerTransactions.size)
        assertEquals(original.id, state.ledgerTransactions[1].reversesTransactionId)
    }
}
