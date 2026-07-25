package com.apptolast.fledge.presentation.foundation.childpin

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.presentation.theme.FledgeTheme
import fledge.shared.generated.resources.Res
import fledge.shared.generated.resources.child_pin_body
import fledge.shared.generated.resources.child_pin_placeholder
import fledge.shared.generated.resources.child_pin_reset
import fledge.shared.generated.resources.child_pin_title
import fledge.shared.generated.resources.child_pin_unlock
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChildPinScreen(
    childProfileId: ChildProfileId,
    onUnlocked: () -> Unit,
    onParentalGateRequired: () -> Unit,
    viewModel: ChildPinViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.requiresParentalGate) {
        if (state.requiresParentalGate) {
            onParentalGateRequired()
        }
    }

    ChildPinContent(
        state = state,
        onPinChange = viewModel::updatePin,
        onUnlock = onUnlocked,
        onReset = {
            scope.launch {
                viewModel.requestPinReset(childProfileId)
            }
        },
    )
}

@Composable
fun ChildPinContent(
    state: ChildPinUiState,
    onPinChange: (String) -> Unit,
    onUnlock: () -> Unit,
    onReset: () -> Unit,
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
                text = stringResource(Res.string.child_pin_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.child_pin_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = state.enteredPin,
                onValueChange = onPinChange,
                label = { Text(stringResource(Res.string.child_pin_placeholder)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onUnlock,
                enabled = state.canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(stringResource(Res.string.child_pin_unlock))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(stringResource(Res.string.child_pin_reset))
            }
        }
    }
}

@Preview
@Composable
fun PreviewChildPinContent() {
    FledgeTheme {
        ChildPinContent(
            state = ChildPinUiState(enteredPin = "1234"),
            onPinChange = {},
            onUnlock = {},
            onReset = {},
        )
    }
}
