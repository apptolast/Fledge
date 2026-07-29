package com.apptolast.fledge.domain.service

import com.apptolast.fledge.domain.model.LedgerActor
import com.apptolast.fledge.domain.model.LedgerConcept
import com.apptolast.fledge.domain.model.LedgerTransactionDraft
import com.apptolast.fledge.domain.model.LedgerTransactionType
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.TaskInstance
import com.apptolast.fledge.domain.model.TaskInstanceId
import com.apptolast.fledge.domain.model.TaskInstanceStatus
import com.apptolast.fledge.domain.model.VirtualAccountType
import com.apptolast.fledge.domain.repository.LedgerRepository
import com.apptolast.fledge.domain.repository.TaskInstanceRepository
import kotlin.time.Instant

class TaskApprovalProcessor(
    private val taskInstanceRepository: TaskInstanceRepository,
    private val ledgerRepository: LedgerRepository,
) {
    suspend fun approve(
        instanceId: TaskInstanceId,
        approvedRewardCents: MoneyCents,
        reviewedAt: Instant,
    ): TaskInstance {
        val instance = requireSubmittedInstance(instanceId)
        val transaction = ledgerRepository.appendTransaction(
            LedgerTransactionDraft(
                familyId = instance.familyId,
                childProfileId = instance.childProfileId,
                accountType = VirtualAccountType.Main,
                type = LedgerTransactionType.TaskReward,
                amountCents = approvedRewardCents,
                concept = LedgerConcept(taskRewardConcept(instance.title)),
                createdBy = LedgerActor.Parent,
            ),
        )
        return taskInstanceRepository.approve(
            instanceId = instanceId,
            approvedRewardCents = approvedRewardCents,
            transactionId = transaction.id,
            reviewedAt = reviewedAt,
        )
    }

    suspend fun reject(instanceId: TaskInstanceId, reason: String, reviewedAt: Instant): TaskInstance {
        requireSubmittedInstance(instanceId)
        return taskInstanceRepository.reject(
            instanceId = instanceId,
            reason = reason,
            reviewedAt = reviewedAt,
        )
    }

    private fun requireSubmittedInstance(instanceId: TaskInstanceId): TaskInstance {
        val instance = requireNotNull(taskInstanceRepository.instanceById(instanceId)) {
            "Task instance does not exist."
        }
        require(instance.status == TaskInstanceStatus.Submitted) {
            "Only submitted task instances can be reviewed."
        }
        return instance
    }
}

private fun taskRewardConcept(title: String): String {
    val concept = "Tarea aprobada: ${title.trim()}"
    return if (concept.length <= MAX_LEDGER_CONCEPT_LENGTH) {
        concept
    } else {
        concept.take(MAX_LEDGER_CONCEPT_LENGTH)
    }
}

private const val MAX_LEDGER_CONCEPT_LENGTH = 120
