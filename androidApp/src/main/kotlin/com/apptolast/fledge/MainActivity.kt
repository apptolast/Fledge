package com.apptolast.fledge

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apptolast.fledge.data.auth.FledgeAndroidAuth
import com.apptolast.fledge.di.initFledgeKoin
import com.apptolast.fledge.navigation.DeepLinkManager
import com.apptolast.fledge.navigation.deepLinkFromNotificationPayload
import com.apptolast.fledge.notification.FledgeAndroidPush
import org.koin.android.ext.koin.androidContext

class MainActivity : ComponentActivity() {
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        // The feature remains usable without permission; the user can re-enable it in system settings.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Must run before any social sign-in: Credential Manager needs the application context.
        FledgeAndroidAuth.attach(this)
        initFledgeKoin {
            androidContext(this@MainActivity)
        }
        initializePushNotifications()
        askNotificationPermission()
        handleNotificationIntent(intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    override fun onDestroy() {
        FledgeAndroidAuth.detach(this)
        super.onDestroy()
    }

    private fun initializePushNotifications() {
        FledgeAndroidPush.initialize(notificationIconResId = R.drawable.ic_notification)
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val extras = intent?.extras ?: return
        val deepLink = deepLinkFromNotificationPayload(
            type = extras.getString("type"),
            familyId = extras.getString("familyId"),
            childProfileId = extras.getString("childProfileId"),
            taskInstanceId = extras.getString("taskInstanceId"),
        )
        deepLink?.let(DeepLinkManager::setDeepLink)
    }

}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
