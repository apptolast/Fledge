package com.apptolast.fledge.presentation.foundation.allowance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apptolast.fledge.domain.model.AllowanceFrequency
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.presentation.foundation.components.SyncNoticeBanner
import com.apptolast.fledge.presentation.theme.FledgeTheme
import fledge.shared.generated.resources.Res
import fledge.shared.generated.resources.allowance_amount_label
import fledge.shared.generated.resources.allowance_back
import fledge.shared.generated.resources.allowance_child_label
import fledge.shared.generated.resources.allowance_concept_label
import fledge.shared.generated.resources.allowance_day_label
import fledge.shared.generated.resources.allowance_error_invalid_amount
import fledge.shared.generated.resources.allowance_error_invalid_day
import fledge.shared.generated.resources.allowance_error_missing_child
import fledge.shared.generated.resources.allowance_error_missing_concept
import fledge.shared.generated.resources.allowance_error_missing_family
import fledge.shared.generated.resources.allowance_frequency_monthly
import fledge.shared.generated.resources.allowance_frequency_weekly
import fledge.shared.generated.resources.allowance_save
import fledge.shared.generated.resources.allowance_title
import fledge.shared.generated.resources.operation_error_sync
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AllowanceRuleScreen(
    childProfileId: ChildProfileId,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AllowanceRuleViewModel = koinViewModel(),
) {
    LaunchedEffect(childProfileId) {
        viewModel.load(childProfileId)
    }
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    AllowanceRuleContent(
        state = state,
        onFrequencySelected = viewModel::selectFrequency,
        onDayChanged = viewModel::updateDay,
        onAmountChanged = viewModel::updateAmount,
        onConceptChanged = viewModel::updateConcept,
        onBack = onBack,
        onSubmit = {
            scope.launch {
                if (viewModel.submit()) onSaved()
            }
        },
    )
}

@Composable
fun AllowanceRuleContent(
    state: AllowanceRuleUiState,
    onFrequencySelected: (AllowanceFrequency) -> Unit,
    onDayChanged: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onConceptChanged: (String) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(Res.string.allowance_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            state.syncNotice?.let { notice ->
                SyncNoticeBanner(notice = notice)
            }
            Text(
                text = childLabel(state.child),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FrequencyChips(
                selected = state.frequency,
                onFrequencySelected = onFrequencySelected,
            )
            OutlinedTextField(
                value = state.dayInput,
                onValueChange = onDayChanged,
                label = { Text(stringResource(Res.string.allowance_day_label)) },
                isError = state.error == AllowanceRuleError.InvalidDay,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.amountInput,
                onValueChange = onAmountChanged,
                label = { Text(stringResource(Res.string.allowance_amount_label)) },
                isError = state.error == AllowanceRuleError.InvalidAmount,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.concept,
                onValueChange = onConceptChanged,
                label = { Text(stringResource(Res.string.allowance_concept_label)) },
                isError = state.error == AllowanceRuleError.MissingConcept,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            state.error?.let { error ->
                Text(
                    text = allowanceErrorText(error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            state.operationError?.let {
                Text(
                    text = stringResource(Res.string.operation_error_sync),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.heightIn(min = 8.dp))
            Button(
                onClick = onSubmit,
                enabled = state.canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(Res.string.allowance_save))
            }
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(Res.string.allowance_back))
            }
        }
    }
}

@Composable
private fun FrequencyChips(selected: AllowanceFrequency, onFrequencySelected: (AllowanceFrequency) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        AllowanceFrequency.entries.forEach { frequency ->
            FilterChip(
                selected = selected == frequency,
                onClick = { onFrequencySelected(frequency) },
                label = { Text(frequencyLabel(frequency)) },
            )
        }
    }
}

@Composable
private fun childLabel(child: ChildProfile?): String =
    child?.let { "${stringResource(Res.string.allowance_child_label)}: ${it.displayName}" }
        ?: stringResource(Res.string.allowance_error_missing_child)

@Composable
private fun frequencyLabel(frequency: AllowanceFrequency): String = when (frequency) {
    AllowanceFrequency.Weekly -> stringResource(Res.string.allowance_frequency_weekly)
    AllowanceFrequency.Monthly -> stringResource(Res.string.allowance_frequency_monthly)
}

@Composable
private fun allowanceErrorText(error: AllowanceRuleError): String = when (error) {
    AllowanceRuleError.MissingFamily -> stringResource(Res.string.allowance_error_missing_family)
    AllowanceRuleError.MissingChild -> stringResource(Res.string.allowance_error_missing_child)
    AllowanceRuleError.MissingConcept -> stringResource(Res.string.allowance_error_missing_concept)
    AllowanceRuleError.InvalidAmount -> stringResource(Res.string.allowance_error_invalid_amount)
    AllowanceRuleError.InvalidDay -> stringResource(Res.string.allowance_error_invalid_day)
}

@Preview
@Composable
fun PreviewAllowanceRuleContent() {
    FledgeTheme {
        AllowanceRuleContent(
            state = AllowanceRuleUiState(
                child = ChildProfile(
                    id = ChildProfileId("child-1"),
                    displayName = "Lucas",
                    birthYear = 2017,
                    avatarKey = "rocket",
                ),
                frequency = AllowanceFrequency.Monthly,
                dayInput = "31",
                amountInput = "10,00",
                concept = "Paga mensual",
                syncNotice = null,
            ),
            onFrequencySelected = {},
            onDayChanged = {},
            onAmountChanged = {},
            onConceptChanged = {},
            onBack = {},
            onSubmit = {},
        )
    }
}
