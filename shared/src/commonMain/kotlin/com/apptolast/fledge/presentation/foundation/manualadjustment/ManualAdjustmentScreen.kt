package com.apptolast.fledge.presentation.foundation.manualadjustment

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.presentation.foundation.components.SyncNoticeBanner
import com.apptolast.fledge.presentation.theme.FledgeTheme
import fledge.shared.generated.resources.Res
import fledge.shared.generated.resources.manual_adjustment_amount_label
import fledge.shared.generated.resources.manual_adjustment_back
import fledge.shared.generated.resources.manual_adjustment_child_label
import fledge.shared.generated.resources.manual_adjustment_concept_label
import fledge.shared.generated.resources.manual_adjustment_error_invalid_amount
import fledge.shared.generated.resources.manual_adjustment_error_missing_child
import fledge.shared.generated.resources.manual_adjustment_error_missing_concept
import fledge.shared.generated.resources.manual_adjustment_error_missing_family
import fledge.shared.generated.resources.manual_adjustment_kind_bonus
import fledge.shared.generated.resources.manual_adjustment_kind_gift
import fledge.shared.generated.resources.manual_adjustment_kind_penalty
import fledge.shared.generated.resources.manual_adjustment_save
import fledge.shared.generated.resources.manual_adjustment_title
import fledge.shared.generated.resources.operation_error_sync
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ManualAdjustmentScreen(
    childProfileId: ChildProfileId,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ManualAdjustmentViewModel = koinViewModel(),
) {
    LaunchedEffect(childProfileId) {
        viewModel.load(childProfileId)
    }
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    ManualAdjustmentContent(
        state = state,
        onKindSelected = viewModel::selectKind,
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
fun ManualAdjustmentContent(
    state: ManualAdjustmentUiState,
    onKindSelected: (ManualAdjustmentKind) -> Unit,
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(Res.string.manual_adjustment_title),
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
            AdjustmentKindChips(
                selected = state.kind,
                onKindSelected = onKindSelected,
            )
            OutlinedTextField(
                value = state.amountInput,
                onValueChange = onAmountChanged,
                label = { Text(stringResource(Res.string.manual_adjustment_amount_label)) },
                isError = state.error == ManualAdjustmentError.InvalidAmount,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.concept,
                onValueChange = onConceptChanged,
                label = { Text(stringResource(Res.string.manual_adjustment_concept_label)) },
                isError = state.error == ManualAdjustmentError.MissingConcept,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            state.error?.let { error ->
                Text(
                    text = manualAdjustmentErrorText(error),
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
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onSubmit,
                enabled = state.canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(Res.string.manual_adjustment_save))
            }
            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(Res.string.manual_adjustment_back))
            }
        }
    }
}

@Composable
private fun AdjustmentKindChips(selected: ManualAdjustmentKind, onKindSelected: (ManualAdjustmentKind) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        ManualAdjustmentKind.entries.forEach { kind ->
            FilterChip(
                selected = selected == kind,
                onClick = { onKindSelected(kind) },
                label = { Text(manualAdjustmentKindLabel(kind)) },
            )
        }
    }
}

@Composable
private fun childLabel(child: ChildProfile?): String =
    child?.let { "${stringResource(Res.string.manual_adjustment_child_label)}: ${it.displayName}" }
        ?: stringResource(Res.string.manual_adjustment_error_missing_child)

@Composable
private fun manualAdjustmentKindLabel(kind: ManualAdjustmentKind): String = when (kind) {
    ManualAdjustmentKind.Bonus -> stringResource(Res.string.manual_adjustment_kind_bonus)
    ManualAdjustmentKind.Penalty -> stringResource(Res.string.manual_adjustment_kind_penalty)
    ManualAdjustmentKind.Gift -> stringResource(Res.string.manual_adjustment_kind_gift)
}

@Composable
private fun manualAdjustmentErrorText(error: ManualAdjustmentError): String = when (error) {
    ManualAdjustmentError.MissingFamily -> stringResource(Res.string.manual_adjustment_error_missing_family)
    ManualAdjustmentError.MissingChild -> stringResource(Res.string.manual_adjustment_error_missing_child)
    ManualAdjustmentError.MissingConcept -> stringResource(Res.string.manual_adjustment_error_missing_concept)
    ManualAdjustmentError.InvalidAmount -> stringResource(Res.string.manual_adjustment_error_invalid_amount)
}

@Preview
@Composable
fun PreviewManualAdjustmentContent() {
    FledgeTheme {
        ManualAdjustmentContent(
            state = ManualAdjustmentUiState(
                child = ChildProfile(
                    id = ChildProfileId("child-1"),
                    displayName = "Lucas",
                    birthYear = 2017,
                    avatarKey = "rocket",
                ),
                amountInput = "5,50",
                concept = "Paga extra por ordenar",
                syncNotice = null,
            ),
            onKindSelected = {},
            onAmountChanged = {},
            onConceptChanged = {},
            onBack = {},
            onSubmit = {},
        )
    }
}
