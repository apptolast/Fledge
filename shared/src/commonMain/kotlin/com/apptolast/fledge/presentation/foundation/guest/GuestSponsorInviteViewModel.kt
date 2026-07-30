package com.apptolast.fledge.presentation.foundation.guest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyGuestInvite
import com.apptolast.fledge.domain.model.FamilyGuestInviteDraft
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import com.apptolast.fledge.domain.repository.GuestSponsorRepository
import com.apptolast.fledge.domain.repository.RepositorySyncStatus
import com.apptolast.fledge.presentation.foundation.FoundationOperationError
import com.apptolast.fledge.presentation.foundation.FoundationSyncNotice
import com.apptolast.fledge.presentation.foundation.toFoundationOperationError
import com.apptolast.fledge.presentation.foundation.toFoundationSyncNotice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GuestSponsorInviteUiState(
    val child: ChildProfile? = null,
    val emailInput: String = "",
    val error: GuestSponsorInviteError? = null,
    val savedInvite: FamilyGuestInvite? = null,
    val syncNotice: FoundationSyncNotice? = FoundationSyncNotice.Loading,
    val operationError: FoundationOperationError? = null,
    val isSaving: Boolean = false,
) {
    val canInvite: Boolean
        get() = !isSaving && child != null && emailInput.isNotBlank()
}

enum class GuestSponsorInviteError {
    MissingFamily,
    MissingChild,
    InvalidEmail,
}

class GuestSponsorInviteViewModel(
    private val familyRepository: FamilyFoundationRepository,
    private val guestSponsorRepository: GuestSponsorRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(GuestSponsorInviteUiState())
    private var loadedChildProfileId: ChildProfileId? = null

    val uiState: StateFlow<GuestSponsorInviteUiState> = mutableUiState

    init {
        viewModelScope.launch {
            combine(familyRepository.syncStatus, guestSponsorRepository.syncStatus) { familyStatus, guestStatus ->
                listOf(familyStatus, guestStatus)
            }.collect { statuses ->
                mutableUiState.update { state ->
                    state.copy(syncNotice = statuses.toFoundationSyncNotice(state.child != null))
                }
                refreshSelectedChild()
            }
        }
        viewModelScope.launch {
            familyRepository.children.collect {
                refreshSelectedChild()
            }
        }
    }

    fun load(childProfileId: ChildProfileId) {
        loadedChildProfileId = childProfileId
        refreshSelectedChild()
    }

    fun updateEmail(input: String) {
        mutableUiState.update {
            it.copy(emailInput = input, error = null, savedInvite = null, operationError = null)
        }
    }

    suspend fun invite(): Boolean {
        val child = mutableUiState.value.child
        val family = familyRepository.activeFamily.value
        val draft = runCatching { FamilyGuestInviteDraft(mutableUiState.value.emailInput) }.getOrNull()
        when {
            family == null -> {
                mutableUiState.update { it.copy(error = GuestSponsorInviteError.MissingFamily, operationError = null) }
                return false
            }
            child == null -> {
                mutableUiState.update { it.copy(error = GuestSponsorInviteError.MissingChild, operationError = null) }
                return false
            }
            draft == null -> {
                mutableUiState.update { it.copy(error = GuestSponsorInviteError.InvalidEmail, operationError = null) }
                return false
            }
        }

        mutableUiState.update { it.copy(isSaving = true, operationError = null) }
        return runCatching {
            guestSponsorRepository.inviteGuest(child.id, draft)
        }.fold(
            onSuccess = { invite ->
                mutableUiState.update {
                    it.copy(
                        emailInput = "",
                        error = null,
                        savedInvite = invite,
                        isSaving = false,
                    )
                }
                true
            },
            onFailure = { error ->
                mutableUiState.update {
                    it.copy(isSaving = false, operationError = error.toFoundationOperationError())
                }
                false
            },
        )
    }

    private fun refreshSelectedChild() {
        val childProfileId = loadedChildProfileId ?: return
        val child = familyRepository.children.value.firstOrNull { it.id == childProfileId }
        val shouldShowMissingChild = child == null && familyRepository.syncStatus.value != RepositorySyncStatus.Loading
        mutableUiState.update { state ->
            state.copy(
                child = child,
                error = when {
                    shouldShowMissingChild -> GuestSponsorInviteError.MissingChild
                    state.error == GuestSponsorInviteError.MissingChild -> null
                    else -> state.error
                },
            )
        }
    }
}
