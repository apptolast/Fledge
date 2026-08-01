package com.apptolast.fledge.presentation.foundation.guest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apptolast.fledge.domain.model.ChildPinHash
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyGuestInvite
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.presentation.foundation.components.SyncNoticeBanner
import com.apptolast.fledge.presentation.theme.FledgeTheme
import fledge.shared.generated.resources.Res
import fledge.shared.generated.resources.guest_invite_back
import fledge.shared.generated.resources.guest_invite_email_label
import fledge.shared.generated.resources.guest_invite_email_support
import fledge.shared.generated.resources.guest_invite_error_invalid_email
import fledge.shared.generated.resources.guest_invite_error_missing_child
import fledge.shared.generated.resources.guest_invite_error_missing_family
import fledge.shared.generated.resources.guest_invite_intro
import fledge.shared.generated.resources.guest_invite_saved
import fledge.shared.generated.resources.guest_invite_submit
import fledge.shared.generated.resources.guest_invite_title
import fledge.shared.generated.resources.operation_error_sync
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GuestSponsorInviteScreen(
    childProfileId: ChildProfileId,
    onBack: () -> Unit,
    viewModel: GuestSponsorInviteViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(childProfileId) {
        viewModel.load(childProfileId)
    }

    GuestSponsorInviteContent(
        state = state,
        onEmailChanged = viewModel::updateEmail,
        onInvite = { scope.launch { viewModel.invite() } },
        onBack = onBack,
    )
}

@Composable
fun GuestSponsorInviteContent(
    state: GuestSponsorInviteUiState,
    onEmailChanged: (String) -> Unit,
    onInvite: () -> Unit,
    onBack: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = stringResource(Res.string.guest_invite_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            state.child?.let { child ->
                item {
                    Text(
                        text = stringResource(Res.string.guest_invite_intro, child.displayName),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            state.syncNotice?.let { notice ->
                item { SyncNoticeBanner(notice = notice) }
            }
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            value = state.emailInput,
                            onValueChange = onEmailChanged,
                            label = { Text(stringResource(Res.string.guest_invite_email_label)) },
                            supportingText = { Text(stringResource(Res.string.guest_invite_email_support)) },
                            isError = state.error == GuestSponsorInviteError.InvalidEmail,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = onInvite,
                            enabled = state.canInvite,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                        ) {
                            Text(stringResource(Res.string.guest_invite_submit))
                        }
                    }
                }
            }
            state.error?.let { error ->
                item {
                    Text(
                        text = guestSponsorInviteErrorText(error),
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
                val childName = state.child?.displayName.orEmpty()
                item {
                    Text(
                        text = stringResource(Res.string.guest_invite_saved, invite.email, childName),
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
                    Text(stringResource(Res.string.guest_invite_back))
                }
            }
        }
    }
}

@Composable
private fun guestSponsorInviteErrorText(error: GuestSponsorInviteError): String = when (error) {
    GuestSponsorInviteError.MissingFamily -> stringResource(Res.string.guest_invite_error_missing_family)
    GuestSponsorInviteError.MissingChild -> stringResource(Res.string.guest_invite_error_missing_child)
    GuestSponsorInviteError.InvalidEmail -> stringResource(Res.string.guest_invite_error_invalid_email)
}

@Preview
@Composable
fun PreviewGuestSponsorInviteContent() {
    FledgeTheme {
        GuestSponsorInviteContent(
            state = GuestSponsorInviteUiState(
                child = ChildProfile(
                    id = ChildProfileId("child-1"),
                    displayName = "Leo",
                    birthYear = 2016,
                    avatarKey = "bike",
                    pinHash = ChildPinHash("0".repeat(64)),
                ),
                emailInput = "abuela@example.com",
                savedInvite = FamilyGuestInvite(
                    familyId = FamilyId("family-1"),
                    email = "abuela@example.com",
                    childProfileIds = listOf(ChildProfileId("child-1")),
                ),
                syncNotice = null,
            ),
            onEmailChanged = {},
            onInvite = {},
            onBack = {},
        )
    }
}
