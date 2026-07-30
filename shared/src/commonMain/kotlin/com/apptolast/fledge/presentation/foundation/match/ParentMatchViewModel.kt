package com.apptolast.fledge.presentation.foundation.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.fledge.domain.model.Family
import com.apptolast.fledge.domain.model.MatchSettings
import com.apptolast.fledge.domain.model.MatchSettingsDraft
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import com.apptolast.fledge.presentation.foundation.FoundationOperationError
import com.apptolast.fledge.presentation.foundation.FoundationSyncNotice
import com.apptolast.fledge.presentation.foundation.manualadjustment.parseAmountCents
import com.apptolast.fledge.presentation.foundation.toFoundationOperationError
import com.apptolast.fledge.presentation.foundation.toFoundationSyncNotice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ParentMatchUiState(
    val hasFamily: Boolean = false,
    val familyName: String = "",
    val currencyCode: String = "EUR",
    val enabled: Boolean = false,
    val matchRateInput: String = "0,00",
    val maxMatchInput: String = "0,00",
    val error: ParentMatchError? = null,
    val savedSettings: MatchSettings? = null,
    val syncNotice: FoundationSyncNotice? = FoundationSyncNotice.Loading,
    val operationError: FoundationOperationError? = null,
    val isSaving: Boolean = false,
) {
    val canSubmit: Boolean
        get() = !isSaving &&
            hasFamily &&
            (!enabled || (matchBasisPoints() != null && maxMatchCents() != null))

    fun matchBasisPoints(): Int? = parseMatchBasisPoints(matchRateInput)

    fun maxMatchCents(): Long? = parseAmountCents(maxMatchInput)
}

enum class ParentMatchError {
    MissingFamily,
    InvalidRate,
    InvalidCap,
}

class ParentMatchViewModel(private val repository: FamilyFoundationRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow(repository.activeFamily.value.toUiState())
    private var userEdited = false

    val uiState: StateFlow<ParentMatchUiState> = mutableUiState

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
            state.copy(
                enabled = enabled,
                matchRateInput = if (enabled && state.matchBasisPoints() == 0) "100,00" else state.matchRateInput,
                maxMatchInput = if (enabled && state.maxMatchCents() == 0L) "5,00" else state.maxMatchInput,
                error = null,
                savedSettings = null,
                operationError = null,
            )
        }
    }

    fun updateMatchRate(input: String) {
        userEdited = true
        mutableUiState.update {
            it.copy(matchRateInput = input, error = null, savedSettings = null, operationError = null)
        }
    }

    fun updateMaxMatch(input: String) {
        userEdited = true
        mutableUiState.update {
            it.copy(maxMatchInput = input, error = null, savedSettings = null, operationError = null)
        }
    }

    suspend fun submit(): Boolean {
        val state = mutableUiState.value
        val matchBasisPoints = state.matchBasisPoints()
        val maxMatchCents = state.maxMatchCents()
        when {
            !state.hasFamily || repository.activeFamily.value == null -> {
                mutableUiState.update { it.copy(error = ParentMatchError.MissingFamily, operationError = null) }
                return false
            }
            state.enabled && (matchBasisPoints == null || matchBasisPoints == 0) -> {
                mutableUiState.update { it.copy(error = ParentMatchError.InvalidRate, operationError = null) }
                return false
            }
            state.enabled && (maxMatchCents == null || maxMatchCents <= 0L) -> {
                mutableUiState.update { it.copy(error = ParentMatchError.InvalidCap, operationError = null) }
                return false
            }
        }

        mutableUiState.update { it.copy(isSaving = true, operationError = null) }
        return runCatching {
            repository.updateMatchSettings(
                MatchSettingsDraft(
                    enabled = state.enabled,
                    matchBasisPoints = if (state.enabled) requireNotNull(matchBasisPoints) else 0,
                    maxMatchCents = if (state.enabled) requireNotNull(maxMatchCents) else 0,
                ),
            )
        }.fold(
            onSuccess = { settings ->
                userEdited = false
                mutableUiState.update {
                    it.copy(
                        enabled = settings.enabled,
                        matchRateInput = formatMatchBasisPoints(settings.matchBasisPoints),
                        maxMatchInput = formatAmountInput(settings.maxMatchCents),
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

internal fun parseMatchBasisPoints(input: String): Int? {
    val match = Regex("""^\s*(\d{1,3})([,.](\d{0,2}))?\s*$""").matchEntire(input) ?: return null
    val whole = match.groupValues[1].toIntOrNull() ?: return null
    val fraction = match.groupValues.getOrNull(3).orEmpty().padEnd(2, '0').take(2).toIntOrNull() ?: 0
    val basisPoints = whole * 100 + fraction
    return basisPoints.takeIf { it in 0..10_000 }
}

internal fun formatMatchBasisPoints(basisPoints: Int): String {
    val whole = basisPoints / 100
    val fraction = (basisPoints % 100).toString().padStart(2, '0')
    return "$whole,$fraction"
}

private fun formatAmountInput(cents: Long): String {
    val whole = cents / 100
    val fraction = (cents % 100).toString().padStart(2, '0')
    return "$whole,$fraction"
}

private fun Family?.toUiState(syncNotice: FoundationSyncNotice? = FoundationSyncNotice.Loading): ParentMatchUiState {
    val settings = this?.matchSettings ?: MatchSettings()
    return ParentMatchUiState(
        hasFamily = this != null,
        familyName = this?.name.orEmpty(),
        currencyCode = this?.currency?.value ?: "EUR",
        enabled = settings.enabled,
        matchRateInput = formatMatchBasisPoints(settings.matchBasisPoints),
        maxMatchInput = formatAmountInput(settings.maxMatchCents),
        syncNotice = syncNotice,
    )
}
