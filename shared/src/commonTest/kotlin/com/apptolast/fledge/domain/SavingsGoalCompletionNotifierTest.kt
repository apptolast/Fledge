package com.apptolast.fledge.domain

import com.apptolast.fledge.domain.model.BalanceCents
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.SavingsGoal
import com.apptolast.fledge.domain.model.SavingsGoalId
import com.apptolast.fledge.domain.service.SavingsGoalCompletionNotifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SavingsGoalCompletionNotifierTest {
    private val notifier = SavingsGoalCompletionNotifier()
    private val familyId = FamilyId("family-1")
    private val childProfileId = ChildProfileId("child-1")
    private val now = Instant.parse("2026-07-30T11:00:00Z")

    @Test
    fun `FLE-40 AC-01 detects completed active goal with child and amount context`() {
        // Given
        val child = childProfile("Lucas")
        val goal = goal(targetCents = 4_000)

        // When
        val notice = notifier.noticeForChild(
            child = child,
            goal = goal,
            goalBalance = BalanceCents(4_200),
            now = now,
        )

        // Then
        assertEquals(SavingsGoalId("goal-1"), notice?.goalId)
        assertEquals(childProfileId, notice?.childProfileId)
        assertEquals("Lucas", notice?.childName)
        assertEquals("Bici nueva", notice?.goalTitle)
        assertEquals(BalanceCents(4_200), notice?.currentCents)
        assertEquals(MoneyCents(4_000), notice?.targetCents)
        assertEquals(now, notice?.completedAt)
    }

    @Test
    fun `FLE-40 AC-01 ignores active goal below target`() {
        // Given
        val child = childProfile("Lucas")
        val goal = goal(targetCents = 4_000)

        // When
        val notice = notifier.noticeForChild(
            child = child,
            goal = goal,
            goalBalance = BalanceCents(3_999),
            now = now,
        )

        // Then
        assertEquals(null, notice)
    }

    private fun childProfile(name: String): ChildProfile = ChildProfile(
        id = childProfileId,
        displayName = name,
        birthYear = 2017,
        avatarKey = "rocket",
    )

    private fun goal(targetCents: Long): SavingsGoal = SavingsGoal(
        id = SavingsGoalId("goal-1"),
        familyId = familyId,
        childProfileId = childProfileId,
        title = "Bici nueva",
        targetCents = MoneyCents(targetCents),
        iconKey = "bike",
        createdAt = Instant.parse("2026-07-01T08:00:00Z"),
        updatedAt = Instant.parse("2026-07-01T08:00:00Z"),
    )
}
