package com.apptolast.fledge.notification

import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.PayloadData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

interface PushNotificationManager {
    suspend fun subscribeToParentApprovals(familyId: FamilyId): String

    suspend fun subscribeToChildApprovals(familyId: FamilyId, childProfileId: ChildProfileId): String
}

object NoOpPushNotificationManager : PushNotificationManager {
    override suspend fun subscribeToParentApprovals(familyId: FamilyId): String =
        TaskPushTopics.parentApprovals(familyId)

    override suspend fun subscribeToChildApprovals(familyId: FamilyId, childProfileId: ChildProfileId): String =
        TaskPushTopics.childApprovals(familyId, childProfileId)
}

class KmpPushNotificationManager(private val topicSubscriber: PushTopicSubscriber = KmpNotifierTopicSubscriber()) :
    PushNotificationManager {
    override suspend fun subscribeToParentApprovals(familyId: FamilyId): String =
        subscribe(TaskPushTopics.parentApprovals(familyId))

    override suspend fun subscribeToChildApprovals(familyId: FamilyId, childProfileId: ChildProfileId): String =
        subscribe(TaskPushTopics.childApprovals(familyId, childProfileId))

    private suspend fun subscribe(topic: String): String {
        RequestedPushTopics.remember(topic)
        topicSubscriber.subscribeToTopic(topic)
        return topic
    }
}

fun interface PushTopicSubscriber {
    suspend fun subscribeToTopic(topic: String)
}

private class KmpNotifierTopicSubscriber : PushTopicSubscriber {
    override suspend fun subscribeToTopic(topic: String) {
        NotifierManager.getPushNotifier().subscribeToTopic(topic)
    }
}

private object RequestedPushTopics {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val topics = MutableStateFlow<Set<String>>(emptySet())

    fun remember(topic: String) {
        topics.value = topics.value + topic
    }

    fun retryAll(topicSubscriber: PushTopicSubscriber = KmpNotifierTopicSubscriber()) {
        topics.value.forEach { topic ->
            scope.launch {
                runCatching { topicSubscriber.subscribeToTopic(topic) }
            }
        }
    }
}

object FledgeNotificationInitializer {
    fun setNotificationListener(onReceived: (title: String?, body: String?) -> Unit = { _, _ -> }) {
        NotifierManager.addListener(
            object : NotifierManager.Listener {
                override fun onNewToken(token: String) {
                    RequestedPushTopics.retryAll()
                }

                override fun onPayloadData(data: PayloadData) = Unit

                override fun onPushNotification(title: String?, body: String?) {
                    super.onPushNotification(title, body)
                    onReceived(title, body)
                }
            },
        )
    }
}
