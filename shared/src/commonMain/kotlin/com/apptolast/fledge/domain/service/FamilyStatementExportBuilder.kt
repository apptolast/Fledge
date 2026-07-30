package com.apptolast.fledge.domain.service

import com.apptolast.fledge.domain.model.BalanceCents
import com.apptolast.fledge.domain.model.CashOutSettlement
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.Family
import com.apptolast.fledge.domain.model.LedgerTransaction
import com.apptolast.fledge.domain.model.StatementExportArtifact
import com.apptolast.fledge.domain.model.StatementExportFormat
import com.apptolast.fledge.domain.model.StatementExportResult
import com.apptolast.fledge.domain.model.StatementExportScope
import kotlin.time.Instant

class FamilyStatementExportBuilder {
    fun build(
        family: Family?,
        children: List<ChildProfile>,
        transactions: List<LedgerTransaction>,
        settlements: List<CashOutSettlement>,
        scope: StatementExportScope,
        format: StatementExportFormat,
        generatedAt: Instant,
    ): StatementExportResult? {
        family ?: return null

        val childById = children.associateBy { it.id }
        val scopedChildId = (scope as? StatementExportScope.Child)?.childProfileId
        val scopedTransactions = transactions
            .filter { it.familyId == family.id }
            .filter { scopedChildId == null || it.childProfileId == scopedChildId }
        val scopedSettlements = settlements
            .filter { it.familyId == family.id }
            .filter { scopedChildId == null || it.childProfileId == scopedChildId }

        val rows = buildRows(
            transactions = scopedTransactions,
            settlements = scopedSettlements,
            childName = { childId -> childById[childId]?.displayName ?: childId.value },
        )
        val scopeLabel = when (scope) {
            StatementExportScope.Family -> family.name
            is StatementExportScope.Child -> childById[scope.childProfileId]?.displayName ?: scope.childProfileId.value
        }
        val generatedDate = generatedAt.toString().substringBefore("T")
        val previewLines = previewLines(
            familyName = family.name,
            scopeLabel = scopeLabel,
            generatedDate = generatedDate,
            rows = rows,
        )
        val fileBaseName = "fledge-${family.name.slug()}-${scopeLabel.slug()}-$generatedDate"
        val artifact = when (format) {
            StatementExportFormat.Csv -> StatementExportArtifact(
                fileName = "$fileBaseName.csv",
                mimeType = "text/csv",
                bytes = rows.toCsv().encodeToByteArray(),
                previewText = rows.toCsv(),
            )
            StatementExportFormat.Pdf -> StatementExportArtifact(
                fileName = "$fileBaseName.pdf",
                mimeType = "application/pdf",
                bytes = buildPdf(previewLines),
                previewText = previewLines.joinToString(separator = "\n"),
            )
        }

        return StatementExportResult(
            familyName = family.name,
            scopeLabel = scopeLabel,
            currencyCode = family.currency.value,
            format = format,
            generatedAt = generatedDate,
            artifact = artifact,
            rowCount = rows.size,
            totalCents = BalanceCents(rows.sumOf { it.amountCents }),
        )
    }

    private fun buildRows(
        transactions: List<LedgerTransaction>,
        settlements: List<CashOutSettlement>,
        childName: (ChildProfileId) -> String,
    ): List<StatementRow> {
        val transactionRows = transactions.map { transaction ->
            StatementRow(
                date = transaction.createdAt.toString().substringBefore("T"),
                childName = childName(transaction.childProfileId),
                source = "ledger",
                account = transaction.accountType.name,
                type = transaction.type.name,
                actor = transaction.createdBy.name,
                status = "",
                concept = transaction.concept.value,
                amountCents = transaction.amountCents.value,
            )
        }
        val settlementRows = settlements.map { settlement ->
            StatementRow(
                date = settlement.requestedAt.toString().substringBefore("T"),
                childName = childName(settlement.childProfileId),
                source = "cash_out",
                account = "Main",
                type = "SettlementRequest",
                actor = "Child",
                status = settlement.status.name,
                concept = settlement.concept.value,
                amountCents = -settlement.amountCents.value,
            )
        }

        return (transactionRows + settlementRows)
            .sortedWith(compareByDescending<StatementRow> { it.date }.thenBy { it.childName }.thenBy { it.concept })
    }

    private fun List<StatementRow>.toCsv(): String {
        val header = listOf("date", "child", "source", "account", "type", "actor", "status", "concept", "amount")
        val rows = map { row ->
            listOf(
                row.date,
                row.childName,
                row.source,
                row.account,
                row.type,
                row.actor,
                row.status,
                row.concept,
                row.amountCents.toDecimalAmount(),
            )
        }
        return (listOf(header) + rows).joinToString(separator = "\n") { cells ->
            cells.joinToString(separator = ",", transform = ::csvCell)
        }
    }

    private fun previewLines(
        familyName: String,
        scopeLabel: String,
        generatedDate: String,
        rows: List<StatementRow>,
    ): List<String> = buildList {
        add("Fledge statement")
        add("Family: $familyName")
        add("Scope: $scopeLabel")
        add("Generated: $generatedDate")
        add("Rows: ${rows.size}")
        add("Total: ${rows.sumOf { it.amountCents }.toDecimalAmount()}")
        add("")
        rows.take(28).forEach { row ->
            add("${row.date}  ${row.childName}  ${row.type}  ${row.amountCents.toDecimalAmount()}  ${row.concept}")
        }
    }

    private fun buildPdf(lines: List<String>): ByteArray {
        val stream = buildString {
            append("BT\n/F1 12 Tf\n50 760 Td\n16 TL\n")
            lines.take(36).forEach { line ->
                append("(")
                append(line.asciiPdfText().take(92).escapePdfText())
                append(") Tj\nT*\n")
            }
            append("ET")
        }
        val objects = listOf(
            "<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
            "<< /Length ${stream.encodeToByteArray().size} >>\nstream\n$stream\nendstream",
        )
        val output = StringBuilder("%PDF-1.4\n")
        val offsets = mutableListOf(0)
        objects.forEachIndexed { index, body ->
            offsets += output.toString().encodeToByteArray().size
            output.append("${index + 1} 0 obj\n")
            output.append(body)
            output.append("\nendobj\n")
        }
        val xrefOffset = output.toString().encodeToByteArray().size
        output.append("xref\n0 ${objects.size + 1}\n")
        output.append("0000000000 65535 f \n")
        offsets.drop(1).forEach { offset ->
            output.append(offset.toString().padStart(10, '0'))
            output.append(" 00000 n \n")
        }
        output.append("trailer\n<< /Size ${objects.size + 1} /Root 1 0 R >>\n")
        output.append("startxref\n$xrefOffset\n%%EOF")
        return output.toString().encodeToByteArray()
    }
}

private data class StatementRow(
    val date: String,
    val childName: String,
    val source: String,
    val account: String,
    val type: String,
    val actor: String,
    val status: String,
    val concept: String,
    val amountCents: Long,
)

private fun csvCell(value: String): String {
    val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
    if (!needsQuotes) return value
    return buildString {
        append('"')
        value.forEach { char ->
            if (char == '"') append("\"\"") else append(char)
        }
        append('"')
    }
}

private fun Long.toDecimalAmount(): String {
    val sign = if (this < 0) "-" else ""
    val absolute = if (this < 0) -this else this
    val whole = absolute / 100
    val cents = (absolute % 100).toString().padStart(2, '0')
    return "$sign$whole.$cents"
}

private fun String.slug(): String = trim()
    .lowercase()
    .map { char -> if (char.isLetterOrDigit()) char else '-' }
    .joinToString(separator = "")
    .replace(Regex("-+"), "-")
    .trim('-')
    .ifBlank { "statement" }

private fun String.asciiPdfText(): String = map { char ->
    if (char.code in 32..126) char else '?'
}.joinToString(separator = "")

private fun String.escapePdfText(): String = replace("\\", "\\\\")
    .replace("(", "\\(")
    .replace(")", "\\)")
