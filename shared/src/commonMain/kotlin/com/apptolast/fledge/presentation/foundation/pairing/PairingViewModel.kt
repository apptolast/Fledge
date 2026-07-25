package com.apptolast.fledge.presentation.foundation.pairing

import androidx.lifecycle.ViewModel
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.PairingSession
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class PairingUiState(
    val pairingSession: PairingSession? = null,
)

class PairingViewModel(
    private val repository: FamilyFoundationRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(PairingUiState())
    val uiState: StateFlow<PairingUiState> = mutableUiState

    suspend fun startPairing(childProfileId: ChildProfileId) {
        mutableUiState.update { it.copy(pairingSession = repository.startPairing(childProfileId)) }
    }
}
