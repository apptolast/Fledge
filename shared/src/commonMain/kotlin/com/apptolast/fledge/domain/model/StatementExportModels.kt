package com.apptolast.fledge.domain.model

enum class StatementExportFormat {
    Csv,
    Pdf,
}

sealed interface StatementExportScope {
    data object Family : StatementExportScope
    data class Child(val childProfileId: ChildProfileId) : StatementExportScope
}

data class StatementExportArtifact(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
    val previewText: String,
) {
    val sizeBytes: Int get() = bytes.size
}

data class StatementExportResult(
    val familyName: String,
    val scopeLabel: String,
    val currencyCode: String,
    val format: StatementExportFormat,
    val generatedAt: String,
    val artifact: StatementExportArtifact,
    val rowCount: Int,
    val totalCents: BalanceCents,
)
