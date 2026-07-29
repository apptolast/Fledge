package com.apptolast.fledge.navigation

import com.apptolast.fledge.notification.FledgePushType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class DeepLink {
    data class ParentApprovalQueue(val familyId: String, val taskInstanceId: String) : DeepLink()

    data class ChildTaskApproved(val familyId: String, val childProfileId: String, val taskInstanceId: String) :
        DeepLink()
}

object DeepLinkManager {
    private val mutablePendingDeepLink = MutableStateFlow<DeepLink?>(null)
    val pendingDeepLink: StateFlow<DeepLink?> = mutablePendingDeepLink.asStateFlow()

    fun setDeepLink(deepLink: DeepLink) {
        mutablePendingDeepLink.value = deepLink
    }

    fun consumeDeepLink() {
        mutablePendingDeepLink.value = null
    }
}

fun deepLinkFromNotificationPayload(
    type: String?,
    familyId: String?,
    childProfileId: String?,
    taskInstanceId: String?,
): DeepLink? = when (FledgePushType.fromWireValue(type)) {
    FledgePushType.TaskSubmitted -> {
        val normalizedFamilyId = familyId?.takeIf { it.isNotBlank() } ?: return null
        val normalizedTaskInstanceId = taskInstanceId?.takeIf { it.isNotBlank() } ?: return null
        DeepLink.ParentApprovalQueue(
            familyId = normalizedFamilyId,
            taskInstanceId = normalizedTaskInstanceId,
        )
    }
    FledgePushType.TaskApproved -> {
        val normalizedFamilyId = familyId?.takeIf { it.isNotBlank() } ?: return null
        val normalizedChildProfileId = childProfileId?.takeIf { it.isNotBlank() } ?: return null
        val normalizedTaskInstanceId = taskInstanceId?.takeIf { it.isNotBlank() } ?: return null
        DeepLink.ChildTaskApproved(
            familyId = normalizedFamilyId,
            childProfileId = normalizedChildProfileId,
            taskInstanceId = normalizedTaskInstanceId,
        )
    }
    null -> null
}
