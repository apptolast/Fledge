package com.apptolast.fledge.domain.repository

import com.apptolast.fledge.domain.model.AllowanceRule
import com.apptolast.fledge.domain.model.AllowanceRuleDraft
import com.apptolast.fledge.domain.model.AllowanceRuleId
import com.apptolast.fledge.domain.model.CashOutSettlement
import com.apptolast.fledge.domain.model.CashOutSettlementDraft
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.SettlementId
import com.apptolast.fledge.domain.model.TransactionId
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Instant

interface MoneyFlowRepository {
    val allowanceRules: StateFlow<List<AllowanceRule>>
    val settlements: StateFlow<List<CashOutSettlement>>

    suspend fun saveAllowanceRule(
        draft: AllowanceRuleDraft,
        nextRunAt: Instant,
        createdAt: Instant,
    ): AllowanceRule

    suspend fun updateAllowanceNextRun(
        ruleId: AllowanceRuleId,
        nextRunAt: Instant,
        updatedAt: Instant,
    ): AllowanceRule

    fun dueAllowanceRules(now: Instant): List<AllowanceRule>

    fun allowanceRuleById(ruleId: AllowanceRuleId): AllowanceRule?

    suspend fun createSettlementRequest(
        draft: CashOutSettlementDraft,
        requestedAt: Instant,
    ): CashOutSettlement

    suspend fun markSettlementPaidByParent(
        settlementId: SettlementId,
        paidAt: Instant,
    ): CashOutSettlement

    suspend fun markSettlementConfirmedByChild(
        settlementId: SettlementId,
        transactionId: TransactionId,
        confirmedAt: Instant,
    ): CashOutSettlement

    fun settlementById(settlementId: SettlementId): CashOutSettlement?

    fun settlementsFor(childProfileId: ChildProfileId): List<CashOutSettlement>
}
