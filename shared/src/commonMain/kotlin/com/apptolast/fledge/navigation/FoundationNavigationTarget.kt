package com.apptolast.fledge.navigation

import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.SharedDeviceRole

sealed interface FoundationNavigationTarget {
    data object ParentAuth : FoundationNavigationTarget
    data class ChildPin(val childProfileId: ChildProfileId) : FoundationNavigationTarget
}

class FoundationRouteDecider {
    fun targetForRole(
        role: SharedDeviceRole,
        childProfileId: ChildProfileId?,
    ): FoundationNavigationTarget = when (role) {
        SharedDeviceRole.Parent -> FoundationNavigationTarget.ParentAuth
        SharedDeviceRole.Child -> FoundationNavigationTarget.ChildPin(
            childProfileId ?: ChildProfileId("demo-child")
        )
    }
}
