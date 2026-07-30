package com.apptolast.fledge.presentation.foundation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.fledge.domain.model.Family
import com.apptolast.fledge.domain.model.FamilyAdminInvite
import com.apptolast.fledge.domain.model.FamilyAdminInviteDraft
import com.apptolast.fledge.domain.model.FamilyAdminRole
import com.apptolast.fledge.domain.model.normalizeFamilyAdminEmail
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import com.apptolast.fledge.presentation.foundation.FoundationOperationError
import com.apptolast.fledge.presentation.foundation.FoundationSyncNotice
import com.apptolast.fledge.presentation.foundation.toFoundationOperationError
import com.apptolast.fledge.presentation.foundation.toFoundationSyncNotice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SecondaryAdminUiState(
    val hasFamily: Boolean = false,
    val familyName: String = "",
    val isOwner: Boolean = false,
    val emailInput: String = "",
    val invites: List<FamilyAdminInvite> = emptyList(),
    val error: SecondaryAdminError? = null,
    val savedInvite: FamilyAdminInvite? = null,
    val revokedEmail: String? = null,
    val syncNotice: FoundationSyncNotice? = FoundationSyncNotice.Loading,
    val operationError: FoundationOperationError? = null,
    val isSaving: Boolean = false,
) {
    val canInvite: Boolean
        get() = hasFamily && isOwner && !isSaving && emailInput.isNotBlank()
}

enum class SecondaryAdminError {
    MissingFamily,
    NotOwner,
    InvalidEmail,
}

class SecondaryAdminViewModel(private val repository: FamilyFoundationRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        repository.activeFamily.value.toUiState(
            role = repository.activeAdminRole.value,
            invites = repository.adminInvites.value,
            syncNotice = listOf(repository.syncStatus.value).toFoundationSyncNotice(
                repository.activeFamily.value != null,
            ),
        ),
    )
    private var userEdited = false

    val uiState: StateFlow<SecondaryAdminUiState> = mutableUiState

    init {
        viewModelScope.launch {
            combine(
                repository.syncStatus,
                repository.activeFamily,
                repository.activeAdminRole,
                repository.adminInvites,
            ) { syncStatus, family, role, invites ->
                val syncNotice = listOf(syncStatus).toFoundationSyncNotice(family != null)
                val currentState = mutableUiState.value
                if (
                    userEdited ||
                    currentState.isSaving ||
                    currentState.savedInvite != null ||
                    currentState.revokedEmail != null ||
                    currentState.error != null
                ) {
                    currentState.copy(
                        hasFamily = family != null,
                        familyName = family?.name.orEmpty(),
                        isOwner = role == FamilyAdminRole.Owner,
                        invites = invites,
                        syncNotice = syncNotice,
                    )
                } else {
                    family.toUiState(role, invites, syncNotice)
                }
            }.collect { state ->
                mutableUiState.value = state
            }
        }
    }

    fun updateEmail(input: String) {
        userEdited = true
        mutableUiState.update {
            it.copy(emailInput = input, error = null, savedInvite = null, revokedEmail = null, operationError = null)
        }
    }

    suspend fun invite(): Boolean {
        val state = mutableUiState.value
        val normalizedEmail = runCatching { normalizeFamilyAdminEmail(state.emailInput) }.getOrNull()
        when {
            !state.hasFamily || repository.activeFamily.value == null -> {
                mutableUiState.update { it.copy(error = SecondaryAdminError.MissingFamily, operationError = null) }
                return false
            }
            !state.isOwner -> {
                mutableUiState.update { it.copy(error = SecondaryAdminError.NotOwner, operationError = null) }
                return false
            }
            normalizedEmail == null -> {
                mutableUiState.update { it.copy(error = SecondaryAdminError.InvalidEmail, operationError = null) }
                return false
            }
        }

        mutableUiState.update { it.copy(isSaving = true, operationError = null) }
        return runCatching {
            repository.inviteAdmin(FamilyAdminInviteDraft(normalizedEmail))
        }.fold(
            onSuccess = { invite ->
                userEdited = false
                mutableUiState.update {
                    it.copy(
                        emailInput = "",
                        error = null,
                        savedInvite = invite,
                        revokedEmail = null,
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

    fun revoke(email: String) {
        if (!mutableUiState.value.isOwner) {
            mutableUiState.update { it.copy(error = SecondaryAdminError.NotOwner, operationError = null) }
            return
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSaving = true, operationError = null) }
            runCatching { repository.revokeAdminInvite(email) }.fold(
                onSuccess = { revoked ->
                    userEdited = false
                    mutableUiState.update {
                        it.copy(
                            revokedEmail = revoked?.email,
                            savedInvite = null,
                            error = null,
                            isSaving = false,
                        )
                    }
                },
                onFailure = { error ->
                    mutableUiState.update {
                        it.copy(isSaving = false, operationError = error.toFoundationOperationError())
                    }
                },
            )
        }
    }
}

private fun Family?.toUiState(
    role: FamilyAdminRole?,
    invites: List<FamilyAdminInvite>,
    syncNotice: FoundationSyncNotice?,
): SecondaryAdminUiState = SecondaryAdminUiState(
    hasFamily = this != null,
    familyName = this?.name.orEmpty(),
    isOwner = role == FamilyAdminRole.Owner,
    invites = invites,
    syncNotice = syncNotice,
)
