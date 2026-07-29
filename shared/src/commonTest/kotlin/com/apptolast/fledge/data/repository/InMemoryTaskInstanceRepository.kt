package com.apptolast.fledge.data.repository

import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.TaskInstance
import com.apptolast.fledge.domain.model.TaskInstanceId
import com.apptolast.fledge.domain.model.TransactionId
import com.apptolast.fledge.domain.model.approvedByParent
import com.apptolast.fledge.domain.model.rejectedByParent
import com.apptolast.fledge.domain.model.submittedForReview
import com.apptolast.fledge.domain.repository.RepositorySyncStatus
import com.apptolast.fledge.domain.repository.TaskInstanceRepository
import kotlin.time.Instant
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
        upsert(submitted)
        return submitted
    }

    override suspend fun approve(
        instanceId: TaskInstanceId,
        approvedRewardCents: MoneyCents,
        transactionId: TransactionId,
        reviewedAt: Instant,
    ): TaskInstance {
        val approved = existingInstance(instanceId).approvedByParent(
            approvedRewardCents = approvedRewardCents,
            transactionId = transactionId,
            reviewedAt = reviewedAt,
        )
        upsert(approved)
        return approved
    }

    override suspend fun reject(instanceId: TaskInstanceId, reason: String, reviewedAt: Instant): TaskInstance {
        val rejected = existingInstance(instanceId).rejectedByParent(reason = reason, reviewedAt = reviewedAt)
        upsert(rejected)
        return rejected
    }

    private fun existingInstance(instanceId: TaskInstanceId): TaskInstance {
        val existing = instanceById(instanceId)
        return requireNotNull(existing) { "Task instance does not exist." }
    }

    private fun upsert(instance: TaskInstance) {
        mutableInstances.value = (instances.value.filterNot { it.id == instance.id } + instance)
            .sortedForInstances()
    }
}
