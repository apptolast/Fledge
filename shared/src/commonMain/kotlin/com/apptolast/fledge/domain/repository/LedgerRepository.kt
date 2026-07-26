package com.apptolast.fledge.domain.repository

import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.BalanceCents
import com.apptolast.fledge.domain.model.ChildLedgerBalances
import com.apptolast.fledge.domain.model.LedgerActor
import com.apptolast.fledge.domain.model.LedgerConcept
import com.apptolast.fledge.domain.model.LedgerTransaction
import com.apptolast.fledge.domain.model.LedgerTransactionDraft
import com.apptolast.fledge.domain.model.TransactionId
import com.apptolast.fledge.domain.model.VirtualAccountType
import kotlinx.coroutines.flow.StateFlow

interface LedgerRepository {
    val transactions: StateFlow<List<LedgerTransaction>>

    suspend fun appendTransaction(draft: LedgerTransactionDraft): LedgerTransaction

    suspend fun reverseTransaction(
        transactionId: TransactionId,
        concept: LedgerConcept,
        createdBy: LedgerActor,
    ): LedgerTransaction

    fun transactionsFor(childProfileId: ChildProfileId): List<LedgerTransaction>

    fun balanceFor(
        childProfileId: ChildProfileId,
        accountType: VirtualAccountType,
    ): BalanceCents

    fun balancesFor(childProfileId: ChildProfileId): ChildLedgerBalances
}
