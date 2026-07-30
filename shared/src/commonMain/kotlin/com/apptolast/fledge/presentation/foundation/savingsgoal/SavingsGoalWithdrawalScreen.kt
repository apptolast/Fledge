package com.apptolast.fledge.presentation.foundation.savingsgoal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apptolast.fledge.domain.model.BalanceCents
import com.apptolast.fledge.domain.model.ChildLedgerBalances
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.SavingsGoal
import com.apptolast.fledge.domain.model.SavingsGoalId
import com.apptolast.fledge.domain.model.VirtualAccountType
import com.apptolast.fledge.presentation.foundation.components.SyncNoticeBanner
import com.apptolast.fledge.presentation.foundation.manualadjustment.parseAmountCents
import com.apptolast.fledge.presentation.theme.FledgeTheme
import fledge.shared.generated.resources.Res
import fledge.shared.generated.resources.operation_error_sync
import fledge.shared.generated.resources.savings_goal_withdrawal_amount_label
import fledge.shared.generated.resources.savings_goal_withdrawal_back
import fledge.shared.generated.resources.savings_goal_withdrawal_confirm
import fledge.shared.generated.resources.savings_goal_withdrawal_confirm_cost
import fledge.shared.generated.resources.savings_goal_withdrawal_error_insufficient_goal
import fledge.shared.generated.resources.savings_goal_withdrawal_error_invalid_amount
import fledge.shared.generated.resources.savings_goal_withdrawal_error_missing_confirmation
import fledge.shared.generated.resources.savings_goal_withdrawal_error_missing_goal
import fledge.shared.generated.resources.savings_goal_withdrawal_goal_after
import fledge.shared.generated.resources.savings_goal_withdrawal_goal_debit
import fledge.shared.generated.resources.savings_goal_withdrawal_main_after
import fledge.shared.generated.resources.savings_goal_withdrawal_main_credit
import fledge.shared.generated.resources.savings_goal_withdrawal_opportunity_body
import fledge.shared.generated.resources.savings_goal_withdrawal_opportunity_title
import fledge.shared.generated.resources.savings_goal_withdrawal_quick_five
import fledge.shared.generated.resources.savings_goal_withdrawal_quick_label
import fledge.shared.generated.resources.savings_goal_withdrawal_quick_one
import fledge.shared.generated.resources.savings_goal_withdrawal_quick_two
import fledge.shared.generated.resources.savings_goal_withdrawal_subtitle
import fledge.shared.generated.resources.savings_goal_withdrawal_summary_title
import fledge.shared.generated.resources.savings_goal_withdrawal_title
import kotlin.time.Clock
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SavingsGoalWithdrawalScreen(
    childProfileId: ChildProfileId,
    goalId: SavingsGoalId,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: SavingsGoalWithdrawalViewModel = koinViewModel(),
) {
    LaunchedEffect(childProfileId, goalId) {
        viewModel.load(childProfileId, goalId)
    }
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    SavingsGoalWithdrawalContent(
        state = state,
        onAmountChanged = viewModel::updateAmount,
        onQuickAmountSelected = viewModel::selectQuickAmount,
        onConfirmOpportunityCostChanged = viewModel::confirmOpportunityCost,
        onBack = onBack,
        onSubmit = {
            scope.launch {
                if (viewModel.submit()) onSaved()
            }
        },
    )
}

@Composable
fun SavingsGoalWithdrawalContent(
    state: SavingsGoalWithdrawalUiState,
    onAmountChanged: (String) -> Unit,
    onQuickAmountSelected: (Long) -> Unit,
    onConfirmOpportunityCostChanged: (Boolean) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(Res.string.savings_goal_withdrawal_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = stringResource(
                            Res.string.savings_goal_withdrawal_subtitle,
                            state.goal?.title.orEmpty(),
                            formatCents(state.balances?.goal?.value ?: 0L, state.currencyCode),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            state.syncNotice?.let { notice ->
                item {
                    SyncNoticeBanner(notice = notice)
                }
            }
            item {
                SavingsGoalWithdrawalForm(
                    state = state,
                    onAmountChanged = onAmountChanged,
                    onQuickAmountSelected = onQuickAmountSelected,
                    onConfirmOpportunityCostChanged = onConfirmOpportunityCostChanged,
                )
            }
            state.error?.let { error ->
                item {
                    Text(
                        text = savingsGoalWithdrawalErrorText(error),
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
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Button(
                        onClick = onSubmit,
                        enabled = state.canAttemptSubmit,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                    ) {
                        Text(stringResource(Res.string.savings_goal_withdrawal_confirm))
                    }
                    OutlinedButton(
                        onClick = onBack,
                        enabled = !state.isSaving,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(Res.string.savings_goal_withdrawal_back))
                    }
                }
            }
        }
    }
}

@Composable
private fun SavingsGoalWithdrawalForm(
    state: SavingsGoalWithdrawalUiState,
    onAmountChanged: (String) -> Unit,
    onQuickAmountSelected: (Long) -> Unit,
    onConfirmOpportunityCostChanged: (Boolean) -> Unit,
) {
    val requestedAmount = parseAmountCents(state.amountInput)?.takeIf { it > 0L } ?: 0L
    val mainBalance = state.balances?.main?.value ?: 0L
    val goalBalance = state.balances?.goal?.value ?: 0L
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                value = state.amountInput,
                onValueChange = onAmountChanged,
                label = { Text(stringResource(Res.string.savings_goal_withdrawal_amount_label)) },
                suffix = { Text(state.currencyCode) },
                isError = state.error == SavingsGoalWithdrawalError.InvalidAmount,
                enabled = !state.isSaving,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(Res.string.savings_goal_withdrawal_quick_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickWithdrawalAmountChip(
                        label = stringResource(Res.string.savings_goal_withdrawal_quick_one),
                        cents = 100,
                        selectedAmount = requestedAmount,
                        enabled = !state.isSaving,
                        onQuickAmountSelected = onQuickAmountSelected,
                    )
                    QuickWithdrawalAmountChip(
                        label = stringResource(Res.string.savings_goal_withdrawal_quick_two),
                        cents = 200,
                        selectedAmount = requestedAmount,
                        enabled = !state.isSaving,
                        onQuickAmountSelected = onQuickAmountSelected,
                    )
                    QuickWithdrawalAmountChip(
                        label = stringResource(Res.string.savings_goal_withdrawal_quick_five),
                        cents = 500,
                        selectedAmount = requestedAmount,
                        enabled = !state.isSaving,
                        onQuickAmountSelected = onQuickAmountSelected,
                    )
                }
            }
            WithdrawalOpportunityCost(
                requestedAmount = requestedAmount,
                goalAfter = goalBalance - requestedAmount,
                currencyCode = state.currencyCode,
                confirmed = state.opportunityCostConfirmed,
                isError = state.error == SavingsGoalWithdrawalError.MissingConfirmation,
                onConfirmOpportunityCostChanged = onConfirmOpportunityCostChanged,
            )
            WithdrawalSummary(
                requestedAmount = requestedAmount,
                goalAfter = goalBalance - requestedAmount,
                mainAfter = mainBalance + requestedAmount,
                currencyCode = state.currencyCode,
            )
        }
    }
}

@Composable
private fun RowScope.QuickWithdrawalAmountChip(
    label: String,
    cents: Long,
    selectedAmount: Long,
    enabled: Boolean,
    onQuickAmountSelected: (Long) -> Unit,
) {
    FilterChip(
        selected = selectedAmount == cents,
        onClick = { onQuickAmountSelected(cents) },
        enabled = enabled,
        label = { Text(label) },
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 44.dp),
    )
}

@Composable
private fun WithdrawalOpportunityCost(
    requestedAmount: Long,
    goalAfter: Long,
    currencyCode: String,
    confirmed: Boolean,
    isError: Boolean,
    onConfirmOpportunityCostChanged: (Boolean) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.savings_goal_withdrawal_opportunity_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(
                    Res.string.savings_goal_withdrawal_opportunity_body,
                    formatCents(requestedAmount, currencyCode),
                    formatCents(goalAfter, currencyCode),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = confirmed,
                    onCheckedChange = onConfirmOpportunityCostChanged,
                )
                Text(
                    text = stringResource(Res.string.savings_goal_withdrawal_confirm_cost),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun WithdrawalSummary(requestedAmount: Long, goalAfter: Long, mainAfter: Long, currencyCode: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.savings_goal_withdrawal_summary_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            WithdrawalSummaryRow(
                label = stringResource(
                    Res.string.savings_goal_withdrawal_goal_debit,
                    formatCents(requestedAmount, currencyCode),
                ),
                value = stringResource(
                    Res.string.savings_goal_withdrawal_goal_after,
                    formatCents(goalAfter, currencyCode),
                ),
            )
            WithdrawalSummaryRow(
                label = stringResource(
                    Res.string.savings_goal_withdrawal_main_credit,
                    formatCents(requestedAmount, currencyCode),
                ),
                value = stringResource(
                    Res.string.savings_goal_withdrawal_main_after,
                    formatCents(mainAfter, currencyCode),
                ),
            )
        }
    }
}

@Composable
private fun WithdrawalSummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun savingsGoalWithdrawalErrorText(error: SavingsGoalWithdrawalError): String = when (error) {
    SavingsGoalWithdrawalError.MissingGoal -> stringResource(Res.string.savings_goal_withdrawal_error_missing_goal)
    SavingsGoalWithdrawalError.InvalidAmount -> stringResource(Res.string.savings_goal_withdrawal_error_invalid_amount)
    SavingsGoalWithdrawalError.InsufficientGoalBalance ->
        stringResource(Res.string.savings_goal_withdrawal_error_insufficient_goal)
    SavingsGoalWithdrawalError.MissingConfirmation ->
        stringResource(Res.string.savings_goal_withdrawal_error_missing_confirmation)
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
fun PreviewSavingsGoalWithdrawalContent() {
    FledgeTheme {
        SavingsGoalWithdrawalContent(
            state = SavingsGoalWithdrawalUiState(
                childProfileId = ChildProfileId("child-1"),
                goalId = SavingsGoalId("goal-1"),
                goal = SavingsGoal(
                    id = SavingsGoalId("goal-1"),
                    familyId = FamilyId("family-1"),
                    childProfileId = ChildProfileId("child-1"),
                    title = "Bici nueva",
                    targetCents = MoneyCents(4_000),
                    accountType = VirtualAccountType.Goal,
                    iconKey = "bike",
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now(),
                ),
                balances = ChildLedgerBalances(
                    childProfileId = ChildProfileId("child-1"),
                    main = BalanceCents(250),
                    goal = BalanceCents(1_230),
                ),
                amountInput = "2,00",
                syncNotice = null,
            ),
            onAmountChanged = {},
            onQuickAmountSelected = {},
            onConfirmOpportunityCostChanged = {},
            onBack = {},
            onSubmit = {},
        )
    }
}
