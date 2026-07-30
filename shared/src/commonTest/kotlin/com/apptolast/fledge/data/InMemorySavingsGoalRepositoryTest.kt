package com.apptolast.fledge.data

import com.apptolast.fledge.data.repository.InMemorySavingsGoalRepository
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.MoneyPotType
import com.apptolast.fledge.domain.model.SavingsGoal
import com.apptolast.fledge.domain.model.SavingsGoalDraft
import com.apptolast.fledge.domain.model.SavingsGoalId
import com.apptolast.fledge.domain.model.SavingsGoalStatus
import com.apptolast.fledge.domain.model.VirtualAccountType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class InMemorySavingsGoalRepositoryTest {

    @Test
    fun `FLE-36 repository saves active savings goal`() = runTest {
        // Given
        val repository = InMemorySavingsGoalRepository()
        val draft = SavingsGoalDraft(
            familyId = FamilyId("family-1"),
            childProfileId = ChildProfileId("child-1"),
            title = "Bici nueva",
            targetCents = MoneyCents(4_000),
            iconKey = "bike",
        )
        val createdAt = Instant.fromEpochSeconds(1_700_100_000)

        // When
        val saved = repository.saveGoal(draft, createdAt)

        // Then
        assertEquals(SavingsGoalStatus.Active, saved.status)
        assertEquals(MoneyPotType.Save, saved.potType)
        assertEquals(VirtualAccountType.Goal, saved.accountType)
        assertEquals(saved, repository.goals.value.single())
        assertEquals(saved, repository.activeGoalForChild(ChildProfileId("child-1")))
    }

    @Test
    fun `FLE-50 repository saves Give goal in GIVE account`() = runTest {
        // Given
        val repository = InMemorySavingsGoalRepository()
        val draft = SavingsGoalDraft(
            familyId = FamilyId("family-1"),
            childProfileId = ChildProfileId("child-1"),
            title = "Donacion",
            targetCents = MoneyCents(2_000),
            potType = MoneyPotType.Give,
            iconKey = "target",
        )

        // When
        val saved = repository.saveGoal(draft, Instant.fromEpochSeconds(1_700_100_000))

        // Then
        assertEquals(MoneyPotType.Give, saved.potType)
        assertEquals(VirtualAccountType.Give, saved.accountType)
    }

    @Test
    fun `FLE-36 repository returns active goal for requested child only`() = runTest {
        // Given
        val child = ChildProfileId("child-1")
        val sibling = ChildProfileId("child-2")
        val repository = InMemorySavingsGoalRepository(
            listOf(
                savingsGoal(
                    id = "archived",
                    childProfileId = child,
                    status = SavingsGoalStatus.Archived,
                    createdAt = Instant.fromEpochSeconds(1),
                ),
                savingsGoal(
                    id = "active",
                    childProfileId = child,
                    status = SavingsGoalStatus.Active,
                    createdAt = Instant.fromEpochSeconds(2),
                ),
                savingsGoal(
                    id = "sibling-active",
                    childProfileId = sibling,
                    status = SavingsGoalStatus.Active,
                    createdAt = Instant.fromEpochSeconds(1),
                ),
            ),
        )

        // When
        val active = repository.activeGoalForChild(child)

        // Then
        assertEquals(SavingsGoalId("active"), active?.id)
        assertEquals(
            listOf(SavingsGoalId("active"), SavingsGoalId("archived")),
            repository.goalsForChild(child).map {
                it.id
            },
        )
    }

    private fun savingsGoal(
        id: String,
        childProfileId: ChildProfileId,
        status: SavingsGoalStatus,
        createdAt: Instant,
    ): SavingsGoal = SavingsGoal(
        id = SavingsGoalId(id),
        familyId = FamilyId("family-1"),
        childProfileId = childProfileId,
        title = "Bici nueva",
        targetCents = MoneyCents(4_000),
        accountType = VirtualAccountType.Goal,
        iconKey = "bike",
        status = status,
        createdAt = createdAt,
        updatedAt = createdAt,
    )
}
