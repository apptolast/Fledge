package com.apptolast.fledge.presentation.foundation.interest

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apptolast.fledge.presentation.foundation.components.SyncNoticeBanner
import com.apptolast.fledge.presentation.theme.FledgeTheme
import fledge.shared.generated.resources.Res
import fledge.shared.generated.resources.operation_error_sync
import fledge.shared.generated.resources.parent_interest_back
import fledge.shared.generated.resources.parent_interest_disabled_summary
import fledge.shared.generated.resources.parent_interest_enable_body
import fledge.shared.generated.resources.parent_interest_enable_label
import fledge.shared.generated.resources.parent_interest_error_invalid_day
import fledge.shared.generated.resources.parent_interest_error_invalid_rate
import fledge.shared.generated.resources.parent_interest_error_missing_family
import fledge.shared.generated.resources.parent_interest_intro
import fledge.shared.generated.resources.parent_interest_last_posted
import fledge.shared.generated.resources.parent_interest_no_last_posted
import fledge.shared.generated.resources.parent_interest_posting_day_label
import fledge.shared.generated.resources.parent_interest_posting_day_support
import fledge.shared.generated.resources.parent_interest_rate_label
import fledge.shared.generated.resources.parent_interest_rate_support
import fledge.shared.generated.resources.parent_interest_save
import fledge.shared.generated.resources.parent_interest_saved
import fledge.shared.generated.resources.parent_interest_summary
import fledge.shared.generated.resources.parent_interest_title
import fledge.shared.generated.resources.parent_interest_virtual_only
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ParentInterestScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ParentInterestViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    ParentInterestContent(
        state = state,
        onEnabledChanged = viewModel::updateEnabled,
        onAnnualRateChanged = viewModel::updateAnnualRate,
        onPostingDayChanged = viewModel::updatePostingDay,
        onBack = onBack,
        onSubmit = {
            scope.launch {
                if (viewModel.submit()) onSaved()
            }
        },
    )
}

@Composable
fun ParentInterestContent(
    state: ParentInterestUiState,
    onEnabledChanged: (Boolean) -> Unit,
    onAnnualRateChanged: (String) -> Unit,
    onPostingDayChanged: (String) -> Unit,
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(Res.string.parent_interest_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(Res.string.parent_interest_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.syncNotice?.let { notice ->
                SyncNoticeBanner(notice = notice)
            }
            InterestSummaryCard(state)
            InterestToggleRow(
                enabled = state.enabled,
                onEnabledChanged = onEnabledChanged,
            )
            OutlinedTextField(
                value = state.annualRateInput,
                onValueChange = onAnnualRateChanged,
                label = { Text(stringResource(Res.string.parent_interest_rate_label)) },
                supportingText = { Text(stringResource(Res.string.parent_interest_rate_support)) },
                isError = state.error == ParentInterestError.InvalidRate,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.postingDayInput,
                onValueChange = onPostingDayChanged,
                label = { Text(stringResource(Res.string.parent_interest_posting_day_label)) },
                supportingText = { Text(stringResource(Res.string.parent_interest_posting_day_support)) },
                isError = state.error == ParentInterestError.InvalidDay,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            state.error?.let { error ->
                Text(
                    text = parentInterestErrorText(error),
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
            state.savedSettings?.let {
                Text(
                    text = stringResource(Res.string.parent_interest_saved),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onSubmit,
                enabled = state.canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(Res.string.parent_interest_save))
            }
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(Res.string.parent_interest_back))
            }
        }
    }
}

@Composable
private fun InterestSummaryCard(state: ParentInterestUiState) {
    Card(
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (state.enabled) {
                    stringResource(
                        Res.string.parent_interest_summary,
                        state.annualRateInput,
                        state.postingDayInput,
                    )
                } else {
                    stringResource(Res.string.parent_interest_disabled_summary)
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(Res.string.parent_interest_virtual_only),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = state.lastPostedPeriodKey?.let {
                    stringResource(Res.string.parent_interest_last_posted, it)
                } ?: stringResource(Res.string.parent_interest_no_last_posted),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InterestToggleRow(enabled: Boolean, onEnabledChanged: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(Res.string.parent_interest_enable_label),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(Res.string.parent_interest_enable_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = onEnabledChanged)
    }
}

@Composable
private fun parentInterestErrorText(error: ParentInterestError): String = when (error) {
    ParentInterestError.MissingFamily -> stringResource(Res.string.parent_interest_error_missing_family)
    ParentInterestError.InvalidRate -> stringResource(Res.string.parent_interest_error_invalid_rate)
    ParentInterestError.InvalidDay -> stringResource(Res.string.parent_interest_error_invalid_day)
}

@Preview
@Composable
fun PreviewParentInterestContent() {
    FledgeTheme {
        ParentInterestContent(
            state = ParentInterestUiState(
                hasFamily = true,
                familyName = "Familia Garcia",
                enabled = true,
                annualRateInput = "2,00",
                postingDayInput = "1",
                lastPostedPeriodKey = "202607",
                syncNotice = null,
            ),
            onEnabledChanged = {},
            onAnnualRateChanged = {},
            onPostingDayChanged = {},
            onBack = {},
            onSubmit = {},
        )
    }
}
