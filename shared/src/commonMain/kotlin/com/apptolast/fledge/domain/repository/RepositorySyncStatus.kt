package com.apptolast.fledge.domain.repository

sealed interface RepositorySyncStatus {
    data object Loading : RepositorySyncStatus
    data object Synced : RepositorySyncStatus
    data object FromCache : RepositorySyncStatus
    data object PendingWrites : RepositorySyncStatus
    data class Error(val message: String? = null) : RepositorySyncStatus
}
