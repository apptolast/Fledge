package com.apptolast.fledge.data.repository

import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.TaskInstance
import com.apptolast.fledge.domain.repository.RepositorySyncStatus
import com.apptolast.fledge.domain.repository.TaskInstanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class InMemoryTaskInstanceRepository(initialInstances: List<TaskInstance> = emptyList()) : TaskInstanceRepository {
    private val mutableInstances = MutableStateFlow(initialInstances.sortedForInstances())

    override val syncStatus: StateFlow<RepositorySyncStatus> =
        MutableStateFlow(RepositorySyncStatus.Synced)
    override val instances: StateFlow<List<TaskInstance>> = mutableInstances

    fun replaceInstances(instances: List<TaskInstance>) {
        mutableInstances.value = instances.sortedForInstances()
    }

    override fun instancesForFamily(familyId: FamilyId): List<TaskInstance> =
        instances.value.filter { it.familyId == familyId }.sortedForInstances()

    override fun instancesForChild(childProfileId: ChildProfileId): List<TaskInstance> = instances.value
        .filter { it.childProfileId == childProfileId }
        .sortedForInstances()
}
