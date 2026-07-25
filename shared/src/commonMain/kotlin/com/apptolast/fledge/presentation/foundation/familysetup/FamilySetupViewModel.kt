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
}

class FamilySetupViewModel(
    private val repository: FamilyFoundationRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(FamilySetupUiState())
    val uiState: StateFlow<FamilySetupUiState> = mutableUiState

    fun updateFamilyName(value: String) {
        mutableUiState.update { it.copy(familyName = value) }
    }

    fun updateCurrency(value: CurrencyCode) {
        mutableUiState.update { it.copy(currency = value) }
    }

    fun updateTimeZone(value: TimeZoneId) {
        mutableUiState.update { it.copy(timeZone = value) }
    }

    suspend fun submit() {
        val state = mutableUiState.value
        if (!state.canSubmit) return
        val family = repository.createFamily(
            name = state.familyName,
            currency = state.currency,
            timeZone = state.timeZone,
        )
        mutableUiState.update { it.copy(createdFamily = family) }
    }
}
