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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apptolast.fledge.domain.model.BalanceCents
import com.apptolast.fledge.domain.model.CashOutSettlement
import com.apptolast.fledge.domain.model.ChildAchievementBadgeId
import com.apptolast.fledge.domain.model.ChildAchievementBadgeProgress
import com.apptolast.fledge.domain.model.ChildAchievementSummary
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
import com.apptolast.fledge.domain.service.CompoundInterestExplanationLevel
import com.apptolast.fledge.domain.service.CompoundInterestProjection
import com.apptolast.fledge.domain.service.SavingsGoalCompletionNotice
import com.apptolast.fledge.domain.service.SavingsGoalProjection
import com.apptolast.fledge.domain.service.SavingsGoalProjectionStatus
import com.apptolast.fledge.presentation.foundation.components.SyncNoticeBanner
import com.apptolast.fledge.presentation.theme.FledgeTheme
import fledge.shared.generated.resources.Res
import fledge.shared.generated.resources.cash_out_child_reminder_fourteen_days
import fledge.shared.generated.resources.cash_out_child_reminder_seven_days
import fledge.shared.generated.resources.cash_out_mark_received
import fledge.shared.generated.resources.cash_out_status_confirmed
import fledge.shared.generated.resources.cash_out_status_paid_by_parent
import fledge.shared.generated.resources.cash_out_status_requested
import fledge.shared.generated.resources.child_home_achievement_approved_total
import fledge.shared.generated.resources.child_home_achievement_badge_first_task
import fledge.shared.generated.resources.child_home_achievement_badge_progress
import fledge.shared.generated.resources.child_home_achievement_badge_three_day_streak
import fledge.shared.generated.resources.child_home_achievement_badge_three_tasks
import fledge.shared.generated.resources.child_home_achievement_badge_unlocked
import fledge.shared.generated.resources.child_home_achievement_best_streak
import fledge.shared.generated.resources.child_home_achievement_current_active
import fledge.shared.generated.resources.child_home_achievement_current_empty
import fledge.shared.generated.resources.child_home_achievement_current_label
import fledge.shared.generated.resources.child_home_achievement_subtitle
import fledge.shared.generated.resources.child_home_achievement_title
import fledge.shared.generated.resources.child_home_action_cd_cash_out
import fledge.shared.generated.resources.child_home_action_cd_cash_out_disabled
import fledge.shared.generated.resources.child_home_action_cd_empty_goal
import fledge.shared.generated.resources.child_home_action_cd_empty_tasks
import fledge.shared.generated.resources.child_home_action_cd_external_link
import fledge.shared.generated.resources.child_home_action_cd_goal_deposit
import fledge.shared.generated.resources.child_home_action_cd_goal_withdraw
import fledge.shared.generated.resources.child_home_action_cd_goal_withdraw_disabled
import fledge.shared.generated.resources.child_home_action_cd_parent_zone
import fledge.shared.generated.resources.child_home_action_cd_purchase
import fledge.shared.generated.resources.child_home_action_cd_settings
import fledge.shared.generated.resources.child_home_action_cd_task_add_photo
import fledge.shared.generated.resources.child_home_action_cd_task_submit
import fledge.shared.generated.resources.child_home_active_goal_amount
import fledge.shared.generated.resources.child_home_active_goal_completed_body
import fledge.shared.generated.resources.child_home_active_goal_completed_title
import fledge.shared.generated.resources.child_home_active_goal_deposit
import fledge.shared.generated.resources.child_home_active_goal_no_pace_body
import fledge.shared.generated.resources.child_home_active_goal_no_pace_title
import fledge.shared.generated.resources.child_home_active_goal_pace
import fledge.shared.generated.resources.child_home_active_goal_progress
import fledge.shared.generated.resources.child_home_active_goal_projection_body
import fledge.shared.generated.resources.child_home_active_goal_projection_title
import fledge.shared.generated.resources.child_home_active_goal_remaining
import fledge.shared.generated.resources.child_home_active_goal_title
import fledge.shared.generated.resources.child_home_active_goal_withdraw
import fledge.shared.generated.resources.child_home_body
import fledge.shared.generated.resources.child_home_cash_out
import fledge.shared.generated.resources.child_home_compound_interest_gain
import fledge.shared.generated.resources.child_home_compound_interest_older_body
import fledge.shared.generated.resources.child_home_compound_interest_one_year
import fledge.shared.generated.resources.child_home_compound_interest_rate
import fledge.shared.generated.resources.child_home_compound_interest_three_years
import fledge.shared.generated.resources.child_home_compound_interest_title
import fledge.shared.generated.resources.child_home_compound_interest_today
import fledge.shared.generated.resources.child_home_compound_interest_younger_body
import fledge.shared.generated.resources.child_home_empty_goal_action
import fledge.shared.generated.resources.child_home_empty_goal_body
import fledge.shared.generated.resources.child_home_empty_goal_title
import fledge.shared.generated.resources.child_home_empty_ledger_body
import fledge.shared.generated.resources.child_home_empty_settlements_body
import fledge.shared.generated.resources.child_home_empty_tasks_action
import fledge.shared.generated.resources.child_home_empty_tasks_body
import fledge.shared.generated.resources.child_home_empty_tasks_title
import fledge.shared.generated.resources.child_home_external_link
import fledge.shared.generated.resources.child_home_goal_completion_body
import fledge.shared.generated.resources.child_home_goal_completion_title
import fledge.shared.generated.resources.child_home_goal_status_completed
import fledge.shared.generated.resources.child_home_goal_status_needs_contribution
import fledge.shared.generated.resources.child_home_goal_status_on_track
import fledge.shared.generated.resources.child_home_ledger_title
import fledge.shared.generated.resources.child_home_parent_zone
import fledge.shared.generated.resources.child_home_pot_give
import fledge.shared.generated.resources.child_home_pot_save
import fledge.shared.generated.resources.child_home_pot_spend
import fledge.shared.generated.resources.child_home_pots_body
import fledge.shared.generated.resources.child_home_pots_title
import fledge.shared.generated.resources.child_home_purchase
import fledge.shared.generated.resources.child_home_settings
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
import fledge.shared.generated.resources.child_home_tasks_title
import fledge.shared.generated.resources.child_home_title
import fledge.shared.generated.resources.ledger_account_give
import fledge.shared.generated.resources.ledger_account_goal
import fledge.shared.generated.resources.ledger_account_main
import fledge.shared.generated.resources.ledger_actor_child
import fledge.shared.generated.resources.ledger_actor_guest
import fledge.shared.generated.resources.ledger_actor_parent
import fledge.shared.generated.resources.ledger_actor_system
import fledge.shared.generated.resources.ledger_reversal_of
import fledge.shared.generated.resources.ledger_type_allowance
import fledge.shared.generated.resources.ledger_type_bonus
import fledge.shared.generated.resources.ledger_type_gift
import fledge.shared.generated.resources.ledger_type_goal_transfer
import fledge.shared.generated.resources.ledger_type_interest
import fledge.shared.generated.resources.ledger_type_match
import fledge.shared.generated.resources.ledger_type_penalty
import fledge.shared.generated.resources.ledger_type_reversal
import fledge.shared.generated.resources.ledger_type_settlement
import fledge.shared.generated.resources.ledger_type_task_reward
import fledge.shared.generated.resources.operation_error_sync
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChildHomeScreen(
    childProfileId: ChildProfileId,
    onRequestCashOut: (ChildProfileId) -> Unit,
    onOpenSavingsGoal: (ChildProfileId, SavingsGoalId) -> Unit,
    onWithdrawSavingsGoal: (ChildProfileId, SavingsGoalId) -> Unit,
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
        onOpenSavingsGoal = onOpenSavingsGoal,
        onWithdrawSavingsGoal = onWithdrawSavingsGoal,
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
    onOpenSavingsGoal: (ChildProfileId, SavingsGoalId) -> Unit,
    onWithdrawSavingsGoal: (ChildProfileId, SavingsGoalId) -> Unit,
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
                ChildMoneyPotsCard(state = state)
            }
            state.activeSavingsGoal?.let { goal ->
                item {
                    ActiveSavingsGoalCard(
                        goal = goal,
                        currentCents = state.balances.balanceFor(goal.accountType),
                        projection = state.activeSavingsGoalProjection,
                        currencyCode = state.currencyCode,
                        onDeposit = {
                            state.childProfileId?.let { childProfileId ->
                                onOpenSavingsGoal(childProfileId, goal.id)
                            }
                        },
                        onWithdraw = {
                            state.childProfileId?.let { childProfileId ->
                                onWithdrawSavingsGoal(childProfileId, goal.id)
                            }
                        },
                    )
                }
            }
            if (state.activeSavingsGoal == null) {
                item {
                    ChildHomeEmptyStateCard(
                        emptyState = state.emptyState(ChildHomeEmptyStateKind.SavingsGoal)
                            ?: ChildHomeEmptyState(kind = ChildHomeEmptyStateKind.SavingsGoal),
                        onProtectedAction = onProtectedAction,
                    )
                }
            }
            state.activeSavingsGoalCompletionNotice?.let { notice ->
                item {
                    ChildGoalCompletionNoticeCard(
                        notice = notice,
                        currencyCode = state.currencyCode,
                    )
                }
            }
            state.compoundInterestProjection?.let { projection ->
                item {
                    CompoundInterestProjectionCard(
                        projection = projection,
                        currencyCode = state.currencyCode,
                    )
                }
            }
            if (state.taskInstances.isNotEmpty() || state.achievementSummary.hasApprovedActivity) {
                item {
                    ChildAchievementSummaryCard(summary = state.achievementSummary)
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
                    ChildHomeEmptyStateCard(
                        emptyState = state.emptyState(ChildHomeEmptyStateKind.Tasks)
                            ?: ChildHomeEmptyState(kind = ChildHomeEmptyStateKind.Tasks),
                        onProtectedAction = onProtectedAction,
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
                val cashOutSpec = childHomeActionSpec(ChildHomeActionKind.CashOut)
                val isCashOutEnabled = state.childProfileId != null && (state.balances?.main?.value ?: 0L) > 0
                ChildHomeActionButton(
                    spec = cashOutSpec,
                    onClick = { state.childProfileId?.let(onRequestCashOut) },
                    enabled = isCashOutEnabled,
                    contentDescription = if (isCashOutEnabled) {
                        childHomeText(cashOutSpec.contentDescriptionKey)
                    } else {
                        stringResource(Res.string.child_home_action_cd_cash_out_disabled)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = cashOutSpec.minTouchTargetDp.dp),
                )
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
                    ChildHomeEmptyStateCard(
                        emptyState = state.emptyState(ChildHomeEmptyStateKind.Settlements)
                            ?: ChildHomeEmptyState(kind = ChildHomeEmptyStateKind.Settlements),
                        onProtectedAction = onProtectedAction,
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
                    ChildHomeEmptyStateCard(
                        emptyState = state.emptyState(ChildHomeEmptyStateKind.Ledger)
                            ?: ChildHomeEmptyState(kind = ChildHomeEmptyStateKind.Ledger),
                        onProtectedAction = onProtectedAction,
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
                ChildHomeActionButton(
                    spec = childHomeActionSpec(ChildHomeActionKind.ParentZone),
                    onClick = { onProtectedAction(FoundationAction.OpenParentZone) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                )
            }
            item {
                ChildHomeActionButton(
                    spec = childHomeActionSpec(ChildHomeActionKind.Settings),
                    onClick = { onProtectedAction(FoundationAction.ManageSettings) },
                    outlined = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                )
            }
            item {
                ChildHomeActionButton(
                    spec = childHomeActionSpec(ChildHomeActionKind.Purchase),
                    onClick = { onProtectedAction(FoundationAction.StartPurchase) },
                    outlined = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                )
            }
            item {
                ChildHomeActionButton(
                    spec = childHomeActionSpec(ChildHomeActionKind.ExternalLink),
                    onClick = { onProtectedAction(FoundationAction.OpenExternalLink) },
                    outlined = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                )
            }
        }
    }
}

@Composable
private fun ChildHomeActionButton(
    spec: ChildHomeActionSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    outlined: Boolean = false,
    label: String? = null,
    contentDescription: String? = null,
) {
    val resolvedLabel = label ?: childHomeText(spec.labelKey)
    val resolvedContentDescription = contentDescription ?: childHomeText(spec.contentDescriptionKey)
    val contentColor = if (!enabled) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else if (outlined) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onPrimary
    }
    val childModifier = modifier
        .heightIn(min = spec.minTouchTargetDp.dp)
        .semantics(mergeDescendants = true) {
            role = spec.semanticsRole.toComposeRole()
            this.contentDescription = resolvedContentDescription
        }

    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
            modifier = childModifier,
        ) {
            ChildHomeActionButtonContent(
                spec = spec,
                label = resolvedLabel,
                contentColor = contentColor,
            )
        }
    } else {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = contentColor,
            ),
            modifier = childModifier,
        ) {
            ChildHomeActionButtonContent(
                spec = spec,
                label = resolvedLabel,
                contentColor = contentColor,
            )
        }
    }
}

@Composable
private fun ChildHomeActionButtonContent(
    spec: ChildHomeActionSpec,
    label: String,
    contentColor: androidx.compose.ui.graphics.Color,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChildHomeInlineIcon(
            iconKey = spec.iconKey,
            contentColor = contentColor,
        )
        Text(
            text = label,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ChildHomeIcon(
    iconKey: ChildHomeIconKey,
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = iconKey.symbol,
                style = if (iconKey.symbol.length > 2) {
                    MaterialTheme.typography.labelSmall
                } else {
                    MaterialTheme.typography.titleMedium
                },
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ChildHomeInlineIcon(
    iconKey: ChildHomeIconKey,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
    Text(
        text = iconKey.symbol,
        style = if (iconKey.symbol.length > 2) {
            MaterialTheme.typography.labelSmall
        } else {
            MaterialTheme.typography.labelLarge
        },
        color = contentColor,
        fontWeight = FontWeight.Bold,
    )
}

private fun ChildHomeSemanticsRole.toComposeRole(): Role = when (this) {
    ChildHomeSemanticsRole.Button -> Role.Button
}

@Composable
private fun childHomeText(key: ChildHomeTextKey): String = stringResource(childHomeTextResource(key))

private fun childHomeTextResource(key: ChildHomeTextKey): StringResource = when (key) {
    ChildHomeTextKey.TaskAddPhotoLabel -> Res.string.child_home_task_add_photo
    ChildHomeTextKey.TaskAddPhotoDescription -> Res.string.child_home_action_cd_task_add_photo
    ChildHomeTextKey.TaskSubmitLabel -> Res.string.child_home_task_submit
    ChildHomeTextKey.TaskSubmitDescription -> Res.string.child_home_action_cd_task_submit
    ChildHomeTextKey.GoalDepositLabel -> Res.string.child_home_active_goal_deposit
    ChildHomeTextKey.GoalDepositDescription -> Res.string.child_home_action_cd_goal_deposit
    ChildHomeTextKey.GoalWithdrawLabel -> Res.string.child_home_active_goal_withdraw
    ChildHomeTextKey.GoalWithdrawDescription -> Res.string.child_home_action_cd_goal_withdraw
    ChildHomeTextKey.GoalWithdrawDisabledDescription -> Res.string.child_home_action_cd_goal_withdraw_disabled
    ChildHomeTextKey.CashOutLabel -> Res.string.child_home_cash_out
    ChildHomeTextKey.CashOutDescription -> Res.string.child_home_action_cd_cash_out
    ChildHomeTextKey.CashOutDisabledDescription -> Res.string.child_home_action_cd_cash_out_disabled
    ChildHomeTextKey.ParentZoneLabel -> Res.string.child_home_parent_zone
    ChildHomeTextKey.ParentZoneDescription -> Res.string.child_home_action_cd_parent_zone
    ChildHomeTextKey.SettingsLabel -> Res.string.child_home_settings
    ChildHomeTextKey.SettingsDescription -> Res.string.child_home_action_cd_settings
    ChildHomeTextKey.PurchaseLabel -> Res.string.child_home_purchase
    ChildHomeTextKey.PurchaseDescription -> Res.string.child_home_action_cd_purchase
    ChildHomeTextKey.ExternalLinkLabel -> Res.string.child_home_external_link
    ChildHomeTextKey.ExternalLinkDescription -> Res.string.child_home_action_cd_external_link
    ChildHomeTextKey.EmptyTasksActionLabel -> Res.string.child_home_empty_tasks_action
    ChildHomeTextKey.EmptyTasksActionDescription -> Res.string.child_home_action_cd_empty_tasks
    ChildHomeTextKey.EmptyGoalActionLabel -> Res.string.child_home_empty_goal_action
    ChildHomeTextKey.EmptyGoalActionDescription -> Res.string.child_home_action_cd_empty_goal
    ChildHomeTextKey.TaskStatusPendingLabel -> Res.string.child_home_task_status_pending
    ChildHomeTextKey.TaskStatusSubmittedLabel -> Res.string.child_home_task_status_submitted
    ChildHomeTextKey.TaskStatusApprovedLabel -> Res.string.child_home_task_status_approved
    ChildHomeTextKey.TaskStatusRejectedLabel -> Res.string.child_home_task_status_rejected
    ChildHomeTextKey.TaskStatusExpiredLabel -> Res.string.child_home_task_status_expired
    ChildHomeTextKey.GoalStatusCompletedLabel -> Res.string.child_home_goal_status_completed
    ChildHomeTextKey.GoalStatusOnTrackLabel -> Res.string.child_home_goal_status_on_track
    ChildHomeTextKey.GoalStatusNeedsContributionLabel -> Res.string.child_home_goal_status_needs_contribution
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
                ChildHomeIcon(
                    iconKey = ChildHomeIconKey.Tasks,
                    modifier = Modifier.size(44.dp),
                )
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
                ChildHomeActionButton(
                    spec = childHomeActionSpec(ChildHomeActionKind.TaskAddPhoto),
                    onClick = { onAttachPhotoEvidence(instance.id) },
                    enabled = !isBusy,
                    outlined = true,
                    label = if (hasPhotoEvidence) {
                        stringResource(Res.string.child_home_task_photo_ready)
                    } else {
                        stringResource(Res.string.child_home_task_add_photo)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                )
            }
            if (isActionable) {
                ChildHomeActionButton(
                    spec = childHomeActionSpec(ChildHomeActionKind.TaskSubmit),
                    onClick = { onSubmitTask(instance.id) },
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                )
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
                        horizontalArrangement = Arrangement.spacedBy(
                            space = 8.dp,
                            alignment = Alignment.CenterHorizontally,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ChildHomeInlineIcon(iconKey = ChildHomeIconKey.Clock)
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
private fun ChildGoalCompletionNoticeCard(notice: SavingsGoalCompletionNotice, currencyCode: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChildHomeIcon(
                iconKey = ChildHomeIconKey.Trophy,
                modifier = Modifier.size(44.dp),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(Res.string.child_home_goal_completion_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(
                        Res.string.child_home_goal_completion_body,
                        notice.goalTitle,
                        formatCents(notice.currentCents.value, currencyCode),
                        formatCents(notice.targetCents.value, currencyCode),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ActiveSavingsGoalCard(
    goal: SavingsGoal,
    currentCents: Long,
    projection: SavingsGoalProjection?,
    currencyCode: String,
    onDeposit: () -> Unit,
    onWithdraw: () -> Unit,
) {
    val progressPercent = projection?.progressPercent
        ?: ((currentCents.coerceAtLeast(0L) * 100) / goal.targetCents.value).coerceIn(0, 100).toInt()
    val progress = progressPercent / 100f

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
                ChildHomeIcon(
                    iconKey = childHomeGoalIconKey(goal.iconKey),
                    modifier = Modifier.size(44.dp),
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                )
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
            ActiveSavingsGoalProjectionSummary(
                projection = projection,
                progressPercent = progressPercent,
                currencyCode = currencyCode,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ChildHomeActionButton(
                    spec = childHomeActionSpec(ChildHomeActionKind.GoalDeposit),
                    onClick = onDeposit,
                    modifier = Modifier.weight(1f),
                )
                val withdrawSpec = childHomeActionSpec(ChildHomeActionKind.GoalWithdraw)
                val canWithdraw = currentCents > 0
                ChildHomeActionButton(
                    spec = withdrawSpec,
                    onClick = onWithdraw,
                    enabled = canWithdraw,
                    outlined = true,
                    contentDescription = if (canWithdraw) {
                        childHomeText(withdrawSpec.contentDescriptionKey)
                    } else {
                        stringResource(Res.string.child_home_action_cd_goal_withdraw_disabled)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ActiveSavingsGoalProjectionSummary(
    projection: SavingsGoalProjection?,
    progressPercent: Int,
    currencyCode: String,
) {
    val visual = childHomeGoalProgressVisual(projection?.status ?: SavingsGoalProjectionStatus.NeedsContribution)
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChildHomeInlineIcon(iconKey = visual.iconKey)
                Text(
                    text = stringResource(Res.string.child_home_active_goal_progress, progressPercent),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            projection?.dailyPaceCents?.let { dailyPace ->
                Text(
                    text = stringResource(
                        Res.string.child_home_active_goal_pace,
                        formatCents(dailyPace.value, currencyCode),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        projection?.let {
            Text(
                text = stringResource(
                    Res.string.child_home_active_goal_remaining,
                    formatCents(it.remainingCents.value, currencyCode),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ActiveSavingsGoalProjectionBand(
                projection = it,
                currencyCode = currencyCode,
            )
        }
    }
}

@Composable
private fun ActiveSavingsGoalProjectionBand(projection: SavingsGoalProjection, currencyCode: String) {
    val visual = childHomeGoalProgressVisual(projection.status)
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChildHomeInlineIcon(iconKey = visual.iconKey)
                Text(
                    text = goalProjectionTitle(projection),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = goalProjectionBody(projection, currencyCode),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun goalProjectionTitle(projection: SavingsGoalProjection): String = when (projection.status) {
    SavingsGoalProjectionStatus.Completed -> stringResource(Res.string.child_home_active_goal_completed_title)
    SavingsGoalProjectionStatus.OnTrack -> stringResource(
        Res.string.child_home_active_goal_projection_title,
        projection.estimatedCompletionAt?.let(::formatDate) ?: "",
    )
    SavingsGoalProjectionStatus.NeedsContribution -> stringResource(Res.string.child_home_active_goal_no_pace_title)
}

@Composable
private fun goalProjectionBody(projection: SavingsGoalProjection, currencyCode: String): String =
    when (projection.status) {
        SavingsGoalProjectionStatus.Completed -> stringResource(Res.string.child_home_active_goal_completed_body)
        SavingsGoalProjectionStatus.OnTrack -> stringResource(
            Res.string.child_home_active_goal_projection_body,
            projection.estimatedDaysRemaining ?: 0,
            projection.dailyPaceCents?.let { formatCents(it.value, currencyCode) }.orEmpty(),
        )
        SavingsGoalProjectionStatus.NeedsContribution -> stringResource(
            Res.string.child_home_active_goal_no_pace_body,
            formatCents(projection.remainingCents.value, currencyCode),
        )
    }

@Composable
private fun CompoundInterestProjectionCard(projection: CompoundInterestProjection, currencyCode: String) {
    val maxCents = projection.threeYearsCents.value.coerceAtLeast(1)
    Card(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChildHomeIcon(
                    iconKey = ChildHomeIconKey.CoinsIn,
                    modifier = Modifier.size(44.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.child_home_compound_interest_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = compoundInterestBody(projection.explanationLevel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
            Text(
                text = stringResource(
                    Res.string.child_home_compound_interest_rate,
                    formatBasisPoints(projection.annualRateBasisPoints),
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.SemiBold,
            )
            CompoundInterestMilestone(
                label = stringResource(Res.string.child_home_compound_interest_today),
                amount = projection.currentCents,
                gain = null,
                maxCents = maxCents,
                currencyCode = currencyCode,
            )
            CompoundInterestMilestone(
                label = stringResource(Res.string.child_home_compound_interest_one_year),
                amount = projection.oneYearCents,
                gain = projection.oneYearGainCents,
                maxCents = maxCents,
                currencyCode = currencyCode,
            )
            CompoundInterestMilestone(
                label = stringResource(Res.string.child_home_compound_interest_three_years),
                amount = projection.threeYearsCents,
                gain = projection.threeYearsGainCents,
                maxCents = maxCents,
                currencyCode = currencyCode,
            )
        }
    }
}

@Composable
private fun CompoundInterestMilestone(
    label: String,
    amount: BalanceCents,
    gain: BalanceCents?,
    maxCents: Long,
    currencyCode: String,
) {
    val progress = (amount.value.toDouble() / maxCents.toDouble()).toFloat().coerceIn(0.08f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = formatCents(amount.value, currencyCode),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 8.dp),
        )
        gain?.takeIf { it.value > 0 }?.let {
            Text(
                text = stringResource(
                    Res.string.child_home_compound_interest_gain,
                    formatCents(it.value, currencyCode),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun compoundInterestBody(level: CompoundInterestExplanationLevel): String = when (level) {
    CompoundInterestExplanationLevel.Younger ->
        stringResource(Res.string.child_home_compound_interest_younger_body)
    CompoundInterestExplanationLevel.Older ->
        stringResource(Res.string.child_home_compound_interest_older_body)
}

@Composable
private fun TaskStatusPill(status: TaskInstanceStatus) {
    val visual = childHomeTaskStatusVisual(status)
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChildHomeInlineIcon(iconKey = visual.iconKey)
            Text(
                text = taskStatusLabel(status),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ChildMoneyPotsCard(state: ChildHomeUiState) {
    val balances = state.balances
    Card(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.child_home_pots_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(Res.string.child_home_pots_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ChildMoneyPot(
                    label = stringResource(Res.string.child_home_pot_spend),
                    amount = balances?.main?.value ?: 0L,
                    currencyCode = state.currencyCode,
                    modifier = Modifier.weight(1f),
                )
                ChildMoneyPot(
                    label = stringResource(Res.string.child_home_pot_save),
                    amount = balances?.goal?.value ?: 0L,
                    currencyCode = state.currencyCode,
                    modifier = Modifier.weight(1f),
                )
                ChildMoneyPot(
                    label = stringResource(Res.string.child_home_pot_give),
                    amount = balances?.give?.value ?: 0L,
                    currencyCode = state.currencyCode,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ChildMoneyPot(label: String, amount: Long, currencyCode: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.heightIn(min = 84.dp),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = formatCents(amount, currencyCode),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ChildAchievementSummaryCard(summary: ChildAchievementSummary) {
    Card(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChildHomeIcon(
                    iconKey = ChildHomeIconKey.Flame,
                    modifier = Modifier.size(44.dp),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.child_home_achievement_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(Res.string.child_home_achievement_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = stringResource(
                            Res.string.child_home_achievement_best_streak,
                            summary.bestStreakDays,
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(width = 104.dp, height = 84.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = summary.currentStreakDays.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(Res.string.child_home_achievement_current_label),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = if (summary.currentStreakDays > 0) {
                            stringResource(Res.string.child_home_achievement_current_active)
                        } else {
                            stringResource(Res.string.child_home_achievement_current_empty)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(
                            Res.string.child_home_achievement_approved_total,
                            summary.approvedTaskCount,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                summary.badges.forEach { badge ->
                    ChildAchievementBadgeChip(
                        badge = badge,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChildAchievementBadgeChip(badge: ChildAchievementBadgeProgress, modifier: Modifier = Modifier) {
    val containerColor = when {
        badge.unlocked && badge.id == ChildAchievementBadgeId.ThreeDayStreak ->
            MaterialTheme.colorScheme.tertiaryContainer
        badge.unlocked -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val iconKey = when {
        !badge.unlocked -> ChildHomeIconKey.Lock
        badge.id == ChildAchievementBadgeId.ThreeDayStreak -> ChildHomeIconKey.Flame
        else -> ChildHomeIconKey.Check
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        modifier = modifier.heightIn(min = 78.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ChildHomeInlineIcon(
                iconKey = iconKey,
                contentColor = if (badge.unlocked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                text = childAchievementBadgeTitle(badge.id),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (badge.unlocked) {
                    stringResource(Res.string.child_home_achievement_badge_unlocked)
                } else {
                    stringResource(
                        Res.string.child_home_achievement_badge_progress,
                        badge.progress,
                        badge.target,
                    )
                },
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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

private fun ChildHomeUiState.emptyState(kind: ChildHomeEmptyStateKind): ChildHomeEmptyState? =
    actionableEmptyStates.firstOrNull { it.kind == kind }

@Composable
private fun ChildHomeEmptyStateCard(emptyState: ChildHomeEmptyState, onProtectedAction: (FoundationAction) -> Unit) {
    val action = when (emptyState.action) {
        ChildHomeEmptyStateAction.OpenParentZone -> ChildEmptyStateActionUi(
            spec = when (emptyState.kind) {
                ChildHomeEmptyStateKind.SavingsGoal -> childHomeActionSpec(ChildHomeActionKind.EmptyGoalAction)
                else -> childHomeActionSpec(ChildHomeActionKind.EmptyTasksAction)
            },
            onClick = { onProtectedAction(FoundationAction.OpenParentZone) },
        )

        null -> null
    }
    Card(
        shape = RoundedCornerShape(18.dp),
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChildHomeIcon(
                    iconKey = childHomeEmptyStateIcon(emptyState.kind),
                    modifier = Modifier.size(44.dp),
                )
                Text(
                    text = childHomeEmptyStateTitle(emptyState.kind),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = childHomeEmptyStateBody(emptyState.kind),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (action != null) {
                ChildHomeActionButton(
                    spec = action.spec,
                    onClick = action.onClick,
                    outlined = action.spec.priority == ChildHomeActionPriority.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private data class ChildEmptyStateActionUi(val spec: ChildHomeActionSpec, val onClick: () -> Unit)

private fun childHomeEmptyStateIcon(kind: ChildHomeEmptyStateKind): ChildHomeIconKey = when (kind) {
    ChildHomeEmptyStateKind.Tasks -> ChildHomeIconKey.Tasks
    ChildHomeEmptyStateKind.SavingsGoal -> ChildHomeIconKey.Target
    ChildHomeEmptyStateKind.Settlements -> ChildHomeIconKey.Banknote
    ChildHomeEmptyStateKind.Ledger -> ChildHomeIconKey.Ledger
}

@Composable
private fun childHomeEmptyStateTitle(kind: ChildHomeEmptyStateKind): String = when (kind) {
    ChildHomeEmptyStateKind.Tasks -> stringResource(Res.string.child_home_empty_tasks_title)
    ChildHomeEmptyStateKind.SavingsGoal -> stringResource(Res.string.child_home_empty_goal_title)
    ChildHomeEmptyStateKind.Settlements -> stringResource(Res.string.child_home_settlements_title)
    ChildHomeEmptyStateKind.Ledger -> stringResource(Res.string.child_home_ledger_title)
}

@Composable
private fun childHomeEmptyStateBody(kind: ChildHomeEmptyStateKind): String = when (kind) {
    ChildHomeEmptyStateKind.Tasks -> stringResource(Res.string.child_home_empty_tasks_body)
    ChildHomeEmptyStateKind.SavingsGoal -> stringResource(Res.string.child_home_empty_goal_body)
    ChildHomeEmptyStateKind.Settlements -> stringResource(Res.string.child_home_empty_settlements_body)
    ChildHomeEmptyStateKind.Ledger -> stringResource(Res.string.child_home_empty_ledger_body)
}

@Composable
private fun childAchievementBadgeTitle(id: ChildAchievementBadgeId): String = when (id) {
    ChildAchievementBadgeId.FirstApprovedTask ->
        stringResource(Res.string.child_home_achievement_badge_first_task)
    ChildAchievementBadgeId.ThreeApprovedTasks ->
        stringResource(Res.string.child_home_achievement_badge_three_tasks)
    ChildAchievementBadgeId.ThreeDayStreak ->
        stringResource(Res.string.child_home_achievement_badge_three_day_streak)
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
    LedgerTransactionType.Interest -> stringResource(Res.string.ledger_type_interest)
    LedgerTransactionType.Match -> stringResource(Res.string.ledger_type_match)
    LedgerTransactionType.Reversal -> stringResource(Res.string.ledger_type_reversal)
}

@Composable
private fun ledgerActorLabel(actor: LedgerActor): String = when (actor) {
    LedgerActor.Parent -> stringResource(Res.string.ledger_actor_parent)
    LedgerActor.Child -> stringResource(Res.string.ledger_actor_child)
    LedgerActor.Guest -> stringResource(Res.string.ledger_actor_guest)
    LedgerActor.System -> stringResource(Res.string.ledger_actor_system)
}

@Composable
private fun ledgerAccountLabel(accountType: VirtualAccountType): String = when (accountType) {
    VirtualAccountType.Main -> stringResource(Res.string.ledger_account_main)
    VirtualAccountType.Goal -> stringResource(Res.string.ledger_account_goal)
    VirtualAccountType.Give -> stringResource(Res.string.ledger_account_give)
}

private fun ChildLedgerBalances?.balanceFor(accountType: VirtualAccountType): Long = when (accountType) {
    VirtualAccountType.Main -> this?.main
    VirtualAccountType.Goal -> this?.goal
    VirtualAccountType.Give -> this?.give
}?.value ?: 0L

private fun formatCents(value: Long, currencyCode: String): String {
    val sign = if (value < 0) "-" else ""
    val absolute = if (value < 0) -value else value
    val whole = absolute / 100
    val cents = (absolute % 100).toString().padStart(2, '0')
    return "$sign$whole,$cents $currencyCode"
}

private fun formatBasisPoints(value: Int): String {
    val whole = value / 100
    val cents = (value % 100).toString().padStart(2, '0')
    return if (cents == "00") whole.toString() else "$whole,$cents"
}

private fun formatDate(value: Instant): String = value.toString().substringBefore("T")

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
                    give = BalanceCents(320),
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
                activeSavingsGoalProjection = SavingsGoalProjection(
                    progressPercent = 30,
                    remainingCents = BalanceCents(2_770),
                    dailyPaceCents = BalanceCents(150),
                    estimatedDaysRemaining = 19,
                    estimatedCompletionAt = Clock.System.now().plus(19.days),
                    status = SavingsGoalProjectionStatus.OnTrack,
                ),
                activeSavingsGoalCompletionNotice = SavingsGoalCompletionNotice(
                    goalId = SavingsGoalId("goal-1"),
                    familyId = FamilyId("family-1"),
                    childProfileId = ChildProfileId("child-1"),
                    childName = "Lucas",
                    goalTitle = "Bici nueva",
                    currentCents = BalanceCents(4_200),
                    targetCents = MoneyCents(4_000),
                    completedAt = Clock.System.now(),
                ),
                compoundInterestProjection = CompoundInterestProjection(
                    annualRateBasisPoints = 333,
                    currentCents = BalanceCents(1_230),
                    oneYearCents = BalanceCents(1_270),
                    threeYearsCents = BalanceCents(1_357),
                    oneYearGainCents = BalanceCents(40),
                    threeYearsGainCents = BalanceCents(127),
                    explanationLevel = CompoundInterestExplanationLevel.Younger,
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
                achievementSummary = ChildAchievementSummary(
                    currentStreakDays = 3,
                    bestStreakDays = 5,
                    approvedTaskCount = 8,
                    badges = listOf(
                        ChildAchievementBadgeProgress(
                            id = ChildAchievementBadgeId.FirstApprovedTask,
                            progress = 1,
                            target = 1,
                        ),
                        ChildAchievementBadgeProgress(
                            id = ChildAchievementBadgeId.ThreeApprovedTasks,
                            progress = 3,
                            target = 3,
                        ),
                        ChildAchievementBadgeProgress(
                            id = ChildAchievementBadgeId.ThreeDayStreak,
                            progress = 3,
                            target = 3,
                        ),
                    ),
                ),
                syncNotice = null,
            ),
            onRequestCashOut = {},
            onOpenSavingsGoal = { _, _ -> },
            onWithdrawSavingsGoal = { _, _ -> },
            onConfirmSettlement = {},
            onAttachPhotoEvidence = {},
            onSubmitTask = {},
            onProtectedAction = {},
        )
    }
}

@Preview
@Composable
fun PreviewChildHomeEmptyContent() {
    FledgeTheme {
        ChildHomeContent(
            state = ChildHomeUiState(
                childProfileId = ChildProfileId("child-1"),
                balances = ChildLedgerBalances(
                    childProfileId = ChildProfileId("child-1"),
                    main = BalanceCents(0),
                    goal = BalanceCents(0),
                    give = BalanceCents(0),
                ),
                activeSavingsGoal = null,
                taskInstances = emptyList(),
                settlements = emptyList(),
                ledgerTransactions = emptyList(),
                syncNotice = null,
            ),
            onRequestCashOut = {},
            onOpenSavingsGoal = { _, _ -> },
            onWithdrawSavingsGoal = { _, _ -> },
            onConfirmSettlement = {},
            onAttachPhotoEvidence = {},
            onSubmitTask = {},
            onProtectedAction = {},
        )
    }
}
