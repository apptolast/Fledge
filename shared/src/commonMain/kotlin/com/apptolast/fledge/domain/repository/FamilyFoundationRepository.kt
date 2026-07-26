package com.apptolast.fledge.domain.repository

import com.apptolast.fledge.domain.model.ChildPin
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.Family
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.FoundationAction
import com.apptolast.fledge.domain.model.PairingSession
import com.apptolast.fledge.domain.model.ParentalGateRequest
import com.apptolast.fledge.domain.model.TimeZoneId
import kotlinx.coroutines.flow.StateFlow

interface FamilyFoundationRepository {
    val activeFamily: StateFlow<Family?>
    val children: StateFlow<List<ChildProfile>>
    val parentalGateRequest: StateFlow<ParentalGateRequest?>

    suspend fun createFamily(
        name: String,
        currency: CurrencyCode,
        timeZone: TimeZoneId,
    ): Family

    suspend fun addChildProfile(
        familyId: FamilyId,
        displayName: String,
        birthYear: Int,
        avatarKey: String,
        pin: ChildPin,
    ): ChildProfile

    suspend fun setChildPin(childProfileId: ChildProfileId, pin: ChildPin)

    suspend fun startPairing(childProfileId: ChildProfileId): PairingSession

    suspend fun requireParentalGate(action: FoundationAction): ParentalGateRequest

    suspend fun confirmParentalGate(): FoundationAction?
}
