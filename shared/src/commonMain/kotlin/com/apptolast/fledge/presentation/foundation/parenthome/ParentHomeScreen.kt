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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.apptolast.fledge.domain.model.TaskAssignmentId
import com.apptolast.fledge.domain.model.TaskInstance
import com.apptolast.fledge.domain.model.TaskInstanceId
import com.apptolast.fledge.domain.model.TaskInstanceStatus
import com.apptolast.fledge.domain.model.TaskTemplateId
import com.apptolast.fledge.domain.service.SavingsGoalCompletionNotice
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
import fledge.shared.generated.resources.parent_home_create_goal
import fledge.shared.generated.resources.parent_home_create_task
import fledge.shared.generated.resources.parent_home_gate
import fledge.shared.generated.resources.parent_home_gate_setup
import fledge.shared.generated.resources.parent_home_goal_balance
import fledge.shared.generated.resources.parent_home_goal_completion_body
import fledge.shared.generated.resources.parent_home_goal_completion_title
import fledge.shared.generated.resources.parent_home_main_balance
import fledge.shared.generated.resources.parent_home_pairing
import fledge.shared.generated.resources.parent_home_pending_count
import fledge.shared.generated.resources.parent_home_pending_liquidation
import fledge.shared.generated.resources.parent_home_pending_total
import fledge.shared.generated.resources.parent_home_settlements_empty
import fledge.shared.generated.resources.parent_home_settlements_title
import fledge.shared.generated.resources.parent_home_setup
import fledge.shared.generated.resources.parent_home_task_approval_amount
import fledge.shared.generated.resources.parent_home_task_approval_approve
import fledge.shared.generated.resources.parent_home_task_approval_empty
import fledge.shared.generated.resources.parent_home_task_approval_error_amount
import fledge.shared.generated.resources.parent_home_task_approval_error_reason
import fledge.shared.generated.resources.parent_home_task_approval_original
import fledge.shared.generated.resources.parent_home_task_approval_photo
import fledge.shared.generated.resources.parent_home_task_approval_reason
import fledge.shared.generated.resources.parent_home_task_approval_reject
import fledge.shared.generated.resources.parent_home_task_approvals_title
import fledge.shared.generated.resources.parent_home_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ParentHomeScreen(
    onPairChild: (ChildProfileId) -> Unit,
    onConfigureAllowance: (ChildProfileId) -> Unit,
    onAdjustChild: (ChildProfileId) -> Unit,
    onCreateSavingsGoal: (ChildProfileId) -> Unit,
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
        onCreateSavingsGoal = onCreateSavingsGoal,
        onCreateTask = onCreateTask,
        onMarkSettlementPaid = { settlementId ->
            scope.launch {
                viewModel.markSettlementPaid(settlementId)
            }
        },
        onUpdateApprovalAmount = viewModel::updateApprovalAmount,
        onUpdateRejectionReason = viewModel::updateRejectionReason,
        onApproveTask = { instanceId ->
            scope.launch {
                viewModel.approveTask(instanceId)
            }
        },
        onRejectTask = { instanceId ->
            scope.launch {
                viewModel.rejectTask(instanceId)
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
    onCreateSavingsGoal: (ChildProfileId) -> Unit,
    onCreateTask: () -> Unit,
    onMarkSettlementPaid: (SettlementId) -> Unit,
    onUpdateApprovalAmount: (TaskInstanceId, String) -> Unit,
    onUpdateRejectionReason: (TaskInstanceId, String) -> Unit,
    onApproveTask: (TaskInstanceId) -> Unit,
    onRejectTask: (TaskInstanceId) -> Unit,
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
            if (state.goalCompletionNotices.isNotEmpty()) {
                items(
                    items = state.goalCompletionNotices,
                    key = { "${it.childProfileId.value}-${it.goalId.value}" },
                ) { notice ->
                    ParentGoalCompletionNoticeCard(
                        notice = notice,
                        currencyCode = state.currencyCode,
                    )
                }
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
                    text = stringResource(Res.string.parent_home_task_approvals_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            state.taskApprovalError?.let { error ->
                item {
                    Text(
                        text = taskApprovalErrorText(error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (state.pendingTaskApprovals.isEmpty()) {
                item {
                    Text(
                        text = stringResource(Res.string.parent_home_task_approval_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(state.pendingTaskApprovals, key = { it.id.value }) { instance ->
                    ParentTaskApprovalRow(
                        instance = instance,
                        childName = state.children.firstOrNull { it.id == instance.childProfileId }?.displayName
                            ?: instance.childProfileId.value,
                        currencyCode = state.currencyCode,
                        amountInput = state.approvalAmountInputs[instance.id].orEmpty(),
                        rejectionReason = state.rejectionReasonInputs[instance.id].orEmpty(),
                        onUpdateApprovalAmount = { onUpdateApprovalAmount(instance.id, it) },
                        onUpdateRejectionReason = { onUpdateRejectionReason(instance.id, it) },
                        onApproveTask = { onApproveTask(instance.id) },
                        onRejectTask = { onRejectTask(instance.id) },
                        isBusy = state.isBusy,
                    )
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
                        onCreateSavingsGoal = onCreateSavingsGoal,
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
private fun ParentGoalCompletionNoticeCard(notice: SavingsGoalCompletionNotice, currencyCode: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(Res.string.parent_home_goal_completion_title, notice.childName),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = stringResource(
                    Res.string.parent_home_goal_completion_body,
                    notice.goalTitle,
                    formatCents(notice.currentCents.value, currencyCode),
                    formatCents(notice.targetCents.value, currencyCode),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
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
private fun ParentTaskApprovalRow(
    instance: TaskInstance,
    childName: String,
    currencyCode: String,
    amountInput: String,
    rejectionReason: String,
    onUpdateApprovalAmount: (String) -> Unit,
    onUpdateRejectionReason: (String) -> Unit,
    onApproveTask: () -> Unit,
    onRejectTask: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = childName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = instance.title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Text(
                    text = stringResource(
                        Res.string.parent_home_task_approval_original,
                        formatCents(instance.rewardCents.value, currencyCode),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (instance.photoEvidenceUri != null) {
                Text(
                    text = stringResource(Res.string.parent_home_task_approval_photo),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = amountInput,
                onValueChange = onUpdateApprovalAmount,
                enabled = !isBusy,
                singleLine = true,
                label = { Text(stringResource(Res.string.parent_home_task_approval_amount)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = onApproveTask,
                enabled = !isBusy,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(Res.string.parent_home_task_approval_approve))
            }
            OutlinedTextField(
                value = rejectionReason,
                onValueChange = onUpdateRejectionReason,
                enabled = !isBusy,
                label = { Text(stringResource(Res.string.parent_home_task_approval_reason)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                onClick = onRejectTask,
                enabled = !isBusy,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(Res.string.parent_home_task_approval_reject))
            }
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
    onCreateSavingsGoal: (ChildProfileId) -> Unit,
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(
                    onClick = { onCreateSavingsGoal(child.id) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                ) {
                    Text(stringResource(Res.string.parent_home_create_goal))
                }
                Button(
                    onClick = { onAdjustChild(child.id) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                ) {
                    Text(stringResource(Res.string.parent_home_adjustment))
                }
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
private fun taskApprovalErrorText(error: ParentTaskApprovalError): String = when (error) {
    ParentTaskApprovalError.InvalidAmount -> stringResource(Res.string.parent_home_task_approval_error_amount)
    ParentTaskApprovalError.MissingRejectionReason -> stringResource(Res.string.parent_home_task_approval_error_reason)
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
                goalCompletionNotices = listOf(
                    SavingsGoalCompletionNotice(
                        goalId = com.apptolast.fledge.domain.model.SavingsGoalId("goal-1"),
                        familyId = FamilyId("family-1"),
                        childProfileId = ChildProfileId("child-1"),
                        childName = "Lucas",
                        goalTitle = "Bici nueva",
                        currentCents = BalanceCents(4_200),
                        targetCents = MoneyCents(4_000),
                        completedAt = kotlin.time.Clock.System.now(),
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
                pendingTaskApprovals = listOf(
                    TaskInstance(
                        id = TaskInstanceId("task-1"),
                        familyId = FamilyId("family-1"),
                        taskAssignmentId = TaskAssignmentId("assignment-1"),
                        taskTemplateId = TaskTemplateId("template-1"),
                        childProfileId = ChildProfileId("child-1"),
                        title = "Poner la mesa",
                        rewardCents = MoneyCents(50),
                        requiresPhoto = false,
                        status = TaskInstanceStatus.Submitted,
                        dueAt = kotlin.time.Clock.System.now(),
                        periodKey = "20260729",
                        createdAt = kotlin.time.Clock.System.now(),
                        updatedAt = kotlin.time.Clock.System.now(),
                        submittedAt = kotlin.time.Clock.System.now(),
                    ),
                ),
                approvalAmountInputs = mapOf(TaskInstanceId("task-1") to "0,50"),
                rejectionReasonInputs = mapOf(TaskInstanceId("task-1") to "Falta recoger los vasos."),
                syncNotice = null,
            ),
            onPairChild = {},
            onConfigureAllowance = {},
            onAdjustChild = {},
            onCreateSavingsGoal = {},
            onCreateTask = {},
            onMarkSettlementPaid = {},
            onUpdateApprovalAmount = { _, _ -> },
            onUpdateRejectionReason = { _, _ -> },
            onApproveTask = {},
            onRejectTask = {},
            onRequireParentalGate = {},
        )
    }
}
