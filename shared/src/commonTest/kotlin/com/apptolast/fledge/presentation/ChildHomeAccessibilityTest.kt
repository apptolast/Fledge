package com.apptolast.fledge.presentation

import com.apptolast.fledge.domain.model.TaskInstanceStatus
import com.apptolast.fledge.domain.service.SavingsGoalProjectionStatus
import com.apptolast.fledge.presentation.foundation.childhome.ChildHomeActionKind
import com.apptolast.fledge.presentation.foundation.childhome.ChildHomeActionPriority
import com.apptolast.fledge.presentation.foundation.childhome.ChildHomeSemanticsRole
import com.apptolast.fledge.presentation.foundation.childhome.childHomeActionSpec
import com.apptolast.fledge.presentation.foundation.childhome.childHomeGoalProgressVisual
import com.apptolast.fledge.presentation.foundation.childhome.childHomeTaskStatusVisual
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ChildHomeAccessibilityTest {

    @Test
    fun `FLE-44 AC-01 primary child actions expose icon and short label`() {
        val primaryActions = ChildHomeActionKind.entries
            .map(::childHomeActionSpec)
            .filter { it.priority == ChildHomeActionPriority.Primary }

        assertTrue(primaryActions.isNotEmpty())
        primaryActions.forEach { spec ->
            assertNotNull(spec.iconKey, "${spec.kind} must expose an icon")
            assertNotNull(spec.labelKey, "${spec.kind} must expose a short label")
            assertTrue(spec.isIconFirst, "${spec.kind} must render the icon before the label")
        }
    }

    @Test
    fun `FLE-44 AC-02 child actions use large minimum touch targets`() {
        ChildHomeActionKind.entries.map(::childHomeActionSpec).forEach { spec ->
            val expectedMinimum = when (spec.priority) {
                ChildHomeActionPriority.Primary -> 56
                ChildHomeActionPriority.Secondary -> 48
            }
            assertTrue(
                spec.minTouchTargetDp >= expectedMinimum,
                "${spec.kind} must be at least ${expectedMinimum}dp",
            )
        }
    }

    @Test
    fun `FLE-44 AC-03 interactive child actions announce button semantics`() {
        ChildHomeActionKind.entries.map(::childHomeActionSpec).forEach { spec ->
            assertEquals(ChildHomeSemanticsRole.Button, spec.semanticsRole, "${spec.kind} must be a button")
            assertNotNull(spec.contentDescriptionKey, "${spec.kind} must expose a content description")
        }
    }

    @Test
    fun `FLE-44 AC-04 task and goal states do not depend on color alone`() {
        TaskInstanceStatus.entries.forEach { status ->
            val visual = childHomeTaskStatusVisual(status)
            assertNotNull(visual.iconKey, "$status must expose a status icon")
            assertNotNull(visual.textKey, "$status must expose status text")
        }

        SavingsGoalProjectionStatus.entries.forEach { status ->
            val visual = childHomeGoalProgressVisual(status)
            assertNotNull(visual.iconKey, "$status must expose a goal progress icon")
            assertNotNull(visual.textKey, "$status must expose goal progress text")
        }
    }

    @Test
    fun `FLE-44 AC-05 accessibility contract avoids exact rigid heights`() {
        ChildHomeActionKind.entries.map(::childHomeActionSpec).forEach { spec ->
            assertFalse(spec.enforcesExactHeight, "${spec.kind} must use min height, not exact height")
        }
    }
}
