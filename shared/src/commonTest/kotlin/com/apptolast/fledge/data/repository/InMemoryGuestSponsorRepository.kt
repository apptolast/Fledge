package com.apptolast.fledge.data.repository

import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyGuestInvite
import com.apptolast.fledge.domain.model.FamilyGuestInviteDraft
import com.apptolast.fledge.domain.model.FamilyGuestInviteStatus
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.GuestContributionDraft
import com.apptolast.fledge.domain.model.GuestContributionKind
import com.apptolast.fledge.domain.model.GuestSponsorAccess
import com.apptolast.fledge.domain.model.LedgerActor
import com.apptolast.fledge.domain.model.LedgerTransaction
import com.apptolast.fledge.domain.model.LedgerTransactionDraft
import com.apptolast.fledge.domain.model.LedgerTransactionType
import com.apptolast.fledge.domain.model.TransactionId
import com.apptolast.fledge.domain.model.VirtualAccountType
import com.apptolast.fledge.domain.model.normalizeFamilyGuestEmail
import com.apptolast.fledge.domain.model.toLedgerTransaction
import com.apptolast.fledge.domain.repository.GuestSponsorRepository
import com.apptolast.fledge.domain.repository.RepositorySyncStatus
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class InMemoryGuestSponsorRepository(private val ownerFamilyId: FamilyId = FamilyId("family-1")) :
    GuestSponsorRepository {
    private var transactionCounter = 1
    private val mutableSyncStatus = MutableStateFlow<RepositorySyncStatus>(RepositorySyncStatus.Synced)
    private val mutableActiveGuestAccess = MutableStateFlow<GuestSponsorAccess?>(null)
    private val mutableTransactions = mutableListOf<LedgerTransaction>()
    private val mutableInvites = mutableListOf<FamilyGuestInvite>()

    override val syncStatus: StateFlow<RepositorySyncStatus> = mutableSyncStatus
    override val activeGuestAccess: StateFlow<GuestSponsorAccess?> = mutableActiveGuestAccess

    val transactions: List<LedgerTransaction> get() = mutableTransactions.toList()
    val invites: List<FamilyGuestInvite> get() = mutableInvites.toList()

    fun setActiveGuestAccess(access: GuestSponsorAccess?) {
        mutableActiveGuestAccess.value = access
    }

    override suspend fun inviteGuest(
        childProfileId: ChildProfileId,
        draft: FamilyGuestInviteDraft,
    ): FamilyGuestInvite {
        val email = normalizeFamilyGuestEmail(draft.email)
        val previous = mutableInvites.firstOrNull { it.email == email && it.familyId == ownerFamilyId }
        val childProfileIds = (previous?.childProfileIds.orEmpty() + childProfileId).distinctBy { it.value }
        val invite = FamilyGuestInvite(
            familyId = ownerFamilyId,
            email = email,
            childProfileIds = childProfileIds,
            status = FamilyGuestInviteStatus.Active,
            invitedAt = previous?.invitedAt ?: Clock.System.now(),
            invitedByUid = ownerFamilyId.value,
        )
        mutableInvites.removeAll { it.email == email }
        mutableInvites += invite
        return invite
    }

    override suspend fun contribute(draft: GuestContributionDraft): LedgerTransaction {
        val access = requireNotNull(activeGuestAccess.value) { "A signed-in guest is required." }
        require(access.familyId == draft.familyId)
        require(access.invite.childProfileIds.any { it == draft.childProfileId })
        val transaction = LedgerTransactionDraft(
            familyId = draft.familyId,
            childProfileId = draft.childProfileId,
            accountType = VirtualAccountType.Main,
            type = when (draft.kind) {
                GuestContributionKind.Gift -> LedgerTransactionType.Gift
                GuestContributionKind.Match -> LedgerTransactionType.Match
            },
            amountCents = draft.amountCents,
            concept = draft.concept,
            createdBy = LedgerActor.Guest,
        ).toLedgerTransaction(
            id = TransactionId("guest-tx-${transactionCounter++}"),
            createdAt = Clock.System.now(),
        )
        mutableTransactions += transaction
        return transaction
    }
}
