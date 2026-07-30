package com.apptolast.fledge.presentation.foundation.admin

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
import com.apptolast.fledge.domain.model.FamilyAdminInvite
import com.apptolast.fledge.domain.model.FamilyAdminRole
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.presentation.foundation.components.SyncNoticeBanner
import com.apptolast.fledge.presentation.theme.FledgeTheme
import fledge.shared.generated.resources.Res
import fledge.shared.generated.resources.operation_error_sync
import fledge.shared.generated.resources.secondary_admin_active_title
import fledge.shared.generated.resources.secondary_admin_back
import fledge.shared.generated.resources.secondary_admin_email_label
import fledge.shared.generated.resources.secondary_admin_email_support
import fledge.shared.generated.resources.secondary_admin_empty
import fledge.shared.generated.resources.secondary_admin_error_invalid_email
import fledge.shared.generated.resources.secondary_admin_error_missing_family
import fledge.shared.generated.resources.secondary_admin_error_not_owner
import fledge.shared.generated.resources.secondary_admin_intro
import fledge.shared.generated.resources.secondary_admin_invite
import fledge.shared.generated.resources.secondary_admin_invited
import fledge.shared.generated.resources.secondary_admin_not_owner_body
import fledge.shared.generated.resources.secondary_admin_not_owner_title
import fledge.shared.generated.resources.secondary_admin_revoke
import fledge.shared.generated.resources.secondary_admin_revoked
import fledge.shared.generated.resources.secondary_admin_role_admin
import fledge.shared.generated.resources.secondary_admin_summary_body
import fledge.shared.generated.resources.secondary_admin_summary_owner_only
import fledge.shared.generated.resources.secondary_admin_summary_title
import fledge.shared.generated.resources.secondary_admin_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SecondaryAdminScreen(onBack: () -> Unit, viewModel: SecondaryAdminViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    SecondaryAdminContent(
        state = state,
        onEmailChanged = viewModel::updateEmail,
        onInvite = {
            scope.launch { viewModel.invite() }
        },
        onRevoke = viewModel::revoke,
        onBack = onBack,
    )
}

@Composable
fun SecondaryAdminContent(
    state: SecondaryAdminUiState,
    onEmailChanged: (String) -> Unit,
    onInvite: () -> Unit,
    onRevoke: (String) -> Unit,
    onBack: () -> Unit,
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
                    text = stringResource(Res.string.secondary_admin_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            item {
                Text(
                    text = stringResource(Res.string.secondary_admin_intro),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.syncNotice?.let { notice ->
                item { SyncNoticeBanner(notice = notice) }
            }
            item {
                AdminSummaryCard(isOwner = state.isOwner)
            }
            if (!state.isOwner) {
                item {
                    LimitedAdminNotice()
                }
            } else {
                item {
                    OutlinedTextField(
                        value = state.emailInput,
                        onValueChange = onEmailChanged,
                        label = { Text(stringResource(Res.string.secondary_admin_email_label)) },
                        supportingText = { Text(stringResource(Res.string.secondary_admin_email_support)) },
                        isError = state.error == SecondaryAdminError.InvalidEmail,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Button(
                        onClick = onInvite,
                        enabled = state.canInvite,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(Res.string.secondary_admin_invite))
                    }
                }
            }
            item {
                AdminInviteList(
                    invites = state.invites,
                    isOwner = state.isOwner,
                    onRevoke = onRevoke,
                )
            }
            state.error?.let { error ->
                item {
                    Text(
                        text = secondaryAdminErrorText(error),
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
            state.savedInvite?.let { invite ->
                item {
                    Text(
                        text = stringResource(Res.string.secondary_admin_invited, invite.email),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            state.revokedEmail?.let { email ->
                item {
                    Text(
                        text = stringResource(Res.string.secondary_admin_revoked, email),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            item {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text(stringResource(Res.string.secondary_admin_back))
                }
            }
        }
    }
}

@Composable
private fun AdminSummaryCard(isOwner: Boolean) {
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
                text = stringResource(Res.string.secondary_admin_summary_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(Res.string.secondary_admin_summary_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isOwner) {
                Text(
                    text = stringResource(Res.string.secondary_admin_summary_owner_only),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LimitedAdminNotice() {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.secondary_admin_not_owner_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = stringResource(Res.string.secondary_admin_not_owner_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun AdminInviteList(invites: List<FamilyAdminInvite>, isOwner: Boolean, onRevoke: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(Res.string.secondary_admin_active_title),
            style = MaterialTheme.typography.titleMedium,
        )
        if (invites.isEmpty()) {
            Text(
                text = stringResource(Res.string.secondary_admin_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            invites.forEach { invite ->
                AdminInviteRow(invite = invite, isOwner = isOwner, onRevoke = onRevoke)
            }
        }
    }
}

@Composable
private fun AdminInviteRow(invite: FamilyAdminInvite, isOwner: Boolean, onRevoke: (String) -> Unit) {
    Card(
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = invite.email,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(Res.string.secondary_admin_role_admin),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isOwner) {
                OutlinedButton(onClick = { onRevoke(invite.email) }) {
                    Text(stringResource(Res.string.secondary_admin_revoke))
                }
            }
        }
    }
}

@Composable
private fun secondaryAdminErrorText(error: SecondaryAdminError): String = when (error) {
    SecondaryAdminError.MissingFamily -> stringResource(Res.string.secondary_admin_error_missing_family)
    SecondaryAdminError.NotOwner -> stringResource(Res.string.secondary_admin_error_not_owner)
    SecondaryAdminError.InvalidEmail -> stringResource(Res.string.secondary_admin_error_invalid_email)
}

@Preview
@Composable
fun PreviewSecondaryAdminContent() {
    FledgeTheme {
        SecondaryAdminContent(
            state = SecondaryAdminUiState(
                hasFamily = true,
                familyName = "Familia Garcia",
                isOwner = true,
                emailInput = "cristina@example.com",
                invites = listOf(
                    FamilyAdminInvite(
                        familyId = FamilyId("family-1"),
                        email = "cristina@example.com",
                        role = FamilyAdminRole.Admin,
                    ),
                ),
                syncNotice = null,
            ),
            onEmailChanged = {},
            onInvite = {},
            onRevoke = {},
            onBack = {},
        )
    }
}
