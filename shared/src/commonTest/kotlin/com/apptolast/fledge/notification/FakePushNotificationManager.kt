package com.apptolast.fledge.notification

import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId

class FakePushNotificationManager : PushNotificationManager {
    val parentSubscriptions = mutableListOf<FamilyId>()
    val childSubscriptions = mutableListOf<Pair<FamilyId, ChildProfileId>>()

    override suspend fun subscribeToParentApprovals(familyId: FamilyId): String {
        parentSubscriptions += familyId
        return TaskPushTopics.parentApprovals(familyId, appEnv = "debug")
    }

    override suspend fun subscribeToChildApprovals(familyId: FamilyId, childProfileId: ChildProfileId): String {
        childSubscriptions += familyId to childProfileId
        return TaskPushTopics.childApprovals(familyId, childProfileId, appEnv = "debug")
    }
}
