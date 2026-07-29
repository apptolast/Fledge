package com.apptolast.fledge.presentation.foundation.postlogin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apptolast.fledge.navigation.PostLoginNavigationTarget
import com.apptolast.fledge.presentation.theme.FledgeTheme
import fledge.shared.generated.resources.Res
import fledge.shared.generated.resources.post_login_loading
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PostLoginScreen(
    onNavigateToFamilySetup: () -> Unit,
    onNavigateToVirtualMoneyConsent: () -> Unit,
    onNavigateToChildProfileSetup: () -> Unit,
    onNavigateToParentHome: () -> Unit,
    viewModel: PostLoginViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.target) {
        when (state.target) {
            PostLoginNavigationTarget.Pending -> Unit
            PostLoginNavigationTarget.FamilySetup -> onNavigateToFamilySetup()
            PostLoginNavigationTarget.VirtualMoneyConsent -> onNavigateToVirtualMoneyConsent()
            PostLoginNavigationTarget.ChildProfileSetup -> onNavigateToChildProfileSetup()
            PostLoginNavigationTarget.ParentHome -> onNavigateToParentHome()
        }
    }

    PostLoginResolvingContent()
}

@Composable
fun PostLoginResolvingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator()
            Text(
                text = stringResource(Res.string.post_login_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Preview
@Composable
fun PreviewPostLoginResolvingContent() {
    FledgeTheme {
        PostLoginResolvingContent()
    }
}
