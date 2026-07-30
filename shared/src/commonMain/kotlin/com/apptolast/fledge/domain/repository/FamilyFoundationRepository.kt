package com.apptolast.fledge.domain.repository

import com.apptolast.fledge.domain.model.ChildDevice
import com.apptolast.fledge.domain.model.ChildPin
import com.apptolast.fledge.domain.model.ChildPinPolicy
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.ChildSession
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.DeviceId
import com.apptolast.fledge.domain.model.Family
import com.apptolast.fledge.domain.model.FamilyAdminInvite
import com.apptolast.fledge.domain.model.FamilyAdminInviteDraft
import com.apptolast.fledge.domain.model.FamilyAdminRole
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.FoundationAction
import com.apptolast.fledge.domain.model.InterestSettings
import com.apptolast.fledge.domain.model.InterestSettingsDraft
import com.apptolast.fledge.domain.model.MatchSettings
import com.apptolast.fledge.domain.model.MatchSettingsDraft
import com.apptolast.fledge.domain.model.PairingCode
import com.apptolast.fledge.domain.model.PairingSession
import com.apptolast.fledge.domain.model.ParentalGateRequest
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.domain.model.VirtualMoneyConsent
import kotlinx.coroutines.flow.StateFlow

interface FamilyFoundationRepository {
    val syncStatus: StateFlow<RepositorySyncStatus>
    val activeFamily: StateFlow<Family?>
    val activeAdminRole: StateFlow<FamilyAdminRole?>
    val adminInvites: StateFlow<List<FamilyAdminInvite>>
    val children: StateFlow<List<ChildProfile>>
    val childDevices: StateFlow<List<ChildDevice>>
    val childPinPolicy: StateFlow<ChildPinPolicy>
    val virtualMoneyConsent: StateFlow<VirtualMoneyConsent?>
    val parentalGateRequest: StateFlow<ParentalGateRequest?>

    suspend fun createFamily(name: String, currency: CurrencyCode, timeZone: TimeZoneId): Family

    suspend fun inviteAdmin(draft: FamilyAdminInviteDraft): FamilyAdminInvite

    suspend fun revokeAdminInvite(email: String): FamilyAdminInvite?

    suspend fun addChildProfile(
        familyId: FamilyId,
        displayName: String,
        birthYear: Int,
        avatarKey: String,
        pin: ChildPin,
    ): ChildProfile

    suspend fun setChildPin(childProfileId: ChildProfileId, pin: ChildPin)

    suspend fun validateChildPin(childProfileId: ChildProfileId, pin: ChildPin): ChildSession?

    suspend fun setChildPinTimeout(timeoutMinutes: Int): ChildPinPolicy

    suspend fun updateInterestSettings(draft: InterestSettingsDraft): InterestSettings

    suspend fun updateMatchSettings(draft: MatchSettingsDraft): MatchSettings

    suspend fun startPairing(childProfileId: ChildProfileId): PairingSession

    suspend fun registerChildDevice(pairingCode: PairingCode, label: String): ChildDevice

    suspend fun touchChildDevice(deviceId: DeviceId): ChildDevice?

    suspend fun recordVirtualMoneyConsent(): VirtualMoneyConsent

    suspend fun requireParentalGate(action: FoundationAction): ParentalGateRequest

    suspend fun confirmParentalGate(): FoundationAction?
}
