package com.apptolast.fledge.data.repository

import com.apptolast.customlogin.domain.AuthProvider
import com.apptolast.fledge.data.remote.firebase.FirestoreProvider
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.TaskInstance
import com.apptolast.fledge.domain.repository.RepositorySyncStatus
import com.apptolast.fledge.domain.repository.TaskInstanceRepository
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

class FirestoreTaskInstanceRepository(
    private val firestoreProvider: FirestoreProvider,
    private val authProvider: AuthProvider,
) : TaskInstanceRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var syncJob: Job? = null
    private val mutableSyncStatus = MutableStateFlow<RepositorySyncStatus>(RepositorySyncStatus.Loading)
    private val mutableInstances = MutableStateFlow<List<TaskInstance>>(emptyList())

    override val syncStatus: StateFlow<RepositorySyncStatus> = mutableSyncStatus
    override val instances: StateFlow<List<TaskInstance>> = mutableInstances

    init {
        scope.launch {
            authProvider.authenticatedFamilyIds().collectLatest { familyId ->
                syncJob?.cancelAndJoin()
                if (familyId == null) {
                    mutableInstances.value = emptyList()
                    mutableSyncStatus.value = RepositorySyncStatus.Synced
                } else {
                    mutableSyncStatus.value = RepositorySyncStatus.Loading
                    syncJob = launch { bindTaskInstances(familyId) }
                }
            }
        }
    }

    private suspend fun bindTaskInstances(familyId: FamilyId) = coroutineScope {
        val jobs = listOf(
            launch {
                taskInstanceCollection(familyId).snapshots(includeMetadataChanges = true)
                    .catch { error ->
                        mutableSyncStatus.value = error.toRepositorySyncError()
                    }
                    .collect { snapshot ->
                        mutableSyncStatus.value = snapshot.metadata.toRepositorySyncStatus()
                        mutableInstances.value = snapshot.documents
                            .filter { it.exists }
                            .mapNotNull { runCatching { it.toTaskInstance() }.getOrNull() }
                            .sortedForInstances()
                    }
            },
        )
        jobs.joinAll()
    }

    override fun instancesForFamily(familyId: FamilyId): List<TaskInstance> =
        instances.value.filter { it.familyId == familyId }.sortedForInstances()

    override fun instancesForChild(childProfileId: ChildProfileId): List<TaskInstance> = instances.value
        .filter { it.childProfileId == childProfileId }
        .sortedForInstances()

    private fun taskInstanceCollection(familyId: FamilyId) = firestoreProvider
        .firestoreOrThrow()
        .collection(FAMILIES_COLLECTION)
        .document(familyId.value)
        .collection(TASK_INSTANCES_COLLECTION)

    private companion object {
        const val TASK_INSTANCES_COLLECTION = "taskInstances"
    }
}

internal fun List<TaskInstance>.sortedForInstances(): List<TaskInstance> =
    sortedWith(compareBy<TaskInstance> { it.dueAt }.thenBy { it.id.value })
