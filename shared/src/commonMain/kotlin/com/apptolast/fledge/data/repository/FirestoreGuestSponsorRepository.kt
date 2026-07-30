package com.apptolast.fledge.data.repository

import com.apptolast.customlogin.domain.AuthProvider
import com.apptolast.customlogin.domain.model.AuthState
import com.apptolast.customlogin.domain.model.UserSession
import com.apptolast.fledge.data.remote.firebase.FirestoreProvider
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyGuestInvite
import com.apptolast.fledge.domain.model.FamilyGuestInviteDraft
import com.apptolast.fledge.domain.model.FamilyGuestInviteStatus
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FirestoreGuestSponsorRepository(
    private val firestoreProvider: FirestoreProvider,
    private val authProvider: AuthProvider,
) : GuestSponsorRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutableSyncStatus = MutableStateFlow<RepositorySyncStatus>(RepositorySyncStatus.Loading)
    private val mutableActiveGuestAccess = MutableStateFlow<GuestSponsorAccess?>(null)

    override val syncStatus: StateFlow<RepositorySyncStatus> = mutableSyncStatus
    override val activeGuestAccess: StateFlow<GuestSponsorAccess?> = mutableActiveGuestAccess

    init {
        scope.launch {
            authProvider.observeAuthState()
                .catch {
                    mutableActiveGuestAccess.value = null
                    mutableSyncStatus.value = RepositorySyncStatus.Error(it.message)
                }
                .collectLatest { authState ->
                    val session = (authState as? AuthState.Authenticated)?.session
                    if (session == null) {
                        mutableActiveGuestAccess.value = null
                        mutableSyncStatus.value = RepositorySyncStatus.Synced
                    } else {
                        resolveGuestAccess(session)
                    }
                }
        }
    }

    override suspend fun inviteGuest(
        childProfileId: ChildProfileId,
        draft: FamilyGuestInviteDraft,
    ): FamilyGuestInvite {
        val ownerFamilyId = authProvider.currentOwnerFamilyId()
        val firestore = firestoreProvider.firestoreOrThrow()
        val childSnapshot = firestore.collection(FAMILIES_COLLECTION)
            .document(ownerFamilyId.value)
            .collection(CHILD_PROFILES_COLLECTION)
            .document(childProfileId.value)
            .get()
        require(childSnapshot.exists) { "Child profile does not exist." }

        val email = normalizeFamilyGuestEmail(draft.email)
        val inviteRef = firestore.collection(FAMILY_GUEST_INVITES_COLLECTION).document(email)
        val previousInvite = runCatching {
            inviteRef.get()
                .takeIf { it.exists }
                ?.toFamilyGuestInvite()
        }.getOrNull()
        val childProfileIds = (
            previousInvite
                ?.takeIf { it.familyId == ownerFamilyId && it.status == FamilyGuestInviteStatus.Active }
                ?.childProfileIds
                .orEmpty() + childProfileId
            ).distinctBy { it.value }
            .sortedBy { it.value }
        val now = Clock.System.now()
        val invite = FamilyGuestInvite(
            familyId = ownerFamilyId,
            email = email,
            childProfileIds = childProfileIds,
            status = FamilyGuestInviteStatus.Active,
            invitedAt = previousInvite?.invitedAt ?: now,
            invitedByUid = ownerFamilyId.value,
        )
        inviteRef.set(invite.toFirestoreMap() + ("updatedAt" to now.toFirestoreTimestamp()))
        return invite
    }

    override suspend fun contribute(draft: GuestContributionDraft): LedgerTransaction {
        val access = requireNotNull(activeGuestAccess.value) { "A signed-in guest is required." }
        require(draft.familyId == access.familyId) { "Guest contribution must target the active guest family." }
        require(access.invite.childProfileIds.any { it == draft.childProfileId }) {
            "Guest contribution must target a sponsored child."
        }

        val ref = firestoreProvider.firestoreOrThrow()
            .collection(FAMILIES_COLLECTION)
            .document(draft.familyId.value)
            .collection(LEDGER_COLLECTION)
            .document
        val transaction = LedgerTransactionDraft(
            familyId = draft.familyId,
            childProfileId = draft.childProfileId,
            accountType = VirtualAccountType.Main,
            type = draft.kind.toLedgerTransactionType(),
            amountCents = draft.amountCents,
            concept = draft.concept,
            createdBy = LedgerActor.Guest,
        ).toLedgerTransaction(
            id = TransactionId(ref.id),
            createdAt = Clock.System.now(),
        )
        ref.set(transaction.toFirestoreMap())
        return transaction
    }

    private suspend fun resolveGuestAccess(session: UserSession) {
        mutableSyncStatus.value = RepositorySyncStatus.Loading
        val access = runCatching {
            val email = session.email?.let { normalizeFamilyGuestEmail(it) } ?: return@runCatching null
            val firestore = firestoreProvider.firestoreOrThrow()
            val invite = firestore.collection(FAMILY_GUEST_INVITES_COLLECTION)
                .document(email)
                .get()
                .takeIf { it.exists }
                ?.toFamilyGuestInvite()
                ?.takeIf { it.status == FamilyGuestInviteStatus.Active }
                ?: return@runCatching null
            val family = firestore.collection(FAMILIES_COLLECTION)
                .document(invite.familyId.value)
                .get()
                .takeIf { it.exists }
                ?.toFamily()
                ?: return@runCatching null
            val children = invite.childProfileIds.mapNotNull { childProfileId ->
                firestore.collection(FAMILIES_COLLECTION)
                    .document(invite.familyId.value)
                    .collection(CHILD_PROFILES_COLLECTION)
                    .document(childProfileId.value)
                    .get()
                    .takeIf { it.exists }
                    ?.toChildProfile()
            }
            if (children.isEmpty()) {
                null
            } else {
                GuestSponsorAccess(
                    invite = invite,
                    familyName = family.name,
                    currency = family.currency,
                    children = children,
                )
            }
        }.getOrNull()
        mutableActiveGuestAccess.value = access
        mutableSyncStatus.value = RepositorySyncStatus.Synced
    }

    private fun GuestContributionKind.toLedgerTransactionType(): LedgerTransactionType = when (this) {
        GuestContributionKind.Gift -> LedgerTransactionType.Gift
        GuestContributionKind.Match -> LedgerTransactionType.Match
    }

    private companion object {
        const val CHILD_PROFILES_COLLECTION = "childProfiles"
        const val LEDGER_COLLECTION = "ledgerTransactions"
    }
}
