package com.apptolast.fledge.data.repository

import com.apptolast.fledge.domain.model.ChildPin
import com.apptolast.fledge.domain.model.ChildPinHash
import com.apptolast.fledge.domain.model.ChildPinPolicy
import com.apptolast.fledge.domain.model.ChildDevice
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.ChildSession
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.DeviceId
import com.apptolast.fledge.domain.model.Family
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.FoundationAction
import com.apptolast.fledge.domain.model.PairingCode
import com.apptolast.fledge.domain.model.PairingSession
import com.apptolast.fledge.domain.model.ParentalGateRequest
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.domain.model.VirtualMoneyConsent
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import com.apptolast.fledge.domain.security.Sha256
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class InMemoryFamilyFoundationRepository : FamilyFoundationRepository {
    private var familyCounter = 1
    private var childCounter = 1
    private var pairingCounter = 100_000
    private var deviceCounter = 1

    private val mutableActiveFamily = MutableStateFlow<Family?>(null)
    private val mutableChildren = MutableStateFlow<List<ChildProfile>>(emptyList())
    private val mutableChildDevices = MutableStateFlow<List<ChildDevice>>(emptyList())
    private val mutableChildPinPolicy = MutableStateFlow(ChildPinPolicy())
    private val mutableVirtualMoneyConsent = MutableStateFlow<VirtualMoneyConsent?>(null)
    private val mutableParentalGateRequest = MutableStateFlow<ParentalGateRequest?>(null)
    private val activePairingSessions = mutableListOf<PairingSession>()

    override val activeFamily: StateFlow<Family?> = mutableActiveFamily
    override val children: StateFlow<List<ChildProfile>> = mutableChildren
    override val childDevices: StateFlow<List<ChildDevice>> = mutableChildDevices
    override val childPinPolicy: StateFlow<ChildPinPolicy> = mutableChildPinPolicy
    override val virtualMoneyConsent: StateFlow<VirtualMoneyConsent?> = mutableVirtualMoneyConsent
    override val parentalGateRequest: StateFlow<ParentalGateRequest?> = mutableParentalGateRequest

    override suspend fun createFamily(
        name: String,
        currency: CurrencyCode,
        timeZone: TimeZoneId,
    ): Family {
        require(mutableActiveFamily.value == null) {
            "Family money settings are locked after creation."
        }
        val family = Family(
            id = FamilyId("family-${familyCounter++}"),
            name = name.trim(),
            currency = currency,
            timeZone = timeZone,
        )
        mutableActiveFamily.value = family
        return family
    }

    override suspend fun addChildProfile(
        familyId: FamilyId,
        displayName: String,
        birthYear: Int,
        avatarKey: String,
        pin: ChildPin,
    ): ChildProfile {
        require(mutableActiveFamily.value?.id == familyId) { "Family does not exist." }
        require(mutableVirtualMoneyConsent.value != null) { "Virtual money consent must be recorded first." }
        val childProfileId = ChildProfileId("child-${childCounter++}")
        val child = ChildProfile(
            id = childProfileId,
            displayName = displayName.trim(),
            birthYear = birthYear,
            avatarKey = avatarKey,
            pinHash = pin.hashFor(childProfileId.value),
        )
        mutableChildren.value = mutableChildren.value + child
        return child
    }

    override suspend fun setChildPin(childProfileId: ChildProfileId, pin: ChildPin) {
        mutableChildren.value = mutableChildren.value.map { child ->
            if (child.id == childProfileId) child.copy(pinHash = pin.hashFor(child.id.value)) else child
        }
    }

    override suspend fun validateChildPin(childProfileId: ChildProfileId, pin: ChildPin): ChildSession? {
        val child = mutableChildren.value.firstOrNull { it.id == childProfileId } ?: return null
        if (child.pinHash != pin.hashFor(child.id.value)) return null

        val unlockedAt = Clock.System.now()
        return ChildSession(
            childProfileId = childProfileId,
            unlockedAt = unlockedAt,
            expiresAt = unlockedAt.plus(mutableChildPinPolicy.value.timeoutMinutes.minutes),
        )
    }

    override suspend fun setChildPinTimeout(timeoutMinutes: Int): ChildPinPolicy {
        val policy = ChildPinPolicy(timeoutMinutes)
        mutableChildPinPolicy.value = policy
        return policy
    }

    override suspend fun startPairing(childProfileId: ChildProfileId): PairingSession {
        require(mutableChildren.value.any { it.id == childProfileId }) { "Child profile does not exist." }
        val code = PairingCode((pairingCounter++).toString())
        val session = PairingSession(
            childProfileId = childProfileId,
            code = code,
            expiresAt = Clock.System.now().plus(15.minutes),
        )
        activePairingSessions += session
        return session
    }

    override suspend fun registerChildDevice(pairingCode: PairingCode, label: String): ChildDevice {
        val now = Clock.System.now()
        require(label.isNotBlank()) { "Device label cannot be blank." }
        val pairingSession = activePairingSessions.firstOrNull { session ->
            session.code == pairingCode && session.expiresAt > now
        }
        requireNotNull(pairingSession) { "Pairing code is invalid or expired." }

        val device = ChildDevice(
            id = DeviceId("device-${deviceCounter++}"),
            childProfileId = pairingSession.childProfileId,
            label = label.trim(),
            pairingCode = pairingCode,
            pairedAt = now,
            lastSeenAt = now,
        )
        mutableChildDevices.value = mutableChildDevices.value + device
        return device
    }

    override suspend fun touchChildDevice(deviceId: DeviceId): ChildDevice? {
        val now = Clock.System.now()
        var touched: ChildDevice? = null
        mutableChildDevices.value = mutableChildDevices.value.map { device ->
            if (device.id == deviceId) {
                device.copy(lastSeenAt = now).also { touched = it }
            } else {
                device
            }
        }
        return touched
    }

    override suspend fun recordVirtualMoneyConsent(): VirtualMoneyConsent {
        val consent = VirtualMoneyConsent(
            acceptedAt = Clock.System.now(),
            disclosureVersion = VIRTUAL_MONEY_DISCLOSURE_VERSION,
        )
        mutableVirtualMoneyConsent.value = consent
        return consent
    }

    override suspend fun requireParentalGate(action: FoundationAction): ParentalGateRequest {
        val request = ParentalGateRequest(action)
        mutableParentalGateRequest.value = request
        return request
    }

    override suspend fun confirmParentalGate(): FoundationAction? {
        val action = mutableParentalGateRequest.value?.action
        mutableParentalGateRequest.value = null
        return action
    }

    private fun ChildPin.hashFor(salt: String): ChildPinHash =
        ChildPinHash(Sha256.hashHex("fledge-child-pin:$salt:$value"))

    private companion object {
        const val VIRTUAL_MONEY_DISCLOSURE_VERSION = "virtual-money-v1"
    }
}
