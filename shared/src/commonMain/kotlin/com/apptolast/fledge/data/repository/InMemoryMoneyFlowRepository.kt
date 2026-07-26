package com.apptolast.fledge.data.repository

import com.apptolast.fledge.domain.model.AllowanceRule
import com.apptolast.fledge.domain.model.AllowanceRuleDraft
import com.apptolast.fledge.domain.model.AllowanceRuleId
import com.apptolast.fledge.domain.model.CashOutSettlement
import com.apptolast.fledge.domain.model.CashOutSettlementDraft
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.SettlementId
import com.apptolast.fledge.domain.model.SettlementStatus
import com.apptolast.fledge.domain.model.TransactionId
import com.apptolast.fledge.domain.repository.MoneyFlowRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Instant

class InMemoryMoneyFlowRepository : MoneyFlowRepository {
    private var allowanceRuleCounter = 1
    private var settlementCounter = 1

    private val mutableAllowanceRules = MutableStateFlow<List<AllowanceRule>>(emptyList())
    private val mutableSettlements = MutableStateFlow<List<CashOutSettlement>>(emptyList())

    override val allowanceRules: StateFlow<List<AllowanceRule>> = mutableAllowanceRules
    override val settlements: StateFlow<List<CashOutSettlement>> = mutableSettlements

    override suspend fun saveAllowanceRule(
        draft: AllowanceRuleDraft,
        nextRunAt: Instant,
        createdAt: Instant,
    ): AllowanceRule {
        val existingIndex = mutableAllowanceRules.value.indexOfFirst {
            it.familyId == draft.familyId && it.childProfileId == draft.childProfileId
        }
        val rule = AllowanceRule(
            id = existingIndex.takeIf { it >= 0 }
                ?.let { mutableAllowanceRules.value[it].id }
                ?: AllowanceRuleId("allowance-rule-${allowanceRuleCounter++}"),
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
            createdAt = existingIndex.takeIf { it >= 0 }
                ?.let { mutableAllowanceRules.value[it].createdAt }
                ?: createdAt,
            updatedAt = createdAt,
        )
        mutableAllowanceRules.value = if (existingIndex >= 0) {
            mutableAllowanceRules.value.toMutableList().also { it[existingIndex] = rule }
        } else {
            mutableAllowanceRules.value + rule
        }
        return rule
    }

    override suspend fun updateAllowanceNextRun(
        ruleId: AllowanceRuleId,
        nextRunAt: Instant,
        updatedAt: Instant,
    ): AllowanceRule {
        var updatedRule: AllowanceRule? = null
        mutableAllowanceRules.value = mutableAllowanceRules.value.map { rule ->
            if (rule.id == ruleId) {
                rule.copy(nextRunAt = nextRunAt, updatedAt = updatedAt).also { updatedRule = it }
            } else {
                rule
            }
        }
        return requireNotNull(updatedRule) { "Allowance rule does not exist." }
    }

    override fun dueAllowanceRules(now: Instant): List<AllowanceRule> =
        mutableAllowanceRules.value.filter { it.active && it.nextRunAt <= now }

    override fun allowanceRuleById(ruleId: AllowanceRuleId): AllowanceRule? =
        mutableAllowanceRules.value.firstOrNull { it.id == ruleId }

    override suspend fun createSettlementRequest(
        draft: CashOutSettlementDraft,
        requestedAt: Instant,
    ): CashOutSettlement {
        val settlement = CashOutSettlement(
            id = SettlementId("settlement-${settlementCounter++}"),
            familyId = draft.familyId,
            childProfileId = draft.childProfileId,
            amountCents = draft.amountCents,
            concept = draft.concept,
            status = SettlementStatus.Requested,
            requestedAt = requestedAt,
        )
        mutableSettlements.value = mutableSettlements.value + settlement
        return settlement
    }

    override suspend fun markSettlementPaidByParent(
        settlementId: SettlementId,
        paidAt: Instant,
    ): CashOutSettlement =
        updateSettlement(settlementId) { settlement ->
            require(settlement.status == SettlementStatus.Requested) {
                "Only requested settlements can be marked as paid."
            }
            settlement.copy(
                status = SettlementStatus.PaidByParent,
                paidByParentAt = paidAt,
            )
        }

    override suspend fun markSettlementConfirmedByChild(
        settlementId: SettlementId,
        transactionId: TransactionId,
        confirmedAt: Instant,
    ): CashOutSettlement =
        updateSettlement(settlementId) { settlement ->
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
        mutableSettlements.value.firstOrNull { it.id == settlementId }

    override fun settlementsFor(childProfileId: ChildProfileId): List<CashOutSettlement> =
        mutableSettlements.value.filter { it.childProfileId == childProfileId }

    private fun updateSettlement(
        settlementId: SettlementId,
        transform: (CashOutSettlement) -> CashOutSettlement,
    ): CashOutSettlement {
        var updatedSettlement: CashOutSettlement? = null
        mutableSettlements.value = mutableSettlements.value.map { settlement ->
            if (settlement.id == settlementId) {
                transform(settlement).also { updatedSettlement = it }
            } else {
                settlement
            }
        }
        return requireNotNull(updatedSettlement) { "Settlement does not exist." }
    }
}
