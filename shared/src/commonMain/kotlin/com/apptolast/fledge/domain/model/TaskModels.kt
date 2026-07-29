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

@Serializable
@JvmInline
value class TaskAssignmentId(val value: String) {
    init {
        require(value.isNotBlank()) { "Task assignment id cannot be blank." }
    }
}

@Serializable
enum class TaskRecurrence {
    Once,
    Daily,
    Weekly,
    Custom,
}

@Serializable
data class TaskAssignment(
    val id: TaskAssignmentId,
    val familyId: FamilyId,
    val taskTemplateId: TaskTemplateId,
    val title: String,
    val rewardCents: MoneyCents,
    val requiresPhoto: Boolean,
    val childProfileIds: List<ChildProfileId>,
    val recurrence: TaskRecurrence,
    val dueAt: Instant,
    val customIntervalDays: Int? = null,
    val active: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        validateTaskAssignmentFields(
            title = title,
            rewardCents = rewardCents,
            childProfileIds = childProfileIds,
            recurrence = recurrence,
            customIntervalDays = customIntervalDays,
        )
    }
}

data class TaskAssignmentDraft(
    val familyId: FamilyId,
    val taskTemplateId: TaskTemplateId,
    val title: String,
    val rewardCents: MoneyCents,
    val requiresPhoto: Boolean,
    val childProfileIds: List<ChildProfileId>,
    val recurrence: TaskRecurrence,
    val dueAt: Instant,
    val customIntervalDays: Int? = null,
) {
    init {
        validateTaskAssignmentFields(
            title = title,
            rewardCents = rewardCents,
            childProfileIds = childProfileIds,
            recurrence = recurrence,
            customIntervalDays = customIntervalDays,
        )
    }
}

@Serializable
@JvmInline
value class TaskInstanceId(val value: String) {
    init {
        require(value.isNotBlank()) { "Task instance id cannot be blank." }
    }
}

@Serializable
enum class TaskInstanceStatus {
    Pending,
    Submitted,
    Approved,
    Rejected,
    Expired,
}

@Serializable
data class TaskInstance(
    val id: TaskInstanceId,
    val familyId: FamilyId,
    val taskAssignmentId: TaskAssignmentId,
    val taskTemplateId: TaskTemplateId,
    val childProfileId: ChildProfileId,
    val title: String,
    val rewardCents: MoneyCents,
    val requiresPhoto: Boolean,
    val status: TaskInstanceStatus,
    val dueAt: Instant,
    val periodKey: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val submittedAt: Instant? = null,
    val reviewedAt: Instant? = null,
    val expiredAt: Instant? = null,
    val photoEvidenceUri: String? = null,
) {
    init {
        validateTaskInstanceFields(
            title = title,
            rewardCents = rewardCents,
            periodKey = periodKey,
            requiresPhoto = requiresPhoto,
            status = status,
            submittedAt = submittedAt,
            photoEvidenceUri = photoEvidenceUri,
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

fun TaskInstance.submittedForReview(
    childProfileId: ChildProfileId,
    photoEvidenceUri: String?,
    submittedAt: Instant,
): TaskInstance {
    require(this.childProfileId == childProfileId) { "Task instance belongs to a different child." }
    require(status == TaskInstanceStatus.Pending || status == TaskInstanceStatus.Rejected) {
        "Only pending or rejected task instances can be submitted."
    }
    val normalizedPhotoEvidenceUri = photoEvidenceUri?.trim()?.takeIf { it.isNotBlank() }
    require(!requiresPhoto || normalizedPhotoEvidenceUri != null) {
        "Photo evidence is required before submitting this task."
    }
    return copy(
        status = TaskInstanceStatus.Submitted,
        updatedAt = submittedAt,
        submittedAt = submittedAt,
        reviewedAt = null,
        expiredAt = null,
        photoEvidenceUri = normalizedPhotoEvidenceUri,
    )
}

private fun validateTaskInstanceFields(
    title: String,
    rewardCents: MoneyCents,
    periodKey: String,
    requiresPhoto: Boolean,
    status: TaskInstanceStatus,
    submittedAt: Instant?,
    photoEvidenceUri: String?,
) {
    require(title.isNotBlank()) { "Task instance title cannot be blank." }
    require(rewardCents.value > 0) { "Task instance reward must be positive." }
    require(periodKey.isNotBlank()) { "Task instance period key cannot be blank." }
    require(photoEvidenceUri == null || photoEvidenceUri.isNotBlank()) {
        "Task instance photo evidence cannot be blank."
    }
    if (status == TaskInstanceStatus.Submitted) {
        require(submittedAt != null) { "Submitted task instance must include submittedAt." }
        require(!requiresPhoto || photoEvidenceUri != null) {
            "Photo evidence is required before submitting this task."
        }
    }
}

private fun validateTaskAssignmentFields(
    title: String,
    rewardCents: MoneyCents,
    childProfileIds: List<ChildProfileId>,
    recurrence: TaskRecurrence,
    customIntervalDays: Int?,
) {
    require(title.isNotBlank()) { "Task assignment title cannot be blank." }
    require(rewardCents.value > 0) { "Task assignment reward must be positive." }
    require(childProfileIds.isNotEmpty()) { "Task assignment must target at least one child." }
    require(childProfileIds.distinct().size == childProfileIds.size) {
        "Task assignment cannot repeat the same child."
    }
    if (recurrence == TaskRecurrence.Custom) {
        require(customIntervalDays != null && customIntervalDays >= 1) {
            "Custom recurrence interval must be at least one day."
        }
    } else {
        require(customIntervalDays == null) {
            "Custom recurrence interval is only valid for custom recurrence."
        }
    }
}
