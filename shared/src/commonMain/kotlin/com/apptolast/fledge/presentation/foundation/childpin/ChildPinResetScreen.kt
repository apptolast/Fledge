package com.apptolast.fledge.presentation.foundation.childpin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import fledge.shared.generated.resources.child_pin_error_invalid
import fledge.shared.generated.resources.child_pin_placeholder
import fledge.shared.generated.resources.child_pin_reset_body
import fledge.shared.generated.resources.child_pin_reset_save
import fledge.shared.generated.resources.child_pin_reset_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChildPinResetScreen(
    childProfileId: ChildProfileId,
    onPinSaved: () -> Unit,
    viewModel: ChildPinResetViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    ChildPinResetContent(
        state = state,
        onPinChange = viewModel::updatePin,
        onSubmit = {
            scope.launch {
                if (viewModel.submit(childProfileId)) {
                    onPinSaved()
                }
            }
        },
    )
}

@Composable
fun ChildPinResetContent(state: ChildPinResetUiState, onPinChange: (String) -> Unit, onSubmit: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(Res.string.child_pin_reset_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.child_pin_reset_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = state.newPin,
                onValueChange = onPinChange,
                label = { Text(stringResource(Res.string.child_pin_placeholder)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.error == ChildPinResetError.InvalidPin) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.child_pin_error_invalid),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onSubmit,
                enabled = state.canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(stringResource(Res.string.child_pin_reset_save))
            }
        }
    }
}

@Preview
@Composable
fun PreviewChildPinResetContent() {
    FledgeTheme {
        ChildPinResetContent(
            state = ChildPinResetUiState(newPin = "9876"),
            onPinChange = {},
            onSubmit = {},
        )
    }
}
