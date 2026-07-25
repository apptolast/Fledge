package com.apptolast.fledge.presentation

import app.cash.turbine.test
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.navigation.FoundationNavigationTarget
import com.apptolast.fledge.presentation.foundation.roles.RoleSelectorViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class RoleSelectorViewModelTest {

    @Test
    fun `AC-09 role selector exposes immutable StateFlow state`() = runTest {
        // Given
        val viewModel = RoleSelectorViewModel()

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
        val viewModel = RoleSelectorViewModel()
        val childId = ChildProfileId("child-1")

        // When / Then
        viewModel.uiState.test {
            awaitItem()
            viewModel.selectChildMode(childId)
            assertEquals(FoundationNavigationTarget.ChildPin(childId), awaitItem().navigationTarget)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
