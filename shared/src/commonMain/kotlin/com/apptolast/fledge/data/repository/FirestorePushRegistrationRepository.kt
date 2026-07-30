package com.apptolast.fledge.data.repository

import com.apptolast.customlogin.domain.AuthProvider
import com.apptolast.fledge.data.remote.firebase.FirestoreProvider
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

class FirestorePushRegistrationRepository(
    private val firestoreProvider: FirestoreProvider,
    private val authProvider: AuthProvider,
) : PushRegistrationRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var syncJob: Job? = null
    private val mutableSyncStatus = MutableStateFlow<RepositorySyncStatus>(RepositorySyncStatus.Loading)
    private val mutableRegistrations = MutableStateFlow<List<PushRegistration>>(emptyList())

    override val syncStatus: StateFlow<RepositorySyncStatus> = mutableSyncStatus
    override val registrations: StateFlow<List<PushRegistration>> = mutableRegistrations

    init {
        scope.launch {
            authProvider.authenticatedFamilyIds().collectLatest { familyId ->
                syncJob?.cancelAndJoin()
                if (familyId == null) {
                    mutableRegistrations.value = emptyList()
                    mutableSyncStatus.value = RepositorySyncStatus.Synced
                } else {
                    mutableSyncStatus.value = RepositorySyncStatus.Loading
                    syncJob = launch { bindPushRegistrations(familyId) }
                }
            }
        }
    }

    private suspend fun bindPushRegistrations(familyId: FamilyId) = coroutineScope {
        val jobs = listOf(
            launch {
                pushRegistrationCollection(familyId).snapshots(includeMetadataChanges = true)
                    .catch { error ->
                        mutableSyncStatus.value = error.toRepositorySyncError()
                    }
                    .collect { snapshot ->
                        mutableSyncStatus.value = snapshot.metadata.toRepositorySyncStatus()
                        mutableRegistrations.value = snapshot.documents
                            .filter { it.exists }
                            .mapNotNull { runCatching { it.toPushRegistration() }.getOrNull() }
                            .sortedForPushRegistrations()
                    }
            },
        )
        jobs.joinAll()
    }

    override suspend fun upsertRegistration(draft: PushRegistrationDraft, updatedAt: Instant): PushRegistration {
        require(authProvider.currentFamilyId() == draft.familyId) {
            "A signed-in parent can only register push tokens for the active family."
        }
        val id = draft.stableRegistrationId()
        val registration = draft.toPushRegistration(id = id, updatedAt = updatedAt)
        pushRegistrationCollection(draft.familyId).document(id.value).set(registration.toFirestoreMap(), merge = true)
        upsertLocal(registration)
        return registration
    }

    override suspend fun deactivateRegistration(
        familyId: FamilyId,
        registrationId: PushRegistrationId,
        updatedAt: Instant,
    ): PushRegistration? {
        require(authProvider.currentFamilyId() == familyId) {
            "A signed-in parent can only deactivate push tokens for the active family."
        }
        val ref = pushRegistrationCollection(familyId).document(registrationId.value)
        val existing = registrations.value.firstOrNull { it.id == registrationId }
            ?: ref.get().takeIf { it.exists }?.toPushRegistration()
            ?: return null
        val updated = existing.deactivate(updatedAt)
        ref.set(updated.toFirestoreMap(), merge = true)
        upsertLocal(updated)
        return updated
    }

    private fun pushRegistrationCollection(familyId: FamilyId) = firestoreProvider
        .firestoreOrThrow()
        .collection(FAMILIES_COLLECTION)
        .document(familyId.value)
        .collection(PUSH_REGISTRATIONS_COLLECTION)

    private fun upsertLocal(registration: PushRegistration) {
        mutableRegistrations.value = (registrations.value.filterNot { it.id == registration.id } + registration)
            .sortedForPushRegistrations()
    }

    private companion object {
        const val PUSH_REGISTRATIONS_COLLECTION = "pushRegistrations"
    }
}
