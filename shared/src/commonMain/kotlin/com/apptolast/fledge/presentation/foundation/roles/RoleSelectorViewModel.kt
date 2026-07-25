package com.apptolast.fledge.presentation.foundation.roles

import androidx.lifecycle.ViewModel
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.SharedDeviceRole
import com.apptolast.fledge.navigation.FoundationNavigationTarget
import com.apptolast.fledge.navigation.FoundationRouteDecider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class RoleSelectorUiState(
    val navigationTarget: FoundationNavigationTarget? = null,
)

class RoleSelectorViewModel(
    private val routeDecider: FoundationRouteDecider = FoundationRouteDecider(),
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(RoleSelectorUiState())
    val uiState: StateFlow<RoleSelectorUiState> = mutableUiState

    fun selectParentMode() {
        mutableUiState.update {
            it.copy(navigationTarget = routeDecider.targetForRole(SharedDeviceRole.Parent, null))
        }
    }

    fun selectChildMode(childProfileId: ChildProfileId) {
        mutableUiState.update {
            it.copy(
                navigationTarget = routeDecider.targetForRole(
                    role = SharedDeviceRole.Child,
                    childProfileId = childProfileId,
                )
            )
        }
    }
}
