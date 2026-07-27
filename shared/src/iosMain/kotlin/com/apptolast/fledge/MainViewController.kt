package com.apptolast.fledge

import androidx.compose.ui.window.ComposeUIViewController
import com.apptolast.fledge.di.initFledgeKoin

// Swift calls this entry point as MainViewControllerKt.MainViewController(), so it keeps PascalCase.
@Suppress("ktlint:standard:function-naming")
fun MainViewController() = run {
    initFledgeKoin()
    ComposeUIViewController { App() }
}
