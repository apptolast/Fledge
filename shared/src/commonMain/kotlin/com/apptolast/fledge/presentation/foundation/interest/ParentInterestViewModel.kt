package com.apptolast.fledge.presentation.foundation.interest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.fledge.domain.model.Family
import com.apptolast.fledge.domain.model.InterestSettings
import com.apptolast.fledge.domain.model.InterestSettingsDraft
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

data class ParentInterestUiState(
    val hasFamily: Boolean = false,
    val familyName: String = "",
    val currencyCode: String = "EUR",
    val enabled: Boolean = false,
    val annualRateInput: String = "0,00",
    val postingDayInput: String = "1",
    val lastPostedPeriodKey: String? = null,
    val error: ParentInterestError? = null,
    val savedSettings: InterestSettings? = null,
    val syncNotice: FoundationSyncNotice? = FoundationSyncNotice.Loading,
    val operationError: FoundationOperationError? = null,
    val isSaving: Boolean = false,
) {
    val canSubmit: Boolean
        get() = !isSaving && hasFamily && annualRateBasisPoints() != null && postingDay() != null

    fun annualRateBasisPoints(): Int? = parseAnnualRateBasisPoints(annualRateInput)

    fun postingDay(): Int? = postingDayInput.toIntOrNull()?.takeIf { it in 1..28 }
}

enum class ParentInterestError {
    MissingFamily,
    InvalidRate,
    InvalidDay,
}

class ParentInterestViewModel(private val repository: FamilyFoundationRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow(repository.activeFamily.value.toUiState())
    private var userEdited = false

    val uiState: StateFlow<ParentInterestUiState> = mutableUiState

    init {
        viewModelScope.launch {
            combine(repository.syncStatus, repository.activeFamily) { syncStatus, family ->
                syncStatus to family
            }.collect { (syncStatus, family) ->
                mutableUiState.update { state ->
                    val baseState = if (!userEdited && !state.isSaving) {
                        family.toUiState(syncNotice = listOf(syncStatus).toFoundationSyncNotice(family != null))
                    } else {
                        state.copy(
                            hasFamily = family != null,
                            familyName = family?.name.orEmpty(),
                            currencyCode = family?.currency?.value ?: state.currencyCode,
                            lastPostedPeriodKey = family?.interestSettings?.lastPostedPeriodKey,
                        )
                    }
                    baseState.copy(syncNotice = listOf(syncStatus).toFoundationSyncNotice(family != null))
                }
            }
        }
    }

    fun updateEnabled(enabled: Boolean) {
        userEdited = true
        mutableUiState.update { state ->
            val rateInput = if (enabled && state.annualRateBasisPoints() == 0) "2,00" else state.annualRateInput
            state.copy(
                enabled = enabled,
                annualRateInput = rateInput,
                error = null,
                savedSettings = null,
                operationError = null,
            )
        }
    }

    fun updateAnnualRate(input: String) {
        userEdited = true
        mutableUiState.update {
            it.copy(annualRateInput = input, error = null, savedSettings = null, operationError = null)
        }
    }

    fun updatePostingDay(input: String) {
        userEdited = true
        mutableUiState.update {
            it.copy(
                postingDayInput = input.filter(Char::isDigit),
                error = null,
                savedSettings = null,
                operationError = null,
            )
        }
    }

    suspend fun submit(): Boolean {
        val state = mutableUiState.value
        val rateBasisPoints = state.annualRateBasisPoints()
        val postingDay = state.postingDay()
        when {
            !state.hasFamily || repository.activeFamily.value == null -> {
                mutableUiState.update { it.copy(error = ParentInterestError.MissingFamily, operationError = null) }
                return false
            }
            rateBasisPoints == null || state.enabled && rateBasisPoints == 0 -> {
                mutableUiState.update { it.copy(error = ParentInterestError.InvalidRate, operationError = null) }
                return false
            }
            postingDay == null -> {
                mutableUiState.update { it.copy(error = ParentInterestError.InvalidDay, operationError = null) }
                return false
            }
        }

        mutableUiState.update { it.copy(isSaving = true, operationError = null) }
        return runCatching {
            repository.updateInterestSettings(
                InterestSettingsDraft(
                    enabled = state.enabled,
                    annualRateBasisPoints = rateBasisPoints,
                    postingDayOfMonth = postingDay,
                ),
            )
        }.fold(
            onSuccess = { settings ->
                userEdited = false
                mutableUiState.update {
                    it.copy(
                        enabled = settings.enabled,
                        annualRateInput = formatInterestBasisPoints(settings.annualRateBasisPoints),
                        postingDayInput = settings.postingDayOfMonth.toString(),
                        lastPostedPeriodKey = settings.lastPostedPeriodKey,
                        error = null,
                        savedSettings = settings,
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
}

internal fun parseAnnualRateBasisPoints(input: String): Int? {
    val match = Regex("""^\s*(\d{1,2})([,.](\d{0,2}))?\s*$""").matchEntire(input) ?: return null
    val whole = match.groupValues[1].toIntOrNull() ?: return null
    val fraction = match.groupValues.getOrNull(3).orEmpty().padEnd(2, '0').take(2).toIntOrNull() ?: 0
    val basisPoints = whole * 100 + fraction
    return basisPoints.takeIf { it in 0..5_000 }
}

internal fun formatInterestBasisPoints(basisPoints: Int): String {
    val whole = basisPoints / 100
    val fraction = (basisPoints % 100).toString().padStart(2, '0')
    return "$whole,$fraction"
}

private fun Family?.toUiState(syncNotice: FoundationSyncNotice? = FoundationSyncNotice.Loading): ParentInterestUiState {
    val settings = this?.interestSettings ?: InterestSettings()
    return ParentInterestUiState(
        hasFamily = this != null,
        familyName = this?.name.orEmpty(),
        currencyCode = this?.currency?.value ?: "EUR",
        enabled = settings.enabled,
        annualRateInput = formatInterestBasisPoints(settings.annualRateBasisPoints),
        postingDayInput = settings.postingDayOfMonth.toString(),
        lastPostedPeriodKey = settings.lastPostedPeriodKey,
        syncNotice = syncNotice,
    )
}
