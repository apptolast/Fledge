package com.apptolast.fledge.presentation.foundation

import com.apptolast.fledge.domain.repository.RepositorySyncStatus

enum class FoundationSyncNotice {
    Loading,
    CachedData,
    Syncing,
    Error,
}

enum class FoundationOperationError {
    SyncFailed,
}

internal fun List<RepositorySyncStatus>.toFoundationSyncNotice(hasKnownData: Boolean): FoundationSyncNotice? = when {
    any { it is RepositorySyncStatus.Error } -> FoundationSyncNotice.Error
    any { it == RepositorySyncStatus.Loading } && !hasKnownData -> FoundationSyncNotice.Loading
    any { it == RepositorySyncStatus.PendingWrites } -> FoundationSyncNotice.Syncing
    any { it == RepositorySyncStatus.FromCache } -> FoundationSyncNotice.CachedData
    else -> null
}

internal fun Throwable.toFoundationOperationError(): FoundationOperationError = FoundationOperationError.SyncFailed
