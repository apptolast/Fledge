package com.apptolast.fledge.presentation.foundation.guest

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.FamilyGuestInvite
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.GuestContributionKind
import com.apptolast.fledge.domain.model.GuestSponsorAccess
import com.apptolast.fledge.presentation.foundation.components.SyncNoticeBanner
import com.apptolast.fledge.presentation.theme.FledgeTheme
import fledge.shared.generated.resources.Res
import fledge.shared.generated.resources.guest_home_amount_currency_label
import fledge.shared.generated.resources.guest_home_amount_label
import fledge.shared.generated.resources.guest_home_child_label
import fledge.shared.generated.resources.guest_home_concept_label
import fledge.shared.generated.resources.guest_home_concept_placeholder
import fledge.shared.generated.resources.guest_home_error_invalid_amount
import fledge.shared.generated.resources.guest_home_error_missing_access
import fledge.shared.generated.resources.guest_home_error_missing_child
import fledge.shared.generated.resources.guest_home_error_missing_concept
import fledge.shared.generated.resources.guest_home_intro
import fledge.shared.generated.resources.guest_home_kind_gift
import fledge.shared.generated.resources.guest_home_kind_match
import fledge.shared.generated.resources.guest_home_no_access_body
import fledge.shared.generated.resources.guest_home_no_access_title
import fledge.shared.generated.resources.guest_home_saved
import fledge.shared.generated.resources.guest_home_submit
import fledge.shared.generated.resources.guest_home_title
import fledge.shared.generated.resources.operation_error_sync
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GuestHomeScreen(viewModel: GuestHomeViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    GuestHomeContent(
        state = state,
        onChildSelected = viewModel::selectChild,
        onKindSelected = viewModel::selectKind,
        onAmountChanged = viewModel::updateAmount,
        onConceptChanged = viewModel::updateConcept,
        onSubmit = { scope.launch { viewModel.submit() } },
    )
}

@Composable
fun GuestHomeContent(
    state: GuestHomeUiState,
    onChildSelected: (ChildProfileId) -> Unit,
    onKindSelected: (GuestContributionKind) -> Unit,
    onAmountChanged: (String) -> Unit,
    onConceptChanged: (String) -> Unit,
    onSubmit: () -> Unit,
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
                    text = stringResource(Res.string.guest_home_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            state.syncNotice?.let { notice ->
                item { SyncNoticeBanner(notice = notice) }
            }
            val access = state.access
            if (access == null) {
                item { GuestNoAccessCard() }
            } else {
                item {
                    Text(
                        text = stringResource(Res.string.guest_home_intro, access.familyName),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    GuestChildSelector(
                        children = access.children,
                        selectedChildProfileId = state.selectedChildProfileId,
                        onChildSelected = onChildSelected,
                    )
                }
                item {
                    GuestContributionForm(
                        state = state,
                        currencyCode = access.currency.value,
                        onKindSelected = onKindSelected,
                        onAmountChanged = onAmountChanged,
                        onConceptChanged = onConceptChanged,
                        onSubmit = onSubmit,
                    )
                }
            }
            state.error?.let { error ->
                item {
                    Text(
                        text = guestHomeErrorText(error),
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
            state.savedTransaction?.let { transaction ->
                item {
                    Text(
                        text = stringResource(
                            Res.string.guest_home_saved,
                            formatGuestCents(transaction.amountCents.value, state.access?.currency?.value ?: "EUR"),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun GuestNoAccessCard() {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.guest_home_no_access_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(Res.string.guest_home_no_access_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GuestChildSelector(
    children: List<ChildProfile>,
    selectedChildProfileId: ChildProfileId?,
    onChildSelected: (ChildProfileId) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.guest_home_child_label),
            style = MaterialTheme.typography.titleMedium,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            children.forEach { child ->
                FilterChip(
                    selected = child.id == selectedChildProfileId,
                    onClick = { onChildSelected(child.id) },
                    label = { Text(child.displayName) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun GuestContributionForm(
    state: GuestHomeUiState,
    currencyCode: String,
    onKindSelected: (GuestContributionKind) -> Unit,
    onAmountChanged: (String) -> Unit,
    onConceptChanged: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val amountLabel = stringResource(Res.string.guest_home_amount_label)
    Card(
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                GuestContributionKind.entries.forEach { kind ->
                    FilterChip(
                        selected = state.kind == kind,
                        onClick = { onKindSelected(kind) },
                        label = { Text(guestContributionKindLabel(kind)) },
                    )
                }
            }
            OutlinedTextField(
                value = state.amountInput,
                onValueChange = onAmountChanged,
                label = {
                    Text(
                        stringResource(
                            Res.string.guest_home_amount_currency_label,
                            amountLabel,
                            currencyCode,
                        ),
                    )
                },
                isError = state.error == GuestHomeError.InvalidAmount,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.concept,
                onValueChange = onConceptChanged,
                label = { Text(stringResource(Res.string.guest_home_concept_label)) },
                placeholder = { Text(stringResource(Res.string.guest_home_concept_placeholder)) },
                isError = state.error == GuestHomeError.MissingConcept,
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = onSubmit,
                enabled = state.canSubmit,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(Res.string.guest_home_submit))
            }
        }
    }
}

@Composable
private fun guestContributionKindLabel(kind: GuestContributionKind): String = when (kind) {
    GuestContributionKind.Gift -> stringResource(Res.string.guest_home_kind_gift)
    GuestContributionKind.Match -> stringResource(Res.string.guest_home_kind_match)
}

@Composable
private fun guestHomeErrorText(error: GuestHomeError): String = when (error) {
    GuestHomeError.MissingAccess -> stringResource(Res.string.guest_home_error_missing_access)
    GuestHomeError.MissingChild -> stringResource(Res.string.guest_home_error_missing_child)
    GuestHomeError.MissingConcept -> stringResource(Res.string.guest_home_error_missing_concept)
    GuestHomeError.InvalidAmount -> stringResource(Res.string.guest_home_error_invalid_amount)
}

private fun formatGuestCents(value: Long, currencyCode: String): String {
    val abs = kotlin.math.abs(value)
    val whole = abs / 100
    val cents = (abs % 100).toString().padStart(2, '0')
    val sign = if (value < 0) "-" else ""
    return "$sign$whole,$cents $currencyCode"
}

@Preview
@Composable
fun PreviewGuestHomeContent() {
    FledgeTheme {
        val child = ChildProfile(
            id = ChildProfileId("child-1"),
            displayName = "Leo",
            birthYear = 2016,
            avatarKey = "bike",
            pinHash = ChildPinHash("0".repeat(64)),
        )
        GuestHomeContent(
            state = GuestHomeUiState(
                access = GuestSponsorAccess(
                    invite = FamilyGuestInvite(
                        familyId = FamilyId("family-1"),
                        email = "abuela@example.com",
                        childProfileIds = listOf(child.id),
                    ),
                    familyName = "Familia Garcia",
                    currency = CurrencyCode("EUR"),
                    children = listOf(child),
                ),
                selectedChildProfileId = child.id,
                amountInput = "10,00",
                concept = "Cumpleanos",
                syncNotice = null,
            ),
            onChildSelected = {},
            onKindSelected = {},
            onAmountChanged = {},
            onConceptChanged = {},
            onSubmit = {},
        )
    }
}
