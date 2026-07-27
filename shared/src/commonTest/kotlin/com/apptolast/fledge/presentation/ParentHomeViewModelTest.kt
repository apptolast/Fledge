package com.apptolast.fledge.presentation

import app.cash.turbine.test
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
import com.apptolast.fledge.presentation.foundation.parenthome.ParentHomeViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ParentHomeViewModelTest {

    @Test
    fun `AC-10 parent home exposes stable lists with unique ids`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()
        val ledgerRepository = InMemoryLedgerRepository()
        val moneyFlowRepository = InMemoryMoneyFlowRepository()
        val family = repository.createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid"))
        repository.recordVirtualMoneyConsent()
        val child = repository.addChildProfile(
            family.id,
            "Lucas",
            birthYear = 2017,
            avatarKey = "rocket",
            pin = ChildPin("1234"),
        )
        ledgerRepository.appendTransaction(
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
        val viewModel = ParentHomeViewModel(
            repository,
            ledgerRepository,
            moneyFlowRepository,
            CashOutProcessor(moneyFlowRepository, ledgerRepository),
        )

        // When / Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.children.size)
            assertEquals(BalanceCents(500), state.mainBalances[child.id])
            assertTrue(state.setupActions.isNotEmpty())
            assertEquals(state.setupActions.size, state.setupActions.map { it.id }.toSet().size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
