package com.apptolast.fledge

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.apptolast.fledge.navigation.FledgeNavHost
import com.apptolast.fledge.presentation.foundation.onboarding.OnboardingPreviewContent
import com.apptolast.fledge.presentation.theme.FledgeTheme

@Composable
fun App() {
    FledgeTheme {
        FledgeNavHost()
    }
}

@Preview
@Composable
fun PreviewApp() {
    FledgeTheme {
        OnboardingPreviewContent()
    }
}
