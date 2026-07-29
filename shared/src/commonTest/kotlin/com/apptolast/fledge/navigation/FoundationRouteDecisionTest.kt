package com.apptolast.fledge.navigation

import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.SharedDeviceRole
import com.apptolast.fledge.domain.repository.RepositorySyncStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class FoundationRouteDecisionTest {

    @Test
    fun `AC-02 parent mode delegates auth to BaseLogin`() {
        // Given
        val decider = FoundationRouteDecider()

        // When
        val target = decider.targetForRole(SharedDeviceRole.Parent, childProfileId = null)

        // Then
        assertEquals(FoundationNavigationTarget.ParentAuth, target)
    }

    @Test
    fun `AC-05 child mode requests child pin`() {
        // Given
        val decider = FoundationRouteDecider()
        val childId = ChildProfileId("child-1")

        // When
        val target = decider.targetForRole(SharedDeviceRole.Child, childProfileId = childId)

        // Then
        assertEquals(FoundationNavigationTarget.ChildPin(childId), target)
    }

    @Test
    fun `FLE-92 post login waits while family state is loading`() {
        // Given
        val decider = FoundationRouteDecider()

        // When
        val target = decider.postLoginTarget(
            hasFamily = false,
            hasVirtualMoneyConsent = false,
            hasChildProfiles = false,
            syncStatus = RepositorySyncStatus.Loading,
        )

        // Then
        assertEquals(PostLoginNavigationTarget.Pending, target)
    }

    @Test
    fun `FLE-92 post login sends parents with existing child profiles to parent home`() {
        // Given
        val decider = FoundationRouteDecider()

        // When
        val target = decider.postLoginTarget(
            hasFamily = true,
            hasVirtualMoneyConsent = true,
            hasChildProfiles = true,
            syncStatus = RepositorySyncStatus.Synced,
        )

        // Then
        assertEquals(PostLoginNavigationTarget.ParentHome, target)
    }

    @Test
    fun `FLE-92 post login sends new parents to family setup after sync completes`() {
        // Given
        val decider = FoundationRouteDecider()

        // When
        val target = decider.postLoginTarget(
            hasFamily = false,
            hasVirtualMoneyConsent = false,
            hasChildProfiles = false,
            syncStatus = RepositorySyncStatus.Synced,
        )

        // Then
        assertEquals(PostLoginNavigationTarget.FamilySetup, target)
    }

    @Test
    fun `FLE-92 post login keeps onboarding sequence for partial setup`() {
        // Given
        val decider = FoundationRouteDecider()

        // When / Then
        assertEquals(
            PostLoginNavigationTarget.VirtualMoneyConsent,
            decider.postLoginTarget(
                hasFamily = true,
                hasVirtualMoneyConsent = false,
                hasChildProfiles = false,
                syncStatus = RepositorySyncStatus.Synced,
            ),
        )
        assertEquals(
            PostLoginNavigationTarget.ChildProfileSetup,
            decider.postLoginTarget(
                hasFamily = true,
                hasVirtualMoneyConsent = true,
                hasChildProfiles = false,
                syncStatus = RepositorySyncStatus.Synced,
            ),
        )
    }
}
