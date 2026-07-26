package com.apptolast.fledge.presentation.foundation.cashout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.presentation.theme.FledgeTheme
import fledge.shared.generated.resources.Res
import fledge.shared.generated.resources.cash_out_amount_label
import fledge.shared.generated.resources.cash_out_back
import fledge.shared.generated.resources.cash_out_balance
import fledge.shared.generated.resources.cash_out_child_label
import fledge.shared.generated.resources.cash_out_concept_label
import fledge.shared.generated.resources.cash_out_error_insufficient_balance
import fledge.shared.generated.resources.cash_out_error_invalid_amount
import fledge.shared.generated.resources.cash_out_error_missing_child
import fledge.shared.generated.resources.cash_out_error_missing_concept
import fledge.shared.generated.resources.cash_out_error_missing_family
import fledge.shared.generated.resources.cash_out_save
import fledge.shared.generated.resources.cash_out_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CashOutRequestScreen(
    childProfileId: ChildProfileId,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: CashOutRequestViewModel = koinViewModel(),
) {
    LaunchedEffect(childProfileId) {
        viewModel.load(childProfileId)
    }
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    CashOutRequestContent(
        state = state,
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
fun CashOutRequestContent(
    state: CashOutRequestUiState,
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
                text = stringResource(Res.string.cash_out_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = childLabel(state.child),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    Res.string.cash_out_balance,
                    formatCents(state.mainBalanceCents, state.currencyCode),
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            OutlinedTextField(
                value = state.amountInput,
                onValueChange = onAmountChanged,
                label = { Text(stringResource(Res.string.cash_out_amount_label)) },
                isError = state.error == CashOutRequestError.InvalidAmount ||
                    state.error == CashOutRequestError.InsufficientBalance,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.concept,
                onValueChange = onConceptChanged,
                label = { Text(stringResource(Res.string.cash_out_concept_label)) },
                isError = state.error == CashOutRequestError.MissingConcept,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            state.error?.let { error ->
                Text(
                    text = cashOutErrorText(error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onSubmit,
                enabled = state.child != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(Res.string.cash_out_save))
            }
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(Res.string.cash_out_back))
            }
        }
    }
}

@Composable
private fun childLabel(child: ChildProfile?): String =
    child?.let { "${stringResource(Res.string.cash_out_child_label)}: ${it.displayName}" }
        ?: stringResource(Res.string.cash_out_error_missing_child)

@Composable
private fun cashOutErrorText(error: CashOutRequestError): String = when (error) {
    CashOutRequestError.MissingFamily -> stringResource(Res.string.cash_out_error_missing_family)
    CashOutRequestError.MissingChild -> stringResource(Res.string.cash_out_error_missing_child)
    CashOutRequestError.MissingConcept -> stringResource(Res.string.cash_out_error_missing_concept)
    CashOutRequestError.InvalidAmount -> stringResource(Res.string.cash_out_error_invalid_amount)
    CashOutRequestError.InsufficientBalance -> stringResource(Res.string.cash_out_error_insufficient_balance)
}

private fun formatCents(value: Long, currencyCode: String): String {
    val sign = if (value < 0) "-" else ""
    val absolute = if (value < 0) -value else value
    val whole = absolute / 100
    val cents = (absolute % 100).toString().padStart(2, '0')
    return "$sign$whole,$cents $currencyCode"
}

@Preview
@Composable
fun PreviewCashOutRequestContent() {
    FledgeTheme {
        CashOutRequestContent(
            state = CashOutRequestUiState(
                child = ChildProfile(
                    id = ChildProfileId("child-1"),
                    displayName = "Lucas",
                    birthYear = 2017,
                    avatarKey = "rocket",
                ),
                mainBalanceCents = 1_250,
                amountInput = "5,00",
                concept = "Cromos",
            ),
            onAmountChanged = {},
            onConceptChanged = {},
            onBack = {},
            onSubmit = {},
        )
    }
}
