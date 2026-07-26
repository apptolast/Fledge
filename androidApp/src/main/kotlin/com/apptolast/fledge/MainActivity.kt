package com.apptolast.fledge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apptolast.fledge.data.auth.SocialAuthActivityHolder
import com.apptolast.fledge.di.initFledgeKoin
import org.koin.android.ext.koin.androidContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        SocialAuthActivityHolder.attach(this)
        initFledgeKoin {
            androidContext(this@MainActivity)
        }

        setContent {
            App()
        }
    }

    override fun onDestroy() {
        SocialAuthActivityHolder.detach(this)
        super.onDestroy()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
