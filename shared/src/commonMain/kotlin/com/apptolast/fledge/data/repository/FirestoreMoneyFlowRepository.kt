package com.apptolast.fledge.data.repository

import com.apptolast.customlogin.domain.AuthProvider
import com.apptolast.fledge.data.remote.firebase.FirestoreProvider
import com.apptolast.fledge.domain.model.AllowanceRule
import com.apptolast.fledge.domain.model.AllowanceRuleDraft
import com.apptolast.fledge.domain.model.AllowanceRuleId
import com.apptolast.fledge.domain.model.CashOutSettlement
import com.apptolast.fledge.domain.model.CashOutSettlementDraft
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.SettlementId
import com.apptolast.fledge.domain.model.SettlementStatus
import com.apptolast.fledge.domain.model.TransactionId
import com.apptolast.fledge.domain.repository.MoneyFlowRepository
import com.apptolast.fledge.domain.repository.RepositorySyncStatus
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

class FirestoreMoneyFlowRepository(
    private val firestoreProvider: FirestoreProvider,
    private val authProvider: AuthProvider,
) : MoneyFlowRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var syncJob: Job? = null
    private val mutableSyncStatus = MutableStateFlow<RepositorySyncStatus>(RepositorySyncStatus.Loading)
    private val allowanceRulesSyncStatus = MutableStateFlow<RepositorySyncStatus>(RepositorySyncStatus.Loading)
    private val settlementsSyncStatus = MutableStateFlow<RepositorySyncStatus>(RepositorySyncStatus.Loading)
    private val mutableAllowanceRules = MutableStateFlow<List<AllowanceRule>>(emptyList())
    private val mutableSettlements = MutableStateFlow<List<CashOutSettlement>>(emptyList())

    override val syncStatus: StateFlow<RepositorySyncStatus> = mutableSyncStatus
    override val allowanceRules: StateFlow<List<AllowanceRule>> = mutableAllowanceRules
    override val settlements: StateFlow<List<CashOutSettlement>> = mutableSettlements

    init {
        scope.launch {
            combine(allowanceRulesSyncStatus, settlementsSyncStatus) { allowanceRules, settlements ->
                listOf(allowanceRules, settlements).aggregateRepositorySyncStatus()
            }.collect { status ->
                mutableSyncStatus.value = status
            }
        }
        scope.launch {
            authProvider.authenticatedFamilyIds(firestoreProvider).collectLatest { familyId ->
                syncJob?.cancelAndJoin()
                if (familyId == null) {
                    mutableAllowanceRules.value = emptyList()
                    mutableSettlements.value = emptyList()
                    resetSyncStatuses(RepositorySyncStatus.Synced)
                } else {
                    resetSyncStatuses(RepositorySyncStatus.Loading)
                    syncJob = launch { bindMoneyFlows(familyId) }
                }
            }
        }
    }

    private suspend fun bindMoneyFlows(familyId: FamilyId) = coroutineScope {
        val familyRef = firestoreProvider.firestoreOrThrow()
            .collection(FAMILIES_COLLECTION)
            .document(familyId.value)
        val jobs = listOf(
            launch {
                familyRef.collection(ALLOWANCE_RULES_COLLECTION).snapshots(includeMetadataChanges = true)
                    .catch { error ->
                        allowanceRulesSyncStatus.value = error.toRepositorySyncError()
                    }
                    .collect { snapshot ->
                        allowanceRulesSyncStatus.value = snapshot.metadata.toRepositorySyncStatus()
                        mutableAllowanceRules.value = snapshot.documents
                            .filter { it.exists }
                            .mapNotNull { runCatching { it.toAllowanceRule() }.getOrNull() }
                            .sortedBy { it.createdAt }
                    }
            },
            launch {
                familyRef.collection(SETTLEMENTS_COLLECTION).snapshots(includeMetadataChanges = true)
                    .catch { error ->
                        settlementsSyncStatus.value = error.toRepositorySyncError()
                    }
                    .collect { snapshot ->
                        settlementsSyncStatus.value = snapshot.metadata.toRepositorySyncStatus()
                        mutableSettlements.value = snapshot.documents
                            .filter { it.exists }
                            .mapNotNull { runCatching { it.toCashOutSettlement() }.getOrNull() }
                            .sortedBy { it.requestedAt }
                    }
            },
        )
        jobs.joinAll()
    }

    private fun resetSyncStatuses(status: RepositorySyncStatus) {
        allowanceRulesSyncStatus.value = status
        settlementsSyncStatus.value = status
        mutableSyncStatus.value = status
    }

    override suspend fun saveAllowanceRule(
        draft: AllowanceRuleDraft,
        nextRunAt: Instant,
        createdAt: Instant,
    ): AllowanceRule {
        require(authProvider.currentFamilyId(firestoreProvider) == draft.familyId) {
            "A signed-in parent can only save allowance rules for the active family."
        }
        val existing = allowanceRules.value.firstOrNull {
            it.familyId == draft.familyId && it.childProfileId == draft.childProfileId
        }
        val ref = if (existing != null) {
            allowanceCollection(draft.familyId).document(existing.id.value)
        } else {
            allowanceCollection(draft.familyId).document
        }
        val rule = AllowanceRule(
            id = AllowanceRuleId(ref.id),
            familyId = draft.familyId,
            childProfileId = draft.childProfileId,
            accountType = draft.accountType,
            frequency = draft.frequency,
            day = draft.day,
            amountCents = draft.amountCents,
            concept = draft.concept,
            timeZone = draft.timeZone,
            nextRunAt = nextRunAt,
            active = true,
            createdAt = existing?.createdAt ?: createdAt,
            updatedAt = createdAt,
        )
        ref.set(rule.toFirestoreMap(), merge = existing != null)
        mutableAllowanceRules.value = if (existing == null) {
            (allowanceRules.value + rule).sortedBy { it.createdAt }
        } else {
            allowanceRules.value.map { if (it.id == rule.id) rule else it }
        }
        return rule
    }

    override suspend fun updateAllowanceNextRun(
        ruleId: AllowanceRuleId,
        nextRunAt: Instant,
        updatedAt: Instant,
    ): AllowanceRule {
        val familyId = authProvider.currentFamilyId(firestoreProvider)
        val existing = allowanceRuleById(ruleId)
            ?: allowanceCollection(familyId).document(ruleId.value).get().takeIf { it.exists }?.toAllowanceRule()
        requireNotNull(existing) { "Allowance rule does not exist." }
        val updated = existing.copy(nextRunAt = nextRunAt, updatedAt = updatedAt)
        allowanceCollection(existing.familyId).document(ruleId.value).set(updated.toFirestoreMap(), merge = true)
        mutableAllowanceRules.value = allowanceRules.value.map { if (it.id == ruleId) updated else it }
        return updated
    }

    override fun dueAllowanceRules(now: Instant): List<AllowanceRule> =
        allowanceRules.value.filter { it.active && it.nextRunAt <= now }

    override fun allowanceRuleById(ruleId: AllowanceRuleId): AllowanceRule? =
        allowanceRules.value.firstOrNull { it.id == ruleId }

    override suspend fun createSettlementRequest(
        draft: CashOutSettlementDraft,
        requestedAt: Instant,
    ): CashOutSettlement {
        require(authProvider.currentFamilyId(firestoreProvider) == draft.familyId) {
            "A signed-in parent can only create settlements for the active family."
        }
        val ref = settlementCollection(draft.familyId).document
        val settlement = CashOutSettlement(
            id = SettlementId(ref.id),
            familyId = draft.familyId,
            childProfileId = draft.childProfileId,
            amountCents = draft.amountCents,
            concept = draft.concept,
            status = SettlementStatus.Requested,
            requestedAt = requestedAt,
        )
        ref.set(settlement.toFirestoreMap())
        mutableSettlements.value = (settlements.value + settlement).sortedBy { it.requestedAt }
        return settlement
    }

    override suspend fun markSettlementPaidByParent(settlementId: SettlementId, paidAt: Instant): CashOutSettlement =
        updateSettlement(settlementId) { settlement ->
            require(settlement.status == SettlementStatus.Requested) {
                "Only requested settlements can be marked as paid."
            }
            settlement.copy(status = SettlementStatus.PaidByParent, paidByParentAt = paidAt)
        }

    override suspend fun markSettlementConfirmedByChild(
        settlementId: SettlementId,
        transactionId: TransactionId,
        confirmedAt: Instant,
    ): CashOutSettlement = updateSettlement(settlementId) { settlement ->
        require(settlement.status == SettlementStatus.PaidByParent) {
            "Only paid settlements can be confirmed by the child."
        }
        settlement.copy(
            status = SettlementStatus.ConfirmedByChild,
            confirmedByChildAt = confirmedAt,
            settlementTransactionId = transactionId,
        )
    }

    override fun settlementById(settlementId: SettlementId): CashOutSettlement? =
        settlements.value.firstOrNull { it.id == settlementId }

    override fun settlementsFor(childProfileId: ChildProfileId): List<CashOutSettlement> =
        settlements.value.filter { it.childProfileId == childProfileId }

    private suspend fun updateSettlement(
        settlementId: SettlementId,
        transform: (CashOutSettlement) -> CashOutSettlement,
    ): CashOutSettlement {
        val familyId = authProvider.currentFamilyId(firestoreProvider)
        val existing = settlementById(settlementId)
            ?: settlementCollection(familyId).document(settlementId.value).get()
                .takeIf { it.exists }
                ?.toCashOutSettlement()
        requireNotNull(existing) { "Settlement does not exist." }
        val updated = transform(existing)
        settlementCollection(existing.familyId).document(settlementId.value).set(updated.toFirestoreMap(), merge = true)
        mutableSettlements.value = settlements.value.map { if (it.id == settlementId) updated else it }
        return updated
    }

    private fun allowanceCollection(familyId: FamilyId) = firestoreProvider
        .firestoreOrThrow()
        .collection(FAMILIES_COLLECTION)
        .document(familyId.value)
        .collection(ALLOWANCE_RULES_COLLECTION)

    private fun settlementCollection(familyId: FamilyId) = firestoreProvider
        .firestoreOrThrow()
        .collection(FAMILIES_COLLECTION)
        .document(familyId.value)
        .collection(SETTLEMENTS_COLLECTION)

    private companion object {
        const val ALLOWANCE_RULES_COLLECTION = "allowanceRules"
        const val SETTLEMENTS_COLLECTION = "settlements"
    }
}
