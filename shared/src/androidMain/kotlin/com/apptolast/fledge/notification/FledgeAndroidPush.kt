package com.apptolast.fledge.notification

import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration

object FledgeAndroidPush {
    private var initialized = false

    fun initialize(notificationIconResId: Int) {
        if (initialized) return
        NotifierManager.initialize(
            configuration = NotificationPlatformConfiguration.Android(
                notificationIconResId = notificationIconResId,
                showPushNotification = true,
            ),
        )
        FledgeNotificationInitializer.setNotificationListener()
        initialized = true
    }
}
