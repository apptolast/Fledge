package com.apptolast.fledge.presentation.foundation.childhome

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.apptolast.fledge.domain.model.ChildLedgerBalances
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.FoundationAction
import com.apptolast.fledge.domain.model.LedgerActor
import com.apptolast.fledge.domain.model.LedgerConcept
import com.apptolast.fledge.domain.model.LedgerTransaction
import com.apptolast.fledge.domain.model.LedgerTransactionType
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.SavingsGoal
import com.apptolast.fledge.domain.model.SavingsGoalId
import com.apptolast.fledge.domain.model.SettlementId
import com.apptolast.fledge.domain.model.SettlementReminderAudience
import com.apptolast.fledge.domain.model.SettlementReminderLevel
import com.apptolast.fledge.domain.model.SettlementStatus
import com.apptolast.fledge.domain.model.TaskAssignmentId
import com.apptolast.fledge.domain.model.TaskInstance
import com.apptolast.fledge.domain.model.TaskInstanceId
import com.apptolast.fledge.domain.model.TaskInstanceStatus
import com.apptolast.fledge.domain.model.TaskTemplateId
import com.apptolast.fledge.domain.model.TransactionId
import com.apptolast.fledge.domain.model.VirtualAccountType
import com.apptolast.fledge.presentation.foundation.components.SyncNoticeBanner
import com.apptolast.fledge.presentation.theme.FledgeTheme
import fledge.shared.generated.resources.Res
import fledge.shared.generated.resources.cash_out_child_reminder_fourteen_days
import fledge.shared.generated.resources.cash_out_child_reminder_seven_days
import fledge.shared.generated.resources.cash_out_mark_received
import fledge.shared.generated.resources.cash_out_status_confirmed
import fledge.shared.generated.resources.cash_out_status_paid_by_parent
import fledge.shared.generated.resources.cash_out_status_requested
import fledge.shared.generated.resources.child_home_active_goal_amount
import fledge.shared.generated.resources.child_home_active_goal_progress
import fledge.shared.generated.resources.child_home_active_goal_title
import fledge.shared.generated.resources.child_home_body
import fledge.shared.generated.resources.child_home_cash_out
import fledge.shared.generated.resources.child_home_external_link
import fledge.shared.generated.resources.child_home_goal_balance
import fledge.shared.generated.resources.child_home_ledger_empty
import fledge.shared.generated.resources.child_home_ledger_title
import fledge.shared.generated.resources.child_home_main_balance
import fledge.shared.generated.resources.child_home_parent_zone
import fledge.shared.generated.resources.child_home_purchase
import fledge.shared.generated.resources.child_home_settings
import fledge.shared.generated.resources.child_home_settlements_empty
import fledge.shared.generated.resources.child_home_settlements_title
import fledge.shared.generated.resources.child_home_task_add_photo
import fledge.shared.generated.resources.child_home_task_due_today
import fledge.shared.generated.resources.child_home_task_error_missing_photo
import fledge.shared.generated.resources.child_home_task_error_submit
import fledge.shared.generated.resources.child_home_task_money_after_approval
import fledge.shared.generated.resources.child_home_task_photo_ready
import fledge.shared.generated.resources.child_home_task_reward
import fledge.shared.generated.resources.child_home_task_status_approved
import fledge.shared.generated.resources.child_home_task_status_expired
import fledge.shared.generated.resources.child_home_task_status_pending
import fledge.shared.generated.resources.child_home_task_status_rejected
import fledge.shared.generated.resources.child_home_task_status_submitted
import fledge.shared.generated.resources.child_home_task_submit
import fledge.shared.generated.resources.child_home_task_waiting
import fledge.shared.generated.resources.child_home_tasks_empty
import fledge.shared.generated.resources.child_home_tasks_title
import fledge.shared.generated.resources.child_home_title
import fledge.shared.generated.resources.ledger_account_goal
import fledge.shared.generated.resources.ledger_account_main
import fledge.shared.generated.resources.ledger_actor_child
import fledge.shared.generated.resources.ledger_actor_parent
import fledge.shared.generated.resources.ledger_actor_system
import fledge.shared.generated.resources.ledger_reversal_of
import fledge.shared.generated.resources.ledger_type_allowance
import fledge.shared.generated.resources.ledger_type_bonus
import fledge.shared.generated.resources.ledger_type_gift
import fledge.shared.generated.resources.ledger_type_goal_transfer
import fledge.shared.generated.resources.ledger_type_penalty
import fledge.shared.generated.resources.ledger_type_reversal
import fledge.shared.generated.resources.ledger_type_settlement
import fledge.shared.generated.resources.ledger_type_task_reward
import fledge.shared.generated.resources.operation_error_sync
import kotlin.time.Clock
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChildHomeScreen(
    childProfileId: ChildProfileId,
    onRequestCashOut: (ChildProfileId) -> Unit,
    onParentalGateRequired: () -> Unit,
    viewModel: ChildHomeViewModel = koinViewModel(),
) {
    LaunchedEffect(childProfileId) {
        viewModel.load(childProfileId)
    }
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    ChildHomeContent(
        state = state,
        onRequestCashOut = onRequestCashOut,
        onConfirmSettlement = { settlementId ->
            scope.launch {
                viewModel.confirmSettlement(settlementId)
            }
        },
        onAttachPhotoEvidence = { instanceId ->
            viewModel.attachPhotoEvidence(instanceId, "local://task-photo/${instanceId.value}")
        },
        onSubmitTask = { instanceId ->
            scope.launch {
                viewModel.submitTask(instanceId)
            }
        },
        onProtectedAction = { action ->
            scope.launch {
                if (viewModel.requestProtectedAction(action)) {
                    onParentalGateRequired()
                }
            }
        },
    )
}

@Composable
fun ChildHomeContent(
    state: ChildHomeUiState,
    onRequestCashOut: (ChildProfileId) -> Unit,
    onConfirmSettlement: (SettlementId) -> Unit,
    onAttachPhotoEvidence: (TaskInstanceId) -> Unit,
    onSubmitTask: (TaskInstanceId) -> Unit,
    onProtectedAction: (FoundationAction) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = stringResource(Res.string.child_home_title),
                    style = MaterialTheme.typography.headlineMedium,
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
                Text(
                    text = stringResource(Res.string.child_home_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                ChildBalanceCard(state = state)
            }
            state.activeSavingsGoal?.let { goal ->
                item {
                    ActiveSavingsGoalCard(
                        goal = goal,
                        currentCents = state.balances?.goal?.value ?: 0L,
                        currencyCode = state.currencyCode,
                    )
                }
            }
            item {
                Text(
                    text = stringResource(Res.string.child_home_tasks_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            state.taskSubmissionError?.let { error ->
                item {
                    Text(
                        text = taskSubmissionErrorText(error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (state.taskInstances.isEmpty()) {
                item {
                    Text(
                        text = stringResource(Res.string.child_home_tasks_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(
                    items = state.taskInstances,
                    key = { it.id.value },
                ) { instance ->
                    ChildTaskInstanceRow(
                        instance = instance,
                        currencyCode = state.currencyCode,
                        selectedPhotoEvidenceUri = state.selectedPhotoEvidenceByTaskId[instance.id],
                        isBusy = state.isBusy,
                        onAttachPhotoEvidence = onAttachPhotoEvidence,
                        onSubmitTask = onSubmitTask,
                    )
                }
            }
            item {
                Button(
                    onClick = { state.childProfileId?.let(onRequestCashOut) },
                    enabled = state.childProfileId != null && (state.balances?.main?.value ?: 0L) > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                ) {
                    Text(stringResource(Res.string.child_home_cash_out))
                }
            }
            item {
                Text(
                    text = stringResource(Res.string.child_home_settlements_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            val pendingSettlements = state.settlements.filter { it.status != SettlementStatus.ConfirmedByChild }
            if (pendingSettlements.isEmpty()) {
                item {
                    Text(
                        text = stringResource(Res.string.child_home_settlements_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(
                    items = pendingSettlements.sortedByDescending { it.requestedAt },
                    key = { it.id.value },
                ) { settlement ->
                    ChildSettlementRow(
                        settlement = settlement,
                        currencyCode = state.currencyCode,
                        hasReminder = state.settlementReminders.any {
                            it.settlementId == settlement.id &&
                                it.audience == SettlementReminderAudience.Child
                        },
                        reminderLevel = state.settlementReminders.firstOrNull {
                            it.settlementId == settlement.id &&
                                it.audience == SettlementReminderAudience.Child
                        }?.level,
                        onConfirmSettlement = onConfirmSettlement,
                        isBusy = state.isBusy,
                    )
                }
            }
            item {
                Text(
                    text = stringResource(Res.string.child_home_ledger_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            if (state.ledgerTransactions.isEmpty()) {
                item {
                    Text(
                        text = stringResource(Res.string.child_home_ledger_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(
                    items = state.ledgerTransactions.sortedByDescending { it.createdAt },
                    key = { it.id.value },
                ) { transaction ->
                    LedgerTransactionRow(
                        transaction = transaction,
                        currencyCode = state.currencyCode,
                    )
                }
            }
            item {
                Spacer(Modifier.heightIn(min = 8.dp))
            }
            item {
                Button(
                    onClick = { onProtectedAction(FoundationAction.OpenParentZone) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                ) {
                    Text(stringResource(Res.string.child_home_parent_zone))
                }
            }
            item {
                OutlinedButton(
                    onClick = { onProtectedAction(FoundationAction.ManageSettings) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                ) {
                    Text(stringResource(Res.string.child_home_settings))
                }
            }
            item {
                OutlinedButton(
                    onClick = { onProtectedAction(FoundationAction.StartPurchase) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                ) {
                    Text(stringResource(Res.string.child_home_purchase))
                }
            }
            item {
                OutlinedButton(
                    onClick = { onProtectedAction(FoundationAction.OpenExternalLink) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                ) {
                    Text(stringResource(Res.string.child_home_external_link))
                }
            }
        }
    }
}

@Composable
private fun ChildTaskInstanceRow(
    instance: TaskInstance,
    currencyCode: String,
    selectedPhotoEvidenceUri: String?,
    isBusy: Boolean,
    onAttachPhotoEvidence: (TaskInstanceId) -> Unit,
    onSubmitTask: (TaskInstanceId) -> Unit,
) {
    val hasPhotoEvidence = !selectedPhotoEvidenceUri.isNullOrBlank() || !instance.photoEvidenceUri.isNullOrBlank()
    val isActionable = instance.status == TaskInstanceStatus.Pending || instance.status == TaskInstanceStatus.Rejected
    Card(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = instance.title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(
                            Res.string.child_home_task_reward,
                            formatCents(instance.rewardCents.value, currencyCode),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TaskStatusPill(instance.status)
            }
            Text(
                text = stringResource(Res.string.child_home_task_due_today),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (instance.requiresPhoto && isActionable) {
                OutlinedButton(
                    onClick = { onAttachPhotoEvidence(instance.id) },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text(
                        text = if (hasPhotoEvidence) {
                            stringResource(Res.string.child_home_task_photo_ready)
                        } else {
                            stringResource(Res.string.child_home_task_add_photo)
                        },
                    )
                }
            }
            if (isActionable) {
                Button(
                    onClick = { onSubmitTask(instance.id) },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                ) {
                    Text(stringResource(Res.string.child_home_task_submit))
                }
            } else if (instance.status == TaskInstanceStatus.Submitted) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(Res.string.child_home_task_waiting),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (instance.status == TaskInstanceStatus.Submitted) {
                Text(
                    text = stringResource(Res.string.child_home_task_money_after_approval),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ActiveSavingsGoalCard(goal: SavingsGoal, currentCents: Long, currencyCode: String) {
    val progress = (currentCents.toFloat() / goal.targetCents.value.toFloat()).coerceIn(0f, 1f)
    val progressPercent = (progress * 100).toInt()

    Card(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(Res.string.child_home_active_goal_title),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(44.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = goal.iconKey?.firstOrNull()?.uppercaseChar()?.toString().orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onTertiary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(
                            Res.string.child_home_active_goal_amount,
                            formatCents(currentCents, currencyCode),
                            formatCents(goal.targetCents.value, currencyCode),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(Res.string.child_home_active_goal_progress, progressPercent),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TaskStatusPill(status: TaskInstanceStatus) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(18.dp),
    ) {
        Text(
            text = taskStatusLabel(status),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun ChildBalanceCard(state: ChildHomeUiState) {
    val balances = state.balances
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(
                    Res.string.child_home_main_balance,
                    formatCents(balances?.main?.value ?: 0L, state.currencyCode),
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(
                    Res.string.child_home_goal_balance,
                    formatCents(balances?.goal?.value ?: 0L, state.currencyCode),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ChildSettlementRow(
    settlement: CashOutSettlement,
    currencyCode: String,
    hasReminder: Boolean,
    reminderLevel: SettlementReminderLevel?,
    onConfirmSettlement: (SettlementId) -> Unit,
    isBusy: Boolean,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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
            if (hasReminder && reminderLevel != null) {
                Text(
                    text = childReminderText(reminderLevel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (settlement.status == SettlementStatus.PaidByParent) {
                Button(
                    onClick = { onConfirmSettlement(settlement.id) },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.cash_out_mark_received))
                }
            }
        }
    }
}

@Composable
private fun LedgerTransactionRow(transaction: LedgerTransaction, currencyCode: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = ledgerTransactionTypeLabel(transaction.type),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = formatCents(transaction.amountCents.value, currencyCode),
                style = MaterialTheme.typography.titleLarge,
                color = if (transaction.amountCents.value < 0) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
            Text(
                text = transaction.concept.value,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "${ledgerAccountLabel(transaction.accountType)} - ${ledgerActorLabel(transaction.createdBy)} - ${
                    transaction.createdAt.toString().substringBefore("T")
                }",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            transaction.reversesTransactionId?.let { originalId ->
                Text(
                    text = stringResource(Res.string.ledger_reversal_of, originalId.value),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun taskSubmissionErrorText(error: ChildTaskSubmissionError): String = when (error) {
    ChildTaskSubmissionError.MissingPhotoEvidence -> stringResource(Res.string.child_home_task_error_missing_photo)
    ChildTaskSubmissionError.SubmitFailed -> stringResource(Res.string.child_home_task_error_submit)
}

@Composable
private fun taskStatusLabel(status: TaskInstanceStatus): String = when (status) {
    TaskInstanceStatus.Pending -> stringResource(Res.string.child_home_task_status_pending)
    TaskInstanceStatus.Submitted -> stringResource(Res.string.child_home_task_status_submitted)
    TaskInstanceStatus.Approved -> stringResource(Res.string.child_home_task_status_approved)
    TaskInstanceStatus.Rejected -> stringResource(Res.string.child_home_task_status_rejected)
    TaskInstanceStatus.Expired -> stringResource(Res.string.child_home_task_status_expired)
}

@Composable
private fun cashOutStatusLabel(status: SettlementStatus): String = when (status) {
    SettlementStatus.Requested -> stringResource(Res.string.cash_out_status_requested)
    SettlementStatus.PaidByParent -> stringResource(Res.string.cash_out_status_paid_by_parent)
    SettlementStatus.ConfirmedByChild -> stringResource(Res.string.cash_out_status_confirmed)
}

@Composable
private fun childReminderText(level: SettlementReminderLevel): String = when (level) {
    SettlementReminderLevel.SevenDays -> stringResource(Res.string.cash_out_child_reminder_seven_days)
    SettlementReminderLevel.FourteenDays -> stringResource(Res.string.cash_out_child_reminder_fourteen_days)
}

@Composable
private fun ledgerTransactionTypeLabel(type: LedgerTransactionType): String = when (type) {
    LedgerTransactionType.Allowance -> stringResource(Res.string.ledger_type_allowance)
    LedgerTransactionType.TaskReward -> stringResource(Res.string.ledger_type_task_reward)
    LedgerTransactionType.Bonus -> stringResource(Res.string.ledger_type_bonus)
    LedgerTransactionType.Penalty -> stringResource(Res.string.ledger_type_penalty)
    LedgerTransactionType.Gift -> stringResource(Res.string.ledger_type_gift)
    LedgerTransactionType.GoalTransfer -> stringResource(Res.string.ledger_type_goal_transfer)
    LedgerTransactionType.Settlement -> stringResource(Res.string.ledger_type_settlement)
    LedgerTransactionType.Reversal -> stringResource(Res.string.ledger_type_reversal)
}

@Composable
private fun ledgerActorLabel(actor: LedgerActor): String = when (actor) {
    LedgerActor.Parent -> stringResource(Res.string.ledger_actor_parent)
    LedgerActor.Child -> stringResource(Res.string.ledger_actor_child)
    LedgerActor.System -> stringResource(Res.string.ledger_actor_system)
}

@Composable
private fun ledgerAccountLabel(accountType: VirtualAccountType): String = when (accountType) {
    VirtualAccountType.Main -> stringResource(Res.string.ledger_account_main)
    VirtualAccountType.Goal -> stringResource(Res.string.ledger_account_goal)
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
fun PreviewChildHomeContent() {
    FledgeTheme {
        ChildHomeContent(
            state = ChildHomeUiState(
                childProfileId = ChildProfileId("child-1"),
                balances = ChildLedgerBalances(
                    childProfileId = ChildProfileId("child-1"),
                    main = BalanceCents(550),
                    goal = BalanceCents(1_230),
                ),
                activeSavingsGoal = SavingsGoal(
                    id = SavingsGoalId("goal-1"),
                    familyId = FamilyId("family-1"),
                    childProfileId = ChildProfileId("child-1"),
                    title = "Bici nueva",
                    targetCents = MoneyCents(4_000),
                    iconKey = "bike",
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now(),
                ),
                ledgerTransactions = listOf(
                    LedgerTransaction(
                        id = TransactionId("transaction-1"),
                        familyId = FamilyId("family-1"),
                        childProfileId = ChildProfileId("child-1"),
                        accountType = VirtualAccountType.Main,
                        type = LedgerTransactionType.Bonus,
                        amountCents = MoneyCents(550),
                        concept = LedgerConcept("Paga extra"),
                        createdBy = LedgerActor.Parent,
                        createdAt = kotlin.time.Clock.System.now(),
                    ),
                ),
                settlements = listOf(
                    CashOutSettlement(
                        id = SettlementId("settlement-1"),
                        familyId = FamilyId("family-1"),
                        childProfileId = ChildProfileId("child-1"),
                        amountCents = MoneyCents(500),
                        concept = LedgerConcept("Cromos"),
                        status = SettlementStatus.PaidByParent,
                        requestedAt = Clock.System.now(),
                        paidByParentAt = Clock.System.now(),
                    ),
                ),
                taskInstances = listOf(
                    TaskInstance(
                        id = TaskInstanceId("task-1"),
                        familyId = FamilyId("family-1"),
                        taskAssignmentId = TaskAssignmentId("assignment-1"),
                        taskTemplateId = TaskTemplateId("template-1"),
                        childProfileId = ChildProfileId("child-1"),
                        title = "Poner la mesa",
                        rewardCents = MoneyCents(50),
                        requiresPhoto = true,
                        status = TaskInstanceStatus.Pending,
                        dueAt = Clock.System.now(),
                        periodKey = "20260729",
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    ),
                    TaskInstance(
                        id = TaskInstanceId("task-2"),
                        familyId = FamilyId("family-1"),
                        taskAssignmentId = TaskAssignmentId("assignment-1"),
                        taskTemplateId = TaskTemplateId("template-1"),
                        childProfileId = ChildProfileId("child-1"),
                        title = "Leer 20 minutos",
                        rewardCents = MoneyCents(75),
                        requiresPhoto = false,
                        status = TaskInstanceStatus.Submitted,
                        dueAt = Clock.System.now(),
                        periodKey = "20260729",
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                        submittedAt = Clock.System.now(),
                    ),
                ),
                syncNotice = null,
            ),
            onRequestCashOut = {},
            onConfirmSettlement = {},
            onAttachPhotoEvidence = {},
            onSubmitTask = {},
            onProtectedAction = {},
        )
    }
}
