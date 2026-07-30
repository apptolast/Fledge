package com.apptolast.fledge.data

import app.cash.turbine.test
import com.apptolast.fledge.data.repository.InMemoryLedgerRepository
import com.apptolast.fledge.domain.model.BalanceCents
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.LedgerActor
import com.apptolast.fledge.domain.model.LedgerConcept
import com.apptolast.fledge.domain.model.LedgerTransactionDraft
import com.apptolast.fledge.domain.model.LedgerTransactionType
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.VirtualAccountType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlinx.coroutines.test.runTest

class InMemoryLedgerRepositoryTest {

    @Test
    fun `FLE-18 appends transactions and derives balance from the ledger`() = runTest {
        // Given
        val repository = InMemoryLedgerRepository()
        val childId = ChildProfileId("child-1")

        // When / Then
        repository.transactions.test {
            assertEquals(emptyList(), awaitItem())

            val allowance = repository.appendTransaction(
                sampleDraft(
                    childProfileId = childId,
                    type = LedgerTransactionType.Allowance,
                    amountCents = MoneyCents(500),
                    concept = LedgerConcept("Paga semanal"),
                ),
            )

            assertEquals(listOf(allowance), awaitItem())
            assertEquals(BalanceCents(500), repository.balanceFor(childId, VirtualAccountType.Main))

            val penalty = repository.appendTransaction(
                sampleDraft(
                    childProfileId = childId,
                    type = LedgerTransactionType.Penalty,
                    amountCents = MoneyCents(-125),
                    concept = LedgerConcept("Multa explicada"),
                ),
            )

            assertEquals(listOf(allowance, penalty), awaitItem())
            assertEquals(BalanceCents(375), repository.balanceFor(childId, VirtualAccountType.Main))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `FLE-19 balances are calculated independently for MAIN and GOAL accounts`() = runTest {
        // Given
        val repository = InMemoryLedgerRepository()
        val childId = ChildProfileId("child-1")

        // When
        repository.appendTransaction(
            sampleDraft(
                childProfileId = childId,
                accountType = VirtualAccountType.Main,
                amountCents = MoneyCents(1_000),
            ),
        )
        repository.appendTransaction(
            sampleDraft(
                childProfileId = childId,
                accountType = VirtualAccountType.Goal,
                amountCents = MoneyCents(250),
            ),
        )
        repository.appendTransaction(
            sampleDraft(
                childProfileId = childId,
                accountType = VirtualAccountType.Main,
                amountCents = MoneyCents(-150),
                type = LedgerTransactionType.Penalty,
            ),
        )

        // Then
        val balances = repository.balancesFor(childId)
        assertEquals(BalanceCents(850), balances.main)
        assertEquals(BalanceCents(250), balances.goal)
    }

    @Test
    fun `FLE-19 missing virtual account balance is zero cents`() = runTest {
        // Given
        val repository = InMemoryLedgerRepository()

        // When / Then
        assertEquals(BalanceCents(0), repository.balanceFor(ChildProfileId("child-1"), VirtualAccountType.Main))
    }

    @Test
    fun `FLE-48 interest entries increase the main virtual balance`() = runTest {
        // Given
        val repository = InMemoryLedgerRepository()
        val childId = ChildProfileId("child-1")

        // When
        val interest = repository.appendTransaction(
            sampleDraft(
                childProfileId = childId,
                type = LedgerTransactionType.Interest,
                amountCents = MoneyCents(12),
                concept = LedgerConcept("Interes 202607"),
                createdBy = LedgerActor.System,
            ),
        )

        // Then
        assertEquals(LedgerTransactionType.Interest, interest.type)
        assertEquals(BalanceCents(12), repository.balanceFor(childId, VirtualAccountType.Main))
    }

    @Test
    fun `FLE-18 reversing a transaction appends a compensating entry and keeps the original`() = runTest {
        // Given
        val repository = InMemoryLedgerRepository()
        val original = repository.appendTransaction(
            sampleDraft(
                amountCents = MoneyCents(700),
                concept = LedgerConcept("Regalo abuela"),
                type = LedgerTransactionType.Gift,
            ),
        )

        // When
        val reversal = repository.reverseTransaction(
            transactionId = original.id,
            concept = LedgerConcept("Correccion del regalo"),
            createdBy = LedgerActor.Parent,
        )

        // Then
        assertNotEquals(original.id, reversal.id)
        assertEquals(LedgerTransactionType.Reversal, reversal.type)
        assertEquals(MoneyCents(-700), reversal.amountCents)
        assertEquals(original.id, reversal.reversesTransactionId)
        assertEquals(listOf(original, reversal), repository.transactions.value)
        assertEquals(BalanceCents(0), repository.balanceFor(original.childProfileId, original.accountType))
    }

    @Test
    fun `FLE-18 a transaction cannot be reversed twice`() = runTest {
        // Given
        val repository = InMemoryLedgerRepository()
        val original = repository.appendTransaction(sampleDraft(amountCents = MoneyCents(250)))
        repository.reverseTransaction(original.id, LedgerConcept("Primera correccion"), LedgerActor.Parent)

        // When / Then
        assertFailsWith<IllegalArgumentException> {
            repository.reverseTransaction(original.id, LedgerConcept("Segunda correccion"), LedgerActor.Parent)
        }
    }

    @Test
    fun `FLE-18 reversal entries cannot be reversed`() = runTest {
        // Given
        val repository = InMemoryLedgerRepository()
        val original = repository.appendTransaction(sampleDraft(amountCents = MoneyCents(250)))
        val reversal = repository.reverseTransaction(original.id, LedgerConcept("Correccion"), LedgerActor.Parent)

        // When / Then
        assertFailsWith<IllegalArgumentException> {
            repository.reverseTransaction(reversal.id, LedgerConcept("Revertir correccion"), LedgerActor.Parent)
        }
    }

    private fun sampleDraft(
        familyId: FamilyId = FamilyId("family-1"),
        childProfileId: ChildProfileId = ChildProfileId("child-1"),
        accountType: VirtualAccountType = VirtualAccountType.Main,
        type: LedgerTransactionType = LedgerTransactionType.Bonus,
        amountCents: MoneyCents = MoneyCents(100),
        concept: LedgerConcept = LedgerConcept("Bonus"),
        createdBy: LedgerActor = LedgerActor.Parent,
    ): LedgerTransactionDraft = LedgerTransactionDraft(
        familyId = familyId,
        childProfileId = childProfileId,
        accountType = accountType,
        type = type,
        amountCents = amountCents,
        concept = concept,
        createdBy = createdBy,
    )
}
