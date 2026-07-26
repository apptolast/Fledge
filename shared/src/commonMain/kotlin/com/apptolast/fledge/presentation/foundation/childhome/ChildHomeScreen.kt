package com.apptolast.fledge.presentation.foundation.childhome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apptolast.fledge.domain.model.FoundationAction
import com.apptolast.fledge.presentation.theme.FledgeTheme
import fledge.shared.generated.resources.Res
import fledge.shared.generated.resources.child_home_external_link
import fledge.shared.generated.resources.child_home_body
import fledge.shared.generated.resources.child_home_parent_zone
import fledge.shared.generated.resources.child_home_purchase
import fledge.shared.generated.resources.child_home_settings
import fledge.shared.generated.resources.child_home_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChildHomeScreen(
    onParentalGateRequired: () -> Unit,
    viewModel: ChildHomeViewModel = koinViewModel(),
) {
    val scope = rememberCoroutineScope()

    ChildHomeContent(
        onProtectedAction = { action ->
            scope.launch {
                viewModel.requestProtectedAction(action)
                onParentalGateRequired()
            }
        }
    )
}

@Composable
fun ChildHomeContent(
    onProtectedAction: (FoundationAction) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(Res.string.child_home_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(Res.string.child_home_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onProtectedAction(FoundationAction.OpenParentZone) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(stringResource(Res.string.child_home_parent_zone))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { onProtectedAction(FoundationAction.ManageSettings) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(stringResource(Res.string.child_home_settings))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { onProtectedAction(FoundationAction.StartPurchase) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(stringResource(Res.string.child_home_purchase))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { onProtectedAction(FoundationAction.OpenExternalLink) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(stringResource(Res.string.child_home_external_link))
            }
        }
    }
}

@Preview
@Composable
fun PreviewChildHomeContent() {
    FledgeTheme {
        ChildHomeContent(onProtectedAction = {})
    }
}
