package com.apptolast.fledge.presentation.foundation.weeklydigest

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apptolast.fledge.domain.model.BalanceCents
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.WeeklyChildDigest
import com.apptolast.fledge.domain.model.WeeklyParentDigest
import com.apptolast.fledge.domain.model.WeeklyParentDigestPeriod
import com.apptolast.fledge.presentation.foundation.components.SyncNoticeBanner
import com.apptolast.fledge.presentation.theme.FledgeTheme
import fledge.shared.generated.resources.Res
import fledge.shared.generated.resources.weekly_digest_back
import fledge.shared.generated.resources.weekly_digest_cash_body
import fledge.shared.generated.resources.weekly_digest_cash_title
import fledge.shared.generated.resources.weekly_digest_child_debt
import fledge.shared.generated.resources.weekly_digest_child_goals
import fledge.shared.generated.resources.weekly_digest_child_savings
import fledge.shared.generated.resources.weekly_digest_child_tasks
import fledge.shared.generated.resources.weekly_digest_children_title
import fledge.shared.generated.resources.weekly_digest_empty_body
import fledge.shared.generated.resources.weekly_digest_empty_title
import fledge.shared.generated.resources.weekly_digest_missing_family_body
import fledge.shared.generated.resources.weekly_digest_missing_family_title
import fledge.shared.generated.resources.weekly_digest_pending_actions_body
import fledge.shared.generated.resources.weekly_digest_pending_actions_title
import fledge.shared.generated.resources.weekly_digest_period_label
import fledge.shared.generated.resources.weekly_digest_savings_body
import fledge.shared.generated.resources.weekly_digest_savings_title
import fledge.shared.generated.resources.weekly_digest_tasks_body
import fledge.shared.generated.resources.weekly_digest_tasks_title
import fledge.shared.generated.resources.weekly_digest_title
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ParentWeeklyDigestScreen(onBack: () -> Unit, viewModel: ParentWeeklyDigestViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()

    ParentWeeklyDigestContent(
        state = state,
        onBack = onBack,
    )
}

@Composable
fun ParentWeeklyDigestContent(state: ParentWeeklyDigestUiState, onBack: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                DigestHeader(onBack = onBack)
            }
            state.syncNotice?.let { notice ->
                item { SyncNoticeBanner(notice = notice) }
            }

            val digest = state.digest
            if (digest == null) {
                item { MissingFamilyCard() }
            } else {
                item {
                    Text(
                        text = stringResource(Res.string.weekly_digest_period_label, digest.familyName),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!digest.hasActivity) {
                    item { WeeklyDigestEmptyCard() }
                }
                item { WeeklyDigestTaskCard(digest = digest) }
                item { WeeklyDigestSavingsCard(digest = digest) }
                item { WeeklyDigestCashCard(digest = digest) }
                item { WeeklyDigestPendingActionsCard(digest = digest) }
                item {
                    Text(
                        text = stringResource(Res.string.weekly_digest_children_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                items(digest.children, key = { it.childProfileId.value }) { child ->
                    WeeklyChildDigestCard(
                        child = child,
                        currencyCode = digest.currency.value,
                    )
                }
            }
        }
    }
}

@Composable
private fun DigestHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.weekly_digest_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(
            onClick = onBack,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text(stringResource(Res.string.weekly_digest_back))
        }
    }
}

@Composable
private fun WeeklyDigestTaskCard(digest: WeeklyParentDigest) {
    DigestMetricCard(
        title = stringResource(Res.string.weekly_digest_tasks_title),
        value = stringResource(
            Res.string.weekly_digest_tasks_body,
            digest.submittedTaskCount,
            digest.approvedTaskCount,
            digest.rejectedTaskCount,
        ),
    )
}

@Composable
private fun WeeklyDigestSavingsCard(digest: WeeklyParentDigest) {
    DigestMetricCard(
        title = stringResource(Res.string.weekly_digest_savings_title),
        value = stringResource(
            Res.string.weekly_digest_savings_body,
            formatDigestCents(digest.savedCents.value, digest.currency.value),
            formatDigestCents(digest.withdrawnFromGoalsCents.value, digest.currency.value),
        ),
    )
}

@Composable
private fun WeeklyDigestCashCard(digest: WeeklyParentDigest) {
    DigestMetricCard(
        title = stringResource(Res.string.weekly_digest_cash_title),
        value = stringResource(
            Res.string.weekly_digest_cash_body,
            formatDigestCents(digest.requestedCashOutCents.value, digest.currency.value),
            digest.pendingSettlementCount,
        ),
    )
}

@Composable
private fun WeeklyDigestPendingActionsCard(digest: WeeklyParentDigest) {
    DigestMetricCard(
        title = stringResource(Res.string.weekly_digest_pending_actions_title),
        value = stringResource(
            Res.string.weekly_digest_pending_actions_body,
            digest.pendingTaskApprovalCount,
            formatDigestCents(digest.pendingDebtCents.value, digest.currency.value),
        ),
    )
}

@Composable
private fun DigestMetricCard(title: String, value: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun WeeklyChildDigestCard(child: WeeklyChildDigest, currencyCode: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = child.childName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(
                    Res.string.weekly_digest_child_tasks,
                    child.submittedTaskCount,
                    child.approvedTaskCount,
                    child.pendingTaskApprovalCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    Res.string.weekly_digest_child_savings,
                    formatDigestCents(child.savedCents.value, currencyCode),
                    formatDigestCents(child.withdrawnFromGoalsCents.value, currencyCode),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    Res.string.weekly_digest_child_debt,
                    formatDigestCents(child.pendingDebtCents.value, currencyCode),
                    child.pendingSettlementCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    Res.string.weekly_digest_child_goals,
                    child.activeGoalCount,
                    formatDigestCents(child.goalBalanceCents.value, currencyCode),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeeklyDigestEmptyCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.weekly_digest_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(Res.string.weekly_digest_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MissingFamilyCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.weekly_digest_missing_family_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(Res.string.weekly_digest_missing_family_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatDigestCents(value: Long, currencyCode: String): String {
    val sign = if (value < 0) "-" else ""
    val absolute = if (value < 0) -value else value
    val whole = absolute / 100
    val cents = (absolute % 100).toString().padStart(2, '0')
    return "$sign$whole,$cents $currencyCode"
}

@Preview
@Composable
fun PreviewParentWeeklyDigestContent() {
    FledgeTheme {
        ParentWeeklyDigestContent(
            state = ParentWeeklyDigestUiState(
                digest = WeeklyParentDigest(
                    familyId = FamilyId("family-1"),
                    familyName = "Familia Garcia",
                    currency = CurrencyCode("EUR"),
                    period = WeeklyParentDigestPeriod(
                        startAt = Instant.parse("2026-07-23T12:00:00Z"),
                        endAt = Instant.parse("2026-07-30T12:00:00Z"),
                    ),
                    children = listOf(
                        WeeklyChildDigest(
                            childProfileId = ChildProfileId("child-1"),
                            childName = "Lucas",
                            submittedTaskCount = 3,
                            approvedTaskCount = 2,
                            rejectedTaskCount = 0,
                            pendingTaskApprovalCount = 1,
                            savedCents = BalanceCents(1_200),
                            withdrawnFromGoalsCents = BalanceCents(250),
                            requestedCashOutCents = BalanceCents(700),
                            pendingDebtCents = BalanceCents(700),
                            pendingSettlementCount = 1,
                            activeGoalCount = 1,
                            goalBalanceCents = BalanceCents(4_200),
                        ),
                    ),
                    submittedTaskCount = 3,
                    approvedTaskCount = 2,
                    rejectedTaskCount = 0,
                    pendingTaskApprovalCount = 1,
                    savedCents = BalanceCents(1_200),
                    withdrawnFromGoalsCents = BalanceCents(250),
                    requestedCashOutCents = BalanceCents(700),
                    pendingDebtCents = BalanceCents(700),
                    pendingSettlementCount = 1,
                ),
                syncNotice = null,
            ),
            onBack = {},
        )
    }
}

@Preview
@Composable
fun PreviewParentWeeklyDigestEmptyContent() {
    FledgeTheme {
        ParentWeeklyDigestContent(
            state = ParentWeeklyDigestUiState(
                digest = WeeklyParentDigest(
                    familyId = FamilyId("family-1"),
                    familyName = "Familia Garcia",
                    currency = CurrencyCode("EUR"),
                    period = WeeklyParentDigestPeriod(
                        startAt = Instant.parse("2026-07-23T12:00:00Z"),
                        endAt = Instant.parse("2026-07-30T12:00:00Z"),
                    ),
                    children = emptyList(),
                    submittedTaskCount = 0,
                    approvedTaskCount = 0,
                    rejectedTaskCount = 0,
                    pendingTaskApprovalCount = 0,
                    savedCents = BalanceCents(0),
                    withdrawnFromGoalsCents = BalanceCents(0),
                    requestedCashOutCents = BalanceCents(0),
                    pendingDebtCents = BalanceCents(0),
                    pendingSettlementCount = 0,
                ),
                syncNotice = null,
            ),
            onBack = {},
        )
    }
}
