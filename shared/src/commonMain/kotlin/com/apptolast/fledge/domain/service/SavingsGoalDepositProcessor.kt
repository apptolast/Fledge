package com.apptolast.fledge.domain.service

import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.LedgerActor
import com.apptolast.fledge.domain.model.LedgerConcept
import com.apptolast.fledge.domain.model.LedgerTransactionDraft
import com.apptolast.fledge.domain.model.LedgerTransactionType
import com.apptolast.fledge.domain.model.LedgerTransferGroupId
import com.apptolast.fledge.domain.model.MatchSettings
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.SavingsGoalId
import com.apptolast.fledge.domain.model.SavingsGoalStatus
import com.apptolast.fledge.domain.model.VirtualAccountType
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import com.apptolast.fledge.domain.repository.LedgerRepository
import com.apptolast.fledge.domain.repository.LedgerTransferPair
import com.apptolast.fledge.domain.repository.SavingsGoalRepository
import kotlin.math.min
import kotlin.time.Clock
import kotlin.time.Instant

class SavingsGoalDepositProcessor(
    private val savingsGoalRepository: SavingsGoalRepository,
    private val ledgerRepository: LedgerRepository,
    private val familyRepository: FamilyFoundationRepository? = null,
) {
    suspend fun depositToGoal(
        goalId: SavingsGoalId,
        childProfileId: ChildProfileId,
        amountCents: MoneyCents,
        createdBy: LedgerActor = LedgerActor.Child,
        createdAt: Instant = Clock.System.now(),
    ): LedgerTransferPair {
        require(amountCents.value > 0) { "Deposit amount must be positive." }

        val goal = savingsGoalRepository.goals.value.firstOrNull { it.id == goalId }
        requireNotNull(goal) { "Savings goal does not exist." }
        require(goal.childProfileId == childProfileId) { "Savings goal belongs to another child." }
        require(goal.status == SavingsGoalStatus.Active) { "Savings goal is not active." }

        val mainBalance = ledgerRepository.balanceFor(childProfileId, VirtualAccountType.Main)
        require(mainBalance.value >= amountCents.value) { "Main balance is insufficient." }

        val transferGroupId = LedgerTransferGroupId(
            "goal-transfer-${goal.id.value}-${createdAt.epochSeconds}-${createdAt.nanosecondsOfSecond}",
        )
        val concept = LedgerConcept("Ahorro: ${goal.title}")
        val transfer = ledgerRepository.appendTransferPair(
            debitDraft = LedgerTransactionDraft(
                familyId = goal.familyId,
                childProfileId = childProfileId,
                accountType = VirtualAccountType.Main,
                type = LedgerTransactionType.GoalTransfer,
                amountCents = MoneyCents(-amountCents.value),
                concept = concept,
                createdBy = createdBy,
                transferGroupId = transferGroupId,
            ),
            creditDraft = LedgerTransactionDraft(
                familyId = goal.familyId,
                childProfileId = childProfileId,
                accountType = goal.accountType,
                type = LedgerTransactionType.GoalTransfer,
                amountCents = amountCents,
                concept = concept,
                createdBy = createdBy,
                transferGroupId = transferGroupId,
            ),
            createdAt = createdAt,
        )
        if (createdBy == LedgerActor.Child) {
            calculateParentalMatchCents(
                contributionCents = amountCents,
                settings = familyRepository?.activeFamily?.value?.matchSettings ?: MatchSettings(),
            )?.let { matchCents ->
                ledgerRepository.appendTransaction(
                    LedgerTransactionDraft(
                        familyId = goal.familyId,
                        childProfileId = childProfileId,
                        accountType = goal.accountType,
                        type = LedgerTransactionType.Match,
                        amountCents = matchCents,
                        concept = LedgerConcept("Match parental: ${goal.title}"),
                        createdBy = LedgerActor.System,
                    ),
                    createdAt = createdAt,
                )
            }
        }
        return transfer
    }
}

internal fun calculateParentalMatchCents(contributionCents: MoneyCents, settings: MatchSettings): MoneyCents? {
    if (!settings.enabled) return null
    val uncapped = contributionCents.value * settings.matchBasisPoints / 10_000
    val capped = min(uncapped, settings.maxMatchCents)
    return capped.takeIf { it > 0 }?.let(::MoneyCents)
}
