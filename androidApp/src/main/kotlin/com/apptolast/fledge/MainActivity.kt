package com.apptolast.fledge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apptolast.fledge.data.auth.FledgeAndroidAuth
import com.apptolast.fledge.di.initFledgeKoin
import org.koin.android.ext.koin.androidContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Must run before any social sign-in: Credential Manager needs the application context.
        FledgeAndroidAuth.attach(this)
        initFledgeKoin {
            androidContext(this@MainActivity)
        }

        setContent {
            App()
        }
    }

    override fun onDestroy() {
        FledgeAndroidAuth.detach(this)
        super.onDestroy()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
