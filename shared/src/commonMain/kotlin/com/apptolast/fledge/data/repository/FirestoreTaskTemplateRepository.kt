package com.apptolast.fledge.data.repository

import com.apptolast.customlogin.domain.AuthProvider
import com.apptolast.fledge.data.remote.firebase.FirestoreProvider
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.TaskTemplate
import com.apptolast.fledge.domain.model.TaskTemplateDraft
import com.apptolast.fledge.domain.model.TaskTemplateId
import com.apptolast.fledge.domain.model.TaskTemplateSource
import com.apptolast.fledge.domain.repository.RepositorySyncStatus
import com.apptolast.fledge.domain.repository.TaskTemplateRepository
import com.apptolast.fledge.domain.service.InitialTaskTemplateCatalog
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

class FirestoreTaskTemplateRepository(
    private val firestoreProvider: FirestoreProvider,
    private val authProvider: AuthProvider,
) : TaskTemplateRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var syncJob: Job? = null
    private val mutableSyncStatus = MutableStateFlow<RepositorySyncStatus>(RepositorySyncStatus.Loading)
    private val mutableTemplates = MutableStateFlow<List<TaskTemplate>>(emptyList())

    override val syncStatus: StateFlow<RepositorySyncStatus> = mutableSyncStatus
    override val templates: StateFlow<List<TaskTemplate>> = mutableTemplates

    init {
        scope.launch {
            authProvider.authenticatedFamilyIds(firestoreProvider).collectLatest { familyId ->
                syncJob?.cancelAndJoin()
                if (familyId == null) {
                    mutableTemplates.value = emptyList()
                    mutableSyncStatus.value = RepositorySyncStatus.Synced
                } else {
                    mutableSyncStatus.value = RepositorySyncStatus.Loading
                    syncJob = launch { bindTaskTemplates(familyId) }
                }
            }
        }
    }

    private suspend fun bindTaskTemplates(familyId: FamilyId) = coroutineScope {
        val jobs = listOf(
            launch {
                taskTemplateCollection(familyId).snapshots(includeMetadataChanges = true)
                    .catch { error ->
                        mutableSyncStatus.value = error.toRepositorySyncError()
                    }
                    .collect { snapshot ->
                        mutableSyncStatus.value = snapshot.metadata.toRepositorySyncStatus()
                        mutableTemplates.value = snapshot.documents
                            .filter { it.exists }
                            .mapNotNull { runCatching { it.toTaskTemplate() }.getOrNull() }
                            .sortedForCatalog()
                    }
            },
        )
        jobs.joinAll()
    }

    override suspend fun saveTemplate(draft: TaskTemplateDraft, createdAt: Instant): TaskTemplate {
        require(authProvider.currentFamilyId(firestoreProvider) == draft.familyId) {
            "A signed-in parent can only save task templates for the active family."
        }
        val ref = taskTemplateCollection(draft.familyId).document
        val template = draft.toTaskTemplate(
            id = TaskTemplateId(ref.id),
            createdAt = createdAt,
        )
        ref.set(template.toFirestoreMap())
        upsertLocal(template)
        return template
    }

    override suspend fun seedInitialSuggestionsForChild(
        familyId: FamilyId,
        child: ChildProfile,
        currentYear: Int,
        createdAt: Instant,
    ): List<TaskTemplate> {
        require(authProvider.currentFamilyId(firestoreProvider) == familyId) {
            "A signed-in parent can only seed task templates for the active family."
        }

        val collection = taskTemplateCollection(familyId)
        val knownTemplates = (
            templates.value +
                collection.get().documents
                    .filter { it.exists }
                    .mapNotNull { runCatching { it.toTaskTemplate() }.getOrNull() }
            ).distinctBy { it.id }

        val existing = knownTemplates.filter {
            it.familyId == familyId &&
                it.source == TaskTemplateSource.InitialSuggestion &&
                it.sourceChildProfileId == child.id
        }
        val existingKeys = existing.mapNotNull { it.sourceKey }.toSet()
        val created = InitialTaskTemplateCatalog.suggestionsFor(child, familyId, currentYear)
            .filterNot { it.sourceKey in existingKeys }
            .map { draft ->
                val id = TaskTemplateId("initial-${child.id.value}-${requireNotNull(draft.sourceKey)}")
                val ref = collection.document(id.value)
                val remoteExisting = ref.get().takeIf { it.exists }?.let { snapshot ->
                    runCatching { snapshot.toTaskTemplate() }.getOrNull()
                }
                remoteExisting ?: draft.toTaskTemplate(id = id, createdAt = createdAt).also { template ->
                    ref.set(template.toFirestoreMap())
                }
            }

        val seeded = (existing + created).distinctBy { it.id }.sortedForCatalog()
        mutableTemplates.value = (templates.value + seeded).distinctBy { it.id }.sortedForCatalog()
        return seeded
    }

    override fun templatesForFamily(familyId: FamilyId): List<TaskTemplate> =
        templates.value.filter { it.familyId == familyId }.sortedForCatalog()

    private fun taskTemplateCollection(familyId: FamilyId) = firestoreProvider
        .firestoreOrThrow()
        .collection(FAMILIES_COLLECTION)
        .document(familyId.value)
        .collection(TASK_TEMPLATES_COLLECTION)

    private fun upsertLocal(template: TaskTemplate) {
        mutableTemplates.value = (templates.value.filterNot { it.id == template.id } + template).sortedForCatalog()
    }

    private fun TaskTemplateDraft.toTaskTemplate(id: TaskTemplateId, createdAt: Instant): TaskTemplate = TaskTemplate(
        id = id,
        familyId = familyId,
        title = title.trim(),
        description = description.trim(),
        iconKey = iconKey.trim(),
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

    private companion object {
        const val TASK_TEMPLATES_COLLECTION = "taskTemplates"
    }
}

internal fun List<TaskTemplate>.sortedForCatalog(): List<TaskTemplate> =
    sortedWith(compareBy<TaskTemplate> { it.archived }.thenBy { it.title.lowercase() }.thenBy { it.id.value })
