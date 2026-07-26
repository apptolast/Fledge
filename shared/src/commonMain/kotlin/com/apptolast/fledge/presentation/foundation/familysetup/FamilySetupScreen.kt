package com.apptolast.fledge.presentation.foundation.familysetup

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.presentation.theme.FledgeTheme
import fledge.shared.generated.resources.Res
import fledge.shared.generated.resources.continue_action
import fledge.shared.generated.resources.currency_label
import fledge.shared.generated.resources.family_name_label
import fledge.shared.generated.resources.family_setup_title
import fledge.shared.generated.resources.timezone_label
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FamilySetupScreen(
    onFamilyCreated: () -> Unit,
    viewModel: FamilySetupViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    FamilySetupContent(
        state = state,
        onNameChange = viewModel::updateFamilyName,
        onCurrencyChange = { value ->
            if (value.length == 3) {
                viewModel.updateCurrency(CurrencyCode(value.uppercase()))
            }
        },
        onTimeZoneChange = { value ->
            if (value.isNotBlank()) {
                viewModel.updateTimeZone(TimeZoneId(value))
            }
        },
        onSubmit = {
            scope.launch {
                if (viewModel.submit()) {
                    onFamilyCreated()
                }
            }
        },
    )
}

@Composable
fun FamilySetupContent(
    state: FamilySetupUiState,
    onNameChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onTimeZoneChange: (String) -> Unit,
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
                text = stringResource(Res.string.family_setup_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = state.familyName,
                onValueChange = onNameChange,
                label = { Text(stringResource(Res.string.family_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.currency.value,
                onValueChange = onCurrencyChange,
                label = { Text(stringResource(Res.string.currency_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.timeZone.value,
                onValueChange = onTimeZoneChange,
                label = { Text(stringResource(Res.string.timezone_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
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
fun PreviewFamilySetupContent() {
    FledgeTheme {
        FamilySetupContent(
            state = FamilySetupUiState(familyName = "Familia Garcia"),
            onNameChange = {},
            onCurrencyChange = {},
            onTimeZoneChange = {},
            onSubmit = {},
        )
    }
}
