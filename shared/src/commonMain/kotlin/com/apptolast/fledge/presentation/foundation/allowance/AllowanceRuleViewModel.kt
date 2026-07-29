package com.apptolast.fledge.presentation.foundation.allowance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.fledge.domain.model.AllowanceDay
import com.apptolast.fledge.domain.model.AllowanceFrequency
import com.apptolast.fledge.domain.model.AllowanceRule
import com.apptolast.fledge.domain.model.AllowanceRuleDraft
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.LedgerConcept
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.VirtualAccountType
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import com.apptolast.fledge.domain.repository.MoneyFlowRepository
import com.apptolast.fledge.domain.repository.RepositorySyncStatus
import com.apptolast.fledge.domain.service.AllowanceSchedule
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

data class AllowanceRuleUiState(
    val child: ChildProfile? = null,
    val frequency: AllowanceFrequency = AllowanceFrequency.Weekly,
    val dayInput: String = "1",
    val amountInput: String = "",
    val concept: String = "Paga",
    val error: AllowanceRuleError? = null,
    val savedRule: AllowanceRule? = null,
    val syncNotice: FoundationSyncNotice? = FoundationSyncNotice.Loading,
    val operationError: FoundationOperationError? = null,
    val isSaving: Boolean = false,
) {
    val canSubmit: Boolean
        get() = !isSaving && child != null && dayInput.isNotBlank() && amountInput.isNotBlank() && concept.isNotBlank()
}

enum class AllowanceRuleError {
    MissingFamily,
    MissingChild,
    MissingConcept,
    InvalidAmount,
    InvalidDay,
}

class AllowanceRuleViewModel(
    private val familyRepository: FamilyFoundationRepository,
    private val moneyFlowRepository: MoneyFlowRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(AllowanceRuleUiState())
    private var loadedChildProfileId: ChildProfileId? = null
    val uiState: StateFlow<AllowanceRuleUiState> = mutableUiState

    init {
        viewModelScope.launch {
            combine(familyRepository.syncStatus, moneyFlowRepository.syncStatus) { familyStatus, moneyStatus ->
                listOf(familyStatus, moneyStatus)
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
    }

    fun load(childProfileId: ChildProfileId) {
        loadedChildProfileId = childProfileId
        refreshSelectedChild()
    }

    fun selectFrequency(frequency: AllowanceFrequency) {
        mutableUiState.update {
            it.copy(
                frequency = frequency,
                dayInput = if (frequency == AllowanceFrequency.Weekly) "1" else "31",
                error = null,
            )
        }
    }

    fun updateDay(input: String) {
        mutableUiState.update { it.copy(dayInput = input.filter(Char::isDigit), error = null, operationError = null) }
    }

    fun updateAmount(input: String) {
        mutableUiState.update { it.copy(amountInput = input, error = null, operationError = null) }
    }

    fun updateConcept(input: String) {
        mutableUiState.update { it.copy(concept = input, error = null, operationError = null) }
    }

    suspend fun submit(): Boolean {
        val state = mutableUiState.value
        val family = familyRepository.activeFamily.value
        val child = state.child
        val concept = state.concept.trim()
        val amountCents = parseAmountCents(state.amountInput)
        val dayValue = state.dayInput.toIntOrNull()

        when {
            family == null -> {
                mutableUiState.update { it.copy(error = AllowanceRuleError.MissingFamily, operationError = null) }
                return false
            }
            child == null -> {
                mutableUiState.update { it.copy(error = AllowanceRuleError.MissingChild, operationError = null) }
                return false
            }
            concept.isBlank() -> {
                mutableUiState.update { it.copy(error = AllowanceRuleError.MissingConcept, operationError = null) }
                return false
            }
            amountCents == null || amountCents <= 0L -> {
                mutableUiState.update { it.copy(error = AllowanceRuleError.InvalidAmount, operationError = null) }
                return false
            }
            dayValue == null -> {
                mutableUiState.update { it.copy(error = AllowanceRuleError.InvalidDay, operationError = null) }
                return false
            }
        }

        val day = runCatching { AllowanceDay(dayValue) }.getOrNull()
        if (day == null || state.frequency == AllowanceFrequency.Weekly && day.value > 7) {
            mutableUiState.update { it.copy(error = AllowanceRuleError.InvalidDay, operationError = null) }
            return false
        }

        val now = Clock.System.now()
        mutableUiState.update { it.copy(isSaving = true, operationError = null) }
        return runCatching {
            moneyFlowRepository.saveAllowanceRule(
                draft = AllowanceRuleDraft(
                    familyId = family.id,
                    childProfileId = child.id,
                    accountType = VirtualAccountType.Main,
                    frequency = state.frequency,
                    day = day,
                    amountCents = MoneyCents(amountCents),
                    concept = LedgerConcept(concept),
                    timeZone = family.timeZone,
                ),
                nextRunAt = AllowanceSchedule.nextRunAt(
                    frequency = state.frequency,
                    day = day,
                    from = now,
                    timeZone = family.timeZone,
                ),
                createdAt = now,
            )
        }.fold(
            onSuccess = { rule ->
                mutableUiState.update { it.copy(error = null, savedRule = rule, isSaving = false) }
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
                    shouldShowMissingChild -> AllowanceRuleError.MissingChild
                    state.error == AllowanceRuleError.MissingChild -> null
                    else -> state.error
                },
            )
        }
    }
}
