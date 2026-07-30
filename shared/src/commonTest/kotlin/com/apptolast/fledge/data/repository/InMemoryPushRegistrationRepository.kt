package com.apptolast.fledge.data.repository

import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.PushRegistration
import com.apptolast.fledge.domain.model.PushRegistrationDraft
import com.apptolast.fledge.domain.model.PushRegistrationId
import com.apptolast.fledge.domain.model.stableRegistrationId
import com.apptolast.fledge.domain.model.toPushRegistration
import com.apptolast.fledge.domain.repository.PushRegistrationRepository
import com.apptolast.fledge.domain.repository.RepositorySyncStatus
import com.apptolast.fledge.domain.repository.sortedForPushRegistrations
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class InMemoryPushRegistrationRepository(initialRegistrations: List<PushRegistration> = emptyList()) :
    PushRegistrationRepository {
    private val mutableSyncStatus = MutableStateFlow<RepositorySyncStatus>(RepositorySyncStatus.Synced)
    private val mutableRegistrations = MutableStateFlow(initialRegistrations.sortedForPushRegistrations())

    override val syncStatus: StateFlow<RepositorySyncStatus> = mutableSyncStatus
    override val registrations: StateFlow<List<PushRegistration>> = mutableRegistrations

    override suspend fun upsertRegistration(draft: PushRegistrationDraft, updatedAt: Instant): PushRegistration {
        val registration = draft.toPushRegistration(
            id = draft.stableRegistrationId(),
            updatedAt = updatedAt,
        )
        upsert(registration)
        return registration
    }

    override suspend fun deactivateRegistration(
        familyId: FamilyId,
        registrationId: PushRegistrationId,
        updatedAt: Instant,
    ): PushRegistration? {
        val registration = registrations.value.firstOrNull {
            it.familyId == familyId && it.id == registrationId
        }?.deactivate(updatedAt) ?: return null
        upsert(registration)
        return registration
    }

    override fun registrationsForChild(childProfileId: ChildProfileId): List<PushRegistration> =
        registrations.value.filter { it.childProfileId == childProfileId }.sortedForPushRegistrations()

    private fun upsert(registration: PushRegistration) {
        mutableRegistrations.value = (registrations.value.filterNot { it.id == registration.id } + registration)
            .sortedForPushRegistrations()
    }
}
