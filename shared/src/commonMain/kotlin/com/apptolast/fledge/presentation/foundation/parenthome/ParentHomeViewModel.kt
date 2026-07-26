package com.apptolast.fledge.presentation.foundation.parenthome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.SetupAction
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ParentHomeUiState(
    val children: List<ChildProfile> = emptyList(),
    val setupActions: List<SetupAction> = defaultSetupActions,
)

class ParentHomeViewModel(
    repository: FamilyFoundationRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        ParentHomeUiState(children = repository.children.value)
    )
    val uiState: StateFlow<ParentHomeUiState> = mutableUiState

    init {
        viewModelScope.launch {
            repository.children.collect { children ->
                mutableUiState.update { it.copy(children = children) }
            }
        }
    }
}

private val defaultSetupActions = listOf(
    SetupAction(id = "add-child", label = "Anadir hijo"),
    SetupAction(id = "pair-device", label = "Emparejar dispositivo"),
    SetupAction(id = "parental-gate", label = "Configurar parental gate"),
)
