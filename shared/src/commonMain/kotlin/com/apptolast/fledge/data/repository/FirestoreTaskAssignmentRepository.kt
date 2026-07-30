package com.apptolast.fledge.data.repository

import com.apptolast.customlogin.domain.AuthProvider
import com.apptolast.fledge.data.remote.firebase.FirestoreProvider
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.TaskAssignment
import com.apptolast.fledge.domain.model.TaskAssignmentDraft
import com.apptolast.fledge.domain.model.TaskAssignmentId
import com.apptolast.fledge.domain.model.TaskTemplateId
import com.apptolast.fledge.domain.repository.RepositorySyncStatus
import com.apptolast.fledge.domain.repository.TaskAssignmentRepository
import com.apptolast.fledge.domain.repository.TaskTemplateRepository
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

class FirestoreTaskAssignmentRepository(
    private val firestoreProvider: FirestoreProvider,
    private val authProvider: AuthProvider,
    private val taskTemplateRepository: TaskTemplateRepository,
) : TaskAssignmentRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var syncJob: Job? = null
    private val mutableSyncStatus = MutableStateFlow<RepositorySyncStatus>(RepositorySyncStatus.Loading)
    private val mutableAssignments = MutableStateFlow<List<TaskAssignment>>(emptyList())

    override val syncStatus: StateFlow<RepositorySyncStatus> = mutableSyncStatus
    override val assignments: StateFlow<List<TaskAssignment>> = mutableAssignments

    init {
        scope.launch {
            authProvider.authenticatedFamilyIds(firestoreProvider).collectLatest { familyId ->
                syncJob?.cancelAndJoin()
                if (familyId == null) {
                    mutableAssignments.value = emptyList()
                    mutableSyncStatus.value = RepositorySyncStatus.Synced
                } else {
                    mutableSyncStatus.value = RepositorySyncStatus.Loading
                    syncJob = launch { bindTaskAssignments(familyId) }
                }
            }
        }
    }

    private suspend fun bindTaskAssignments(familyId: FamilyId) = coroutineScope {
        val jobs = listOf(
            launch {
                taskAssignmentCollection(familyId).snapshots(includeMetadataChanges = true)
                    .catch { error ->
                        mutableSyncStatus.value = error.toRepositorySyncError()
                    }
                    .combine(taskTemplateRepository.templates) { snapshot, templates ->
                        mutableSyncStatus.value = snapshot.metadata.toRepositorySyncStatus()
                        val templatesById = templates
                            .filter { it.familyId == familyId }
                            .associateBy { it.id }
                        snapshot.documents
                            .filter { it.exists }
                            .mapNotNull { document ->
                                runCatching {
                                    val templateId = TaskTemplateId(document.requiredString("taskTemplateId"))
                                    document.toTaskAssignment(templateFallback = templatesById[templateId])
                                }.getOrNull()
                            }
                            .sortedForAssignments()
                    }
                    .collect { assignments ->
                        mutableAssignments.value = assignments
                    }
            },
        )
        jobs.joinAll()
    }

    override suspend fun saveAssignment(draft: TaskAssignmentDraft, createdAt: Instant): TaskAssignment {
        require(authProvider.currentFamilyId(firestoreProvider) == draft.familyId) {
            "A signed-in parent can only save task assignments for the active family."
        }
        val ref = taskAssignmentCollection(draft.familyId).document
        val assignment = draft.toTaskAssignment(
            id = TaskAssignmentId(ref.id),
            createdAt = createdAt,
        )
        ref.set(assignment.toFirestoreMap())
        upsertLocal(assignment)
        return assignment
    }

    override fun assignmentsForFamily(familyId: FamilyId): List<TaskAssignment> =
        assignments.value.filter { it.familyId == familyId }.sortedForAssignments()

    override fun activeAssignmentsForChild(childProfileId: ChildProfileId): List<TaskAssignment> = assignments.value
        .filter { it.active && childProfileId in it.childProfileIds }
        .sortedForAssignments()

    private fun taskAssignmentCollection(familyId: FamilyId) = firestoreProvider
        .firestoreOrThrow()
        .collection(FAMILIES_COLLECTION)
        .document(familyId.value)
        .collection(TASK_ASSIGNMENTS_COLLECTION)

    private fun upsertLocal(assignment: TaskAssignment) {
        mutableAssignments.value = (assignments.value.filterNot { it.id == assignment.id } + assignment)
            .sortedForAssignments()
    }

    private fun TaskAssignmentDraft.toTaskAssignment(id: TaskAssignmentId, createdAt: Instant): TaskAssignment =
        TaskAssignment(
            id = id,
            familyId = familyId,
            taskTemplateId = taskTemplateId,
            title = title.trim(),
            rewardCents = rewardCents,
            requiresPhoto = requiresPhoto,
            childProfileIds = childProfileIds,
            recurrence = recurrence,
            dueAt = dueAt,
            customIntervalDays = customIntervalDays,
            active = true,
            createdAt = createdAt,
            updatedAt = createdAt,
        )

    private companion object {
        const val TASK_ASSIGNMENTS_COLLECTION = "taskAssignments"
    }
}

internal fun List<TaskAssignment>.sortedForAssignments(): List<TaskAssignment> =
    sortedWith(compareBy<TaskAssignment> { !it.active }.thenBy { it.dueAt }.thenBy { it.id.value })
