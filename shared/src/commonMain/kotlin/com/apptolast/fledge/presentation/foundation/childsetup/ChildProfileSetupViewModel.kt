package com.apptolast.fledge.presentation.foundation.childsetup

import androidx.lifecycle.ViewModel
import com.apptolast.fledge.domain.model.ChildPin
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import com.apptolast.fledge.domain.repository.TaskTemplateRepository
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class ChildProfileSetupUiState(
    val displayName: String = "",
    val birthYear: String = "",
    val avatarKey: String = "rocket",
    val pin: String = "",
    val createdChild: ChildProfile? = null,
    val error: ChildProfileSetupError? = null,
) {
    val canSubmit: Boolean =
        displayName.isNotBlank() &&
            (birthYear.toIntOrNull()?.let { it in MIN_BIRTH_YEAR..MAX_BIRTH_YEAR } == true) &&
            pin.matches(Regex("\\d{4}"))
}

enum class ChildProfileSetupError {
    MissingFamily,
    MissingVirtualMoneyConsent,
    InvalidInput,
}

class ChildProfileSetupViewModel(
    private val repository: FamilyFoundationRepository,
    private val taskTemplateRepository: TaskTemplateRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ChildProfileSetupUiState())
    val uiState: StateFlow<ChildProfileSetupUiState> = mutableUiState

    fun updateDisplayName(value: String) {
        mutableUiState.update { it.copy(displayName = value, error = null) }
    }

    fun updateBirthYear(value: String) {
        mutableUiState.update { it.copy(birthYear = value.filter(Char::isDigit).take(4), error = null) }
    }

    fun updateAvatarKey(value: String) {
        mutableUiState.update { it.copy(avatarKey = value.ifBlank { "rocket" }, error = null) }
    }

    fun updatePin(value: String) {
        mutableUiState.update { it.copy(pin = value.filter(Char::isDigit).take(4), error = null) }
    }

    suspend fun submit(): Boolean {
        val state = mutableUiState.value
        if (!state.canSubmit) {
            mutableUiState.update { it.copy(error = ChildProfileSetupError.InvalidInput) }
            return false
        }

        val family = repository.activeFamily.value
        if (family == null) {
            mutableUiState.update { it.copy(error = ChildProfileSetupError.MissingFamily) }
            return false
        }
        if (repository.virtualMoneyConsent.value == null) {
            mutableUiState.update { it.copy(error = ChildProfileSetupError.MissingVirtualMoneyConsent) }
            return false
        }

        val shouldSeedInitialSuggestions = repository.children.value.isEmpty()
        val child = repository.addChildProfile(
            familyId = family.id,
            displayName = state.displayName,
            birthYear = state.birthYear.toInt(),
            avatarKey = state.avatarKey,
            pin = ChildPin(state.pin),
        )
        if (shouldSeedInitialSuggestions) {
            taskTemplateRepository.seedInitialSuggestionsForChild(
                familyId = family.id,
                child = child,
                currentYear = currentYear(),
            )
        }
        mutableUiState.update { it.copy(createdChild = child) }
        return true
    }
}

private fun currentYear(): Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year

private const val MIN_BIRTH_YEAR = 2008
private const val MAX_BIRTH_YEAR = 2023
