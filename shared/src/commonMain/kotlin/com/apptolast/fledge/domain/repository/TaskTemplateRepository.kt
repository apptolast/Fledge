package com.apptolast.fledge.domain.repository

import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.TaskTemplate
import com.apptolast.fledge.domain.model.TaskTemplateDraft
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.StateFlow

interface TaskTemplateRepository {
    val syncStatus: StateFlow<RepositorySyncStatus>
    val templates: StateFlow<List<TaskTemplate>>

    suspend fun saveTemplate(draft: TaskTemplateDraft, createdAt: Instant = Clock.System.now()): TaskTemplate

    suspend fun seedInitialSuggestionsForChild(
        familyId: FamilyId,
        child: ChildProfile,
        currentYear: Int,
        createdAt: Instant = Clock.System.now(),
    ): List<TaskTemplate>

    fun templatesForFamily(familyId: FamilyId): List<TaskTemplate>
}
