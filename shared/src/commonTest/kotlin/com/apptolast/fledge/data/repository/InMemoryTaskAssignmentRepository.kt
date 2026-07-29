package com.apptolast.fledge.data.repository

import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.TaskAssignment
import com.apptolast.fledge.domain.model.TaskAssignmentDraft
import com.apptolast.fledge.domain.model.TaskAssignmentId
import com.apptolast.fledge.domain.repository.RepositorySyncStatus
import com.apptolast.fledge.domain.repository.TaskAssignmentRepository
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class InMemoryTaskAssignmentRepository : TaskAssignmentRepository {
    private var assignmentCounter = 1
    private val mutableAssignments = MutableStateFlow<List<TaskAssignment>>(emptyList())
    private val mutableSyncStatus = MutableStateFlow<RepositorySyncStatus>(RepositorySyncStatus.Synced)

    override val syncStatus: StateFlow<RepositorySyncStatus> = mutableSyncStatus
    override val assignments: StateFlow<List<TaskAssignment>> = mutableAssignments

    override suspend fun saveAssignment(draft: TaskAssignmentDraft, createdAt: Instant): TaskAssignment {
        val assignment = draft.toTaskAssignment(
            id = TaskAssignmentId("assignment-${assignmentCounter++}"),
            createdAt = createdAt,
        )
        mutableAssignments.value = (assignments.value.filterNot { it.id == assignment.id } + assignment)
            .sortedForAssignments()
        return assignment
    }

    override fun assignmentsForFamily(familyId: FamilyId): List<TaskAssignment> =
        assignments.value.filter { it.familyId == familyId }.sortedForAssignments()

    override fun activeAssignmentsForChild(childProfileId: ChildProfileId): List<TaskAssignment> = assignments.value
        .filter { it.active && childProfileId in it.childProfileIds }
        .sortedForAssignments()

    private fun TaskAssignmentDraft.toTaskAssignment(
        id: TaskAssignmentId,
        createdAt: Instant = Clock.System.now(),
    ): TaskAssignment = TaskAssignment(
        id = id,
        familyId = familyId,
        taskTemplateId = taskTemplateId,
        title = title,
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
}

private fun List<TaskAssignment>.sortedForAssignments(): List<TaskAssignment> =
    sortedWith(compareBy<TaskAssignment> { !it.active }.thenBy { it.dueAt }.thenBy { it.id.value })
