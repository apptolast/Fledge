package com.apptolast.fledge.domain

import com.apptolast.fledge.domain.service.ClosedBetaMetricSnapshot
import com.apptolast.fledge.domain.service.ClosedBetaMetricStatus
import com.apptolast.fledge.domain.service.ClosedBetaReadinessEvaluator
import com.apptolast.fledge.domain.service.ClosedBetaReadinessStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class ClosedBetaReadinessEvaluatorTest {

    @Test
    fun `FLE-45 AC-01 accepts a closed cohort without identifiers`() {
        // Given
        val snapshot = readySnapshot(activeFamilyCount = 12)

        // When
        val report = ClosedBetaReadinessEvaluator.evaluate(snapshot)

        // Then
        assertEquals(ClosedBetaMetricStatus.Pass, report.cohort.status)
        assertEquals(12, report.cohort.sampleCount)
        assertTrue(report.isAggregateOnly)
    }

    @Test
    fun `FLE-45 AC-02 passes approval SLA when mean review time is within 24 hours`() {
        // Given
        val snapshot = readySnapshot(
            taskReviewDurations = listOf(6.hours, 18.hours, 24.hours),
        )

        // When
        val report = ClosedBetaReadinessEvaluator.evaluate(snapshot)

        // Then
        assertEquals(ClosedBetaMetricStatus.Pass, report.approvalSla.status)
        assertEquals(16.hours, report.approvalSla.averageDuration)
    }

    @Test
    fun `FLE-45 AC-03 passes balance integrity only after four weeks with no discrepancies`() {
        // Given
        val passingSnapshot = readySnapshot(observationDays = 28, balanceDiscrepancyCount = 0)
        val tooShortSnapshot = readySnapshot(observationDays = 27, balanceDiscrepancyCount = 0)
        val discrepancySnapshot = readySnapshot(observationDays = 28, balanceDiscrepancyCount = 1)

        // When
        val passing = ClosedBetaReadinessEvaluator.evaluate(passingSnapshot)
        val tooShort = ClosedBetaReadinessEvaluator.evaluate(tooShortSnapshot)
        val discrepancy = ClosedBetaReadinessEvaluator.evaluate(discrepancySnapshot)

        // Then
        assertEquals(ClosedBetaMetricStatus.Pass, passing.balanceIntegrity.status)
        assertEquals(ClosedBetaMetricStatus.NeedsMoreData, tooShort.balanceIntegrity.status)
        assertEquals(ClosedBetaMetricStatus.Fail, discrepancy.balanceIntegrity.status)
    }

    @Test
    fun `FLE-45 AC-04 passes cash-out SLA when confirmed mean is within 72 hours and no open cash-out is overdue`() {
        // Given
        val passingSnapshot = readySnapshot(
            cashOutDurations = listOf(24.hours, 72.hours),
            openCashOutAges = listOf(12.hours),
        )
        val overdueOpenSnapshot = readySnapshot(
            cashOutDurations = listOf(24.hours),
            openCashOutAges = listOf(73.hours),
        )

        // When
        val passing = ClosedBetaReadinessEvaluator.evaluate(passingSnapshot)
        val overdueOpen = ClosedBetaReadinessEvaluator.evaluate(overdueOpenSnapshot)

        // Then
        assertEquals(ClosedBetaMetricStatus.Pass, passing.cashOutSla.status)
        assertEquals(48.hours, passing.cashOutSla.averageDuration)
        assertEquals(ClosedBetaMetricStatus.Fail, overdueOpen.cashOutSla.status)
        assertEquals(1, overdueOpen.cashOutSla.violatingCount)
    }

    @Test
    fun `FLE-45 AC-05 marks beta ready when all aggregate gates pass`() {
        // Given
        val snapshot = readySnapshot()

        // When
        val report = ClosedBetaReadinessEvaluator.evaluate(snapshot)

        // Then
        assertEquals(ClosedBetaReadinessStatus.Ready, report.status)
    }

    @Test
    fun `FLE-45 AC-06 marks beta as needs more data or at risk when aggregate gates are not ready`() {
        // Given
        val needsMoreDataSnapshot = readySnapshot(
            activeFamilyCount = 9,
            taskReviewDurations = emptyList(),
        )
        val atRiskSnapshot = readySnapshot(
            activeFamilyCount = 12,
            taskReviewDurations = listOf(30.hours),
        )

        // When
        val needsMoreData = ClosedBetaReadinessEvaluator.evaluate(needsMoreDataSnapshot)
        val atRisk = ClosedBetaReadinessEvaluator.evaluate(atRiskSnapshot)

        // Then
        assertEquals(ClosedBetaReadinessStatus.NeedsMoreData, needsMoreData.status)
        assertEquals(ClosedBetaReadinessStatus.AtRisk, atRisk.status)
    }

    private fun readySnapshot(
        activeFamilyCount: Int = 12,
        taskReviewDurations: List<kotlin.time.Duration> = listOf(4.hours, 20.hours),
        cashOutDurations: List<kotlin.time.Duration> = listOf(24.hours, 48.hours),
        openCashOutAges: List<kotlin.time.Duration> = emptyList(),
        balanceDiscrepancyCount: Int = 0,
        observationDays: Int = 28,
    ) = ClosedBetaMetricSnapshot(
        activeFamilyCount = activeFamilyCount,
        taskReviewDurations = taskReviewDurations,
        cashOutDurations = cashOutDurations,
        openCashOutAges = openCashOutAges,
        balanceDiscrepancyCount = balanceDiscrepancyCount,
        observationPeriod = observationDays.days,
    )
}
