package com.apptolast.fledge.presentation.foundation.savingsgoal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.MoneyPotType
import com.apptolast.fledge.domain.model.SavingsGoal
import com.apptolast.fledge.domain.model.SavingsGoalDraft
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import com.apptolast.fledge.domain.repository.RepositorySyncStatus
import com.apptolast.fledge.domain.repository.SavingsGoalRepository
import com.apptolast.fledge.presentation.foundation.FoundationOperationError
import com.apptolast.fledge.presentation.foundation.FoundationSyncNotice
import com.apptolast.fledge.presentation.foundation.manualadjustment.parseAmountCents
import com.apptolast.fledge.presentation.foundation.toFoundationOperationError
import com.apptolast.fledge.presentation.foundation.toFoundationSyncNotice
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SavingsGoalSetupUiState(
    val child: ChildProfile? = null,
    val title: String = "",
    val targetAmountInput: String = "",
    val selectedPotType: MoneyPotType = MoneyPotType.Save,
    val selectedIconKey: String = DEFAULT_SAVINGS_GOAL_ICON,
    val imageUri: String? = null,
    val currencyCode: String = "EUR",
    val error: SavingsGoalSetupError? = null,
    val savedGoal: SavingsGoal? = null,
    val syncNotice: FoundationSyncNotice? = FoundationSyncNotice.Loading,
    val operationError: FoundationOperationError? = null,
    val isSaving: Boolean = false,
) {
    val canSubmit: Boolean
        get() = !isSaving && child != null && title.isNotBlank() && targetAmountInput.isNotBlank()
}

enum class SavingsGoalSetupError {
    MissingFamily,
    MissingChild,
    MissingTitle,
    InvalidTarget,
    MissingVisual,
}

class SavingsGoalSetupViewModel(
    private val familyRepository: FamilyFoundationRepository,
    private val savingsGoalRepository: SavingsGoalRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SavingsGoalSetupUiState())
    private var loadedChildProfileId: ChildProfileId? = null
    val uiState: StateFlow<SavingsGoalSetupUiState> = mutableUiState

    init {
        viewModelScope.launch {
            combine(familyRepository.syncStatus, savingsGoalRepository.syncStatus) { familyStatus, goalStatus ->
                listOf(familyStatus, goalStatus)
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
        viewModelScope.launch {
            familyRepository.activeFamily.collect { family ->
                mutableUiState.update { it.copy(currencyCode = family?.currency?.value ?: "EUR") }
            }
        }
    }

    fun load(childProfileId: ChildProfileId) {
        loadedChildProfileId = childProfileId
        refreshSelectedChild()
    }

    fun updateTitle(input: String) {
        mutableUiState.update { it.copy(title = input, error = null, operationError = null, savedGoal = null) }
    }

    fun updateTargetAmount(input: String) {
        mutableUiState.update {
            it.copy(targetAmountInput = input, error = null, operationError = null, savedGoal = null)
        }
    }

    fun selectPotType(potType: MoneyPotType) {
        if (potType == MoneyPotType.Spend) return
        mutableUiState.update {
            it.copy(
                selectedPotType = potType,
                error = null,
                operationError = null,
                savedGoal = null,
            )
        }
    }

    fun selectIcon(iconKey: String) {
        mutableUiState.update {
            it.copy(
                selectedIconKey = iconKey.trim().ifBlank { DEFAULT_SAVINGS_GOAL_ICON },
                imageUri = null,
                error = null,
                operationError = null,
                savedGoal = null,
            )
        }
    }

    fun selectImage(imageUri: String) {
        mutableUiState.update {
            it.copy(
                imageUri = imageUri.trim().takeIf { uri -> uri.isNotBlank() },
                error = null,
                operationError = null,
                savedGoal = null,
            )
        }
    }

    suspend fun submit(): Boolean {
        val state = mutableUiState.value
        val family = familyRepository.activeFamily.value
        val child = state.child
        val title = state.title.trim()
        val targetCents = parseAmountCents(state.targetAmountInput)
        val iconKey = state.selectedIconKey.trim().takeIf { it.isNotBlank() }
        val imageUri = state.imageUri?.trim()?.takeIf { it.isNotBlank() }

        when {
            family == null -> {
                mutableUiState.update { it.copy(error = SavingsGoalSetupError.MissingFamily, operationError = null) }
                return false
            }
            child == null -> {
                mutableUiState.update { it.copy(error = SavingsGoalSetupError.MissingChild, operationError = null) }
                return false
            }
            title.isBlank() -> {
                mutableUiState.update { it.copy(error = SavingsGoalSetupError.MissingTitle, operationError = null) }
                return false
            }
            targetCents == null || targetCents <= 0L -> {
                mutableUiState.update { it.copy(error = SavingsGoalSetupError.InvalidTarget, operationError = null) }
                return false
            }
            iconKey == null && imageUri == null -> {
                mutableUiState.update { it.copy(error = SavingsGoalSetupError.MissingVisual, operationError = null) }
                return false
            }
        }

        mutableUiState.update { it.copy(isSaving = true, operationError = null) }
        return runCatching {
            savingsGoalRepository.saveGoal(
                SavingsGoalDraft(
                    familyId = family.id,
                    childProfileId = child.id,
                    title = title,
                    targetCents = MoneyCents(targetCents),
                    potType = state.selectedPotType,
                    iconKey = iconKey,
                    imageUri = imageUri,
                ),
                createdAt = Clock.System.now(),
            )
        }.fold(
            onSuccess = { goal ->
                mutableUiState.update {
                    it.copy(error = null, savedGoal = goal, isSaving = false)
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
                    shouldShowMissingChild -> SavingsGoalSetupError.MissingChild
                    state.error == SavingsGoalSetupError.MissingChild -> null
                    else -> state.error
                },
            )
        }
    }
}

const val DEFAULT_SAVINGS_GOAL_ICON = "target"
