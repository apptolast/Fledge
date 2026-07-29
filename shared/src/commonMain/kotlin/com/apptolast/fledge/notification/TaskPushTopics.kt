package com.apptolast.fledge.notification

import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.shared.BuildKonfig

enum class FledgePushType(val wireValue: String) {
    TaskSubmitted("task_submitted"),
    TaskApproved("task_approved"),
    ApprovalQueueReminder("approval_queue_reminder"),
    ;

    companion object {
        fun fromWireValue(value: String?): FledgePushType? = entries.firstOrNull { it.wireValue == value }
    }
}

object TaskPushTopics {
    fun parentApprovals(familyId: FamilyId, appEnv: String = BuildKonfig.APP_ENV): String =
        "fledge_${normalizedEnv(appEnv)}_family_${safeTopicPart(familyId.value)}_parents"

    fun parentApprovalReminders(familyId: FamilyId, appEnv: String = BuildKonfig.APP_ENV): String =
        parentApprovals(familyId = familyId, appEnv = appEnv)

    fun childApprovals(
        familyId: FamilyId,
        childProfileId: ChildProfileId,
        appEnv: String = BuildKonfig.APP_ENV,
    ): String = "fledge_${normalizedEnv(appEnv)}_family_${safeTopicPart(familyId.value)}" +
        "_child_${safeTopicPart(childProfileId.value)}"

    private fun normalizedEnv(appEnv: String): String =
        if (appEnv.equals("release", ignoreCase = true)) "release" else "debug"

    private fun safeTopicPart(value: String): String = value.replace(Regex("[^A-Za-z0-9_-]"), "_")
}
