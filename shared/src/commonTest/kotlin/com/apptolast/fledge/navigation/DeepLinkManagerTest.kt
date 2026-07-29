package com.apptolast.fledge.navigation

import com.apptolast.fledge.notification.FledgePushType
import kotlin.test.Test
import kotlin.test.assertEquals

class DeepLinkManagerTest {

    @Test
    fun `FLE-32 task submitted payload creates parent approvals deep link`() {
        val deepLink = deepLinkFromNotificationPayload(
            type = FledgePushType.TaskSubmitted.wireValue,
            familyId = "family-1",
            childProfileId = "child-1",
            taskInstanceId = "task-1",
        )

        assertEquals(
            DeepLink.ParentApprovalQueue(
                familyId = "family-1",
                taskInstanceId = "task-1",
            ),
            deepLink,
        )
    }

    @Test
    fun `FLE-32 task approved payload creates child pin deep link`() {
        val deepLink = deepLinkFromNotificationPayload(
            type = FledgePushType.TaskApproved.wireValue,
            familyId = "family-1",
            childProfileId = "child-1",
            taskInstanceId = "task-1",
        )

        assertEquals(
            DeepLink.ChildTaskApproved(
                familyId = "family-1",
                childProfileId = "child-1",
                taskInstanceId = "task-1",
            ),
            deepLink,
        )
    }

    @Test
    fun `FLE-32 invalid push payload is ignored`() {
        assertEquals(
            null,
            deepLinkFromNotificationPayload(
                type = FledgePushType.TaskApproved.wireValue,
                familyId = "family-1",
                childProfileId = null,
                taskInstanceId = "task-1",
            ),
        )
    }

    @Test
    fun `FLE-33 approval queue reminder payload creates parent approvals deep link`() {
        val deepLink = deepLinkFromNotificationPayload(
            type = FledgePushType.ApprovalQueueReminder.wireValue,
            familyId = "family-1",
            childProfileId = null,
            taskInstanceId = null,
        )

        assertEquals(
            DeepLink.ParentApprovalQueue(
                familyId = "family-1",
                taskInstanceId = null,
            ),
            deepLink,
        )
    }
}
