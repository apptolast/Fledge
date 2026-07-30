package com.apptolast.fledge.navigation

import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.SharedDeviceRole
import com.apptolast.fledge.domain.repository.RepositorySyncStatus

sealed interface FoundationNavigationTarget {
    data object ParentAuth : FoundationNavigationTarget
    data class ChildPin(val childProfileId: ChildProfileId) : FoundationNavigationTarget
}

sealed interface PostLoginNavigationTarget {
    data object Pending : PostLoginNavigationTarget
    data object FamilySetup : PostLoginNavigationTarget
    data object VirtualMoneyConsent : PostLoginNavigationTarget
    data object ChildProfileSetup : PostLoginNavigationTarget
    data object ParentHome : PostLoginNavigationTarget
    data object GuestHome : PostLoginNavigationTarget
}

class FoundationRouteDecider {
    fun targetForRole(role: SharedDeviceRole, childProfileId: ChildProfileId?): FoundationNavigationTarget =
        when (role) {
            SharedDeviceRole.Parent -> FoundationNavigationTarget.ParentAuth
            SharedDeviceRole.Child -> FoundationNavigationTarget.ChildPin(
                requireNotNull(childProfileId) { "Child mode requires a real child profile." },
            )
        }

    fun postLoginTarget(
        hasFamily: Boolean,
        hasGuestAccess: Boolean,
        hasVirtualMoneyConsent: Boolean,
        hasChildProfiles: Boolean,
        syncStatus: RepositorySyncStatus,
        guestSyncStatus: RepositorySyncStatus = RepositorySyncStatus.Synced,
    ): PostLoginNavigationTarget = when {
        !hasFamily &&
            !hasGuestAccess &&
            (syncStatus == RepositorySyncStatus.Loading || guestSyncStatus == RepositorySyncStatus.Loading) ->
            PostLoginNavigationTarget.Pending
        !hasFamily && hasGuestAccess -> PostLoginNavigationTarget.GuestHome
        !hasFamily -> PostLoginNavigationTarget.FamilySetup
        !hasVirtualMoneyConsent -> PostLoginNavigationTarget.VirtualMoneyConsent
        !hasChildProfiles && syncStatus == RepositorySyncStatus.Loading -> PostLoginNavigationTarget.Pending
        !hasChildProfiles -> PostLoginNavigationTarget.ChildProfileSetup
        else -> PostLoginNavigationTarget.ParentHome
    }
}
