package com.apptolast.fledge.notification

import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import kotlin.test.Test
import kotlin.test.assertEquals

class TaskPushTopicsTest {

    @Test
    fun `FLE-32 parent approval topic is deterministic and environment scoped`() {
        assertEquals(
            "fledge_debug_family_family-1_parents",
            TaskPushTopics.parentApprovals(FamilyId("family-1"), appEnv = "debug"),
        )
        assertEquals(
            "fledge_release_family_family_1_parents",
            TaskPushTopics.parentApprovals(FamilyId("family/1"), appEnv = "release"),
        )
    }

    @Test
    fun `FLE-32 child approval topic is deterministic and scoped to the child profile`() {
        assertEquals(
            "fledge_debug_family_family-1_child_child-1",
            TaskPushTopics.childApprovals(
                familyId = FamilyId("family-1"),
                childProfileId = ChildProfileId("child-1"),
                appEnv = "debug",
            ),
        )
    }

    @Test
    fun `FLE-33 approval queue reminders reuse the parent approval topic`() {
        assertEquals(
            TaskPushTopics.parentApprovals(FamilyId("family-1"), appEnv = "release"),
            TaskPushTopics.parentApprovalReminders(FamilyId("family-1"), appEnv = "release"),
        )
    }
}
