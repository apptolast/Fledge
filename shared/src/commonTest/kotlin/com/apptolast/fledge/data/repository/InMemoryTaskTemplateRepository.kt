package com.apptolast.fledge.data.repository

import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.TaskTemplate
import com.apptolast.fledge.domain.model.TaskTemplateDraft
import com.apptolast.fledge.domain.model.TaskTemplateId
import com.apptolast.fledge.domain.model.TaskTemplateSource
import com.apptolast.fledge.domain.repository.RepositorySyncStatus
import com.apptolast.fledge.domain.repository.TaskTemplateRepository
import com.apptolast.fledge.domain.service.InitialTaskTemplateCatalog
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class InMemoryTaskTemplateRepository : TaskTemplateRepository {
    private var templateCounter = 1
    private val mutableTemplates = MutableStateFlow<List<TaskTemplate>>(emptyList())
    private val mutableSyncStatus = MutableStateFlow<RepositorySyncStatus>(RepositorySyncStatus.Synced)

    override val syncStatus: StateFlow<RepositorySyncStatus> = mutableSyncStatus
    override val templates: StateFlow<List<TaskTemplate>> = mutableTemplates

    override suspend fun saveTemplate(draft: TaskTemplateDraft, createdAt: Instant): TaskTemplate {
        val template = draft.toTaskTemplate(
            id = TaskTemplateId("template-${templateCounter++}"),
            createdAt = createdAt,
        )
        upsert(template)
        return template
    }

    override suspend fun seedInitialSuggestionsForChild(
        familyId: FamilyId,
        child: ChildProfile,
        currentYear: Int,
        createdAt: Instant,
    ): List<TaskTemplate> {
        val existing = templates.value.filter {
            it.familyId == familyId &&
                it.source == TaskTemplateSource.InitialSuggestion &&
                it.sourceChildProfileId == child.id
        }
        val existingKeys = existing.mapNotNull { it.sourceKey }.toSet()
        val created = InitialTaskTemplateCatalog.suggestionsFor(child, familyId, currentYear)
            .filterNot { it.sourceKey in existingKeys }
            .map { draft ->
                draft.toTaskTemplate(
                    id = TaskTemplateId("initial-${child.id.value}-${requireNotNull(draft.sourceKey)}"),
                    createdAt = createdAt,
                )
            }

        created.forEach(::upsert)
        return (existing + created).sortedForCatalog()
    }

    override fun templatesForFamily(familyId: FamilyId): List<TaskTemplate> =
        templates.value.filter { it.familyId == familyId }.sortedForCatalog()

    private fun upsert(template: TaskTemplate) {
        mutableTemplates.value = (templates.value.filterNot { it.id == template.id } + template).sortedForCatalog()
    }

    private fun TaskTemplateDraft.toTaskTemplate(id: TaskTemplateId, createdAt: Instant = Clock.System.now()) =
        TaskTemplate(
            id = id,
            familyId = familyId,
            title = title,
            description = description,
            iconKey = iconKey,
            defaultValueCents = defaultValueCents,
            requiresPhoto = requiresPhoto,
            suggestedMinAge = suggestedMinAge,
            suggestedMaxAge = suggestedMaxAge,
            source = source,
            sourceChildProfileId = sourceChildProfileId,
            sourceKey = sourceKey,
            createdAt = createdAt,
            updatedAt = createdAt,
        )
}

private fun List<TaskTemplate>.sortedForCatalog(): List<TaskTemplate> =
    sortedWith(compareBy<TaskTemplate> { it.archived }.thenBy { it.title.lowercase() }.thenBy { it.id.value })
