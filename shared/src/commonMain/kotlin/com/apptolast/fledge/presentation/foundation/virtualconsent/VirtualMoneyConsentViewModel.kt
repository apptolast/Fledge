package com.apptolast.fledge.presentation.foundation.virtualconsent

import androidx.lifecycle.ViewModel
import com.apptolast.fledge.domain.model.VirtualMoneyConsent
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class VirtualMoneyConsentUiState(
    val acceptedTerms: Boolean = false,
    val recordedConsent: VirtualMoneyConsent? = null,
)

class VirtualMoneyConsentViewModel(private val repository: FamilyFoundationRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        VirtualMoneyConsentUiState(recordedConsent = repository.virtualMoneyConsent.value),
    )
    val uiState: StateFlow<VirtualMoneyConsentUiState> = mutableUiState

    fun updateAcceptedTerms(value: Boolean) {
        mutableUiState.update { it.copy(acceptedTerms = value) }
    }

    suspend fun submit(): Boolean {
        if (!mutableUiState.value.acceptedTerms) return false

        val consent = repository.recordVirtualMoneyConsent()
        mutableUiState.update { it.copy(recordedConsent = consent) }
        return true
    }
}
