package com.apptolast.fledge.domain

import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.SavingsGoalDraft
import com.apptolast.fledge.domain.model.SavingsGoalId
import com.apptolast.fledge.domain.model.SavingsGoalStatus
import com.apptolast.fledge.domain.model.VirtualAccountType
import com.apptolast.fledge.domain.model.toSavingsGoal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class SavingsGoalModelTest {

    @Test
    fun `FLE-36 SavingsGoal valid draft becomes active goal account`() {
        // Given
        val createdAt = Instant.fromEpochSeconds(1_700_100_000)
        val draft = SavingsGoalDraft(
            familyId = FamilyId("family-1"),
            childProfileId = ChildProfileId("child-1"),
            title = "Bici nueva",
            targetCents = MoneyCents(4_000),
            iconKey = "bike",
        )

        // When
        val goal = draft.toSavingsGoal(
            id = SavingsGoalId("goal-1"),
            createdAt = createdAt,
        )

        // Then
        assertEquals(SavingsGoalStatus.Active, goal.status)
        assertEquals(VirtualAccountType.Goal, goal.accountType)
        assertEquals("Bici nueva", goal.title)
        assertEquals(MoneyCents(4_000), goal.targetCents)
        assertEquals(createdAt, goal.createdAt)
        assertEquals(createdAt, goal.updatedAt)
    }

    @Test
    fun `FLE-36 SavingsGoal rejects blank title`() {
        // When / Then
        assertFailsWith<IllegalArgumentException> {
            SavingsGoalDraft(
                familyId = FamilyId("family-1"),
                childProfileId = ChildProfileId("child-1"),
                title = " ",
                targetCents = MoneyCents(4_000),
                iconKey = "bike",
            )
        }
    }

    @Test
    fun `FLE-36 SavingsGoal rejects non positive target`() {
        // When / Then
        assertFailsWith<IllegalArgumentException> {
            SavingsGoalDraft(
                familyId = FamilyId("family-1"),
                childProfileId = ChildProfileId("child-1"),
                title = "Bici nueva",
                targetCents = MoneyCents(-100),
                iconKey = "bike",
            )
        }
    }

    @Test
    fun `FLE-36 SavingsGoal requires icon or image`() {
        // When / Then
        assertFailsWith<IllegalArgumentException> {
            SavingsGoalDraft(
                familyId = FamilyId("family-1"),
                childProfileId = ChildProfileId("child-1"),
                title = "Bici nueva",
                targetCents = MoneyCents(4_000),
            )
        }
    }
}
