package com.apptolast.fledge.data.repository

import com.apptolast.fledge.domain.model.ChildPin
import com.apptolast.fledge.domain.model.ChildPinHash
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.Family
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.FoundationAction
import com.apptolast.fledge.domain.model.PairingCode
import com.apptolast.fledge.domain.model.PairingSession
import com.apptolast.fledge.domain.model.ParentalGateRequest
import com.apptolast.fledge.domain.model.TimeZoneId
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

    private val mutableActiveFamily = MutableStateFlow<Family?>(null)
    private val mutableChildren = MutableStateFlow<List<ChildProfile>>(emptyList())
    private val mutableParentalGateRequest = MutableStateFlow<ParentalGateRequest?>(null)

    override val activeFamily: StateFlow<Family?> = mutableActiveFamily
    override val children: StateFlow<List<ChildProfile>> = mutableChildren
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

    override suspend fun startPairing(childProfileId: ChildProfileId): PairingSession {
        require(mutableChildren.value.any { it.id == childProfileId }) { "Child profile does not exist." }
        val code = PairingCode((pairingCounter++).toString())
        return PairingSession(
            childProfileId = childProfileId,
            code = code,
            expiresAt = Clock.System.now().plus(10.minutes),
        )
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
}
