package com.apptolast.fledge.presentation.foundation.postlogin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.Family
import com.apptolast.fledge.domain.model.GuestSponsorAccess
import com.apptolast.fledge.domain.model.VirtualMoneyConsent
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import com.apptolast.fledge.domain.repository.GuestSponsorRepository
import com.apptolast.fledge.domain.repository.RepositorySyncStatus
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
    private val guestSponsorRepository: GuestSponsorRepository,
    private val routeDecider: FoundationRouteDecider,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(PostLoginUiState())
    val uiState: StateFlow<PostLoginUiState> = mutableUiState

    init {
        viewModelScope.launch {
            combine(
                combine(
                    repository.activeFamily,
                    repository.virtualMoneyConsent,
                    repository.children,
                    repository.syncStatus,
                ) { family, consent, children, syncStatus ->
                    FamilyPostLoginSnapshot(family, consent, children, syncStatus)
                },
                combine(
                    guestSponsorRepository.activeGuestAccess,
                    guestSponsorRepository.syncStatus,
                ) { access, syncStatus ->
                    GuestPostLoginSnapshot(access, syncStatus)
                },
            ) { familySnapshot, guestSnapshot ->
                routeDecider.postLoginTarget(
                    hasFamily = familySnapshot.family != null,
                    hasGuestAccess = guestSnapshot.access != null,
                    hasVirtualMoneyConsent = familySnapshot.consent != null,
                    hasChildProfiles = familySnapshot.children.isNotEmpty(),
                    syncStatus = familySnapshot.syncStatus,
                    guestSyncStatus = guestSnapshot.syncStatus,
                )
            }.collect { target ->
                mutableUiState.update { it.copy(target = target) }
            }
        }
    }
}

private data class FamilyPostLoginSnapshot(
    val family: Family?,
    val consent: VirtualMoneyConsent?,
    val children: List<ChildProfile>,
    val syncStatus: RepositorySyncStatus,
)

private data class GuestPostLoginSnapshot(val access: GuestSponsorAccess?, val syncStatus: RepositorySyncStatus)
