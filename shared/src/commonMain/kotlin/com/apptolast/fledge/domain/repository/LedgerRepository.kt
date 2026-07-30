package com.apptolast.fledge.domain.repository

import com.apptolast.fledge.domain.model.BalanceCents
import com.apptolast.fledge.domain.model.ChildLedgerBalances
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.LedgerActor
import com.apptolast.fledge.domain.model.LedgerConcept
import com.apptolast.fledge.domain.model.LedgerTransaction
import com.apptolast.fledge.domain.model.LedgerTransactionDraft
import com.apptolast.fledge.domain.model.LedgerTransactionType
import com.apptolast.fledge.domain.model.TransactionId
import com.apptolast.fledge.domain.model.VirtualAccountType
import kotlin.math.absoluteValue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.StateFlow

interface LedgerRepository {
    val syncStatus: StateFlow<RepositorySyncStatus>
    val transactions: StateFlow<List<LedgerTransaction>>

    suspend fun appendTransaction(
        draft: LedgerTransactionDraft,
        createdAt: Instant = Clock.System.now(),
    ): LedgerTransaction

    suspend fun appendTransferPair(
        debitDraft: LedgerTransactionDraft,
        creditDraft: LedgerTransactionDraft,
        createdAt: Instant = Clock.System.now(),
    ): LedgerTransferPair {
        requireValidTransferDraftPair(debitDraft, creditDraft)
        val debit = appendTransaction(debitDraft, createdAt)
        val credit = appendTransaction(creditDraft, createdAt)
        return LedgerTransferPair(debit, credit).also(::requireValidTransferPair)
    }

    suspend fun reverseTransaction(
        transactionId: TransactionId,
        concept: LedgerConcept,
        createdBy: LedgerActor,
    ): LedgerTransaction

    fun transactionsFor(childProfileId: ChildProfileId): List<LedgerTransaction>

    fun balanceFor(childProfileId: ChildProfileId, accountType: VirtualAccountType): BalanceCents

    fun balancesFor(childProfileId: ChildProfileId): ChildLedgerBalances
}

data class LedgerTransferPair(val debit: LedgerTransaction, val credit: LedgerTransaction)

internal fun requireValidTransferDraftPair(debitDraft: LedgerTransactionDraft, creditDraft: LedgerTransactionDraft) {
    require(debitDraft.familyId == creditDraft.familyId) { "Transfer pair must stay in the same family." }
    require(debitDraft.childProfileId == creditDraft.childProfileId) { "Transfer pair must stay in the same child." }
    require(debitDraft.type == LedgerTransactionType.GoalTransfer) { "Debit must be a goal transfer." }
    require(creditDraft.type == LedgerTransactionType.GoalTransfer) { "Credit must be a goal transfer." }
    require(isMainGoalPair(debitDraft.accountType, creditDraft.accountType)) {
        "Transfer pair must move between MAIN and a goal pot."
    }
    require(debitDraft.amountCents.value < 0) { "Debit amount must be negative." }
    require(creditDraft.amountCents.value > 0) { "Credit amount must be positive." }
    require(debitDraft.amountCents.value.absoluteValue == creditDraft.amountCents.value) {
        "Transfer pair amounts must match."
    }
    require(debitDraft.transferGroupId != null && debitDraft.transferGroupId == creditDraft.transferGroupId) {
        "Transfer pair must share transfer group id."
    }
}

internal fun requireValidTransferPair(pair: LedgerTransferPair) {
    require(pair.debit.familyId == pair.credit.familyId) { "Transfer pair must stay in the same family." }
    require(pair.debit.childProfileId == pair.credit.childProfileId) { "Transfer pair must stay in the same child." }
    require(pair.debit.type == LedgerTransactionType.GoalTransfer) { "Debit must be a goal transfer." }
    require(pair.credit.type == LedgerTransactionType.GoalTransfer) { "Credit must be a goal transfer." }
    require(isMainGoalPair(pair.debit.accountType, pair.credit.accountType)) {
        "Transfer pair must move between MAIN and a goal pot."
    }
    require(pair.debit.amountCents.value < 0) { "Debit amount must be negative." }
    require(pair.credit.amountCents.value > 0) { "Credit amount must be positive." }
    require(pair.debit.amountCents.value.absoluteValue == pair.credit.amountCents.value) {
        "Transfer pair amounts must match."
    }
    require(pair.debit.transferGroupId != null && pair.debit.transferGroupId == pair.credit.transferGroupId) {
        "Transfer pair must share transfer group id."
    }
}

private fun isMainGoalPair(debitAccountType: VirtualAccountType, creditAccountType: VirtualAccountType): Boolean =
    (debitAccountType == VirtualAccountType.Main && creditAccountType == VirtualAccountType.Goal) ||
        (debitAccountType == VirtualAccountType.Goal && creditAccountType == VirtualAccountType.Main) ||
        (debitAccountType == VirtualAccountType.Main && creditAccountType == VirtualAccountType.Give) ||
        (debitAccountType == VirtualAccountType.Give && creditAccountType == VirtualAccountType.Main)
