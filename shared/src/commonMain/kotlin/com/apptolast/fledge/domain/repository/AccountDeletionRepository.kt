package com.apptolast.fledge.domain.repository

import com.apptolast.fledge.domain.model.AccountDeletionRequest
import com.apptolast.fledge.domain.model.AccountDeletionState
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.StateFlow

interface AccountDeletionRepository {
    val syncStatus: StateFlow<RepositorySyncStatus>
    val deletionState: StateFlow<AccountDeletionState>

    suspend fun requestAccountDeletion(requestedAt: Instant = Clock.System.now()): AccountDeletionRequest
}
