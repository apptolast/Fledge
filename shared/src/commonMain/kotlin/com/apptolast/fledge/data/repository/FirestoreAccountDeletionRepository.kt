package com.apptolast.fledge.data.repository

import com.apptolast.customlogin.domain.AuthProvider
import com.apptolast.fledge.data.remote.firebase.FirestoreProvider
import com.apptolast.fledge.domain.model.AccountDeletionRequest
import com.apptolast.fledge.domain.model.AccountDeletionState
import com.apptolast.fledge.domain.model.AccountDeletionStatus
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.repository.AccountDeletionRepository
import com.apptolast.fledge.domain.repository.RepositorySyncStatus
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FirestoreAccountDeletionRepository(
    private val firestoreProvider: FirestoreProvider,
    private val authProvider: AuthProvider,
) : AccountDeletionRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var syncJob: Job? = null

    private val mutableSyncStatus = MutableStateFlow<RepositorySyncStatus>(RepositorySyncStatus.Loading)
    private val mutableDeletionState = MutableStateFlow(AccountDeletionState())

    override val syncStatus: StateFlow<RepositorySyncStatus> = mutableSyncStatus
    override val deletionState: StateFlow<AccountDeletionState> = mutableDeletionState

    init {
        scope.launch {
            authProvider.authenticatedFamilyIds().collectLatest { familyId ->
                syncJob?.cancelAndJoin()
                if (familyId == null) {
                    mutableDeletionState.value = AccountDeletionState()
                    mutableSyncStatus.value = RepositorySyncStatus.Synced
                } else {
                    mutableSyncStatus.value = RepositorySyncStatus.Loading
                    syncJob = launch { bindDeletionState(familyId) }
                }
            }
        }
    }

    override suspend fun requestAccountDeletion(requestedAt: Instant): AccountDeletionRequest {
        val familyId = authProvider.currentFamilyId()
        val request = AccountDeletionRequest(familyId = familyId, requestedAt = requestedAt)
        familyDoc(familyId).set(
            mapOf(
                "familyId" to familyId.value,
                "accountDeletionStatus" to AccountDeletionStatus.Requested.name,
                "accountDeletionRequestedAt" to requestedAt.toFirestoreTimestamp(),
                "accountDeletionUpdatedAt" to requestedAt.toFirestoreTimestamp(),
                "accountDeletionFailureReason" to null,
            ),
            merge = true,
        )
        mutableDeletionState.value = AccountDeletionState(
            status = AccountDeletionStatus.Requested,
            requestedAt = requestedAt,
            updatedAt = requestedAt,
            failureReason = null,
        )
        return request
    }

    private suspend fun bindDeletionState(familyId: FamilyId) {
        familyDoc(familyId).snapshots(includeMetadataChanges = true).catch { error ->
            mutableSyncStatus.value = error.toRepositorySyncError()
        }.collect { snapshot ->
            mutableSyncStatus.value = snapshot.metadata.toRepositorySyncStatus()
            mutableDeletionState.value = if (snapshot.exists) {
                AccountDeletionState(
                    status = snapshot.optionalString("accountDeletionStatus")?.toAccountDeletionStatus()
                        ?: AccountDeletionStatus.NotRequested,
                    requestedAt = snapshot.optionalTimestamp("accountDeletionRequestedAt"),
                    updatedAt = snapshot.optionalTimestamp("accountDeletionUpdatedAt"),
                    failureReason = snapshot.optionalString("accountDeletionFailureReason"),
                )
            } else {
                AccountDeletionState()
            }
        }
    }

    private fun familyDoc(familyId: FamilyId) = firestoreProvider.firestoreOrThrow()
        .collection(FAMILIES_COLLECTION)
        .document(familyId.value)
}

private fun String.toAccountDeletionStatus(): AccountDeletionStatus =
    AccountDeletionStatus.entries.firstOrNull { it.name == this } ?: AccountDeletionStatus.NotRequested
