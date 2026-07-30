package com.apptolast.fledge.presentation

import com.apptolast.fledge.data.repository.InMemoryFamilyFoundationRepository
import com.apptolast.fledge.data.repository.InMemorySavingsGoalRepository
import com.apptolast.fledge.domain.model.ChildPin
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.MoneyPotType
import com.apptolast.fledge.domain.model.SavingsGoalStatus
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.domain.model.VirtualAccountType
import com.apptolast.fledge.presentation.foundation.savingsgoal.SavingsGoalSetupError
import com.apptolast.fledge.presentation.foundation.savingsgoal.SavingsGoalSetupViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class SavingsGoalSetupViewModelTest {

    @Test
    fun `FLE-36 parent creates child savings goal`() = runTest {
        // Given
        val foundationRepository = InMemoryFamilyFoundationRepository()
        val savingsGoalRepository = InMemorySavingsGoalRepository()
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
        val viewModel = SavingsGoalSetupViewModel(foundationRepository, savingsGoalRepository)

        // When
        viewModel.load(child.id)
        viewModel.updateTitle("Bici nueva")
        viewModel.updateTargetAmount("40,00")
        viewModel.selectIcon("bike")
        val saved = viewModel.submit()

        // Then
        val goal = savingsGoalRepository.goals.value.single()
        assertEquals(true, saved)
        assertEquals(child.id, goal.childProfileId)
        assertEquals("Bici nueva", goal.title)
        assertEquals(MoneyCents(4_000), goal.targetCents)
        assertEquals(MoneyPotType.Save, goal.potType)
        assertEquals(VirtualAccountType.Goal, goal.accountType)
        assertEquals(SavingsGoalStatus.Active, goal.status)
        assertEquals(goal, viewModel.uiState.value.savedGoal)
    }

    @Test
    fun `FLE-50 parent creates child Give goal`() = runTest {
        // Given
        val foundationRepository = InMemoryFamilyFoundationRepository()
        val savingsGoalRepository = InMemorySavingsGoalRepository()
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
        val viewModel = SavingsGoalSetupViewModel(foundationRepository, savingsGoalRepository)

        // When
        viewModel.load(child.id)
        viewModel.selectPotType(MoneyPotType.Give)
        viewModel.updateTitle("Donar al refugio")
        viewModel.updateTargetAmount("20,00")
        viewModel.selectIcon("heart")
        val saved = viewModel.submit()

        // Then
        val goal = savingsGoalRepository.goals.value.single()
        assertEquals(true, saved)
        assertEquals(child.id, goal.childProfileId)
        assertEquals("Donar al refugio", goal.title)
        assertEquals(MoneyCents(2_000), goal.targetCents)
        assertEquals(MoneyPotType.Give, goal.potType)
        assertEquals(VirtualAccountType.Give, goal.accountType)
        assertEquals(SavingsGoalStatus.Active, goal.status)
    }

    @Test
    fun `FLE-36 parent sees validation error for invalid target`() = runTest {
        // Given
        val foundationRepository = InMemoryFamilyFoundationRepository()
        val savingsGoalRepository = InMemorySavingsGoalRepository()
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
        val viewModel = SavingsGoalSetupViewModel(foundationRepository, savingsGoalRepository)

        // When
        viewModel.load(child.id)
        viewModel.updateTitle("Bici nueva")
        viewModel.updateTargetAmount("0")
        viewModel.selectIcon("bike")
        val saved = viewModel.submit()

        // Then
        assertEquals(false, saved)
        assertEquals(SavingsGoalSetupError.InvalidTarget, viewModel.uiState.value.error)
        assertEquals(emptyList(), savingsGoalRepository.goals.value)
    }
}
