package com.apptolast.fledge.data.repository

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
import com.apptolast.fledge.domain.model.toLedgerTransaction
import com.apptolast.fledge.domain.repository.LedgerRepository
import com.apptolast.fledge.domain.repository.LedgerTransferPair
import com.apptolast.fledge.domain.repository.RepositorySyncStatus
import com.apptolast.fledge.domain.repository.requireValidTransferDraftPair
import com.apptolast.fledge.domain.repository.requireValidTransferPair
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class InMemoryLedgerRepository : LedgerRepository {
    private var transactionCounter = 1
    private val mutableSyncStatus = MutableStateFlow<RepositorySyncStatus>(RepositorySyncStatus.Synced)
    private val mutableTransactions = MutableStateFlow<List<LedgerTransaction>>(emptyList())

    override val syncStatus: StateFlow<RepositorySyncStatus> = mutableSyncStatus
    override val transactions: StateFlow<List<LedgerTransaction>> = mutableTransactions

    override suspend fun appendTransaction(draft: LedgerTransactionDraft, createdAt: Instant): LedgerTransaction {
        val transaction = draft.toLedgerTransaction(
            id = nextTransactionId(),
            createdAt = createdAt,
        )
        appendOnly(transaction)
        return transaction
    }

    override suspend fun appendTransferPair(
        debitDraft: LedgerTransactionDraft,
        creditDraft: LedgerTransactionDraft,
        createdAt: Instant,
    ): LedgerTransferPair {
        requireValidTransferDraftPair(debitDraft, creditDraft)
        val transfer = LedgerTransferPair(
            debit = debitDraft.toLedgerTransaction(nextTransactionId(), createdAt),
            credit = creditDraft.toLedgerTransaction(nextTransactionId(), createdAt),
        )
        requireValidTransferPair(transfer)
        appendOnly(transfer.debit)
        appendOnly(transfer.credit)
        return transfer
    }

    override suspend fun reverseTransaction(
        transactionId: TransactionId,
        concept: LedgerConcept,
        createdBy: LedgerActor,
    ): LedgerTransaction {
        val original = mutableTransactions.value.firstOrNull { it.id == transactionId }
        requireNotNull(original) { "Transaction does not exist." }
        require(original.type != LedgerTransactionType.Reversal) { "Reversal transactions cannot be reversed." }
        require(mutableTransactions.value.none { it.reversesTransactionId == transactionId }) {
            "Transaction has already been reversed."
        }

        val reversal = LedgerTransaction(
            id = nextTransactionId(),
            familyId = original.familyId,
            childProfileId = original.childProfileId,
            accountType = original.accountType,
            type = LedgerTransactionType.Reversal,
            amountCents = original.amountCents.reversed(),
            concept = LedgerConcept(concept.value.trim()),
            createdBy = createdBy,
            createdAt = Clock.System.now(),
            reversesTransactionId = original.id,
        )
        appendOnly(reversal)
        return reversal
    }

    override fun transactionsFor(childProfileId: ChildProfileId): List<LedgerTransaction> =
        mutableTransactions.value.filter { it.childProfileId == childProfileId }

    override fun balanceFor(childProfileId: ChildProfileId, accountType: VirtualAccountType): BalanceCents {
        val sum = mutableTransactions.value
            .asSequence()
            .filter { it.childProfileId == childProfileId && it.accountType == accountType }
            .sumOf { it.amountCents.value }

        return BalanceCents(sum)
    }

    override fun balancesFor(childProfileId: ChildProfileId): ChildLedgerBalances = ChildLedgerBalances(
        childProfileId = childProfileId,
        main = balanceFor(childProfileId, VirtualAccountType.Main),
        goal = balanceFor(childProfileId, VirtualAccountType.Goal),
        give = balanceFor(childProfileId, VirtualAccountType.Give),
    )

    private fun appendOnly(transaction: LedgerTransaction) {
        mutableTransactions.value = mutableTransactions.value + transaction
    }

    private fun nextTransactionId(): TransactionId = TransactionId("transaction-${transactionCounter++}")
}
