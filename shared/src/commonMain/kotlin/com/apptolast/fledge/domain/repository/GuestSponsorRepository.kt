package com.apptolast.fledge.domain.repository

import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyGuestInvite
import com.apptolast.fledge.domain.model.FamilyGuestInviteDraft
import com.apptolast.fledge.domain.model.GuestContributionDraft
import com.apptolast.fledge.domain.model.GuestSponsorAccess
import com.apptolast.fledge.domain.model.LedgerTransaction
import kotlinx.coroutines.flow.StateFlow

interface GuestSponsorRepository {
    val syncStatus: StateFlow<RepositorySyncStatus>
    val activeGuestAccess: StateFlow<GuestSponsorAccess?>

    suspend fun inviteGuest(childProfileId: ChildProfileId, draft: FamilyGuestInviteDraft): FamilyGuestInvite

    suspend fun contribute(draft: GuestContributionDraft): LedgerTransaction
}
