package com.apptolast.fledge.presentation.foundation.allowance

import androidx.lifecycle.ViewModel
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
import com.apptolast.fledge.domain.service.AllowanceSchedule
import com.apptolast.fledge.presentation.foundation.manualadjustment.parseAmountCents
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class AllowanceRuleUiState(
    val child: ChildProfile? = null,
    val frequency: AllowanceFrequency = AllowanceFrequency.Weekly,
    val dayInput: String = "1",
    val amountInput: String = "",
    val concept: String = "Paga",
    val error: AllowanceRuleError? = null,
    val savedRule: AllowanceRule? = null,
) {
    val canSubmit: Boolean
        get() = child != null && dayInput.isNotBlank() && amountInput.isNotBlank() && concept.isNotBlank()
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
    val uiState: StateFlow<AllowanceRuleUiState> = mutableUiState

    fun load(childProfileId: ChildProfileId) {
        val child = familyRepository.children.value.firstOrNull { it.id == childProfileId }
        mutableUiState.update { state ->
            state.copy(
                child = child,
                error = if (child == null) AllowanceRuleError.MissingChild else null,
            )
        }
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
        mutableUiState.update { it.copy(dayInput = input.filter(Char::isDigit), error = null) }
    }

    fun updateAmount(input: String) {
        mutableUiState.update { it.copy(amountInput = input, error = null) }
    }

    fun updateConcept(input: String) {
        mutableUiState.update { it.copy(concept = input, error = null) }
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
                mutableUiState.update { it.copy(error = AllowanceRuleError.MissingFamily) }
                return false
            }
            child == null -> {
                mutableUiState.update { it.copy(error = AllowanceRuleError.MissingChild) }
                return false
            }
            concept.isBlank() -> {
                mutableUiState.update { it.copy(error = AllowanceRuleError.MissingConcept) }
                return false
            }
            amountCents == null || amountCents <= 0L -> {
                mutableUiState.update { it.copy(error = AllowanceRuleError.InvalidAmount) }
                return false
            }
            dayValue == null -> {
                mutableUiState.update { it.copy(error = AllowanceRuleError.InvalidDay) }
                return false
            }
        }

        val day = runCatching { AllowanceDay(dayValue) }.getOrNull()
        if (day == null || state.frequency == AllowanceFrequency.Weekly && day.value > 7) {
            mutableUiState.update { it.copy(error = AllowanceRuleError.InvalidDay) }
            return false
        }

        val now = Clock.System.now()
        val rule = moneyFlowRepository.saveAllowanceRule(
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
        mutableUiState.update { it.copy(error = null, savedRule = rule) }
        return true
    }
}
