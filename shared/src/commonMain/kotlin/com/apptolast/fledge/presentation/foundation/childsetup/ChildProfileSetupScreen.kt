package com.apptolast.fledge.presentation.foundation.childsetup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.apptolast.fledge.presentation.theme.FledgeTheme
import fledge.shared.generated.resources.Res
import fledge.shared.generated.resources.child_setup_avatar_label
import fledge.shared.generated.resources.child_setup_birth_year_label
import fledge.shared.generated.resources.child_setup_display_name_label
import fledge.shared.generated.resources.child_setup_error_invalid
import fledge.shared.generated.resources.child_setup_error_missing_consent
import fledge.shared.generated.resources.child_setup_error_missing_family
import fledge.shared.generated.resources.child_setup_pin_label
import fledge.shared.generated.resources.child_setup_title
import fledge.shared.generated.resources.continue_action
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChildProfileSetupScreen(onChildCreated: () -> Unit, viewModel: ChildProfileSetupViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    ChildProfileSetupContent(
        state = state,
        onDisplayNameChange = viewModel::updateDisplayName,
        onBirthYearChange = viewModel::updateBirthYear,
        onAvatarKeyChange = viewModel::updateAvatarKey,
        onPinChange = viewModel::updatePin,
        onSubmit = {
            scope.launch {
                if (viewModel.submit()) {
                    onChildCreated()
                }
            }
        },
    )
}

@Composable
fun ChildProfileSetupContent(
    state: ChildProfileSetupUiState,
    onDisplayNameChange: (String) -> Unit,
    onBirthYearChange: (String) -> Unit,
    onAvatarKeyChange: (String) -> Unit,
    onPinChange: (String) -> Unit,
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
                text = stringResource(Res.string.child_setup_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = state.displayName,
                onValueChange = onDisplayNameChange,
                label = { Text(stringResource(Res.string.child_setup_display_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.birthYear,
                onValueChange = onBirthYearChange,
                label = { Text(stringResource(Res.string.child_setup_birth_year_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.avatarKey,
                onValueChange = onAvatarKeyChange,
                label = { Text(stringResource(Res.string.child_setup_avatar_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.pin,
                onValueChange = onPinChange,
                label = { Text(stringResource(Res.string.child_setup_pin_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
            state.error?.let { error ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = when (error) {
                        ChildProfileSetupError.MissingFamily ->
                            stringResource(Res.string.child_setup_error_missing_family)
                        ChildProfileSetupError.MissingVirtualMoneyConsent ->
                            stringResource(Res.string.child_setup_error_missing_consent)
                        ChildProfileSetupError.InvalidInput ->
                            stringResource(Res.string.child_setup_error_invalid)
                    },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
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
                Text(stringResource(Res.string.continue_action))
            }
        }
    }
}

@Preview
@Composable
fun PreviewChildProfileSetupContent() {
    FledgeTheme {
        ChildProfileSetupContent(
            state = ChildProfileSetupUiState(
                displayName = "Lucas",
                birthYear = "2017",
                avatarKey = "rocket",
                pin = "1234",
            ),
            onDisplayNameChange = {},
            onBirthYearChange = {},
            onAvatarKeyChange = {},
            onPinChange = {},
            onSubmit = {},
        )
    }
}
