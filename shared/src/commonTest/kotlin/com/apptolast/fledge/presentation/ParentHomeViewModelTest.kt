package com.apptolast.fledge.presentation

import app.cash.turbine.test
import com.apptolast.fledge.data.repository.InMemoryFamilyFoundationRepository
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.TimeZoneId
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
        val family = repository.createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid"))
        repository.addChildProfile(family.id, "Lucas", age = 9, avatarKey = "rocket")
        val viewModel = ParentHomeViewModel(repository)

        // When / Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.children.size)
            assertTrue(state.setupActions.isNotEmpty())
            assertEquals(state.setupActions.size, state.setupActions.map { it.id }.toSet().size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
