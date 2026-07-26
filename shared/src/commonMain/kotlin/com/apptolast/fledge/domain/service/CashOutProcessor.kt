package com.apptolast.fledge.domain.service

import com.apptolast.fledge.domain.model.CashOutSettlement
import com.apptolast.fledge.domain.model.CashOutSettlementDraft
import com.apptolast.fledge.domain.model.LedgerActor
import com.apptolast.fledge.domain.model.LedgerTransactionDraft
import com.apptolast.fledge.domain.model.LedgerTransactionType
import com.apptolast.fledge.domain.model.SettlementId
import com.apptolast.fledge.domain.model.SettlementStatus
import com.apptolast.fledge.domain.model.VirtualAccountType
import com.apptolast.fledge.domain.repository.LedgerRepository
import com.apptolast.fledge.domain.repository.MoneyFlowRepository
import kotlin.time.Instant

class CashOutProcessor(
    private val moneyFlowRepository: MoneyFlowRepository,
    private val ledgerRepository: LedgerRepository,
) {
    suspend fun requestCashOut(
        draft: CashOutSettlementDraft,
        requestedAt: Instant,
    ): CashOutSettlement {
        val balance = ledgerRepository.balanceFor(draft.childProfileId, VirtualAccountType.Main)
        require(balance.value >= draft.amountCents.value) {
            "Cash-out amount cannot exceed the main balance."
        }
        return moneyFlowRepository.createSettlementRequest(
            draft = draft,
            requestedAt = requestedAt,
        )
    }

    suspend fun markPaidByParent(
        settlementId: SettlementId,
        paidAt: Instant,
    ): CashOutSettlement =
        moneyFlowRepository.markSettlementPaidByParent(settlementId, paidAt)

    suspend fun confirmByChild(
        settlementId: SettlementId,
        confirmedAt: Instant,
    ): CashOutSettlement {
        val settlement = requireNotNull(moneyFlowRepository.settlementById(settlementId)) {
            "Settlement does not exist."
        }
        require(settlement.status == SettlementStatus.PaidByParent) {
            "Only paid settlements can be confirmed by the child."
        }
        val transaction = ledgerRepository.appendTransaction(
            LedgerTransactionDraft(
                familyId = settlement.familyId,
                childProfileId = settlement.childProfileId,
                accountType = VirtualAccountType.Main,
                type = LedgerTransactionType.Settlement,
                amountCents = settlement.amountCents.reversed(),
                concept = settlement.concept,
                createdBy = LedgerActor.Child,
            )
        )
        return moneyFlowRepository.markSettlementConfirmedByChild(
            settlementId = settlementId,
            transactionId = transaction.id,
            confirmedAt = confirmedAt,
        )
    }
}
