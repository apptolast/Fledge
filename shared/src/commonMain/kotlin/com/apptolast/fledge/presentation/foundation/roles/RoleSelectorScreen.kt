package com.apptolast.fledge.presentation.foundation.roles

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.navigation.FoundationNavigationTarget
import com.apptolast.fledge.presentation.theme.FledgeTheme
import fledge.shared.generated.resources.Res
import fledge.shared.generated.resources.role_body
import fledge.shared.generated.resources.role_child_demo
import fledge.shared.generated.resources.role_parent
import fledge.shared.generated.resources.role_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RoleSelectorScreen(
    onNavigate: (FoundationNavigationTarget) -> Unit,
    viewModel: RoleSelectorViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(state.navigationTarget) {
        state.navigationTarget?.let(onNavigate)
    }

    RoleSelectorContent(
        onParentSelected = viewModel::selectParentMode,
        onChildSelected = { viewModel.selectChildMode(ChildProfileId("demo-child")) },
    )
}

@Composable
fun RoleSelectorContent(
    onParentSelected: () -> Unit,
    onChildSelected: () -> Unit,
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
                text = stringResource(Res.string.role_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.role_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onParentSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(stringResource(Res.string.role_parent))
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onChildSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(stringResource(Res.string.role_child_demo))
            }
        }
    }
}

@Preview
@Composable
fun PreviewRoleSelectorContent() {
    FledgeTheme {
        RoleSelectorContent(
            onParentSelected = {},
            onChildSelected = {},
        )
    }
}
