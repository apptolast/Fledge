package com.apptolast.fledge.presentation

import app.cash.turbine.test
import com.apptolast.fledge.data.repository.InMemoryFamilyFoundationRepository
import com.apptolast.fledge.domain.model.ChildPin
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.navigation.PostLoginNavigationTarget
import com.apptolast.fledge.notification.FakePushNotificationManager
import com.apptolast.fledge.presentation.foundation.postlogin.PostLoginViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class PostLoginViewModelTest {

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `FLE-32 parent home resolution subscribes the parent approval topic`() = runTest {
        val repository = InMemoryFamilyFoundationRepository()
        val pushNotifications = FakePushNotificationManager()
        val family = repository.createFamily("Familia Garcia", CurrencyCode("EUR"), TimeZoneId("Europe/Madrid"))
        repository.recordVirtualMoneyConsent()
        repository.addChildProfile(
            familyId = family.id,
            displayName = "Lucas",
            birthYear = 2017,
            avatarKey = "rocket",
            pin = ChildPin("1234"),
        )

        val viewModel = PostLoginViewModel(
            repository = repository,
            pushNotifications = pushNotifications,
        )
        viewModel.uiState.test {
            assertEquals(PostLoginNavigationTarget.Pending, awaitItem().target)
            assertEquals(PostLoginNavigationTarget.ParentHome, awaitItem().target)
            runCurrent()
            assertEquals(1, pushNotifications.parentSubscriptions.size)
            assertEquals(FamilyId("family-1"), pushNotifications.parentSubscriptions.single())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
