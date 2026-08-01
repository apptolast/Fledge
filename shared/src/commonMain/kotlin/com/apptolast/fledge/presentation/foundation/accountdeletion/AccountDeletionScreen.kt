package com.apptolast.fledge.presentation.foundation.accountdeletion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apptolast.fledge.domain.model.AccountDeletionState
import com.apptolast.fledge.domain.model.AccountDeletionStatus
import com.apptolast.fledge.presentation.theme.FledgeTheme
import fledge.shared.generated.resources.Res
import fledge.shared.generated.resources.account_deletion_back
import fledge.shared.generated.resources.account_deletion_confirmation_label
import fledge.shared.generated.resources.account_deletion_delete_account
import fledge.shared.generated.resources.account_deletion_error
import fledge.shared.generated.resources.account_deletion_scope_access
import fledge.shared.generated.resources.account_deletion_scope_child_data
import fledge.shared.generated.resources.account_deletion_scope_title
import fledge.shared.generated.resources.account_deletion_scope_virtual_money
import fledge.shared.generated.resources.account_deletion_submitted_body
import fledge.shared.generated.resources.account_deletion_submitted_cta
import fledge.shared.generated.resources.account_deletion_submitted_title
import fledge.shared.generated.resources.account_deletion_subtitle
import fledge.shared.generated.resources.account_deletion_title
import fledge.shared.generated.resources.account_deletion_warning
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AccountDeletionScreen(onBack: () -> Unit, viewModel: AccountDeletionViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    AccountDeletionContent(
        state = state,
        onBack = onBack,
        onConfirmationChange = viewModel::updateConfirmationInput,
        onSubmit = {
            scope.launch {
                viewModel.submit()
            }
        },
    )
}

@Composable
fun AccountDeletionContent(
    state: AccountDeletionUiState,
    onBack: () -> Unit,
    onConfirmationChange: (String) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            AccountDeletionHeader()

            if (state.isSubmitted) {
                SubmittedDeletionCard()
                Button(
                    onClick = onBack,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                ) {
                    Text(stringResource(Res.string.account_deletion_submitted_cta))
                }
            } else {
                WarningCard()
                DeletionScopeCard()
                OutlinedTextField(
                    value = state.confirmationInput,
                    onValueChange = onConfirmationChange,
                    enabled = !state.isBusy,
                    singleLine = true,
                    label = {
                        Text(
                            stringResource(
                                Res.string.account_deletion_confirmation_label,
                                ACCOUNT_DELETION_CONFIRMATION_PHRASE,
                            ),
                        )
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth(),
                )
                state.operationError?.let {
                    Text(
                        text = stringResource(Res.string.account_deletion_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Button(
                    onClick = onSubmit,
                    enabled = state.canSubmit,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(stringResource(Res.string.account_deletion_delete_account))
                }
                OutlinedButton(
                    onClick = onBack,
                    enabled = !state.isBusy,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                ) {
                    Text(stringResource(Res.string.account_deletion_back))
                }
            }
        }
    }
}

@Composable
private fun AccountDeletionHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.account_deletion_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(Res.string.account_deletion_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WarningCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(Res.string.account_deletion_warning),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun DeletionScopeCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(Res.string.account_deletion_scope_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            DeletionScopeRow(stringResource(Res.string.account_deletion_scope_access))
            DeletionScopeRow(stringResource(Res.string.account_deletion_scope_child_data))
            DeletionScopeRow(stringResource(Res.string.account_deletion_scope_virtual_money))
        }
    }
}

@Composable
private fun DeletionScopeRow(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "-",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SubmittedDeletionCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(Res.string.account_deletion_submitted_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(Res.string.account_deletion_submitted_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
fun PreviewAccountDeletionContent() {
    FledgeTheme {
        AccountDeletionContent(
            state = AccountDeletionUiState(confirmationInput = ACCOUNT_DELETION_CONFIRMATION_PHRASE),
            onBack = {},
            onConfirmationChange = {},
            onSubmit = {},
        )
    }
}

@Preview
@Composable
fun PreviewAccountDeletionSubmittedContent() {
    FledgeTheme {
        AccountDeletionContent(
            state = AccountDeletionUiState(
                deletionState = AccountDeletionState(status = AccountDeletionStatus.Requested),
            ),
            onBack = {},
            onConfirmationChange = {},
            onSubmit = {},
        )
    }
}
