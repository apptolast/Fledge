package com.apptolast.fledge.presentation.foundation.roles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.SharedDeviceRole
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import com.apptolast.fledge.navigation.FoundationNavigationTarget
import com.apptolast.fledge.navigation.FoundationRouteDecider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoleSelectorUiState(
    val childProfiles: List<ChildProfile> = emptyList(),
    val navigationTarget: FoundationNavigationTarget? = null,
)

class RoleSelectorViewModel(
    private val repository: FamilyFoundationRepository,
    private val routeDecider: FoundationRouteDecider = FoundationRouteDecider(),
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(RoleSelectorUiState())
    val uiState: StateFlow<RoleSelectorUiState> = mutableUiState

    init {
        viewModelScope.launch {
            repository.children.collect { children ->
                mutableUiState.update { it.copy(childProfiles = children) }
            }
        }
    }

    fun selectParentMode() {
        mutableUiState.update {
            it.copy(navigationTarget = routeDecider.targetForRole(SharedDeviceRole.Parent, null))
        }
    }

    fun selectChildMode(childProfileId: ChildProfileId) {
        if (mutableUiState.value.childProfiles.none { it.id == childProfileId }) return

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
