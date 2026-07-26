package com.apptolast.fledge.presentation.foundation.virtualconsent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apptolast.fledge.presentation.theme.FledgeTheme
import fledge.shared.generated.resources.Res
import fledge.shared.generated.resources.continue_action
import fledge.shared.generated.resources.virtual_consent_body
import fledge.shared.generated.resources.virtual_consent_checkbox
import fledge.shared.generated.resources.virtual_consent_point_no_custody
import fledge.shared.generated.resources.virtual_consent_point_parent_settles
import fledge.shared.generated.resources.virtual_consent_point_records
import fledge.shared.generated.resources.virtual_consent_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun VirtualMoneyConsentScreen(
    onConsentRecorded: () -> Unit,
    viewModel: VirtualMoneyConsentViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    VirtualMoneyConsentContent(
        state = state,
        onAcceptedChange = viewModel::updateAcceptedTerms,
        onSubmit = {
            scope.launch {
                if (viewModel.submit()) {
                    onConsentRecorded()
                }
            }
        },
    )
}

@Composable
fun VirtualMoneyConsentContent(
    state: VirtualMoneyConsentUiState,
    onAcceptedChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
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
                text = stringResource(Res.string.virtual_consent_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.virtual_consent_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            ConsentPoint(text = stringResource(Res.string.virtual_consent_point_records))
            ConsentPoint(text = stringResource(Res.string.virtual_consent_point_no_custody))
            ConsentPoint(text = stringResource(Res.string.virtual_consent_point_parent_settles))
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = state.acceptedTerms,
                        role = Role.Checkbox,
                        onValueChange = onAcceptedChange,
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = state.acceptedTerms,
                    onCheckedChange = null,
                )
                Text(
                    text = stringResource(Res.string.virtual_consent_checkbox),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onSubmit,
                enabled = state.acceptedTerms,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(stringResource(Res.string.continue_action))
            }
        }
    }
}

@Composable
private fun ConsentPoint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Preview
@Composable
fun PreviewVirtualMoneyConsentContent() {
    FledgeTheme {
        VirtualMoneyConsentContent(
            state = VirtualMoneyConsentUiState(acceptedTerms = true),
            onAcceptedChange = {},
            onSubmit = {},
        )
    }
}
