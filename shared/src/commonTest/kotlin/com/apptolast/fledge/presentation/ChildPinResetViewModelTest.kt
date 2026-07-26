package com.apptolast.fledge.presentation

import com.apptolast.fledge.data.repository.InMemoryFamilyFoundationRepository
import com.apptolast.fledge.domain.model.ChildPin
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.presentation.foundation.childpin.ChildPinResetViewModel
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ChildPinResetViewModelTest {

    @Test
    fun `FLE-13 parent reset stores a new child pin hash`() = runTest {
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
        val viewModel = ChildPinResetViewModel(repository)

        // When
        viewModel.updatePin("9876")
        val saved = viewModel.submit(child.id)

        // Then
        assertTrue(saved)
        assertTrue(viewModel.uiState.value.saved)
        assertFalse(repository.validateChildPin(child.id, ChildPin("1234")) != null)
        assertTrue(repository.validateChildPin(child.id, ChildPin("9876")) != null)
    }
}
