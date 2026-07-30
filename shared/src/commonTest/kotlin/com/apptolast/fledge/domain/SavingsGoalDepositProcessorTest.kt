package com.apptolast.fledge.domain

import com.apptolast.fledge.data.repository.InMemoryLedgerRepository
import com.apptolast.fledge.data.repository.InMemorySavingsGoalRepository
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.LedgerActor
import com.apptolast.fledge.domain.model.LedgerConcept
import com.apptolast.fledge.domain.model.LedgerTransactionDraft
import com.apptolast.fledge.domain.model.LedgerTransactionType
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.MoneyPotType
import com.apptolast.fledge.domain.model.SavingsGoal
import com.apptolast.fledge.domain.model.SavingsGoalDraft
import com.apptolast.fledge.domain.model.SavingsGoalId
import com.apptolast.fledge.domain.model.SavingsGoalStatus
import com.apptolast.fledge.domain.model.VirtualAccountType
import com.apptolast.fledge.domain.service.SavingsGoalDepositProcessor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class SavingsGoalDepositProcessorTest {

    @Test
    fun `AC-01 child deposits main balance into an active goal with ledger traceability`() = runTest {
        // Given
        val savingsGoalRepository = InMemorySavingsGoalRepository()
        val ledgerRepository = InMemoryLedgerRepository()
        val familyId = FamilyId("family-1")
        val childProfileId = ChildProfileId("child-1")
        val createdAt = Instant.fromEpochSeconds(1_700_000_000)
        val goal = savingsGoalRepository.saveGoal(
            SavingsGoalDraft(
                familyId = familyId,
                childProfileId = childProfileId,
                title = "Bici nueva",
                targetCents = MoneyCents(4_000),
                iconKey = "bike",
            ),
            createdAt = createdAt,
        )
        ledgerRepository.appendTransaction(
            LedgerTransactionDraft(
                familyId = familyId,
                childProfileId = childProfileId,
                accountType = VirtualAccountType.Main,
                type = LedgerTransactionType.Bonus,
                amountCents = MoneyCents(1_230),
                concept = LedgerConcept("Paga inicial"),
                createdBy = LedgerActor.Parent,
            ),
            createdAt = createdAt,
        )
        val processor = SavingsGoalDepositProcessor(savingsGoalRepository, ledgerRepository)

        // When
        val transfer = processor.depositToGoal(
            goalId = goal.id,
            childProfileId = childProfileId,
            amountCents = MoneyCents(500),
            createdBy = LedgerActor.Child,
            createdAt = Instant.fromEpochSeconds(1_700_000_600),
        )

        // Then
        assertEquals(VirtualAccountType.Main, transfer.debit.accountType)
        assertEquals(MoneyCents(-500), transfer.debit.amountCents)
        assertEquals(VirtualAccountType.Goal, transfer.credit.accountType)
        assertEquals(MoneyCents(500), transfer.credit.amountCents)
        assertEquals(LedgerTransactionType.GoalTransfer, transfer.debit.type)
        assertEquals(LedgerTransactionType.GoalTransfer, transfer.credit.type)
        assertNotNull(transfer.debit.transferGroupId)
        assertEquals(transfer.debit.transferGroupId, transfer.credit.transferGroupId)
        assertEquals("Ahorro: Bici nueva", transfer.debit.concept.value)
        assertEquals(730, ledgerRepository.balanceFor(childProfileId, VirtualAccountType.Main).value)
        assertEquals(500, ledgerRepository.balanceFor(childProfileId, VirtualAccountType.Goal).value)
    }

    @Test
    fun `FLE-50 child deposits main balance into a Give goal`() = runTest {
        // Given
        val savingsGoalRepository = InMemorySavingsGoalRepository()
        val ledgerRepository = InMemoryLedgerRepository()
        val familyId = FamilyId("family-1")
        val childProfileId = ChildProfileId("child-1")
        val createdAt = Instant.fromEpochSeconds(1_700_000_000)
        val goal = savingsGoalRepository.saveGoal(
            SavingsGoalDraft(
                familyId = familyId,
                childProfileId = childProfileId,
                title = "Donar al refugio",
                targetCents = MoneyCents(2_000),
                potType = MoneyPotType.Give,
                iconKey = "heart",
            ),
            createdAt = createdAt,
        )
        ledgerRepository.appendTransaction(
            LedgerTransactionDraft(
                familyId = familyId,
                childProfileId = childProfileId,
                accountType = VirtualAccountType.Main,
                type = LedgerTransactionType.Bonus,
                amountCents = MoneyCents(1_230),
                concept = LedgerConcept("Paga inicial"),
                createdBy = LedgerActor.Parent,
            ),
            createdAt = createdAt,
        )
        val processor = SavingsGoalDepositProcessor(savingsGoalRepository, ledgerRepository)

        // When
        val transfer = processor.depositToGoal(
            goalId = goal.id,
            childProfileId = childProfileId,
            amountCents = MoneyCents(500),
            createdBy = LedgerActor.Child,
            createdAt = Instant.fromEpochSeconds(1_700_000_600),
        )

        // Then
        assertEquals(VirtualAccountType.Main, transfer.debit.accountType)
        assertEquals(VirtualAccountType.Give, transfer.credit.accountType)
        assertEquals(MoneyCents(500), transfer.credit.amountCents)
        assertEquals(730, ledgerRepository.balanceFor(childProfileId, VirtualAccountType.Main).value)
        assertEquals(0, ledgerRepository.balanceFor(childProfileId, VirtualAccountType.Goal).value)
        assertEquals(500, ledgerRepository.balanceFor(childProfileId, VirtualAccountType.Give).value)
    }

    @Test
    fun `AC-02 deposit fails when main balance is insufficient and leaves ledger unchanged`() = runTest {
        // Given
        val savingsGoalRepository = InMemorySavingsGoalRepository()
        val ledgerRepository = InMemoryLedgerRepository()
        val familyId = FamilyId("family-1")
        val childProfileId = ChildProfileId("child-1")
        val goal = savingsGoalRepository.saveGoal(
            SavingsGoalDraft(
                familyId = familyId,
                childProfileId = childProfileId,
                title = "Bici nueva",
                targetCents = MoneyCents(4_000),
                iconKey = "bike",
            ),
            createdAt = Instant.fromEpochSeconds(1_700_000_000),
        )
        ledgerRepository.appendTransaction(
            LedgerTransactionDraft(
                familyId = familyId,
                childProfileId = childProfileId,
                accountType = VirtualAccountType.Main,
                type = LedgerTransactionType.Bonus,
                amountCents = MoneyCents(300),
                concept = LedgerConcept("Paga inicial"),
                createdBy = LedgerActor.Parent,
            ),
        )
        val processor = SavingsGoalDepositProcessor(savingsGoalRepository, ledgerRepository)

        // When / Then
        assertFailsWith<IllegalArgumentException> {
            processor.depositToGoal(
                goalId = goal.id,
                childProfileId = childProfileId,
                amountCents = MoneyCents(500),
            )
        }
        assertEquals(1, ledgerRepository.transactions.value.size)
        assertEquals(300, ledgerRepository.balanceFor(childProfileId, VirtualAccountType.Main).value)
        assertEquals(0, ledgerRepository.balanceFor(childProfileId, VirtualAccountType.Goal).value)
    }

    @Test
    fun `AC-03 deposit fails for archived or sibling goal and leaves ledger unchanged`() = runTest {
        // Given
        val familyId = FamilyId("family-1")
        val childProfileId = ChildProfileId("child-1")
        val siblingProfileId = ChildProfileId("child-2")
        val createdAt = Instant.fromEpochSeconds(1_700_000_000)
        val archivedGoal = savingsGoal(
            id = "archived-goal",
            familyId = familyId,
            childProfileId = childProfileId,
            status = SavingsGoalStatus.Archived,
            createdAt = createdAt,
        )
        val siblingGoal = savingsGoal(
            id = "sibling-goal",
            familyId = familyId,
            childProfileId = siblingProfileId,
            createdAt = createdAt,
        )
        val savingsGoalRepository = InMemorySavingsGoalRepository(listOf(archivedGoal, siblingGoal))
        val ledgerRepository = InMemoryLedgerRepository()
        ledgerRepository.appendTransaction(
            LedgerTransactionDraft(
                familyId = familyId,
                childProfileId = childProfileId,
                accountType = VirtualAccountType.Main,
                type = LedgerTransactionType.Bonus,
                amountCents = MoneyCents(1_000),
                concept = LedgerConcept("Paga inicial"),
                createdBy = LedgerActor.Parent,
            ),
        )
        val processor = SavingsGoalDepositProcessor(savingsGoalRepository, ledgerRepository)

        // When / Then
        assertFailsWith<IllegalArgumentException> {
            processor.depositToGoal(archivedGoal.id, childProfileId, MoneyCents(500))
        }
        assertFailsWith<IllegalArgumentException> {
            processor.depositToGoal(siblingGoal.id, childProfileId, MoneyCents(500))
        }
        assertEquals(1, ledgerRepository.transactions.value.size)
        assertEquals(1_000, ledgerRepository.balanceFor(childProfileId, VirtualAccountType.Main).value)
        assertEquals(0, ledgerRepository.balanceFor(childProfileId, VirtualAccountType.Goal).value)
    }
}

private fun savingsGoal(
    id: String,
    familyId: FamilyId,
    childProfileId: ChildProfileId,
    status: SavingsGoalStatus = SavingsGoalStatus.Active,
    createdAt: Instant,
): SavingsGoal = SavingsGoal(
    id = SavingsGoalId(id),
    familyId = familyId,
    childProfileId = childProfileId,
    title = "Bici nueva",
    targetCents = MoneyCents(4_000),
    accountType = VirtualAccountType.Goal,
    iconKey = "bike",
    status = status,
    createdAt = createdAt,
    updatedAt = createdAt,
)
