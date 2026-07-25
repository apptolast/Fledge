package com.apptolast.fledge.navigation

import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.SharedDeviceRole
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
}
