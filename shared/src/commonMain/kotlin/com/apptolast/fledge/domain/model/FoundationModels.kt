package com.apptolast.fledge.domain.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.time.Clock
import kotlin.time.Instant

@Serializable
@JvmInline
value class FamilyId(val value: String)

@Serializable
@JvmInline
value class ChildProfileId(val value: String)

@Serializable
@JvmInline
value class CurrencyCode(val value: String) {
    init {
        require(value.length == 3) { "Currency code must use ISO 4217 format." }
    }
}

@Serializable
@JvmInline
value class TimeZoneId(val value: String) {
    init {
        require(value.isNotBlank()) { "Time zone cannot be blank." }
    }
}

@Serializable
@JvmInline
value class ChildPin(val value: String) {
    init {
        require(value.matches(Regex("\\d{4}"))) { "Child PIN must contain exactly four digits." }
    }
}

@Serializable
@JvmInline
value class ChildPinHash(val value: String) {
    init {
        require(value.matches(Regex("[a-f0-9]{64}"))) { "Child PIN hash must be a SHA-256 hex digest." }
    }
}

@Serializable
@JvmInline
value class PairingCode(val value: String) {
    init {
        require(value.matches(Regex("\\d{6}"))) { "Pairing code must contain exactly six digits." }
    }
}

@Serializable
data class Family(
    val id: FamilyId,
    val name: String,
    val currency: CurrencyCode,
    val timeZone: TimeZoneId,
    val moneySettingsLocked: Boolean = true,
)

@Serializable
data class ChildProfile(
    val id: ChildProfileId,
    val displayName: String,
    val birthYear: Int,
    val avatarKey: String,
    val pinHash: ChildPinHash? = null,
)

@Serializable
data class PairingSession(
    val childProfileId: ChildProfileId,
    val code: PairingCode,
    val expiresAt: Instant,
)

@Serializable
enum class SharedDeviceRole {
    Parent,
    Child,
}

@Serializable
sealed interface FoundationAction {
    @Serializable
    data class ResetChildPin(val childProfileId: ChildProfileId) : FoundationAction

    @Serializable
    data class PairChildDevice(val childProfileId: ChildProfileId) : FoundationAction
}

@Serializable
data class ParentalGateRequest(
    val action: FoundationAction,
    val requestedAt: Instant = Clock.System.now(),
)

@Serializable
data class SetupAction(
    val id: String,
    val label: String,
)
