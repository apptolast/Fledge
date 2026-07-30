package com.apptolast.fledge.domain.service

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

data class ClosedBetaMetricSnapshot(
    val activeFamilyCount: Int,
    val taskReviewDurations: List<Duration>,
    val cashOutDurations: List<Duration>,
    val openCashOutAges: List<Duration>,
    val balanceDiscrepancyCount: Int,
    val observationPeriod: Duration,
) {
    init {
        require(activeFamilyCount >= 0) { "Active family count cannot be negative." }
        require(balanceDiscrepancyCount >= 0) { "Balance discrepancy count cannot be negative." }
        require(observationPeriod >= Duration.ZERO) { "Observation period cannot be negative." }
        require(taskReviewDurations.none { it < Duration.ZERO }) { "Task review durations cannot be negative." }
        require(cashOutDurations.none { it < Duration.ZERO }) { "Cash-out durations cannot be negative." }
        require(openCashOutAges.none { it < Duration.ZERO }) { "Open cash-out ages cannot be negative." }
    }
}

data class ClosedBetaReadinessThresholds(
    val minActiveFamilies: Int = 10,
    val maxActiveFamilies: Int = 15,
    val maxMeanTaskReviewDuration: Duration = 24.hours,
    val maxMeanCashOutDuration: Duration = 72.hours,
    val maxOpenCashOutAge: Duration = 72.hours,
    val minBalanceObservationPeriod: Duration = 28.days,
) {
    init {
        require(minActiveFamilies > 0) { "Minimum family count must be positive." }
        require(maxActiveFamilies >= minActiveFamilies) { "Maximum family count cannot be below minimum." }
        require(maxMeanTaskReviewDuration > Duration.ZERO) { "Task review threshold must be positive." }
        require(maxMeanCashOutDuration > Duration.ZERO) { "Cash-out threshold must be positive." }
        require(maxOpenCashOutAge > Duration.ZERO) { "Open cash-out threshold must be positive." }
        require(minBalanceObservationPeriod > Duration.ZERO) { "Balance observation period must be positive." }
    }
}

enum class ClosedBetaMetricStatus {
    Pass,
    NeedsMoreData,
    Fail,
}

enum class ClosedBetaReadinessStatus {
    Ready,
    NeedsMoreData,
    AtRisk,
}

data class ClosedBetaMetricReport(
    val status: ClosedBetaMetricStatus,
    val sampleCount: Int,
    val averageDuration: Duration? = null,
    val thresholdDuration: Duration? = null,
    val violatingCount: Int = 0,
)

data class ClosedBetaReadinessReport(
    val status: ClosedBetaReadinessStatus,
    val cohort: ClosedBetaMetricReport,
    val approvalSla: ClosedBetaMetricReport,
    val balanceIntegrity: ClosedBetaMetricReport,
    val cashOutSla: ClosedBetaMetricReport,
    val isAggregateOnly: Boolean = true,
)

object ClosedBetaReadinessEvaluator {

    fun evaluate(
        snapshot: ClosedBetaMetricSnapshot,
        thresholds: ClosedBetaReadinessThresholds = ClosedBetaReadinessThresholds(),
    ): ClosedBetaReadinessReport {
        val cohort = evaluateCohort(snapshot, thresholds)
        val approvalSla = evaluateApprovalSla(snapshot, thresholds)
        val balanceIntegrity = evaluateBalanceIntegrity(snapshot, thresholds)
        val cashOutSla = evaluateCashOutSla(snapshot, thresholds)
        val metricStatuses = listOf(cohort.status, approvalSla.status, balanceIntegrity.status, cashOutSla.status)
        val status = when {
            ClosedBetaMetricStatus.Fail in metricStatuses -> ClosedBetaReadinessStatus.AtRisk
            ClosedBetaMetricStatus.NeedsMoreData in metricStatuses -> ClosedBetaReadinessStatus.NeedsMoreData
            else -> ClosedBetaReadinessStatus.Ready
        }

        return ClosedBetaReadinessReport(
            status = status,
            cohort = cohort,
            approvalSla = approvalSla,
            balanceIntegrity = balanceIntegrity,
            cashOutSla = cashOutSla,
        )
    }

    private fun evaluateCohort(
        snapshot: ClosedBetaMetricSnapshot,
        thresholds: ClosedBetaReadinessThresholds,
    ): ClosedBetaMetricReport {
        val status = when {
            snapshot.activeFamilyCount < thresholds.minActiveFamilies -> ClosedBetaMetricStatus.NeedsMoreData
            snapshot.activeFamilyCount > thresholds.maxActiveFamilies -> ClosedBetaMetricStatus.Fail
            else -> ClosedBetaMetricStatus.Pass
        }
        return ClosedBetaMetricReport(
            status = status,
            sampleCount = snapshot.activeFamilyCount,
            violatingCount = if (status == ClosedBetaMetricStatus.Fail) {
                snapshot.activeFamilyCount - thresholds.maxActiveFamilies
            } else {
                0
            },
        )
    }

    private fun evaluateApprovalSla(
        snapshot: ClosedBetaMetricSnapshot,
        thresholds: ClosedBetaReadinessThresholds,
    ): ClosedBetaMetricReport {
        val average = snapshot.taskReviewDurations.averageOrNull()
        val status = when {
            average == null -> ClosedBetaMetricStatus.NeedsMoreData
            average <= thresholds.maxMeanTaskReviewDuration -> ClosedBetaMetricStatus.Pass
            else -> ClosedBetaMetricStatus.Fail
        }
        return ClosedBetaMetricReport(
            status = status,
            sampleCount = snapshot.taskReviewDurations.size,
            averageDuration = average,
            thresholdDuration = thresholds.maxMeanTaskReviewDuration,
            violatingCount = snapshot.taskReviewDurations.count { it > thresholds.maxMeanTaskReviewDuration },
        )
    }

    private fun evaluateBalanceIntegrity(
        snapshot: ClosedBetaMetricSnapshot,
        thresholds: ClosedBetaReadinessThresholds,
    ): ClosedBetaMetricReport {
        val status = when {
            snapshot.observationPeriod < thresholds.minBalanceObservationPeriod -> ClosedBetaMetricStatus.NeedsMoreData
            snapshot.balanceDiscrepancyCount == 0 -> ClosedBetaMetricStatus.Pass
            else -> ClosedBetaMetricStatus.Fail
        }
        return ClosedBetaMetricReport(
            status = status,
            sampleCount = snapshot.balanceDiscrepancyCount,
            thresholdDuration = thresholds.minBalanceObservationPeriod,
            violatingCount = snapshot.balanceDiscrepancyCount,
        )
    }

    private fun evaluateCashOutSla(
        snapshot: ClosedBetaMetricSnapshot,
        thresholds: ClosedBetaReadinessThresholds,
    ): ClosedBetaMetricReport {
        val average = snapshot.cashOutDurations.averageOrNull()
        val overdueOpenCount = snapshot.openCashOutAges.count { it > thresholds.maxOpenCashOutAge }
        val confirmedOverThreshold = snapshot.cashOutDurations.count { it > thresholds.maxMeanCashOutDuration }
        val status = when {
            overdueOpenCount > 0 -> ClosedBetaMetricStatus.Fail
            average == null -> ClosedBetaMetricStatus.NeedsMoreData
            average <= thresholds.maxMeanCashOutDuration -> ClosedBetaMetricStatus.Pass
            else -> ClosedBetaMetricStatus.Fail
        }
        return ClosedBetaMetricReport(
            status = status,
            sampleCount = snapshot.cashOutDurations.size,
            averageDuration = average,
            thresholdDuration = thresholds.maxMeanCashOutDuration,
            violatingCount = overdueOpenCount + confirmedOverThreshold,
        )
    }
}

private fun List<Duration>.averageOrNull(): Duration? {
    if (isEmpty()) return null
    return (sumOf { it.inWholeMilliseconds } / size).milliseconds
}
