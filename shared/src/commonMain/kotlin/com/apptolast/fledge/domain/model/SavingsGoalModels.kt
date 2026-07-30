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
enum class MoneyPotType {
    Spend,
    Save,
    Give,
}

@Serializable
data class SavingsGoal(
    val id: SavingsGoalId,
    val familyId: FamilyId,
    val childProfileId: ChildProfileId,
    val title: String,
    val targetCents: MoneyCents,
    val accountType: VirtualAccountType = VirtualAccountType.Goal,
    val potType: MoneyPotType = MoneyPotType.Save,
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
            potType = potType,
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
    val potType: MoneyPotType = MoneyPotType.Save,
    val iconKey: String? = null,
    val imageUri: String? = null,
) {
    init {
        validateSavingsGoalFields(
            title = title,
            targetCents = targetCents,
            accountType = potType.goalAccountType(),
            potType = potType,
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
    accountType = potType.goalAccountType(),
    potType = potType,
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
    potType: MoneyPotType,
    iconKey: String?,
    imageUri: String?,
) {
    require(title.isNotBlank()) { "Savings goal title cannot be blank." }
    require(title.length <= MAX_SAVINGS_GOAL_TITLE_LENGTH) {
        "Savings goal title cannot exceed $MAX_SAVINGS_GOAL_TITLE_LENGTH characters."
    }
    require(targetCents.value > 0) { "Savings goal target must be positive." }
    require(potType != MoneyPotType.Spend) { "Savings goals cannot use the Spend pot." }
    require(accountType == potType.goalAccountType()) { "Savings goal account must match its pot type." }
    require(!iconKey.isNullOrBlank() || !imageUri.isNullOrBlank()) {
        "Savings goal requires an icon or image."
    }
}

fun MoneyPotType.goalAccountType(): VirtualAccountType = when (this) {
    MoneyPotType.Spend -> VirtualAccountType.Main
    MoneyPotType.Save -> VirtualAccountType.Goal
    MoneyPotType.Give -> VirtualAccountType.Give
}

fun VirtualAccountType.toMoneyPotType(): MoneyPotType = when (this) {
    VirtualAccountType.Main -> MoneyPotType.Spend
    VirtualAccountType.Goal -> MoneyPotType.Save
    VirtualAccountType.Give -> MoneyPotType.Give
}

private const val MAX_SAVINGS_GOAL_TITLE_LENGTH = 80
