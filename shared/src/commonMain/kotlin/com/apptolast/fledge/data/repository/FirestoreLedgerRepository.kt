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
import com.apptolast.fledge.domain.model.toLedgerTransaction
import com.apptolast.fledge.domain.repository.LedgerRepository
import com.apptolast.fledge.domain.repository.LedgerTransferPair
import com.apptolast.fledge.domain.repository.RepositorySyncStatus
import com.apptolast.fledge.domain.repository.requireValidTransferDraftPair
import com.apptolast.fledge.domain.repository.requireValidTransferPair
import kotlin.time.Clock
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

class FirestoreLedgerRepository(
    private val firestoreProvider: FirestoreProvider,
    private val authProvider: AuthProvider,
) : LedgerRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var syncJob: Job? = null
    private val mutableSyncStatus = MutableStateFlow<RepositorySyncStatus>(RepositorySyncStatus.Loading)
    private val mutableTransactions = MutableStateFlow<List<LedgerTransaction>>(emptyList())

    override val syncStatus: StateFlow<RepositorySyncStatus> = mutableSyncStatus
    override val transactions: StateFlow<List<LedgerTransaction>> = mutableTransactions

    init {
        scope.launch {
            authProvider.authenticatedFamilyIds().collectLatest { familyId ->
                syncJob?.cancelAndJoin()
                if (familyId == null) {
                    mutableTransactions.value = emptyList()
                    mutableSyncStatus.value = RepositorySyncStatus.Synced
                } else {
                    mutableSyncStatus.value = RepositorySyncStatus.Loading
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
                ref.snapshots(includeMetadataChanges = true).catch { error ->
                    mutableSyncStatus.value = error.toRepositorySyncError()
                }.collect { snapshot ->
                    mutableSyncStatus.value = snapshot.metadata.toRepositorySyncStatus()
                    mutableTransactions.value = snapshot.documents
                        .filter { it.exists }
                        .mapNotNull { runCatching { it.toLedgerTransaction() }.getOrNull() }
                        .sortedBy { it.createdAt }
                }
            },
        )
        jobs.joinAll()
    }

    override suspend fun appendTransaction(draft: LedgerTransactionDraft, createdAt: Instant): LedgerTransaction {
        require(authProvider.currentFamilyId() == draft.familyId) {
            "A signed-in parent can only append transactions to the active family."
        }
        val ref = ledgerCollection(draft.familyId).document
        val transaction = draft.toLedgerTransaction(
            id = TransactionId(ref.id),
            createdAt = createdAt,
        )
        ref.set(transaction.toFirestoreMap())
        appendLocal(transaction)
        return transaction
    }

    override suspend fun appendTransferPair(
        debitDraft: LedgerTransactionDraft,
        creditDraft: LedgerTransactionDraft,
        createdAt: Instant,
    ): LedgerTransferPair {
        require(authProvider.currentFamilyId() == debitDraft.familyId) {
            "A signed-in parent can only append transactions to the active family."
        }
        requireValidTransferDraftPair(debitDraft, creditDraft)

        val firestore = firestoreProvider.firestoreOrThrow()
        val ledgerRef = ledgerCollection(debitDraft.familyId)
        val debitRef = ledgerRef.document
        val creditRef = ledgerRef.document
        val transfer = LedgerTransferPair(
            debit = debitDraft.toLedgerTransaction(TransactionId(debitRef.id), createdAt),
            credit = creditDraft.toLedgerTransaction(TransactionId(creditRef.id), createdAt),
        )
        requireValidTransferPair(transfer)

        firestore.batch()
            .set(debitRef, transfer.debit.toFirestoreMap())
            .set(creditRef, transfer.credit.toFirestoreMap())
            .commit()
        appendLocal(transfer.debit)
        appendLocal(transfer.credit)
        return transfer
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
        give = balanceFor(childProfileId, VirtualAccountType.Give),
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
