package com.apptolast.fledge.domain.model

import kotlin.jvm.JvmInline
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class SavingsGoalId(val value: String) {
    init {
        require(value.isNotBlank()) { "Savings goal id cannot be blank." }
    }
}

@Serializable
enum class SavingsGoalStatus {
    Active,
    Completed,
    Archived,
}

@Serializable
data class SavingsGoal(
    val id: SavingsGoalId,
    val familyId: FamilyId,
    val childProfileId: ChildProfileId,
    val title: String,
    val targetCents: MoneyCents,
    val accountType: VirtualAccountType = VirtualAccountType.Goal,
    val iconKey: String? = null,
    val imageUri: String? = null,
    val status: SavingsGoalStatus = SavingsGoalStatus.Active,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        validateSavingsGoalFields(
            title = title,
            targetCents = targetCents,
            accountType = accountType,
            iconKey = iconKey,
            imageUri = imageUri,
        )
    }
}

data class SavingsGoalDraft(
    val familyId: FamilyId,
    val childProfileId: ChildProfileId,
    val title: String,
    val targetCents: MoneyCents,
    val iconKey: String? = null,
    val imageUri: String? = null,
) {
    init {
        validateSavingsGoalFields(
            title = title,
            targetCents = targetCents,
            accountType = VirtualAccountType.Goal,
            iconKey = iconKey,
            imageUri = imageUri,
        )
    }
}

fun SavingsGoalDraft.toSavingsGoal(id: SavingsGoalId, createdAt: Instant): SavingsGoal = SavingsGoal(
    id = id,
    familyId = familyId,
    childProfileId = childProfileId,
    title = title.trim(),
    targetCents = targetCents,
    accountType = VirtualAccountType.Goal,
    iconKey = iconKey?.trim()?.takeIf { it.isNotBlank() },
    imageUri = imageUri?.trim()?.takeIf { it.isNotBlank() },
    status = SavingsGoalStatus.Active,
    createdAt = createdAt,
    updatedAt = createdAt,
)

private fun validateSavingsGoalFields(
    title: String,
    targetCents: MoneyCents,
    accountType: VirtualAccountType,
    iconKey: String?,
    imageUri: String?,
) {
    require(title.isNotBlank()) { "Savings goal title cannot be blank." }
    require(title.length <= MAX_SAVINGS_GOAL_TITLE_LENGTH) {
        "Savings goal title cannot exceed $MAX_SAVINGS_GOAL_TITLE_LENGTH characters."
    }
    require(targetCents.value > 0) { "Savings goal target must be positive." }
    require(accountType == VirtualAccountType.Goal) { "Savings goals must use the Goal account." }
    require(!iconKey.isNullOrBlank() || !imageUri.isNullOrBlank()) {
        "Savings goal requires an icon or image."
    }
}

private const val MAX_SAVINGS_GOAL_TITLE_LENGTH = 80
