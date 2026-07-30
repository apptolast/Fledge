package com.apptolast.fledge.presentation

import com.apptolast.fledge.data.repository.InMemoryFamilyFoundationRepository
import com.apptolast.fledge.data.repository.InMemoryLedgerRepository
import com.apptolast.fledge.domain.model.BalanceCents
import com.apptolast.fledge.domain.model.ChildLedgerBalances
import com.apptolast.fledge.domain.model.ChildPin
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.LedgerActor
import com.apptolast.fledge.domain.model.LedgerConcept
import com.apptolast.fledge.domain.model.LedgerTransaction
import com.apptolast.fledge.domain.model.LedgerTransactionDraft
import com.apptolast.fledge.domain.model.LedgerTransactionType
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.domain.model.TransactionId
import com.apptolast.fledge.domain.model.VirtualAccountType
import com.apptolast.fledge.domain.repository.LedgerRepository
import com.apptolast.fledge.domain.repository.RepositorySyncStatus
import com.apptolast.fledge.presentation.foundation.FoundationOperationError
import com.apptolast.fledge.presentation.foundation.FoundationSyncNotice
import com.apptolast.fledge.presentation.foundation.manualadjustment.ManualAdjustmentError
import com.apptolast.fledge.presentation.foundation.manualadjustment.ManualAdjustmentKind
import com.apptolast.fledge.presentation.foundation.manualadjustment.ManualAdjustmentViewModel
import com.apptolast.fledge.presentation.foundation.manualadjustment.parseAmountCents
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
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
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `FLE-84 cached data notice is exposed while the form remains usable`() = runTest {
        // Given
        val foundationRepository = InMemoryFamilyFoundationRepository()
        val ledgerRepository = FailingLedgerRepository(syncStatus = RepositorySyncStatus.FromCache)
        val child = foundationRepository.createChild()
        val viewModel = ManualAdjustmentViewModel(foundationRepository, ledgerRepository)

        // When
        viewModel.load(child.id)
        advanceUntilIdle()

        // Then
        assertEquals(FoundationSyncNotice.CachedData, viewModel.uiState.value.syncNotice)
        assertEquals(child, viewModel.uiState.value.child)
    }

    @Test
    fun `FLE-84 failed save shows retryable operation error without clearing the form`() = runTest {
        // Given
        val foundationRepository = InMemoryFamilyFoundationRepository()
        val ledgerRepository = FailingLedgerRepository()
        val child = foundationRepository.createChild()
        val viewModel = ManualAdjustmentViewModel(foundationRepository, ledgerRepository)

        // When
        viewModel.load(child.id)
        viewModel.updateAmount("5,50")
        viewModel.updateConcept("Paga extra por ordenar")

        // Then
        assertFalse(viewModel.submit())
        assertEquals(FoundationOperationError.SyncFailed, viewModel.uiState.value.operationError)
        assertEquals("5,50", viewModel.uiState.value.amountInput)
        assertEquals("Paga extra por ordenar", viewModel.uiState.value.concept)
        assertFalse(viewModel.uiState.value.isSaving)
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

private class FailingLedgerRepository(syncStatus: RepositorySyncStatus = RepositorySyncStatus.Synced) :
    LedgerRepository {
    override val syncStatus: StateFlow<RepositorySyncStatus> = MutableStateFlow(syncStatus)
    override val transactions: StateFlow<List<LedgerTransaction>> = MutableStateFlow(emptyList())

    override suspend fun appendTransaction(draft: LedgerTransactionDraft, createdAt: Instant): LedgerTransaction {
        error("Network unavailable")
    }

    override suspend fun reverseTransaction(
        transactionId: TransactionId,
        concept: LedgerConcept,
        createdBy: LedgerActor,
    ): LedgerTransaction {
        error("Network unavailable")
    }

    override fun transactionsFor(childProfileId: ChildProfileId): List<LedgerTransaction> = emptyList()

    override fun balanceFor(childProfileId: ChildProfileId, accountType: VirtualAccountType): BalanceCents =
        BalanceCents(0)

    override fun balancesFor(childProfileId: ChildProfileId): ChildLedgerBalances = ChildLedgerBalances(
        childProfileId = childProfileId,
        main = BalanceCents(0),
        goal = BalanceCents(0),
    )
}
