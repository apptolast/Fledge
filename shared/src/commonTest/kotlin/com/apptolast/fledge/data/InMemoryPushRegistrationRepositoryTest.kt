package com.apptolast.fledge.data

import app.cash.turbine.test
import com.apptolast.fledge.data.repository.InMemoryPushRegistrationRepository
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.PushInstallationId
import com.apptolast.fledge.domain.model.PushPlatform
import com.apptolast.fledge.domain.model.PushRegistrationDraft
import com.apptolast.fledge.domain.model.PushRegistrationStatus
import com.apptolast.fledge.domain.model.PushToken
import com.apptolast.fledge.domain.model.SharedDeviceRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class InMemoryPushRegistrationRepositoryTest {

    @Test
    fun `FLE-42 AC-02 upsert replaces rotated token for the same installation`() = runTest {
        // Given
        val repository = InMemoryPushRegistrationRepository()
        val firstSeenAt = Instant.fromEpochSeconds(1_785_000_000)
        val rotatedAt = Instant.fromEpochSeconds(1_785_003_600)

        // When / Then
        repository.registrations.test {
            assertEquals(emptyList(), awaitItem())

            val first = repository.upsertRegistration(
                parentDraft(token = "token-a"),
                updatedAt = firstSeenAt,
            )
            assertEquals(listOf(first), awaitItem())

            val rotated = repository.upsertRegistration(
                parentDraft(token = "token-b"),
                updatedAt = rotatedAt,
            )
            assertEquals(listOf(rotated), awaitItem())

            assertEquals(first.id, rotated.id)
            assertEquals(PushToken("token-b"), rotated.token)
            assertEquals(rotatedAt, rotated.updatedAt)
            assertEquals(1, repository.registrations.value.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `FLE-42 AC-02 parent and child registrations share installation without colliding`() = runTest {
        // Given
        val repository = InMemoryPushRegistrationRepository()
        val updatedAt = Instant.fromEpochSeconds(1_785_000_000)

        // When
        val parent = repository.upsertRegistration(parentDraft(), updatedAt)
        val child = repository.upsertRegistration(
            childDraft(childProfileId = ChildProfileId("child-1")),
            updatedAt,
        )

        // Then
        assertEquals(2, repository.registrations.value.size)
        assertEquals(SharedDeviceRole.Parent, parent.role)
        assertEquals(SharedDeviceRole.Child, child.role)
        assertEquals(listOf(child), repository.registrationsForChild(ChildProfileId("child-1")))
    }

    @Test
    fun `FLE-42 AC-02 deactivate keeps registration as inactive`() = runTest {
        // Given
        val repository = InMemoryPushRegistrationRepository()
        val saved = repository.upsertRegistration(
            childDraft(childProfileId = ChildProfileId("child-1")),
            updatedAt = Instant.fromEpochSeconds(1_785_000_000),
        )
        val deactivatedAt = Instant.fromEpochSeconds(1_785_007_200)

        // When
        val deactivated = repository.deactivateRegistration(
            familyId = FamilyId("family-1"),
            registrationId = saved.id,
            updatedAt = deactivatedAt,
        )

        // Then
        assertEquals(PushRegistrationStatus.Inactive, deactivated?.status)
        assertEquals(deactivatedAt, deactivated?.updatedAt)
        assertEquals(listOf(deactivated), repository.registrations.value)
    }

    private fun parentDraft(token: String = "token-parent"): PushRegistrationDraft = PushRegistrationDraft(
        familyId = FamilyId("family-1"),
        installationId = PushInstallationId("install-shared"),
        token = PushToken(token),
        platform = PushPlatform.Android,
        role = SharedDeviceRole.Parent,
    )

    private fun childDraft(childProfileId: ChildProfileId, token: String = "token-child"): PushRegistrationDraft =
        PushRegistrationDraft(
            familyId = FamilyId("family-1"),
            installationId = PushInstallationId("install-shared"),
            token = PushToken(token),
            platform = PushPlatform.Android,
            role = SharedDeviceRole.Child,
            childProfileId = childProfileId,
        )
}
