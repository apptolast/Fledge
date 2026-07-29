package com.apptolast.fledge.presentation.foundation.postlogin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import com.apptolast.fledge.navigation.FoundationRouteDecider
import com.apptolast.fledge.navigation.PostLoginNavigationTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PostLoginUiState(val target: PostLoginNavigationTarget = PostLoginNavigationTarget.Pending)

class PostLoginViewModel(
    private val repository: FamilyFoundationRepository,
    private val routeDecider: FoundationRouteDecider,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(PostLoginUiState())
    val uiState: StateFlow<PostLoginUiState> = mutableUiState

    init {
        viewModelScope.launch {
            combine(
                repository.activeFamily,
                repository.virtualMoneyConsent,
                repository.children,
                repository.syncStatus,
            ) { family, consent, children, syncStatus ->
                routeDecider.postLoginTarget(
                    hasFamily = family != null,
                    hasVirtualMoneyConsent = consent != null,
                    hasChildProfiles = children.isNotEmpty(),
                    syncStatus = syncStatus,
                )
            }.collect { target ->
                mutableUiState.update { it.copy(target = target) }
            }
        }
    }
}
