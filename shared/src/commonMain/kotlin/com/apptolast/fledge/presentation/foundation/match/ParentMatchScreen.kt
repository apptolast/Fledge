package com.apptolast.fledge.presentation.foundation.match

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import fledge.shared.generated.resources.parent_match_back
import fledge.shared.generated.resources.parent_match_cap_label
import fledge.shared.generated.resources.parent_match_cap_support
import fledge.shared.generated.resources.parent_match_disabled_summary
import fledge.shared.generated.resources.parent_match_enable_body
import fledge.shared.generated.resources.parent_match_enable_label
import fledge.shared.generated.resources.parent_match_error_invalid_cap
import fledge.shared.generated.resources.parent_match_error_invalid_rate
import fledge.shared.generated.resources.parent_match_error_missing_family
import fledge.shared.generated.resources.parent_match_intro
import fledge.shared.generated.resources.parent_match_percent_suffix
import fledge.shared.generated.resources.parent_match_rate_label
import fledge.shared.generated.resources.parent_match_rate_support
import fledge.shared.generated.resources.parent_match_save
import fledge.shared.generated.resources.parent_match_saved
import fledge.shared.generated.resources.parent_match_summary
import fledge.shared.generated.resources.parent_match_title
import fledge.shared.generated.resources.parent_match_virtual_only
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ParentMatchScreen(onBack: () -> Unit, onSaved: () -> Unit, viewModel: ParentMatchViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    ParentMatchContent(
        state = state,
        onEnabledChanged = viewModel::updateEnabled,
        onMatchRateChanged = viewModel::updateMatchRate,
        onMaxMatchChanged = viewModel::updateMaxMatch,
        onBack = onBack,
        onSubmit = {
            scope.launch {
                if (viewModel.submit()) onSaved()
            }
        },
    )
}

@Composable
fun ParentMatchContent(
    state: ParentMatchUiState,
    onEnabledChanged: (Boolean) -> Unit,
    onMatchRateChanged: (String) -> Unit,
    onMaxMatchChanged: (String) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = stringResource(Res.string.parent_match_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            item {
                Text(
                    text = stringResource(Res.string.parent_match_intro),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.syncNotice?.let { notice ->
                item {
                    SyncNoticeBanner(notice = notice)
                }
            }
            item {
                MatchSummaryCard(state)
            }
            item {
                MatchToggleRow(
                    enabled = state.enabled,
                    onEnabledChanged = onEnabledChanged,
                )
            }
            item {
                OutlinedTextField(
                    value = state.matchRateInput,
                    onValueChange = onMatchRateChanged,
                    label = { Text(stringResource(Res.string.parent_match_rate_label)) },
                    supportingText = { Text(stringResource(Res.string.parent_match_rate_support)) },
                    suffix = { Text(stringResource(Res.string.parent_match_percent_suffix)) },
                    isError = state.error == ParentMatchError.InvalidRate,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = state.maxMatchInput,
                    onValueChange = onMaxMatchChanged,
                    label = { Text(stringResource(Res.string.parent_match_cap_label)) },
                    supportingText = { Text(stringResource(Res.string.parent_match_cap_support)) },
                    suffix = { Text(state.currencyCode) },
                    isError = state.error == ParentMatchError.InvalidCap,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            state.error?.let { error ->
                item {
                    Text(
                        text = parentMatchErrorText(error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            state.operationError?.let {
                item {
                    Text(
                        text = stringResource(Res.string.operation_error_sync),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            state.savedSettings?.let {
                item {
                    Text(
                        text = stringResource(Res.string.parent_match_saved),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onSubmit,
                        enabled = state.canSubmit,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(Res.string.parent_match_save))
                    }
                    OutlinedButton(
                        onClick = onBack,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(Res.string.parent_match_back))
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchSummaryCard(state: ParentMatchUiState) {
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
                        Res.string.parent_match_summary,
                        state.matchRateInput,
                        formatCents(state.maxMatchCents() ?: 0L, state.currencyCode),
                    )
                } else {
                    stringResource(Res.string.parent_match_disabled_summary)
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(Res.string.parent_match_virtual_only),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MatchToggleRow(enabled: Boolean, onEnabledChanged: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(Res.string.parent_match_enable_label),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(Res.string.parent_match_enable_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = onEnabledChanged)
    }
}

@Composable
private fun parentMatchErrorText(error: ParentMatchError): String = when (error) {
    ParentMatchError.MissingFamily -> stringResource(Res.string.parent_match_error_missing_family)
    ParentMatchError.InvalidRate -> stringResource(Res.string.parent_match_error_invalid_rate)
    ParentMatchError.InvalidCap -> stringResource(Res.string.parent_match_error_invalid_cap)
}

private fun formatCents(value: Long, currencyCode: String): String {
    val whole = value / 100
    val cents = (value % 100).toString().padStart(2, '0')
    return "$whole,$cents $currencyCode"
}

@Preview
@Composable
fun PreviewParentMatchContent() {
    FledgeTheme {
        ParentMatchContent(
            state = ParentMatchUiState(
                hasFamily = true,
                familyName = "Familia Garcia",
                enabled = true,
                matchRateInput = "100,00",
                maxMatchInput = "5,00",
                syncNotice = null,
            ),
            onEnabledChanged = {},
            onMatchRateChanged = {},
            onMaxMatchChanged = {},
            onBack = {},
            onSubmit = {},
        )
    }
}
