package com.apptolast.fledge.presentation.foundation.postlogin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import com.apptolast.fledge.navigation.FoundationRouteDecider
import com.apptolast.fledge.navigation.PostLoginNavigationTarget
import com.apptolast.fledge.notification.NoOpPushNotificationManager
import com.apptolast.fledge.notification.PushNotificationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PostLoginUiState(val target: PostLoginNavigationTarget = PostLoginNavigationTarget.Pending)

class PostLoginViewModel(
    private val repository: FamilyFoundationRepository,
    private val routeDecider: FoundationRouteDecider = FoundationRouteDecider(),
    private val pushNotifications: PushNotificationManager = NoOpPushNotificationManager,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(PostLoginUiState())
    val uiState: StateFlow<PostLoginUiState> = mutableUiState
    private var subscribedParentFamilyId: FamilyId? = null

    init {
        viewModelScope.launch {
            combine(
                repository.activeFamily,
                repository.virtualMoneyConsent,
                repository.children,
                repository.syncStatus,
            ) { family, consent, children, syncStatus ->
                val target = routeDecider.postLoginTarget(
                    hasFamily = family != null,
                    hasVirtualMoneyConsent = consent != null,
                    hasChildProfiles = children.isNotEmpty(),
                    syncStatus = syncStatus,
                )
                family to target
            }.collect { (family, target) ->
                mutableUiState.update { it.copy(target = target) }
                if (target == PostLoginNavigationTarget.ParentHome && family != null) {
                    subscribeParentApprovals(family.id)
                }
            }
        }
    }

    private suspend fun subscribeParentApprovals(familyId: FamilyId) {
        if (subscribedParentFamilyId == familyId) return
        subscribedParentFamilyId = familyId
        runCatching { pushNotifications.subscribeToParentApprovals(familyId) }
    }
}
