package com.apptolast.fledge.domain

import com.apptolast.fledge.domain.model.ChildAchievementBadgeId
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.TaskAssignmentId
import com.apptolast.fledge.domain.model.TaskInstance
import com.apptolast.fledge.domain.model.TaskInstanceId
import com.apptolast.fledge.domain.model.TaskInstanceStatus
import com.apptolast.fledge.domain.model.TaskTemplateId
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.domain.service.ChildAchievementCalculator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class ChildAchievementCalculatorTest {
    private val calculator = ChildAchievementCalculator()
    private val familyId = FamilyId("family-1")
    private val childId = ChildProfileId("child-1")
    private val siblingId = ChildProfileId("child-2")
    private val madrid = TimeZoneId("Europe/Madrid")
    private val now = Instant.parse("2026-07-31T10:00:00Z")

    @Test
    fun `FLE-57 AC-01 derives current and best streak from approved task review days`() {
        // Given
        val tasks = listOf(
            approvedTask("approved-today", childId, "2026-07-31T07:00:00Z"),
            approvedTask("approved-yesterday-1", childId, "2026-07-30T18:00:00Z"),
            approvedTask("approved-yesterday-2", childId, "2026-07-30T20:00:00Z"),
            approvedTask("approved-two-days-ago", childId, "2026-07-29T12:00:00Z"),
            approvedTask("approved-best-1", childId, "2026-07-20T12:00:00Z"),
            approvedTask("approved-best-2", childId, "2026-07-21T12:00:00Z"),
            approvedTask("approved-best-3", childId, "2026-07-22T12:00:00Z"),
            approvedTask("approved-best-4", childId, "2026-07-23T12:00:00Z"),
            approvedTask("approved-best-5", childId, "2026-07-24T12:00:00Z"),
            approvedTask("sibling-approved", siblingId, "2026-07-31T08:00:00Z"),
            submittedTask("submitted-not-approved", childId, "2026-07-31T08:30:00Z"),
        )

        // When
        val summary = calculator.calculate(
            childProfileId = childId,
            taskInstances = tasks,
            timeZoneId = madrid,
            now = now,
        )

        // Then
        assertEquals(3, summary.currentStreakDays)
        assertEquals(5, summary.bestStreakDays)
        assertEquals(8, summary.approvedTaskCount)
    }

    @Test
    fun `FLE-57 AC-02 exposes badge progress from existing approved tasks`() {
        // Given
        val tasks = listOf(
            approvedTask("approved-1", childId, "2026-07-29T12:00:00Z"),
            approvedTask("approved-2", childId, "2026-07-30T12:00:00Z"),
            approvedTask("approved-3", childId, "2026-07-31T12:00:00Z"),
        )

        // When
        val summary = calculator.calculate(
            childProfileId = childId,
            taskInstances = tasks,
            timeZoneId = madrid,
            now = Instant.parse("2026-07-31T18:00:00Z"),
        )
        val badges = summary.badges.associateBy { it.id }

        // Then
        assertTrue(badges.getValue(ChildAchievementBadgeId.FirstApprovedTask).unlocked)
        assertEquals(1, badges.getValue(ChildAchievementBadgeId.FirstApprovedTask).progress)
        assertEquals(1, badges.getValue(ChildAchievementBadgeId.FirstApprovedTask).target)
        assertTrue(badges.getValue(ChildAchievementBadgeId.ThreeApprovedTasks).unlocked)
        assertEquals(3, badges.getValue(ChildAchievementBadgeId.ThreeApprovedTasks).progress)
        assertEquals(3, badges.getValue(ChildAchievementBadgeId.ThreeApprovedTasks).target)
        assertTrue(badges.getValue(ChildAchievementBadgeId.ThreeDayStreak).unlocked)
        assertEquals(3, badges.getValue(ChildAchievementBadgeId.ThreeDayStreak).progress)
        assertEquals(3, badges.getValue(ChildAchievementBadgeId.ThreeDayStreak).target)
    }

    @Test
    fun `FLE-57 AC-04 empty activity does not fake earned achievements`() {
        // Given
        val tasks = listOf(
            submittedTask("submitted-only", childId, "2026-07-31T08:30:00Z"),
            approvedTask("sibling-approved", siblingId, "2026-07-31T08:00:00Z"),
        )

        // When
        val summary = calculator.calculate(
            childProfileId = childId,
            taskInstances = tasks,
            timeZoneId = madrid,
            now = now,
        )

        // Then
        assertFalse(summary.hasApprovedActivity)
        assertEquals(0, summary.currentStreakDays)
        assertEquals(0, summary.bestStreakDays)
        assertEquals(0, summary.approvedTaskCount)
        summary.badges.forEach { badge ->
            assertFalse(badge.unlocked)
            assertEquals(0, badge.progress)
        }
    }

    private fun approvedTask(id: String, childProfileId: ChildProfileId, reviewedAt: String): TaskInstance = task(
        id = id,
        childProfileId = childProfileId,
        status = TaskInstanceStatus.Approved,
        submittedAt = Instant.parse(reviewedAt).minus(kotlin.time.Duration.parse("PT1H")),
        reviewedAt = Instant.parse(reviewedAt),
    )

    private fun submittedTask(id: String, childProfileId: ChildProfileId, submittedAt: String): TaskInstance = task(
        id = id,
        childProfileId = childProfileId,
        status = TaskInstanceStatus.Submitted,
        submittedAt = Instant.parse(submittedAt),
        reviewedAt = null,
    )

    private fun task(
        id: String,
        childProfileId: ChildProfileId,
        status: TaskInstanceStatus,
        submittedAt: Instant?,
        reviewedAt: Instant?,
    ): TaskInstance = TaskInstance(
        id = TaskInstanceId(id),
        familyId = familyId,
        taskAssignmentId = TaskAssignmentId("assignment-$id"),
        taskTemplateId = TaskTemplateId("template-$id"),
        childProfileId = childProfileId,
        title = "Poner la mesa",
        rewardCents = MoneyCents(50),
        requiresPhoto = false,
        status = status,
        dueAt = Instant.parse("2026-07-31T20:00:00Z"),
        periodKey = "20260731",
        createdAt = Instant.parse("2026-07-31T08:00:00Z"),
        updatedAt = reviewedAt ?: submittedAt ?: Instant.parse("2026-07-31T08:00:00Z"),
        submittedAt = submittedAt,
        reviewedAt = reviewedAt,
        approvedRewardCents = null,
        approvalTransactionId = null,
    )
}
