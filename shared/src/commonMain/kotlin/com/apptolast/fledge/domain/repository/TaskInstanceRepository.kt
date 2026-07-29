package com.apptolast.fledge.domain.repository

import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.TaskInstance
import kotlinx.coroutines.flow.StateFlow

interface TaskInstanceRepository {
    val syncStatus: StateFlow<RepositorySyncStatus>
    val instances: StateFlow<List<TaskInstance>>

    fun instancesForFamily(familyId: FamilyId): List<TaskInstance>

    fun instancesForChild(childProfileId: ChildProfileId): List<TaskInstance>
}
