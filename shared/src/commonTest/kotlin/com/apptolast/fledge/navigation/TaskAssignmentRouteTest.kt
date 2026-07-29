package com.apptolast.fledge.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class TaskAssignmentRouteTest {

    @Test
    fun `AC-07 TaskAssignmentRoute is type safe`() {
        // Given / When
        val route = TaskAssignmentRoute

        // Then
        assertEquals(TaskAssignmentRoute, route)
    }
}
