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
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.SavingsGoalDraft
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.domain.model.VirtualAccountType
import com.apptolast.fledge.domain.service.SavingsGoalDepositProcessor
import com.apptolast.fledge.presentation.foundation.savingsgoal.SavingsGoalDepositError
import com.apptolast.fledge.presentation.foundation.savingsgoal.SavingsGoalDepositViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class SavingsGoalDepositViewModelTest {

    @Test
    fun `AC-04 child can edit deposit amount and submit from main to goal`() = runTest {
        // Given
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
                iconKey = "bike",
            ),
        )
        ledgerRepository.appendTransaction(
            LedgerTransactionDraft(
                familyId = family.id,
                childProfileId = child.id,
                accountType = VirtualAccountType.Main,
                type = LedgerTransactionType.Bonus,
                amountCents = MoneyCents(1_230),
                concept = LedgerConcept("Paga inicial"),
                createdBy = LedgerActor.Parent,
            ),
        )
        val viewModel = SavingsGoalDepositViewModel(
            familyRepository = foundationRepository,
            savingsGoalRepository = savingsGoalRepository,
            ledgerRepository = ledgerRepository,
            processor = SavingsGoalDepositProcessor(savingsGoalRepository, ledgerRepository),
        )

        // When
        viewModel.load(child.id, goal.id)
        viewModel.updateAmount("5,00")
        val saved = viewModel.submit()

        // Then
        val state = viewModel.uiState.value
        assertEquals(true, saved)
        assertEquals(goal, state.goal)
        assertEquals(BalanceCents(730), state.balances?.main)
        assertEquals(BalanceCents(500), state.balances?.goal)
        assertEquals(LedgerTransactionType.GoalTransfer, state.savedTransfer?.credit?.type)
        assertEquals(null, state.error)
    }

    @Test
    fun `AC-04 child sees validation when deposit amount is invalid or too high`() = runTest {
        // Given
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
                iconKey = "bike",
            ),
        )
        ledgerRepository.appendTransaction(
            LedgerTransactionDraft(
                familyId = family.id,
                childProfileId = child.id,
                accountType = VirtualAccountType.Main,
                type = LedgerTransactionType.Bonus,
                amountCents = MoneyCents(300),
                concept = LedgerConcept("Paga inicial"),
                createdBy = LedgerActor.Parent,
            ),
        )
        val viewModel = SavingsGoalDepositViewModel(
            familyRepository = foundationRepository,
            savingsGoalRepository = savingsGoalRepository,
            ledgerRepository = ledgerRepository,
            processor = SavingsGoalDepositProcessor(savingsGoalRepository, ledgerRepository),
        )

        // When / Then
        viewModel.load(child.id, goal.id)
        viewModel.updateAmount("0")
        assertEquals(false, viewModel.submit())
        assertEquals(SavingsGoalDepositError.InvalidAmount, viewModel.uiState.value.error)

        viewModel.updateAmount("5,00")
        assertEquals(false, viewModel.submit())
        assertEquals(SavingsGoalDepositError.InsufficientMainBalance, viewModel.uiState.value.error)
        assertEquals(1, ledgerRepository.transactions.value.size)
    }
}
