package com.apptolast.fledge.data.repository

import com.apptolast.customlogin.domain.AuthProvider
import com.apptolast.fledge.data.remote.firebase.FirestoreProvider
import com.apptolast.fledge.domain.model.BalanceCents
import com.apptolast.fledge.domain.model.ChildLedgerBalances
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.LedgerActor
import com.apptolast.fledge.domain.model.LedgerConcept
import com.apptolast.fledge.domain.model.LedgerTransaction
import com.apptolast.fledge.domain.model.LedgerTransactionDraft
import com.apptolast.fledge.domain.model.LedgerTransactionType
import com.apptolast.fledge.domain.model.TransactionId
import com.apptolast.fledge.domain.model.VirtualAccountType
import com.apptolast.fledge.domain.repository.LedgerRepository
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

class FirestoreLedgerRepository(
    private val firestoreProvider: FirestoreProvider,
    private val authProvider: AuthProvider,
) : LedgerRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var syncJob: Job? = null
    private val mutableTransactions = MutableStateFlow<List<LedgerTransaction>>(emptyList())

    override val transactions: StateFlow<List<LedgerTransaction>> = mutableTransactions

    init {
        scope.launch {
            authProvider.authenticatedFamilyIds().collectLatest { familyId ->
                syncJob?.cancelAndJoin()
                if (familyId == null) {
                    mutableTransactions.value = emptyList()
                } else {
                    syncJob = launch { bindLedger(familyId) }
                }
            }
        }
    }

    private suspend fun bindLedger(familyId: FamilyId) = coroutineScope {
        val ref = firestoreProvider.firestoreOrThrow()
            .collection(FAMILIES_COLLECTION)
            .document(familyId.value)
            .collection(LEDGER_COLLECTION)
        val jobs = listOf(
            launch {
                ref.snapshots.collect { snapshot ->
                    mutableTransactions.value = snapshot.documents
                        .filter { it.exists }
                        .mapNotNull { runCatching { it.toLedgerTransaction() }.getOrNull() }
                        .sortedBy { it.createdAt }
                }
            },
        )
        jobs.joinAll()
    }

    override suspend fun appendTransaction(draft: LedgerTransactionDraft): LedgerTransaction {
        require(authProvider.currentFamilyId() == draft.familyId) {
            "A signed-in parent can only append transactions to the active family."
        }
        val ref = ledgerCollection(draft.familyId).document
        val transaction = LedgerTransaction(
            id = TransactionId(ref.id),
            familyId = draft.familyId,
            childProfileId = draft.childProfileId,
            accountType = draft.accountType,
            type = draft.type,
            amountCents = draft.amountCents,
            concept = LedgerConcept(draft.concept.value.trim()),
            createdBy = draft.createdBy,
            createdAt = Clock.System.now(),
        )
        ref.set(transaction.toFirestoreMap())
        appendLocal(transaction)
        return transaction
    }

    override suspend fun reverseTransaction(
        transactionId: TransactionId,
        concept: LedgerConcept,
        createdBy: LedgerActor,
    ): LedgerTransaction {
        val familyId = authProvider.currentFamilyId()
        val remoteTransactions = if (transactions.value.isEmpty()) {
            ledgerCollection(familyId).get().documents
                .filter { it.exists }
                .mapNotNull { runCatching { it.toLedgerTransaction() }.getOrNull() }
        } else {
            emptyList()
        }
        val knownTransactions = transactions.value + remoteTransactions
        val original = knownTransactions.firstOrNull { it.id == transactionId }
            ?: ledgerCollection(familyId).document(transactionId.value)
                .get()
                .takeIf { it.exists }
                ?.toLedgerTransaction()
        requireNotNull(original) { "Transaction does not exist." }
        require(original.type != LedgerTransactionType.Reversal) { "Reversal transactions cannot be reversed." }
        require(knownTransactions.none { it.reversesTransactionId == transactionId }) {
            "Transaction has already been reversed."
        }

        val ref = ledgerCollection(original.familyId).document
        val reversal = LedgerTransaction(
            id = TransactionId(ref.id),
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
        ref.set(reversal.toFirestoreMap())
        appendLocal(reversal)
        return reversal
    }

    override fun transactionsFor(childProfileId: ChildProfileId): List<LedgerTransaction> =
        transactions.value.filter { it.childProfileId == childProfileId }

    override fun balanceFor(childProfileId: ChildProfileId, accountType: VirtualAccountType): BalanceCents {
        val sum = transactions.value
            .asSequence()
            .filter { it.childProfileId == childProfileId && it.accountType == accountType }
            .sumOf { it.amountCents.value }
        return BalanceCents(sum)
    }

    override fun balancesFor(childProfileId: ChildProfileId): ChildLedgerBalances = ChildLedgerBalances(
        childProfileId = childProfileId,
        main = balanceFor(childProfileId, VirtualAccountType.Main),
        goal = balanceFor(childProfileId, VirtualAccountType.Goal),
    )

    private fun ledgerCollection(familyId: FamilyId) = firestoreProvider
        .firestoreOrThrow()
        .collection(FAMILIES_COLLECTION)
        .document(familyId.value)
        .collection(LEDGER_COLLECTION)

    private fun appendLocal(transaction: LedgerTransaction) {
        mutableTransactions.value = (transactions.value.filterNot { it.id == transaction.id } + transaction)
            .sortedBy { it.createdAt }
    }

    private companion object {
        const val LEDGER_COLLECTION = "ledgerTransactions"
    }
}
