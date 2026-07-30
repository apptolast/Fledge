package com.apptolast.fledge.presentation

import com.apptolast.fledge.data.repository.InMemoryFamilyFoundationRepository
import com.apptolast.fledge.data.repository.InMemoryLedgerRepository
import com.apptolast.fledge.data.repository.InMemorySavingsGoalRepository
import com.apptolast.fledge.domain.model.BalanceCents
import com.apptolast.fledge.domain.model.ChildPin
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.LedgerActor
import com.apptolast.fledge.domain.model.LedgerConcept
import com.apptolast.fledge.domain.model.LedgerTransactionDraft
import com.apptolast.fledge.domain.model.LedgerTransactionType
import com.apptolast.fledge.domain.model.LedgerTransferGroupId
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.MoneyPotType
import com.apptolast.fledge.domain.model.SavingsGoalDraft
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.domain.model.VirtualAccountType
import com.apptolast.fledge.domain.service.SavingsGoalWithdrawalProcessor
import com.apptolast.fledge.presentation.foundation.savingsgoal.SavingsGoalWithdrawalError
import com.apptolast.fledge.presentation.foundation.savingsgoal.SavingsGoalWithdrawalViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class SavingsGoalWithdrawalViewModelTest {

    @Test
    fun `AC-02 child must confirm the opportunity cost before withdrawing`() = runTest {
        // Given
        val fixture = withdrawalFixture(goalBalance = MoneyCents(1_230))
        val viewModel = SavingsGoalWithdrawalViewModel(
            familyRepository = fixture.foundationRepository,
            savingsGoalRepository = fixture.savingsGoalRepository,
            ledgerRepository = fixture.ledgerRepository,
            processor = SavingsGoalWithdrawalProcessor(fixture.savingsGoalRepository, fixture.ledgerRepository),
        )

        // When
        viewModel.load(fixture.childId, fixture.goalId)
        viewModel.updateAmount("5,00")
        val missingConfirmation = viewModel.submit()
        viewModel.confirmOpportunityCost(true)
        val saved = viewModel.submit()

        // Then
        assertEquals(false, missingConfirmation)
        assertEquals(true, saved)
        assertEquals(BalanceCents(500), viewModel.uiState.value.balances?.main)
        assertEquals(BalanceCents(730), viewModel.uiState.value.balances?.goal)
        assertEquals(LedgerTransactionType.GoalTransfer, viewModel.uiState.value.savedTransfer?.credit?.type)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun `FLE-50 child can withdraw Give goal balance back to main`() = runTest {
        // Given
        val fixture = withdrawalFixture(
            goalBalance = MoneyCents(1_230),
            potType = MoneyPotType.Give,
            accountType = VirtualAccountType.Give,
        )
        val viewModel = SavingsGoalWithdrawalViewModel(
            familyRepository = fixture.foundationRepository,
            savingsGoalRepository = fixture.savingsGoalRepository,
            ledgerRepository = fixture.ledgerRepository,
            processor = SavingsGoalWithdrawalProcessor(fixture.savingsGoalRepository, fixture.ledgerRepository),
        )

        // When
        viewModel.load(fixture.childId, fixture.goalId)
        viewModel.updateAmount("5,00")
        viewModel.confirmOpportunityCost(true)
        val saved = viewModel.submit()

        // Then
        assertEquals(true, saved)
        assertEquals(BalanceCents(500), viewModel.uiState.value.balances?.main)
        assertEquals(BalanceCents(0), viewModel.uiState.value.balances?.goal)
        assertEquals(BalanceCents(730), viewModel.uiState.value.balances?.give)
        assertEquals(VirtualAccountType.Give, viewModel.uiState.value.savedTransfer?.debit?.accountType)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun `AC-03 child sees validation when withdrawal amount is invalid or too high`() = runTest {
        // Given
        val fixture = withdrawalFixture(goalBalance = MoneyCents(300))
        val viewModel = SavingsGoalWithdrawalViewModel(
            familyRepository = fixture.foundationRepository,
            savingsGoalRepository = fixture.savingsGoalRepository,
            ledgerRepository = fixture.ledgerRepository,
            processor = SavingsGoalWithdrawalProcessor(fixture.savingsGoalRepository, fixture.ledgerRepository),
        )

        // When / Then
        viewModel.load(fixture.childId, fixture.goalId)
        viewModel.confirmOpportunityCost(true)
        viewModel.updateAmount("0")
        assertEquals(false, viewModel.submit())
        assertEquals(SavingsGoalWithdrawalError.InvalidAmount, viewModel.uiState.value.error)

        viewModel.updateAmount("5,00")
        assertEquals(false, viewModel.submit())
        assertEquals(SavingsGoalWithdrawalError.InsufficientGoalBalance, viewModel.uiState.value.error)
        assertEquals(1, fixture.ledgerRepository.transactions.value.size)
    }
}

private data class WithdrawalFixture(
    val foundationRepository: InMemoryFamilyFoundationRepository,
    val savingsGoalRepository: InMemorySavingsGoalRepository,
    val ledgerRepository: InMemoryLedgerRepository,
    val childId: com.apptolast.fledge.domain.model.ChildProfileId,
    val goalId: com.apptolast.fledge.domain.model.SavingsGoalId,
)

private suspend fun withdrawalFixture(
    goalBalance: MoneyCents,
    potType: MoneyPotType = MoneyPotType.Save,
    accountType: VirtualAccountType = VirtualAccountType.Goal,
): WithdrawalFixture {
    val foundationRepository = InMemoryFamilyFoundationRepository()
    val savingsGoalRepository = InMemorySavingsGoalRepository()
    val ledgerRepository = InMemoryLedgerRepository()
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
    val goal = savingsGoalRepository.saveGoal(
        SavingsGoalDraft(
            familyId = family.id,
            childProfileId = child.id,
            title = "Bici nueva",
            targetCents = MoneyCents(4_000),
            potType = potType,
            iconKey = "bike",
        ),
    )
    ledgerRepository.appendTransaction(
        LedgerTransactionDraft(
            familyId = family.id,
            childProfileId = child.id,
            accountType = accountType,
            type = LedgerTransactionType.GoalTransfer,
            amountCents = goalBalance,
            concept = LedgerConcept("Ahorro bici"),
            createdBy = LedgerActor.Child,
            transferGroupId = LedgerTransferGroupId("seed-transfer"),
        ),
    )
    return WithdrawalFixture(
        foundationRepository = foundationRepository,
        savingsGoalRepository = savingsGoalRepository,
        ledgerRepository = ledgerRepository,
        childId = child.id,
        goalId = goal.id,
    )
}
