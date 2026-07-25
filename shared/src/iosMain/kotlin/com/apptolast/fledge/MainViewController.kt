package com.apptolast.fledge

import androidx.compose.ui.window.ComposeUIViewController
import com.apptolast.fledge.di.initFledgeKoin

fun MainViewController() = run {
    initFledgeKoin()
    ComposeUIViewController { App() }
}
