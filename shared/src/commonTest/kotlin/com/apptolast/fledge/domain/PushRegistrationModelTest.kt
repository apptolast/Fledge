package com.apptolast.fledge.domain

import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.PushInstallationId
import com.apptolast.fledge.domain.model.PushPlatform
import com.apptolast.fledge.domain.model.PushRegistrationDraft
import com.apptolast.fledge.domain.model.PushRegistrationId
import com.apptolast.fledge.domain.model.PushRegistrationStatus
import com.apptolast.fledge.domain.model.PushToken
import com.apptolast.fledge.domain.model.SharedDeviceRole
import com.apptolast.fledge.domain.model.toPushRegistration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Instant

class PushRegistrationModelTest {

    @Test
    fun `FLE-42 AC-01 parent registration has no child profile`() {
        // Given
        val updatedAt = Instant.fromEpochSeconds(1_785_000_000)
        val draft = PushRegistrationDraft(
            familyId = FamilyId("family-1"),
            installationId = PushInstallationId("install-parent"),
            token = PushToken("token-parent"),
            platform = PushPlatform.Android,
            role = SharedDeviceRole.Parent,
        )

        // When
        val registration = draft.toPushRegistration(
            id = PushRegistrationId("parent-android-install-parent"),
            updatedAt = updatedAt,
        )

        // Then
        assertEquals(SharedDeviceRole.Parent, registration.role)
        assertEquals(PushPlatform.Android, registration.platform)
        assertEquals(PushRegistrationStatus.Active, registration.status)
        assertNull(registration.childProfileId)
        assertEquals(updatedAt, registration.updatedAt)
    }

    @Test
    fun `FLE-42 AC-01 child registration requires child profile`() {
        // When / Then
        assertFailsWith<IllegalArgumentException> {
            PushRegistrationDraft(
                familyId = FamilyId("family-1"),
                installationId = PushInstallationId("install-child"),
                token = PushToken("token-child"),
                platform = PushPlatform.Ios,
                role = SharedDeviceRole.Child,
            )
        }
    }

    @Test
    fun `FLE-42 AC-01 parent registration rejects child profile`() {
        // When / Then
        assertFailsWith<IllegalArgumentException> {
            PushRegistrationDraft(
                familyId = FamilyId("family-1"),
                installationId = PushInstallationId("install-parent"),
                token = PushToken("token-parent"),
                platform = PushPlatform.Android,
                role = SharedDeviceRole.Parent,
                childProfileId = ChildProfileId("child-1"),
            )
        }
    }
}
