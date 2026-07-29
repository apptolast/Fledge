package com.apptolast.fledge.domain.model

import kotlin.jvm.JvmInline
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class TaskTemplateId(val value: String) {
    init {
        require(value.isNotBlank()) { "Task template id cannot be blank." }
    }
}

@Serializable
enum class TaskTemplateSource {
    Custom,
    InitialSuggestion,
}

@Serializable
data class TaskTemplate(
    val id: TaskTemplateId,
    val familyId: FamilyId,
    val title: String,
    val description: String,
    val iconKey: String,
    val defaultValueCents: MoneyCents,
    val requiresPhoto: Boolean,
    val suggestedMinAge: Int? = null,
    val suggestedMaxAge: Int? = null,
    val source: TaskTemplateSource = TaskTemplateSource.Custom,
    val sourceChildProfileId: ChildProfileId? = null,
    val sourceKey: String? = null,
    val archived: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        validateTaskTemplateFields(
            title = title,
            description = description,
            iconKey = iconKey,
            defaultValueCents = defaultValueCents,
            suggestedMinAge = suggestedMinAge,
            suggestedMaxAge = suggestedMaxAge,
        )
    }
}

data class TaskTemplateDraft(
    val familyId: FamilyId,
    val title: String,
    val description: String,
    val iconKey: String,
    val defaultValueCents: MoneyCents,
    val requiresPhoto: Boolean,
    val suggestedMinAge: Int? = null,
    val suggestedMaxAge: Int? = null,
    val source: TaskTemplateSource = TaskTemplateSource.Custom,
    val sourceChildProfileId: ChildProfileId? = null,
    val sourceKey: String? = null,
) {
    init {
        validateTaskTemplateFields(
            title = title,
            description = description,
            iconKey = iconKey,
            defaultValueCents = defaultValueCents,
            suggestedMinAge = suggestedMinAge,
            suggestedMaxAge = suggestedMaxAge,
        )
    }
}

private fun validateTaskTemplateFields(
    title: String,
    description: String,
    iconKey: String,
    defaultValueCents: MoneyCents,
    suggestedMinAge: Int?,
    suggestedMaxAge: Int?,
) {
    require(title.isNotBlank()) { "Task template title cannot be blank." }
    require(description.isNotBlank()) { "Task template description cannot be blank." }
    require(iconKey.isNotBlank()) { "Task template icon cannot be blank." }
    require(defaultValueCents.value > 0) { "Task template default value must be positive." }
    require(suggestedMinAge == null || suggestedMinAge >= 0) { "Suggested minimum age cannot be negative." }
    require(suggestedMaxAge == null || suggestedMaxAge >= 0) { "Suggested maximum age cannot be negative." }
    if (suggestedMinAge != null && suggestedMaxAge != null) {
        require(suggestedMinAge <= suggestedMaxAge) { "Suggested age range is invalid." }
    }
}
