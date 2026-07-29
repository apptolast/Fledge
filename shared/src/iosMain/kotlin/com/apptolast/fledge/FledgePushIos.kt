package com.apptolast.fledge

import com.apptolast.fledge.navigation.DeepLinkManager
import com.apptolast.fledge.navigation.deepLinkFromNotificationPayload
import com.apptolast.fledge.notification.FledgeNotificationInitializer
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration

private var fledgePushNotificationsInitialized = false

fun initializeFledgePushNotificationsIos() {
    if (fledgePushNotificationsInitialized) return
    NotifierManager.initialize(
        configuration = NotificationPlatformConfiguration.Ios(
            showPushNotification = true,
            askNotificationPermissionOnStart = false,
            notificationSoundName = null,
        ),
    )
    FledgeNotificationInitializer.setNotificationListener()
    fledgePushNotificationsInitialized = true
}

fun handleFledgeNotificationTap(type: String?, familyId: String?, childProfileId: String?, taskInstanceId: String?) {
    deepLinkFromNotificationPayload(
        type = type,
        familyId = familyId,
        childProfileId = childProfileId,
        taskInstanceId = taskInstanceId,
    )?.let(DeepLinkManager::setDeepLink)
}
