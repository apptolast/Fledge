package com.apptolast.fledge.domain.service

import com.apptolast.fledge.domain.model.LedgerActor
import com.apptolast.fledge.domain.model.LedgerTransaction
import com.apptolast.fledge.domain.model.LedgerTransactionDraft
import com.apptolast.fledge.domain.model.LedgerTransactionType
import com.apptolast.fledge.domain.repository.LedgerRepository
import com.apptolast.fledge.domain.repository.MoneyFlowRepository
import kotlin.time.Instant

class AllowanceProcessor(
    private val moneyFlowRepository: MoneyFlowRepository,
    private val ledgerRepository: LedgerRepository,
) {
    suspend fun runDueAllowances(now: Instant): List<LedgerTransaction> {
        val createdTransactions = mutableListOf<LedgerTransaction>()
        moneyFlowRepository.dueAllowanceRules(now).forEach { rule ->
            val transaction = ledgerRepository.appendTransaction(
                LedgerTransactionDraft(
                    familyId = rule.familyId,
                    childProfileId = rule.childProfileId,
                    accountType = rule.accountType,
                    type = LedgerTransactionType.Allowance,
                    amountCents = rule.amountCents,
                    concept = rule.concept,
                    createdBy = LedgerActor.System,
                )
            )
            val nextRunAt = AllowanceSchedule.nextRunAtAfter(
                previousRunAt = rule.nextRunAt,
                frequency = rule.frequency,
                day = rule.day,
                timeZone = rule.timeZone,
            )
            moneyFlowRepository.updateAllowanceNextRun(
                ruleId = rule.id,
                nextRunAt = nextRunAt,
                updatedAt = now,
            )
            createdTransactions += transaction
        }
        return createdTransactions
    }
}
