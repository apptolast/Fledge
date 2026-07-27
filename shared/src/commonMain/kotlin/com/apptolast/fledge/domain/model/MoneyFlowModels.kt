package com.apptolast.fledge.domain.model

import kotlin.jvm.JvmInline
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class AllowanceRuleId(val value: String) {
    init {
        require(value.isNotBlank()) { "Allowance rule id cannot be blank." }
    }
}

@Serializable
@JvmInline
value class AllowanceDay(val value: Int) {
    init {
        require(value in 1..31) { "Allowance day must be between 1 and 31." }
    }
}

@Serializable
enum class AllowanceFrequency {
    Weekly,
    Monthly,
}

@Serializable
data class AllowanceRule(
    val id: AllowanceRuleId,
    val familyId: FamilyId,
    val childProfileId: ChildProfileId,
    val accountType: VirtualAccountType,
    val frequency: AllowanceFrequency,
    val day: AllowanceDay,
    val amountCents: MoneyCents,
    val concept: LedgerConcept,
    val timeZone: TimeZoneId,
    val nextRunAt: Instant,
    val active: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class AllowanceRuleDraft(
    val familyId: FamilyId,
    val childProfileId: ChildProfileId,
    val accountType: VirtualAccountType = VirtualAccountType.Main,
    val frequency: AllowanceFrequency,
    val day: AllowanceDay,
    val amountCents: MoneyCents,
    val concept: LedgerConcept,
    val timeZone: TimeZoneId,
) {
    init {
        require(amountCents.value > 0) { "Allowance amount must be positive." }
        if (frequency == AllowanceFrequency.Weekly) {
            require(day.value in 1..7) { "Weekly allowance day must use ISO day 1..7." }
        }
    }
}

@Serializable
@JvmInline
value class SettlementId(val value: String) {
    init {
        require(value.isNotBlank()) { "Settlement id cannot be blank." }
    }
}

@Serializable
enum class SettlementStatus {
    Requested,
    PaidByParent,
    ConfirmedByChild,
}

@Serializable
data class CashOutSettlement(
    val id: SettlementId,
    val familyId: FamilyId,
    val childProfileId: ChildProfileId,
    val amountCents: MoneyCents,
    val concept: LedgerConcept,
    val status: SettlementStatus,
    val requestedAt: Instant,
    val paidByParentAt: Instant? = null,
    val confirmedByChildAt: Instant? = null,
    val settlementTransactionId: TransactionId? = null,
)

data class CashOutSettlementDraft(
    val familyId: FamilyId,
    val childProfileId: ChildProfileId,
    val amountCents: MoneyCents,
    val concept: LedgerConcept,
) {
    init {
        require(amountCents.value > 0) { "Cash-out amount must be positive." }
    }
}

@Serializable
enum class SettlementReminderLevel {
    SevenDays,
    FourteenDays,
}

@Serializable
enum class SettlementReminderAudience {
    Parent,
    Child,
}

@Serializable
data class SettlementReminder(
    val settlementId: SettlementId,
    val childProfileId: ChildProfileId,
    val audience: SettlementReminderAudience,
    val level: SettlementReminderLevel,
    val dueAt: Instant,
)
