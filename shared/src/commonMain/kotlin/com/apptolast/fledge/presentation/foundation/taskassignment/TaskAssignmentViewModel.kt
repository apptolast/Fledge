package com.apptolast.fledge.presentation.foundation.taskassignment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.Family
import com.apptolast.fledge.domain.model.TaskAssignment
import com.apptolast.fledge.domain.model.TaskAssignmentDraft
import com.apptolast.fledge.domain.model.TaskRecurrence
import com.apptolast.fledge.domain.model.TaskTemplate
import com.apptolast.fledge.domain.model.TaskTemplateId
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import com.apptolast.fledge.domain.repository.TaskAssignmentRepository
import com.apptolast.fledge.domain.repository.TaskTemplateRepository
import com.apptolast.fledge.presentation.foundation.FoundationOperationError
import com.apptolast.fledge.presentation.foundation.FoundationSyncNotice
import com.apptolast.fledge.presentation.foundation.toFoundationOperationError
import com.apptolast.fledge.presentation.foundation.toFoundationSyncNotice
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskAssignmentUiState(
    val family: Family? = null,
    val templates: List<TaskTemplate> = emptyList(),
    val children: List<ChildProfile> = emptyList(),
    val selectedTemplateId: TaskTemplateId? = null,
    val selectedChildProfileIds: List<ChildProfileId> = emptyList(),
    val recurrence: TaskRecurrence = TaskRecurrence.Daily,
    val dueAt: Instant = defaultTaskAssignmentDueAt(),
    val customIntervalDaysInput: String = "3",
    val error: TaskAssignmentError? = null,
    val savedAssignment: TaskAssignment? = null,
    val syncNotice: FoundationSyncNotice? = FoundationSyncNotice.Loading,
    val operationError: FoundationOperationError? = null,
    val isSaving: Boolean = false,
) {
    val selectedTemplate: TaskTemplate?
        get() = templates.firstOrNull { it.id == selectedTemplateId }

    val canSubmit: Boolean
        get() = !isSaving && family != null && selectedTemplate != null && selectedChildProfileIds.isNotEmpty()
}

enum class TaskAssignmentError {
    MissingFamily,
    MissingTemplate,
    MissingChildren,
    InvalidCustomInterval,
}

class TaskAssignmentViewModel(
    private val familyRepository: FamilyFoundationRepository,
    private val taskTemplateRepository: TaskTemplateRepository,
    private val taskAssignmentRepository: TaskAssignmentRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(TaskAssignmentUiState())
    val uiState: StateFlow<TaskAssignmentUiState> = mutableUiState

    init {
        viewModelScope.launch {
            combine(
                familyRepository.syncStatus,
                taskTemplateRepository.syncStatus,
                taskAssignmentRepository.syncStatus,
            ) { familyStatus, templateStatus, assignmentStatus ->
                listOf(familyStatus, templateStatus, assignmentStatus)
            }.collect { statuses ->
                mutableUiState.update { state ->
                    state.copy(syncNotice = statuses.toFoundationSyncNotice(state.hasKnownData()))
                }
                refreshAvailableData()
            }
        }
        viewModelScope.launch {
            combine(
                familyRepository.activeFamily,
                familyRepository.children,
                taskTemplateRepository.templates,
            ) { _, _, _ -> Unit }.collect {
                refreshAvailableData()
            }
        }
    }

    fun load() {
        refreshAvailableData()
    }

    fun selectTemplate(templateId: TaskTemplateId) {
        mutableUiState.update { state ->
            val knownTemplateId = templateId.takeIf { id -> state.templates.any { it.id == id } }
            state.copy(selectedTemplateId = knownTemplateId, error = null, operationError = null)
        }
    }

    fun useNextSuggestedTemplate() {
        mutableUiState.update { state ->
            if (state.templates.isEmpty()) return@update state
            val currentIndex = state.templates.indexOfFirst { it.id == state.selectedTemplateId }
            val nextIndex = if (currentIndex == -1 || currentIndex == state.templates.lastIndex) 0 else currentIndex + 1
            state.copy(selectedTemplateId = state.templates[nextIndex].id, error = null, operationError = null)
        }
    }

    fun toggleChild(childProfileId: ChildProfileId) {
        mutableUiState.update { state ->
            val current = state.selectedChildProfileIds.toSet()
            val selected = if (childProfileId in current) {
                current - childProfileId
            } else {
                current + childProfileId
            }
            val orderedSelection = state.children
                .map { it.id }
                .filter { it in selected }
            state.copy(selectedChildProfileIds = orderedSelection, error = null, operationError = null)
        }
    }

    fun selectRecurrence(recurrence: TaskRecurrence) {
        mutableUiState.update {
            it.copy(
                recurrence = recurrence,
                error = null,
                operationError = null,
            )
        }
    }

    fun updateDueAt(dueAt: Instant) {
        mutableUiState.update { it.copy(dueAt = dueAt, error = null, operationError = null) }
    }

    fun updateCustomIntervalDays(input: String) {
        mutableUiState.update {
            it.copy(customIntervalDaysInput = input.filter(Char::isDigit), error = null, operationError = null)
        }
    }

    suspend fun submit(): Boolean {
        val state = mutableUiState.value
        val family = familyRepository.activeFamily.value
        val template = state.selectedTemplate
        val customIntervalDays = if (state.recurrence == TaskRecurrence.Custom) {
            state.customIntervalDaysInput.toIntOrNull()
        } else {
            null
        }

        when {
            family == null -> {
                mutableUiState.update { it.copy(error = TaskAssignmentError.MissingFamily, operationError = null) }
                return false
            }
            template == null -> {
                mutableUiState.update { it.copy(error = TaskAssignmentError.MissingTemplate, operationError = null) }
                return false
            }
            state.selectedChildProfileIds.isEmpty() -> {
                mutableUiState.update { it.copy(error = TaskAssignmentError.MissingChildren, operationError = null) }
                return false
            }
            state.recurrence == TaskRecurrence.Custom && (customIntervalDays == null || customIntervalDays < 1) -> {
                mutableUiState.update {
                    it.copy(error = TaskAssignmentError.InvalidCustomInterval, operationError = null)
                }
                return false
            }
        }

        mutableUiState.update { it.copy(isSaving = true, operationError = null) }
        return runCatching {
            taskAssignmentRepository.saveAssignment(
                draft = TaskAssignmentDraft(
                    familyId = family.id,
                    taskTemplateId = template.id,
                    childProfileIds = state.selectedChildProfileIds,
                    recurrence = state.recurrence,
                    dueAt = state.dueAt,
                    customIntervalDays = customIntervalDays,
                ),
                createdAt = Clock.System.now(),
            )
        }.fold(
            onSuccess = { assignment ->
                mutableUiState.update {
                    it.copy(
                        error = null,
                        savedAssignment = assignment,
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

    private fun refreshAvailableData() {
        val family = familyRepository.activeFamily.value
        val templates = family?.let { taskTemplateRepository.templatesForFamily(it.id) }
            ?.filterNot { it.archived }
            .orEmpty()
        val children = familyRepository.children.value

        mutableUiState.update { state ->
            val selectedTemplateId = state.selectedTemplateId
                ?.takeIf { selected -> templates.any { it.id == selected } }
                ?: templates.firstOrNull()?.id
            val knownChildIds = children.map { it.id }.toSet()
            state.copy(
                family = family,
                templates = templates,
                children = children,
                selectedTemplateId = selectedTemplateId,
                selectedChildProfileIds = state.selectedChildProfileIds.filter { it in knownChildIds },
            )
        }
    }

    private fun TaskAssignmentUiState.hasKnownData(): Boolean =
        family != null || templates.isNotEmpty() || children.isNotEmpty()
}

private fun defaultTaskAssignmentDueAt(): Instant = Clock.System.now() + 1.days
