package com.apptolast.fledge.presentation

import com.apptolast.fledge.data.repository.InMemoryFamilyFoundationRepository
import com.apptolast.fledge.data.repository.InMemoryMoneyFlowRepository
import com.apptolast.fledge.domain.model.AllowanceFrequency
import com.apptolast.fledge.domain.model.ChildPin
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.presentation.foundation.allowance.AllowanceRuleError
import com.apptolast.fledge.presentation.foundation.allowance.AllowanceRuleViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class AllowanceRuleViewModelTest {

    @Test
    fun `FLE-22 parent saves monthly allowance rule with day 31`() = runTest {
        // Given
        val foundationRepository = InMemoryFamilyFoundationRepository()
        val moneyFlowRepository = InMemoryMoneyFlowRepository()
        val child = foundationRepository.createChild()
        val viewModel = AllowanceRuleViewModel(foundationRepository, moneyFlowRepository)

        // When
        viewModel.load(child.id)
        viewModel.selectFrequency(AllowanceFrequency.Monthly)
        viewModel.updateDay("31")
        viewModel.updateAmount("10,00")
        viewModel.updateConcept("Paga mensual")

        // Then
        assertTrue(viewModel.submit())
        val rule = checkNotNull(viewModel.uiState.value.savedRule)
        assertEquals(AllowanceFrequency.Monthly, rule.frequency)
        assertEquals(31, rule.day.value)
        assertEquals(MoneyCents(1_000), rule.amountCents)
        assertEquals(listOf(rule), moneyFlowRepository.allowanceRules.value)
    }

    @Test
    fun `FLE-22 weekly allowance rejects day above seven`() = runTest {
        // Given
        val foundationRepository = InMemoryFamilyFoundationRepository()
        val moneyFlowRepository = InMemoryMoneyFlowRepository()
        val child = foundationRepository.createChild()
        val viewModel = AllowanceRuleViewModel(foundationRepository, moneyFlowRepository)

        // When
        viewModel.load(child.id)
        viewModel.selectFrequency(AllowanceFrequency.Weekly)
        viewModel.updateDay("8")
        viewModel.updateAmount("5")
        viewModel.updateConcept("Paga semanal")

        // Then
        assertFalse(viewModel.submit())
        assertEquals(AllowanceRuleError.InvalidDay, viewModel.uiState.value.error)
        assertEquals(emptyList(), moneyFlowRepository.allowanceRules.value)
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
