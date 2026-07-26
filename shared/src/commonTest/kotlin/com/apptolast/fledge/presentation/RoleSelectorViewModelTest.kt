package com.apptolast.fledge.presentation

import app.cash.turbine.test
import com.apptolast.fledge.data.repository.InMemoryFamilyFoundationRepository
import com.apptolast.fledge.domain.model.ChildPin
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.navigation.FoundationNavigationTarget
import com.apptolast.fledge.presentation.foundation.roles.RoleSelectorViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class RoleSelectorViewModelTest {

    @Test
    fun `AC-09 role selector exposes immutable StateFlow state`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()
        val viewModel = RoleSelectorViewModel(repository)

        // When / Then
        viewModel.uiState.test {
            assertEquals(null, awaitItem().navigationTarget)
            viewModel.selectParentMode()
            assertEquals(FoundationNavigationTarget.ParentAuth, awaitItem().navigationTarget)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `AC-05 child role navigates to pin route`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()
        val family = repository.createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid"))
        repository.recordVirtualMoneyConsent()
        val child = repository.addChildProfile(
            family.id,
            "Lucas",
            birthYear = 2017,
            avatarKey = "rocket",
            pin = ChildPin("1234"),
        )
        val viewModel = RoleSelectorViewModel(repository)

        // When / Then
        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals(listOf(child), initial.childProfiles)
            viewModel.selectChildMode(child.id)
            assertEquals(FoundationNavigationTarget.ChildPin(child.id), awaitItem().navigationTarget)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `FLE-12 child role ignores unknown demo profile`() = runTest {
        // Given
        val repository = InMemoryFamilyFoundationRepository()
        val viewModel = RoleSelectorViewModel(repository)

        // When / Then
        viewModel.uiState.test {
            assertEquals(null, awaitItem().navigationTarget)
            viewModel.selectChildMode(ChildProfileId("demo-child"))
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
