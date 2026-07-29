package com.apptolast.fledge.presentation.foundation.parenthome

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apptolast.fledge.domain.model.BalanceCents
import com.apptolast.fledge.domain.model.CashOutSettlement
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.FoundationAction
import com.apptolast.fledge.domain.model.LedgerConcept
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.SettlementId
import com.apptolast.fledge.domain.model.SettlementReminderAudience
import com.apptolast.fledge.domain.model.SettlementReminderLevel
import com.apptolast.fledge.domain.model.SettlementStatus
import com.apptolast.fledge.domain.model.SetupAction
import com.apptolast.fledge.presentation.foundation.components.SyncNoticeBanner
import com.apptolast.fledge.presentation.theme.FledgeTheme
import fledge.shared.generated.resources.Res
import fledge.shared.generated.resources.cash_out_mark_paid
import fledge.shared.generated.resources.cash_out_parent_reminder_fourteen_days
import fledge.shared.generated.resources.cash_out_parent_reminder_seven_days
import fledge.shared.generated.resources.cash_out_status_confirmed
import fledge.shared.generated.resources.cash_out_status_paid_by_parent
import fledge.shared.generated.resources.cash_out_status_requested
import fledge.shared.generated.resources.empty_children
import fledge.shared.generated.resources.operation_error_sync
import fledge.shared.generated.resources.parent_home_add_child
import fledge.shared.generated.resources.parent_home_adjustment
import fledge.shared.generated.resources.parent_home_allowance
import fledge.shared.generated.resources.parent_home_children
import fledge.shared.generated.resources.parent_home_create_task
import fledge.shared.generated.resources.parent_home_gate
import fledge.shared.generated.resources.parent_home_gate_setup
import fledge.shared.generated.resources.parent_home_goal_balance
import fledge.shared.generated.resources.parent_home_main_balance
import fledge.shared.generated.resources.parent_home_pairing
import fledge.shared.generated.resources.parent_home_pending_count
import fledge.shared.generated.resources.parent_home_pending_liquidation
import fledge.shared.generated.resources.parent_home_pending_total
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
    onCreateTask: () -> Unit,
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
        onCreateTask = onCreateTask,
        onMarkSettlementPaid = { settlementId ->
            scope.launch {
                viewModel.markSettlementPaid(settlementId)
            }
        },
        onRequireParentalGate = { action ->
            scope.launch {
                if (viewModel.requestProtectedAction(action)) {
                    onRequireParentalGate()
                }
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
    onCreateTask: () -> Unit,
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
                ParentHomeHeader(
                    title = state.familyName.ifBlank { stringResource(Res.string.parent_home_title) },
                    pendingTotal = state.pendingSettlements.sumOf { it.amountCents.value },
                    currencyCode = state.currencyCode,
                )
            }
            state.syncNotice?.let { notice ->
                item {
                    SyncNoticeBanner(notice = notice)
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
            item {
                SummaryCard(
                    pendingTotal = state.pendingSettlements.sumOf { it.amountCents.value },
                    pendingCount = state.pendingSettlements.size,
                    currencyCode = state.currencyCode,
                )
            }
            item {
                Button(
                    onClick = onCreateTask,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(stringResource(Res.string.parent_home_create_task))
                }
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
                        isBusy = state.isBusy,
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
private fun ParentHomeHeader(title: String, pendingTotal: Long, currencyCode: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(
                Res.string.parent_home_pending_total,
                formatCents(pendingTotal, currencyCode),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SummaryCard(pendingTotal: Long, pendingCount: Int, currencyCode: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.parent_home_pending_liquidation),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatCents(pendingTotal, currencyCode),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(Res.string.parent_home_pending_count, pendingCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(44.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = child.displayName.firstOrNull()?.uppercaseChar()?.toString().orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = child.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(
                            Res.string.parent_home_goal_balance,
                            formatCents(goalBalance.value, currencyCode),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(
                        Res.string.parent_home_main_balance,
                        formatCents(mainBalance.value, currencyCode),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(
                    onClick = { onPairChild(child.id) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                ) {
                    Text(stringResource(Res.string.parent_home_pairing))
                }
                Button(
                    onClick = { onConfigureAllowance(child.id) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                ) {
                    Text(stringResource(Res.string.parent_home_allowance))
                }
            }
            Button(
                onClick = { onAdjustChild(child.id) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
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
    isBusy: Boolean,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
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
                    enabled = !isBusy,
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
private fun SetupActionRow(action: SetupAction, onRequireParentalGate: (FoundationAction) -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
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
                familyName = "Familia Garcia",
                children = listOf(
                    ChildProfile(
                        id = ChildProfileId("child-1"),
                        displayName = "Lucas",
                        birthYear = 2017,
                        avatarKey = "rocket",
                    ),
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
                    ),
                ),
                syncNotice = null,
            ),
            onPairChild = {},
            onConfigureAllowance = {},
            onAdjustChild = {},
            onCreateTask = {},
            onMarkSettlementPaid = {},
            onRequireParentalGate = {},
        )
    }
}
