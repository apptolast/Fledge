package com.apptolast.fledge.domain.model

import kotlin.jvm.JvmInline
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class TransactionId(val value: String) {
    init {
        require(value.isNotBlank()) { "Transaction id cannot be blank." }
    }
}

@Serializable
@JvmInline
value class MoneyCents(val value: Long) {
    init {
        require(value != 0L) { "Transaction amount cannot be zero." }
        require(value != Long.MIN_VALUE) { "Transaction amount cannot be reversed safely." }
    }

    fun reversed(): MoneyCents = MoneyCents(-value)
}

@Serializable
@JvmInline
value class BalanceCents(val value: Long)

@Serializable
@JvmInline
value class LedgerConcept(val value: String) {
    init {
        require(value.isNotBlank()) { "Ledger concept cannot be blank." }
        require(value.length <= 120) { "Ledger concept cannot exceed 120 characters." }
    }
}

@Serializable
enum class VirtualAccountType {
    Main,
    Goal,
}

@Serializable
enum class LedgerTransactionType {
    Allowance,
    TaskReward,
    Bonus,
    Penalty,
    Gift,
    GoalTransfer,
    Settlement,
    Reversal,
}

@Serializable
enum class LedgerActor {
    Parent,
    Child,
    System,
}

@Serializable
data class LedgerTransaction(
    val id: TransactionId,
    val familyId: FamilyId,
    val childProfileId: ChildProfileId,
    val accountType: VirtualAccountType,
    val type: LedgerTransactionType,
    val amountCents: MoneyCents,
    val concept: LedgerConcept,
    val createdBy: LedgerActor,
    val createdAt: Instant,
    val reversesTransactionId: TransactionId? = null,
)

@Serializable
data class ChildLedgerBalances(val childProfileId: ChildProfileId, val main: BalanceCents, val goal: BalanceCents)

data class LedgerTransactionDraft(
    val familyId: FamilyId,
    val childProfileId: ChildProfileId,
    val accountType: VirtualAccountType,
    val type: LedgerTransactionType,
    val amountCents: MoneyCents,
    val concept: LedgerConcept,
    val createdBy: LedgerActor,
) {
    init {
        require(type != LedgerTransactionType.Reversal) {
            "Use reverseTransaction to create reversal entries."
        }
    }
}
