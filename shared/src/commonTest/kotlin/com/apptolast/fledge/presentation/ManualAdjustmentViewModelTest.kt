package com.apptolast.fledge.presentation

import com.apptolast.fledge.data.repository.InMemoryFamilyFoundationRepository
import com.apptolast.fledge.data.repository.InMemoryLedgerRepository
import com.apptolast.fledge.domain.model.ChildPin
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.LedgerTransactionType
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.presentation.foundation.manualadjustment.ManualAdjustmentError
import com.apptolast.fledge.presentation.foundation.manualadjustment.ManualAdjustmentKind
import com.apptolast.fledge.presentation.foundation.manualadjustment.ManualAdjustmentViewModel
import com.apptolast.fledge.presentation.foundation.manualadjustment.parseAmountCents
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ManualAdjustmentViewModelTest {

    @Test
    fun `FLE-20 parent creates bonus adjustment with mandatory concept`() = runTest {
        // Given
        val foundationRepository = InMemoryFamilyFoundationRepository()
        val ledgerRepository = InMemoryLedgerRepository()
        val child = foundationRepository.createChild()
        val viewModel = ManualAdjustmentViewModel(foundationRepository, ledgerRepository)

        // When
        assertFalse(viewModel.uiState.value.canSubmit)
        viewModel.load(child.id)
        viewModel.updateAmount("5,50")
        viewModel.updateConcept("Paga extra por ordenar")

        // Then
        assertTrue(viewModel.uiState.value.canSubmit)
        assertTrue(viewModel.submit())
        val transaction = checkNotNull(viewModel.uiState.value.savedTransaction)
        assertEquals(LedgerTransactionType.Bonus, transaction.type)
        assertEquals(MoneyCents(550), transaction.amountCents)
        assertEquals("Paga extra por ordenar", transaction.concept.value)
        assertEquals(listOf(transaction), ledgerRepository.transactions.value)
    }

    @Test
    fun `FLE-20 penalty without explanation is rejected`() = runTest {
        // Given
        val foundationRepository = InMemoryFamilyFoundationRepository()
        val ledgerRepository = InMemoryLedgerRepository()
        val child = foundationRepository.createChild()
        val viewModel = ManualAdjustmentViewModel(foundationRepository, ledgerRepository)

        // When
        viewModel.load(child.id)
        viewModel.selectKind(ManualAdjustmentKind.Penalty)
        viewModel.updateAmount("1.25")

        // Then
        assertFalse(viewModel.submit())
        assertEquals(ManualAdjustmentError.MissingConcept, viewModel.uiState.value.error)
        assertEquals(emptyList(), ledgerRepository.transactions.value)
    }

    @Test
    fun `FLE-20 penalty stores negative cents after explanation`() = runTest {
        // Given
        val foundationRepository = InMemoryFamilyFoundationRepository()
        val ledgerRepository = InMemoryLedgerRepository()
        val child = foundationRepository.createChild()
        val viewModel = ManualAdjustmentViewModel(foundationRepository, ledgerRepository)

        // When
        viewModel.load(child.id)
        viewModel.selectKind(ManualAdjustmentKind.Penalty)
        viewModel.updateAmount("1.25")
        viewModel.updateConcept("Pantalla rota explicada")

        // Then
        assertTrue(viewModel.submit())
        val transaction = ledgerRepository.transactions.value.single()
        assertEquals(LedgerTransactionType.Penalty, transaction.type)
        assertEquals(MoneyCents(-125), transaction.amountCents)
    }

    @Test
    fun `FLE-19 amount parser uses cents as integers`() {
        // Given / When / Then
        assertEquals(500L, parseAmountCents("5"))
        assertEquals(550L, parseAmountCents("5,5"))
        assertEquals(505L, parseAmountCents("5.05"))
        assertEquals(null, parseAmountCents("5.005"))
        assertEquals(null, parseAmountCents("-5"))
        assertEquals(null, parseAmountCents("5 euros"))
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
}
