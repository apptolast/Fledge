package com.apptolast.fledge.domain.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class AccountDeletionStatus {
    NotRequested,
    Requested,
    Deleting,
    Failed,
}

@Serializable
data class AccountDeletionState(
    val status: AccountDeletionStatus = AccountDeletionStatus.NotRequested,
    val requestedAt: Instant? = null,
    val updatedAt: Instant? = null,
    val failureReason: String? = null,
) {
    val canRequest: Boolean
        get() = status == AccountDeletionStatus.NotRequested || status == AccountDeletionStatus.Failed
}

@Serializable
data class AccountDeletionRequest(val familyId: FamilyId, val requestedAt: Instant)
