package com.apptolast.fledge.domain.repository

import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.PushRegistration
import com.apptolast.fledge.domain.model.PushRegistrationDraft
import com.apptolast.fledge.domain.model.PushRegistrationId
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.StateFlow

interface PushRegistrationRepository {
    val syncStatus: StateFlow<RepositorySyncStatus>
    val registrations: StateFlow<List<PushRegistration>>

    suspend fun upsertRegistration(
        draft: PushRegistrationDraft,
        updatedAt: Instant = Clock.System.now(),
    ): PushRegistration

    suspend fun deactivateRegistration(
        familyId: FamilyId,
        registrationId: PushRegistrationId,
        updatedAt: Instant = Clock.System.now(),
    ): PushRegistration?

    fun registrationsForChild(childProfileId: ChildProfileId): List<PushRegistration> =
        registrations.value.filter { it.childProfileId == childProfileId }.sortedForPushRegistrations()
}

fun List<PushRegistration>.sortedForPushRegistrations(): List<PushRegistration> = sortedWith(
    compareBy<PushRegistration> { it.familyId.value }
        .thenBy { it.role.name }
        .thenBy { it.childProfileId?.value.orEmpty() }
        .thenBy { it.platform.name }
        .thenBy { it.installationId.value }
        .thenBy { it.id.value },
)
