package com.apptolast.fledge.presentation.foundation.parenthome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apptolast.fledge.domain.model.CashOutSettlement
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.FoundationAction
import com.apptolast.fledge.domain.model.LedgerConcept
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.BalanceCents
import com.apptolast.fledge.domain.model.SettlementId
import com.apptolast.fledge.domain.model.SettlementReminderAudience
import com.apptolast.fledge.domain.model.SettlementReminderLevel
import com.apptolast.fledge.domain.model.SettlementStatus
import com.apptolast.fledge.domain.model.SetupAction
import com.apptolast.fledge.presentation.theme.FledgeTheme
import fledge.shared.generated.resources.Res
import fledge.shared.generated.resources.cash_out_mark_paid
import fledge.shared.generated.resources.cash_out_parent_reminder_fourteen_days
import fledge.shared.generated.resources.cash_out_parent_reminder_seven_days
import fledge.shared.generated.resources.cash_out_status_confirmed
import fledge.shared.generated.resources.cash_out_status_paid_by_parent
import fledge.shared.generated.resources.cash_out_status_requested
import fledge.shared.generated.resources.empty_children
import fledge.shared.generated.resources.parent_home_adjustment
import fledge.shared.generated.resources.parent_home_allowance
import fledge.shared.generated.resources.parent_home_balance
import fledge.shared.generated.resources.parent_home_balance_zero
import fledge.shared.generated.resources.parent_home_children
import fledge.shared.generated.resources.parent_home_add_child
import fledge.shared.generated.resources.parent_home_goal_balance
import fledge.shared.generated.resources.parent_home_gate
import fledge.shared.generated.resources.parent_home_gate_setup
import fledge.shared.generated.resources.parent_home_main_balance
import fledge.shared.generated.resources.parent_home_pairing
import fledge.shared.generated.resources.parent_home_settlements_empty
import fledge.shared.generated.resources.parent_home_settlements_title
import fledge.shared.generated.resources.parent_home_setup
import fledge.shared.generated.resources.parent_home_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ParentHomeScreen(
    onPairChild: (ChildProfileId) -> Unit,
    onConfigureAllowance: (ChildProfileId) -> Unit,
    onAdjustChild: (ChildProfileId) -> Unit,
    onRequireParentalGate: () -> Unit,
    viewModel: ParentHomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    ParentHomeContent(
        state = state,
        onPairChild = onPairChild,
        onConfigureAllowance = onConfigureAllowance,
        onAdjustChild = onAdjustChild,
        onMarkSettlementPaid = { settlementId ->
            scope.launch {
                viewModel.markSettlementPaid(settlementId)
            }
        },
        onRequireParentalGate = { action ->
            scope.launch {
                viewModel.requestProtectedAction(action)
                onRequireParentalGate()
            }
        },
    )
}

@Composable
fun ParentHomeContent(
    state: ParentHomeUiState,
    onPairChild: (ChildProfileId) -> Unit,
    onConfigureAllowance: (ChildProfileId) -> Unit,
    onAdjustChild: (ChildProfileId) -> Unit,
    onMarkSettlementPaid: (SettlementId) -> Unit,
    onRequireParentalGate: (FoundationAction) -> Unit,
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
                    text = stringResource(Res.string.parent_home_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            item {
                SummaryCard()
            }
            item {
                Text(
                    text = stringResource(Res.string.parent_home_children),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            if (state.children.isEmpty()) {
                item {
                    Text(
                        text = stringResource(Res.string.empty_children),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(state.children, key = { it.id.value }) { child ->
                    ChildProfileRow(
                        child = child,
                        mainBalance = state.mainBalances[child.id] ?: BalanceCents(0),
                        goalBalance = state.goalBalances[child.id] ?: BalanceCents(0),
                        currencyCode = state.currencyCode,
                        onPairChild = onPairChild,
                        onConfigureAllowance = onConfigureAllowance,
                        onAdjustChild = onAdjustChild,
                    )
                }
            }
            item {
                Text(
                    text = stringResource(Res.string.parent_home_settlements_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            if (state.pendingSettlements.isEmpty()) {
                item {
                    Text(
                        text = stringResource(Res.string.parent_home_settlements_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(
                    items = state.pendingSettlements.sortedByDescending { it.requestedAt },
                    key = { it.id.value },
                ) { settlement ->
                    ParentSettlementRow(
                        settlement = settlement,
                        childName = state.children.firstOrNull { it.id == settlement.childProfileId }?.displayName
                            ?: settlement.childProfileId.value,
                        currencyCode = state.currencyCode,
                        reminderLevel = state.settlementReminders.firstOrNull {
                            it.settlementId == settlement.id &&
                                it.audience == SettlementReminderAudience.Parent
                        }?.level,
                        onMarkSettlementPaid = onMarkSettlementPaid,
                    )
                }
            }
            item {
                Text(
                    text = stringResource(Res.string.parent_home_setup),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            items(state.setupActions, key = { it.id }) { action ->
                SetupActionRow(action = action, onRequireParentalGate = onRequireParentalGate)
            }
        }
    }
}

@Composable
private fun SummaryCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(Res.string.parent_home_balance),
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.parent_home_balance_zero),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
}

@Composable
private fun ChildProfileRow(
    child: ChildProfile,
    mainBalance: BalanceCents,
    goalBalance: BalanceCents,
    currencyCode: String,
    onPairChild: (ChildProfileId) -> Unit,
    onConfigureAllowance: (ChildProfileId) -> Unit,
    onAdjustChild: (ChildProfileId) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                Text(text = child.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${child.avatarKey} - ${child.birthYear}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = stringResource(
                        Res.string.parent_home_main_balance,
                        formatCents(mainBalance.value, currencyCode),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(
                        Res.string.parent_home_goal_balance,
                        formatCents(goalBalance.value, currencyCode),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(
                    onClick = { onPairChild(child.id) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(Res.string.parent_home_pairing))
                }
                Button(
                    onClick = { onConfigureAllowance(child.id) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(Res.string.parent_home_allowance))
                }
            }
            Button(
                onClick = { onAdjustChild(child.id) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                    Text(stringResource(Res.string.parent_home_adjustment))
            }
        }
    }
}

@Composable
private fun ParentSettlementRow(
    settlement: CashOutSettlement,
    childName: String,
    currencyCode: String,
    reminderLevel: SettlementReminderLevel?,
    onMarkSettlementPaid: (SettlementId) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = childName,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = formatCents(settlement.amountCents.value, currencyCode),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = settlement.concept.value,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = cashOutStatusLabel(settlement.status),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            reminderLevel?.let { level ->
                Text(
                    text = parentReminderText(level),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (settlement.status == SettlementStatus.Requested) {
                Button(
                    onClick = { onMarkSettlementPaid(settlement.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.cash_out_mark_paid))
                }
            }
        }
    }
}

@Composable
private fun cashOutStatusLabel(status: SettlementStatus): String = when (status) {
    SettlementStatus.Requested -> stringResource(Res.string.cash_out_status_requested)
    SettlementStatus.PaidByParent -> stringResource(Res.string.cash_out_status_paid_by_parent)
    SettlementStatus.ConfirmedByChild -> stringResource(Res.string.cash_out_status_confirmed)
}

@Composable
private fun parentReminderText(level: SettlementReminderLevel): String = when (level) {
    SettlementReminderLevel.SevenDays -> stringResource(Res.string.cash_out_parent_reminder_seven_days)
    SettlementReminderLevel.FourteenDays -> stringResource(Res.string.cash_out_parent_reminder_fourteen_days)
}

private fun formatCents(value: Long, currencyCode: String): String {
    val sign = if (value < 0) "-" else ""
    val absolute = if (value < 0) -value else value
    val whole = absolute / 100
    val cents = (absolute % 100).toString().padStart(2, '0')
    return "$sign$whole,$cents $currencyCode"
}

@Composable
private fun SetupActionRow(
    action: SetupAction,
    onRequireParentalGate: (FoundationAction) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = setupActionLabel(action), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = { onRequireParentalGate(FoundationAction.ManageSettings) }) {
                Text(stringResource(Res.string.parent_home_gate))
            }
        }
    }
}

@Composable
private fun setupActionLabel(action: SetupAction): String = when (action.id) {
    "add-child" -> stringResource(Res.string.parent_home_add_child)
    "pair-device" -> stringResource(Res.string.parent_home_pairing)
    else -> stringResource(Res.string.parent_home_gate_setup)
}

@Preview
@Composable
fun PreviewParentHomeContent() {
    FledgeTheme {
        ParentHomeContent(
            state = ParentHomeUiState(
                children = listOf(
                    ChildProfile(
                        id = ChildProfileId("child-1"),
                        displayName = "Lucas",
                        birthYear = 2017,
                        avatarKey = "rocket",
                    )
                ),
                pendingSettlements = listOf(
                    CashOutSettlement(
                        id = SettlementId("settlement-1"),
                        familyId = FamilyId("family-1"),
                        childProfileId = ChildProfileId("child-1"),
                        amountCents = MoneyCents(500),
                        concept = LedgerConcept("Cromos"),
                        status = SettlementStatus.Requested,
                        requestedAt = kotlin.time.Clock.System.now(),
                    )
                ),
            ),
            onPairChild = {},
            onConfigureAllowance = {},
            onAdjustChild = {},
            onMarkSettlementPaid = {},
            onRequireParentalGate = {},
        )
    }
}
