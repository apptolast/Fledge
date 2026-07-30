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
import com.apptolast.fledge.presentation.foundation.components.ActionableEmptyStateCard
import com.apptolast.fledge.presentation.foundation.components.SyncNoticeBanner
import com.apptolast.fledge.presentation.theme.FledgeTheme
import fledge.shared.generated.resources.Res
import fledge.shared.generated.resources.cash_out_mark_paid
import fledge.shared.generated.resources.cash_out_parent_reminder_fourteen_days
import fledge.shared.generated.resources.cash_out_parent_reminder_seven_days
import fledge.shared.generated.resources.cash_out_status_confirmed
import fledge.shared.generated.resources.cash_out_status_paid_by_parent
import fledge.shared.generated.resources.cash_out_status_requested
import fledge.shared.generated.resources.operation_error_sync
import fledge.shared.generated.resources.parent_home_account_deletion
import fledge.shared.generated.resources.parent_home_account_deletion_review
import fledge.shared.generated.resources.parent_home_add_child
import fledge.shared.generated.resources.parent_home_adjustment
import fledge.shared.generated.resources.parent_home_allowance
import fledge.shared.generated.resources.parent_home_children
import fledge.shared.generated.resources.parent_home_children_empty_body
import fledge.shared.generated.resources.parent_home_children_empty_title
import fledge.shared.generated.resources.parent_home_create_goal
import fledge.shared.generated.resources.parent_home_create_task
import fledge.shared.generated.resources.parent_home_first_run_empty_body
import fledge.shared.generated.resources.parent_home_first_run_empty_title
import fledge.shared.generated.resources.parent_home_gate
import fledge.shared.generated.resources.parent_home_gate_setup
import fledge.shared.generated.resources.parent_home_goal_balance
import fledge.shared.generated.resources.parent_home_goal_completion_body
import fledge.shared.generated.resources.parent_home_goal_completion_title
import fledge.shared.generated.resources.parent_home_invite_guest
import fledge.shared.generated.resources.parent_home_main_balance
import fledge.shared.generated.resources.parent_home_pairing
import fledge.shared.generated.resources.parent_home_parent_interest
import fledge.shared.generated.resources.parent_home_parent_interest_review
import fledge.shared.generated.resources.parent_home_parent_match
import fledge.shared.generated.resources.parent_home_parent_match_review
import fledge.shared.generated.resources.parent_home_pending_count
import fledge.shared.generated.resources.parent_home_pending_liquidation
import fledge.shared.generated.resources.parent_home_pending_total
import fledge.shared.generated.resources.parent_home_savings_goal_empty_body
import fledge.shared.generated.resources.parent_home_savings_goal_empty_title
import fledge.shared.generated.resources.parent_home_savings_goals_title
import fledge.shared.generated.resources.parent_home_secondary_admin
import fledge.shared.generated.resources.parent_home_secondary_admin_review
import fledge.shared.generated.resources.parent_home_settlements_empty_body
import fledge.shared.generated.resources.parent_home_settlements_title
import fledge.shared.generated.resources.parent_home_setup
import fledge.shared.generated.resources.parent_home_statement_export
import fledge.shared.generated.resources.parent_home_statement_export_body
import fledge.shared.generated.resources.parent_home_statement_export_review
import fledge.shared.generated.resources.parent_home_task_approval_amount
import fledge.shared.generated.resources.parent_home_task_approval_approve
import fledge.shared.generated.resources.parent_home_task_approval_error_amount
import fledge.shared.generated.resources.parent_home_task_approval_error_reason
import fledge.shared.generated.resources.parent_home_task_approval_original
import fledge.shared.generated.resources.parent_home_task_approval_photo
import fledge.shared.generated.resources.parent_home_task_approval_reason
import fledge.shared.generated.resources.parent_home_task_approval_reject
import fledge.shared.generated.resources.parent_home_task_approvals_title
import fledge.shared.generated.resources.parent_home_task_empty_action_body
import fledge.shared.generated.resources.parent_home_task_empty_first_run_body
import fledge.shared.generated.resources.parent_home_title
import fledge.shared.generated.resources.parent_home_weekly_digest
import fledge.shared.generated.resources.parent_home_weekly_digest_body
import fledge.shared.generated.resources.parent_home_weekly_digest_review
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ParentHomeScreen(
    onAddChild: () -> Unit,
    onPairChild: (ChildProfileId) -> Unit,
    onConfigureAllowance: (ChildProfileId) -> Unit,
    onAdjustChild: (ChildProfileId) -> Unit,
    onCreateSavingsGoal: (ChildProfileId) -> Unit,
    onInviteGuest: (ChildProfileId) -> Unit,
    onOpenWeeklyDigest: () -> Unit,
    onOpenStatementExport: () -> Unit,
    onCreateTask: () -> Unit,
    onOpenParentInterest: () -> Unit,
    onOpenParentMatch: () -> Unit,
    onOpenSecondaryAdmin: () -> Unit,
    onOpenAccountDeletion: () -> Unit,
    onRequireParentalGate: () -> Unit,
    viewModel: ParentHomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    ParentHomeContent(
        state = state,
        onAddChild = onAddChild,
        onPairChild = onPairChild,
        onConfigureAllowance = onConfigureAllowance,
        onAdjustChild = onAdjustChild,
        onCreateSavingsGoal = onCreateSavingsGoal,
        onInviteGuest = onInviteGuest,
        onOpenWeeklyDigest = onOpenWeeklyDigest,
        onOpenStatementExport = onOpenStatementExport,
        onCreateTask = onCreateTask,
        onOpenParentInterest = onOpenParentInterest,
        onOpenParentMatch = onOpenParentMatch,
        onOpenSecondaryAdmin = onOpenSecondaryAdmin,
        onOpenAccountDeletion = onOpenAccountDeletion,
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
    onAddChild: () -> Unit,
    onPairChild: (ChildProfileId) -> Unit,
    onConfigureAllowance: (ChildProfileId) -> Unit,
    onAdjustChild: (ChildProfileId) -> Unit,
    onCreateSavingsGoal: (ChildProfileId) -> Unit,
    onInviteGuest: (ChildProfileId) -> Unit,
    onOpenWeeklyDigest: () -> Unit,
    onOpenStatementExport: () -> Unit,
    onCreateTask: () -> Unit,
    onOpenParentInterest: () -> Unit,
    onOpenParentMatch: () -> Unit,
    onOpenSecondaryAdmin: () -> Unit,
    onOpenAccountDeletion: () -> Unit,
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
            if (state.children.isEmpty()) {
                item {
                    ParentHomeEmptyStateCard(
                        emptyState = state.emptyState(ParentHomeEmptyStateKind.FirstRun)
                            ?: ParentHomeEmptyState(kind = ParentHomeEmptyStateKind.FirstRun),
                        onAddChild = onAddChild,
                        onCreateTask = onCreateTask,
                        onCreateSavingsGoal = onCreateSavingsGoal,
                    )
                }
            } else {
                item {
                    SummaryCard(
                        pendingTotal = state.pendingSettlements.sumOf { it.amountCents.value },
                        pendingCount = state.pendingSettlements.size,
                        currencyCode = state.currencyCode,
                    )
                }
                item {
                    WeeklyDigestPromptCard(onOpenWeeklyDigest = onOpenWeeklyDigest)
                }
                item {
                    StatementExportPromptCard(onOpenStatementExport = onOpenStatementExport)
                }
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
                when (state.primaryAction) {
                    ParentHomePrimaryAction.AddChild -> {
                        Button(
                            onClick = onAddChild,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                        ) {
                            Text(stringResource(Res.string.parent_home_add_child))
                        }
                    }

                    ParentHomePrimaryAction.CreateTask -> {
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
                    ParentHomeEmptyStateCard(
                        emptyState = state.emptyState(ParentHomeEmptyStateKind.TaskApprovals)
                            ?: ParentHomeEmptyState(kind = ParentHomeEmptyStateKind.TaskApprovals),
                        onAddChild = onAddChild,
                        onCreateTask = onCreateTask,
                        onCreateSavingsGoal = onCreateSavingsGoal,
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
                    ActionableEmptyStateCard(
                        title = stringResource(Res.string.parent_home_children_empty_title),
                        body = stringResource(Res.string.parent_home_children_empty_body),
                        actionLabel = stringResource(Res.string.parent_home_add_child),
                        onAction = onAddChild,
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
                        onInviteGuest = onInviteGuest,
                    )
                }
            }
            state.emptyState(ParentHomeEmptyStateKind.SavingsGoal)?.let { emptyState ->
                item {
                    Text(
                        text = stringResource(Res.string.parent_home_savings_goals_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                item {
                    ParentHomeEmptyStateCard(
                        emptyState = emptyState,
                        onAddChild = onAddChild,
                        onCreateTask = onCreateTask,
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
                    ParentHomeEmptyStateCard(
                        emptyState = state.emptyState(ParentHomeEmptyStateKind.Settlements)
                            ?: ParentHomeEmptyState(kind = ParentHomeEmptyStateKind.Settlements),
                        onAddChild = onAddChild,
                        onCreateTask = onCreateTask,
                        onCreateSavingsGoal = onCreateSavingsGoal,
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
                SetupActionRow(
                    action = action,
                    firstChildId = state.children.firstOrNull()?.id,
                    onAddChild = onAddChild,
                    onPairChild = onPairChild,
                    onOpenParentInterest = onOpenParentInterest,
                    onOpenParentMatch = onOpenParentMatch,
                    onOpenSecondaryAdmin = onOpenSecondaryAdmin,
                    onOpenAccountDeletion = onOpenAccountDeletion,
                    onRequireParentalGate = onRequireParentalGate,
                )
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
private fun WeeklyDigestPromptCard(onOpenWeeklyDigest: () -> Unit) {
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
                text = stringResource(Res.string.parent_home_weekly_digest),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(Res.string.parent_home_weekly_digest_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = onOpenWeeklyDigest,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(Res.string.parent_home_weekly_digest_review))
            }
        }
    }
}

@Composable
private fun StatementExportPromptCard(onOpenStatementExport: () -> Unit) {
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
                text = stringResource(Res.string.parent_home_statement_export),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(Res.string.parent_home_statement_export_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = onOpenStatementExport,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(Res.string.parent_home_statement_export_review))
            }
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
    onInviteGuest: (ChildProfileId) -> Unit,
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
            OutlinedButton(
                onClick = { onInviteGuest(child.id) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(Res.string.parent_home_invite_guest))
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

private fun ParentHomeUiState.emptyState(kind: ParentHomeEmptyStateKind): ParentHomeEmptyState? =
    actionableEmptyStates.firstOrNull { it.kind == kind }

@Composable
private fun ParentHomeEmptyStateCard(
    emptyState: ParentHomeEmptyState,
    onAddChild: () -> Unit,
    onCreateTask: () -> Unit,
    onCreateSavingsGoal: (ChildProfileId) -> Unit,
) {
    val action = when (emptyState.action) {
        ParentHomeEmptyStateAction.AddChild -> EmptyStateActionUi(
            label = stringResource(Res.string.parent_home_add_child),
            onClick = onAddChild,
        )

        ParentHomeEmptyStateAction.CreateTask -> EmptyStateActionUi(
            label = stringResource(Res.string.parent_home_create_task),
            onClick = onCreateTask,
        )

        ParentHomeEmptyStateAction.CreateSavingsGoal -> emptyState.childProfileId?.let { childId ->
            EmptyStateActionUi(
                label = stringResource(Res.string.parent_home_create_goal),
                onClick = { onCreateSavingsGoal(childId) },
            )
        }

        null -> null
    }
    ActionableEmptyStateCard(
        title = parentHomeEmptyStateTitle(emptyState.kind),
        body = parentHomeEmptyStateBody(emptyState),
        actionLabel = action?.label,
        onAction = action?.onClick,
    )
}

private data class EmptyStateActionUi(val label: String, val onClick: () -> Unit)

@Composable
private fun parentHomeEmptyStateTitle(kind: ParentHomeEmptyStateKind): String = when (kind) {
    ParentHomeEmptyStateKind.FirstRun -> stringResource(Res.string.parent_home_first_run_empty_title)
    ParentHomeEmptyStateKind.TaskApprovals -> stringResource(Res.string.parent_home_task_approvals_title)
    ParentHomeEmptyStateKind.SavingsGoal -> stringResource(Res.string.parent_home_savings_goal_empty_title)
    ParentHomeEmptyStateKind.Settlements -> stringResource(Res.string.parent_home_settlements_title)
}

@Composable
private fun parentHomeEmptyStateBody(emptyState: ParentHomeEmptyState): String = when (emptyState.kind) {
    ParentHomeEmptyStateKind.FirstRun -> stringResource(Res.string.parent_home_first_run_empty_body)
    ParentHomeEmptyStateKind.TaskApprovals -> if (emptyState.action == ParentHomeEmptyStateAction.CreateTask) {
        stringResource(Res.string.parent_home_task_empty_action_body)
    } else {
        stringResource(Res.string.parent_home_task_empty_first_run_body)
    }
    ParentHomeEmptyStateKind.SavingsGoal -> stringResource(Res.string.parent_home_savings_goal_empty_body)
    ParentHomeEmptyStateKind.Settlements -> stringResource(Res.string.parent_home_settlements_empty_body)
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
private fun SetupActionRow(
    action: SetupAction,
    firstChildId: ChildProfileId?,
    onAddChild: () -> Unit,
    onPairChild: (ChildProfileId) -> Unit,
    onOpenParentInterest: () -> Unit,
    onOpenParentMatch: () -> Unit,
    onOpenSecondaryAdmin: () -> Unit,
    onOpenAccountDeletion: () -> Unit,
    onRequireParentalGate: (FoundationAction) -> Unit,
) {
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
            Button(
                onClick = {
                    when (action.id) {
                        "add-child" -> onAddChild()
                        "pair-device" -> firstChildId?.let(onPairChild)
                        "parent-interest" -> onOpenParentInterest()
                        "parent-match" -> onOpenParentMatch()
                        "secondary-admin" -> onOpenSecondaryAdmin()
                        "account-deletion" -> onOpenAccountDeletion()
                        else -> onRequireParentalGate(FoundationAction.ManageSettings)
                    }
                },
                enabled = action.id != "pair-device" || firstChildId != null,
            ) {
                Text(setupActionButtonLabel(action))
            }
        }
    }
}

@Composable
private fun setupActionLabel(action: SetupAction): String = when (action.id) {
    "add-child" -> stringResource(Res.string.parent_home_add_child)
    "pair-device" -> stringResource(Res.string.parent_home_pairing)
    "parent-interest" -> stringResource(Res.string.parent_home_parent_interest)
    "parent-match" -> stringResource(Res.string.parent_home_parent_match)
    "secondary-admin" -> stringResource(Res.string.parent_home_secondary_admin)
    "account-deletion" -> stringResource(Res.string.parent_home_account_deletion)
    else -> stringResource(Res.string.parent_home_gate_setup)
}

@Composable
private fun setupActionButtonLabel(action: SetupAction): String = when (action.id) {
    "add-child" -> stringResource(Res.string.parent_home_add_child)
    "pair-device" -> stringResource(Res.string.parent_home_pairing)
    "parent-interest" -> stringResource(Res.string.parent_home_parent_interest_review)
    "parent-match" -> stringResource(Res.string.parent_home_parent_match_review)
    "secondary-admin" -> stringResource(Res.string.parent_home_secondary_admin_review)
    "account-deletion" -> stringResource(Res.string.parent_home_account_deletion_review)
    else -> stringResource(Res.string.parent_home_gate)
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
            onAddChild = {},
            onPairChild = {},
            onConfigureAllowance = {},
            onAdjustChild = {},
            onCreateSavingsGoal = {},
            onInviteGuest = {},
            onOpenWeeklyDigest = {},
            onOpenStatementExport = {},
            onCreateTask = {},
            onOpenParentInterest = {},
            onOpenParentMatch = {},
            onOpenSecondaryAdmin = {},
            onOpenAccountDeletion = {},
            onMarkSettlementPaid = {},
            onUpdateApprovalAmount = { _, _ -> },
            onUpdateRejectionReason = { _, _ -> },
            onApproveTask = {},
            onRejectTask = {},
            onRequireParentalGate = {},
        )
    }
}

@Preview
@Composable
fun PreviewParentHomeFirstRunEmptyContent() {
    FledgeTheme {
        ParentHomeContent(
            state = ParentHomeUiState(
                familyName = "Familia Garcia",
                children = emptyList(),
                syncNotice = null,
            ),
            onAddChild = {},
            onPairChild = {},
            onConfigureAllowance = {},
            onAdjustChild = {},
            onCreateSavingsGoal = {},
            onInviteGuest = {},
            onOpenWeeklyDigest = {},
            onOpenStatementExport = {},
            onCreateTask = {},
            onOpenParentInterest = {},
            onOpenParentMatch = {},
            onOpenSecondaryAdmin = {},
            onOpenAccountDeletion = {},
            onMarkSettlementPaid = {},
            onUpdateApprovalAmount = { _, _ -> },
            onUpdateRejectionReason = { _, _ -> },
            onApproveTask = {},
            onRejectTask = {},
            onRequireParentalGate = {},
        )
    }
}

@Preview
@Composable
fun PreviewParentHomeEmptyActivityContent() {
    val childId = ChildProfileId("child-1")
    FledgeTheme {
        ParentHomeContent(
            state = ParentHomeUiState(
                familyName = "Familia Garcia",
                children = listOf(
                    ChildProfile(
                        id = childId,
                        displayName = "Lucia",
                        birthYear = 2018,
                        avatarKey = "star",
                    ),
                ),
                mainBalances = mapOf(childId to BalanceCents(0)),
                goalBalances = mapOf(childId to BalanceCents(0)),
                activeSavingsGoalChildIds = emptySet(),
                pendingSettlements = emptyList(),
                pendingTaskApprovals = emptyList(),
                syncNotice = null,
            ),
            onAddChild = {},
            onPairChild = {},
            onConfigureAllowance = {},
            onAdjustChild = {},
            onCreateSavingsGoal = {},
            onInviteGuest = {},
            onOpenWeeklyDigest = {},
            onOpenStatementExport = {},
            onCreateTask = {},
            onOpenParentInterest = {},
            onOpenParentMatch = {},
            onOpenSecondaryAdmin = {},
            onOpenAccountDeletion = {},
            onMarkSettlementPaid = {},
            onUpdateApprovalAmount = { _, _ -> },
            onUpdateRejectionReason = { _, _ -> },
            onApproveTask = {},
            onRejectTask = {},
            onRequireParentalGate = {},
        )
    }
}
