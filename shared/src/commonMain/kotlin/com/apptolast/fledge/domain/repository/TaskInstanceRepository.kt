package com.apptolast.fledge.domain.repository

import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.TaskInstance
import com.apptolast.fledge.domain.model.TaskInstanceId
import com.apptolast.fledge.domain.model.TransactionId
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.StateFlow

interface TaskInstanceRepository {
    val syncStatus: StateFlow<RepositorySyncStatus>
    val instances: StateFlow<List<TaskInstance>>

    fun instancesForFamily(familyId: FamilyId): List<TaskInstance>

    fun instancesForChild(childProfileId: ChildProfileId): List<TaskInstance>

    fun instanceById(instanceId: TaskInstanceId): TaskInstance?

    suspend fun submitForReview(
        instanceId: TaskInstanceId,
        childProfileId: ChildProfileId,
        photoEvidenceUri: String?,
        submittedAt: Instant = Clock.System.now(),
    ): TaskInstance

    suspend fun approve(
        instanceId: TaskInstanceId,
        approvedRewardCents: MoneyCents,
        transactionId: TransactionId,
        reviewedAt: Instant = Clock.System.now(),
    ): TaskInstance

    suspend fun reject(
        instanceId: TaskInstanceId,
        reason: String,
        reviewedAt: Instant = Clock.System.now(),
    ): TaskInstance
}
