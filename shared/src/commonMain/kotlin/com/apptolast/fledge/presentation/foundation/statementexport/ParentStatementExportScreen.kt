package com.apptolast.fledge.presentation.foundation.statementexport

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apptolast.fledge.domain.model.BalanceCents
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.StatementExportArtifact
import com.apptolast.fledge.domain.model.StatementExportFormat
import com.apptolast.fledge.domain.model.StatementExportResult
import com.apptolast.fledge.presentation.foundation.components.SyncNoticeBanner
import com.apptolast.fledge.presentation.theme.FledgeTheme
import fledge.shared.generated.resources.Res
import fledge.shared.generated.resources.statement_export_back
import fledge.shared.generated.resources.statement_export_csv
import fledge.shared.generated.resources.statement_export_empty_body
import fledge.shared.generated.resources.statement_export_empty_title
import fledge.shared.generated.resources.statement_export_family_scope
import fledge.shared.generated.resources.statement_export_file_meta
import fledge.shared.generated.resources.statement_export_format_title
import fledge.shared.generated.resources.statement_export_pdf
import fledge.shared.generated.resources.statement_export_preview_title
import fledge.shared.generated.resources.statement_export_scope_title
import fledge.shared.generated.resources.statement_export_subtitle
import fledge.shared.generated.resources.statement_export_title
import fledge.shared.generated.resources.statement_export_total
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ParentStatementExportScreen(onBack: () -> Unit, viewModel: ParentStatementExportViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()

    ParentStatementExportContent(
        state = state,
        onBack = onBack,
        onSelectFamilyScope = viewModel::selectFamilyScope,
        onSelectChildScope = viewModel::selectChildScope,
        onSelectFormat = viewModel::selectFormat,
    )
}

@Composable
fun ParentStatementExportContent(
    state: ParentStatementExportUiState,
    onBack: () -> Unit,
    onSelectFamilyScope: () -> Unit,
    onSelectChildScope: (ChildProfileId) -> Unit,
    onSelectFormat: (StatementExportFormat) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { StatementExportHeader(onBack = onBack) }
            state.syncNotice?.let { notice ->
                item { SyncNoticeBanner(notice = notice) }
            }
            item { StatementFormatSelector(format = state.format, onSelectFormat = onSelectFormat) }
            item {
                StatementScopeSelector(
                    children = state.children,
                    selectedChildProfileId = state.selectedChildProfileId,
                    onSelectFamilyScope = onSelectFamilyScope,
                    onSelectChildScope = onSelectChildScope,
                )
            }
            val export = state.export
            if (export == null) {
                item { StatementEmptyCard() }
            } else {
                item { StatementExportSummaryCard(export = export) }
                item {
                    Text(
                        text = stringResource(Res.string.statement_export_preview_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                item { StatementPreviewCard(export.artifact.previewText) }
            }
        }
    }
}

@Composable
private fun StatementExportHeader(onBack: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.statement_export_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(Res.string.statement_export_back))
            }
        }
        Text(
            text = stringResource(Res.string.statement_export_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatementFormatSelector(format: StatementExportFormat, onSelectFormat: (StatementExportFormat) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.statement_export_format_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatementFormatButton(
                label = stringResource(Res.string.statement_export_csv),
                selected = format == StatementExportFormat.Csv,
                onClick = { onSelectFormat(StatementExportFormat.Csv) },
                modifier = Modifier.weight(1f),
            )
            StatementFormatButton(
                label = stringResource(Res.string.statement_export_pdf),
                selected = format == StatementExportFormat.Pdf,
                onClick = { onSelectFormat(StatementExportFormat.Pdf) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatementFormatButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    if (selected) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            modifier = modifier.heightIn(min = 48.dp),
        ) {
            Text(label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            modifier = modifier.heightIn(min = 48.dp),
        ) {
            Text(label)
        }
    }
}

@Composable
private fun StatementScopeSelector(
    children: List<ChildProfile>,
    selectedChildProfileId: ChildProfileId?,
    onSelectFamilyScope: () -> Unit,
    onSelectChildScope: (ChildProfileId) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.statement_export_scope_title),
            style = MaterialTheme.typography.titleMedium,
        )
        FilterChip(
            selected = selectedChildProfileId == null,
            onClick = onSelectFamilyScope,
            label = { Text(stringResource(Res.string.statement_export_family_scope)) },
            modifier = Modifier.fillMaxWidth(),
        )
        children.forEach { child ->
            FilterChip(
                selected = selectedChildProfileId == child.id,
                onClick = { onSelectChildScope(child.id) },
                label = { Text(child.displayName) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StatementExportSummaryCard(export: StatementExportResult) {
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
                text = export.artifact.fileName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(
                    Res.string.statement_export_file_meta,
                    export.artifact.mimeType,
                    export.rowCount,
                    export.artifact.sizeBytes,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    Res.string.statement_export_total,
                    formatStatementCents(export.totalCents.value),
                    export.currencyCode,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatementPreviewCard(previewText: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = previewText,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun StatementEmptyCard() {
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
                text = stringResource(Res.string.statement_export_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(Res.string.statement_export_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatStatementCents(value: Long): String {
    val sign = if (value < 0) "-" else ""
    val absolute = if (value < 0) -value else value
    val whole = absolute / 100
    val cents = (absolute % 100).toString().padStart(2, '0')
    return "$sign$whole,$cents"
}

@Preview
@Composable
fun PreviewParentStatementExportContent() {
    FledgeTheme {
        ParentStatementExportContent(
            state = ParentStatementExportUiState(
                children = listOf(ChildProfile(ChildProfileId("child-1"), "Lucas", 2018, "rocket")),
                format = StatementExportFormat.Csv,
                export = StatementExportResult(
                    familyName = "Familia Garcia",
                    scopeLabel = "Familia Garcia",
                    currencyCode = "EUR",
                    format = StatementExportFormat.Csv,
                    generatedAt = "2026-07-30",
                    rowCount = 2,
                    totalCents = BalanceCents(250),
                    artifact = StatementExportArtifact(
                        fileName = "fledge-familia-garcia.csv",
                        mimeType = "text/csv",
                        bytes = "date,child,amount".encodeToByteArray(),
                        previewText = "date,child,amount\n2026-07-30,Lucas,2.50",
                    ),
                ),
                syncNotice = null,
            ),
            onBack = {},
            onSelectFamilyScope = {},
            onSelectChildScope = {},
            onSelectFormat = {},
        )
    }
}
