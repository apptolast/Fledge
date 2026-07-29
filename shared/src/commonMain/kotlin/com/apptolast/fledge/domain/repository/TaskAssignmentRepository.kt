package com.apptolast.fledge.domain.repository

import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.TaskAssignment
import com.apptolast.fledge.domain.model.TaskAssignmentDraft
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.StateFlow

interface TaskAssignmentRepository {
    val syncStatus: StateFlow<RepositorySyncStatus>
    val assignments: StateFlow<List<TaskAssignment>>

    suspend fun saveAssignment(draft: TaskAssignmentDraft, createdAt: Instant = Clock.System.now()): TaskAssignment

    fun assignmentsForFamily(familyId: FamilyId): List<TaskAssignment>

    fun activeAssignmentsForChild(childProfileId: ChildProfileId): List<TaskAssignment>
}
