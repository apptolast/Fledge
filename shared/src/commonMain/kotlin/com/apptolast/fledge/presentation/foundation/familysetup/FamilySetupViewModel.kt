package com.apptolast.fledge.presentation.foundation.familysetup

import androidx.lifecycle.ViewModel
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.Family
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class FamilySetupUiState(
    val familyName: String = "",
    val currency: CurrencyCode = CurrencyCode("EUR"),
    val timeZone: TimeZoneId = TimeZoneId("Europe/Madrid"),
    val createdFamily: Family? = null,
) {
    val canSubmit: Boolean = familyName.isNotBlank()
    val isLocked: Boolean = createdFamily != null
}

class FamilySetupViewModel(
    private val repository: FamilyFoundationRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(FamilySetupUiState())
    val uiState: StateFlow<FamilySetupUiState> = mutableUiState

    fun updateFamilyName(value: String) {
        if (mutableUiState.value.isLocked) return
        mutableUiState.update { it.copy(familyName = value) }
    }

    fun updateCurrency(value: CurrencyCode) {
        if (mutableUiState.value.isLocked) return
        mutableUiState.update { it.copy(currency = value) }
    }

    fun updateTimeZone(value: TimeZoneId) {
        if (mutableUiState.value.isLocked) return
        mutableUiState.update { it.copy(timeZone = value) }
    }

    suspend fun submit(): Boolean {
        val state = mutableUiState.value
        if (state.createdFamily != null) return true
        if (!state.canSubmit) return false
        val family = repository.createFamily(
            name = state.familyName,
            currency = state.currency,
            timeZone = state.timeZone,
        )
        mutableUiState.update { it.copy(createdFamily = family) }
        return true
    }
}
