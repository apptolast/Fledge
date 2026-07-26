package com.apptolast.fledge.presentation.foundation.pairing

import androidx.lifecycle.ViewModel
import com.apptolast.fledge.domain.model.ChildDevice
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.PairingSession
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class PairingUiState(
    val deviceLabel: String = "",
    val pairingSession: PairingSession? = null,
    val pairedDevice: ChildDevice? = null,
    val error: PairingError? = null,
)

enum class PairingError {
    MissingPairingCode,
}

class PairingViewModel(
    private val repository: FamilyFoundationRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(PairingUiState())
    val uiState: StateFlow<PairingUiState> = mutableUiState

    fun updateDeviceLabel(value: String) {
        mutableUiState.update { it.copy(deviceLabel = value, error = null) }
    }

    suspend fun startPairing(childProfileId: ChildProfileId) {
        mutableUiState.update {
            it.copy(
                pairingSession = repository.startPairing(childProfileId),
                pairedDevice = null,
                error = null,
            )
        }
    }

    suspend fun registerDevice(defaultDeviceLabel: String): Boolean {
        val state = mutableUiState.value
        val session = state.pairingSession
        if (session == null) {
            mutableUiState.update { it.copy(error = PairingError.MissingPairingCode) }
            return false
        }

        val device = repository.registerChildDevice(
            pairingCode = session.code,
            label = state.deviceLabel.ifBlank { defaultDeviceLabel },
        )
        mutableUiState.update { it.copy(pairedDevice = device, error = null) }
        return true
    }
}
