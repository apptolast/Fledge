package com.apptolast.fledge.data.repository

import com.apptolast.customlogin.domain.AuthProvider
import com.apptolast.fledge.data.remote.firebase.FirestoreProvider
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.TaskInstance
import com.apptolast.fledge.domain.model.TaskInstanceId
import com.apptolast.fledge.domain.model.TransactionId
import com.apptolast.fledge.domain.model.approvedByParent
import com.apptolast.fledge.domain.model.rejectedByParent
import com.apptolast.fledge.domain.model.retriedForSameDay
import com.apptolast.fledge.domain.model.submittedForReview
import com.apptolast.fledge.domain.repository.RepositorySyncStatus
import com.apptolast.fledge.domain.repository.TaskInstanceRepository
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

    override fun instanceById(instanceId: TaskInstanceId): TaskInstance? =
        instances.value.firstOrNull { it.id == instanceId }

    override suspend fun submitForReview(
        instanceId: TaskInstanceId,
        childProfileId: ChildProfileId,
        photoEvidenceUri: String?,
        submittedAt: Instant,
    ): TaskInstance {
        val existing = existingInstance(instanceId)
        val submitted = existing.submittedForReview(
            childProfileId = childProfileId,
            photoEvidenceUri = photoEvidenceUri,
            submittedAt = submittedAt,
        )
        taskInstanceCollection(submitted.familyId).document(instanceId.value)
            .set(submitted.toFirestoreMap(), merge = true)
        upsertLocal(submitted)
        return submitted
    }

    override suspend fun approve(
        instanceId: TaskInstanceId,
        approvedRewardCents: MoneyCents,
        transactionId: TransactionId,
        reviewedAt: Instant,
    ): TaskInstance {
        val existing = existingInstance(instanceId)
        val approved = existing.approvedByParent(
            approvedRewardCents = approvedRewardCents,
            transactionId = transactionId,
            reviewedAt = reviewedAt,
        )
        taskInstanceCollection(approved.familyId).document(instanceId.value)
            .set(approved.toFirestoreMap(), merge = true)
        upsertLocal(approved)
        return approved
    }

    override suspend fun retryRejected(
        instanceId: TaskInstanceId,
        childProfileId: ChildProfileId,
        retriedAt: Instant,
    ): TaskInstance {
        val existing = existingInstance(instanceId)
        val retried = existing.retriedForSameDay(
            childProfileId = childProfileId,
            retriedAt = retriedAt,
        )
        taskInstanceCollection(retried.familyId).document(instanceId.value)
            .set(retried.toFirestoreMap(), merge = true)
        upsertLocal(retried)
        return retried
    }

    override suspend fun reject(instanceId: TaskInstanceId, reason: String, reviewedAt: Instant): TaskInstance {
        val existing = existingInstance(instanceId)
        val rejected = existing.rejectedByParent(reason = reason, reviewedAt = reviewedAt)
        taskInstanceCollection(rejected.familyId).document(instanceId.value)
            .set(rejected.toFirestoreMap(), merge = true)
        upsertLocal(rejected)
        return rejected
    }

    private suspend fun existingInstance(instanceId: TaskInstanceId): TaskInstance {
        val existing = instanceById(instanceId) ?: instanceByIdFromFirestore(instanceId)
        return requireNotNull(existing) { "Task instance does not exist." }
    }

    private suspend fun instanceByIdFromFirestore(instanceId: TaskInstanceId): TaskInstance? {
        val fallbackFamilyId = authProvider.currentFamilyId()
        return taskInstanceCollection(fallbackFamilyId).document(instanceId.value)
            .get()
            .takeIf { it.exists }
            ?.toTaskInstance()
    }

    private fun upsertLocal(instance: TaskInstance) {
        mutableInstances.value = (instances.value.filterNot { it.id == instance.id } + instance)
            .sortedForInstances()
    }

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
