package com.apptolast.fledge.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class GuestSponsorAccess(
    val invite: FamilyGuestInvite,
    val familyName: String,
    val currency: CurrencyCode,
    val children: List<ChildProfile>,
) {
    val familyId: FamilyId get() = invite.familyId
}

@Serializable
enum class GuestContributionKind {
    Gift,
    Match,
}

data class GuestContributionDraft(
    val familyId: FamilyId,
    val childProfileId: ChildProfileId,
    val kind: GuestContributionKind,
    val amountCents: MoneyCents,
    val concept: LedgerConcept,
) {
    init {
        require(amountCents.value > 0) { "Guest contributions must be positive." }
    }
}
