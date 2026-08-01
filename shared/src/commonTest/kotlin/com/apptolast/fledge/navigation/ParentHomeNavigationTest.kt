package com.apptolast.fledge.navigation

import com.apptolast.fledge.domain.model.ChildProfileId
import kotlin.test.Test
import kotlin.test.assertEquals

class ParentHomeNavigationTest {

    @Test
    fun `FLE-96 AC-02 parent home route can navigate to a child home route`() {
        // Given
        val childProfileId = ChildProfileId("child-elisa")

        // When
        val route = parentChildHomeRoute(childProfileId)

        // Then
        assertEquals(ChildHomeRoute("child-elisa"), route)
    }
}
