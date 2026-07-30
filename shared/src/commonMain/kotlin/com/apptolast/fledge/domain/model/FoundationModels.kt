package com.apptolast.fledge.domain.model

import kotlin.jvm.JvmInline
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class FamilyId(val value: String)

@Serializable
@JvmInline
value class ChildProfileId(val value: String)

@Serializable
@JvmInline
value class DeviceId(val value: String)

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
        require(value.matches(Regex("[A-Z0-9]{6}"))) { "Pairing code must contain exactly six characters." }
    }
}

@Serializable
data class Family(
    val id: FamilyId,
    val name: String,
    val currency: CurrencyCode,
    val timeZone: TimeZoneId,
    val ownerUid: String = id.value,
    val adminEmails: List<String> = emptyList(),
    val moneySettingsLocked: Boolean = true,
    val interestSettings: InterestSettings = InterestSettings(),
    val matchSettings: MatchSettings = MatchSettings(),
)

@Serializable
enum class FamilyAdminRole {
    Owner,
    Admin,
}

@Serializable
enum class FamilyAdminInviteStatus {
    Active,
    Revoked,
}

@Serializable
data class FamilyAdminInvite(
    val familyId: FamilyId,
    val email: String,
    val role: FamilyAdminRole = FamilyAdminRole.Admin,
    val status: FamilyAdminInviteStatus = FamilyAdminInviteStatus.Active,
    val invitedAt: Instant? = null,
    val invitedByUid: String? = null,
    val revokedAt: Instant? = null,
)

@Serializable
data class FamilyAdminInviteDraft(val email: String) {
    init {
        normalizeFamilyAdminEmail(email)
    }
}

fun normalizeFamilyAdminEmail(email: String): String {
    val normalized = email.trim().lowercase()
    require(normalized.matches(Regex("^[^@\\s/]+@[^@\\s/]+\\.[^@\\s/]+$"))) {
        "Admin email must be a valid email address."
    }
    return normalized
}

@Serializable
data class InterestSettings(
    val enabled: Boolean = false,
    val annualRateBasisPoints: Int = 0,
    val postingDayOfMonth: Int = 1,
    val lastPostedPeriodKey: String? = null,
) {
    init {
        require(annualRateBasisPoints in 0..5_000) {
            "Annual interest rate must be between 0.00% and 50.00%."
        }
        require(postingDayOfMonth in 1..28) {
            "Interest posting day must be between 1 and 28."
        }
        require(lastPostedPeriodKey == null || lastPostedPeriodKey.matches(Regex("\\d{6}"))) {
            "Last posted period key must use yyyyMM format."
        }
    }
}

@Serializable
data class InterestSettingsDraft(val enabled: Boolean, val annualRateBasisPoints: Int, val postingDayOfMonth: Int) {
    init {
        InterestSettings(
            enabled = enabled,
            annualRateBasisPoints = annualRateBasisPoints,
            postingDayOfMonth = postingDayOfMonth,
        )
    }
}

@Serializable
data class MatchSettings(val enabled: Boolean = false, val matchBasisPoints: Int = 0, val maxMatchCents: Long = 0) {
    init {
        require(matchBasisPoints in 0..10_000) {
            "Parental match must be between 0.00% and 100.00%."
        }
        require(maxMatchCents >= 0) {
            "Parental match cap cannot be negative."
        }
        require(!enabled || matchBasisPoints > 0) {
            "Enabled parental match requires a positive percentage."
        }
        require(!enabled || maxMatchCents > 0) {
            "Enabled parental match requires a positive cap."
        }
    }
}

@Serializable
data class MatchSettingsDraft(val enabled: Boolean, val matchBasisPoints: Int, val maxMatchCents: Long) {
    init {
        MatchSettings(
            enabled = enabled,
            matchBasisPoints = matchBasisPoints,
            maxMatchCents = maxMatchCents,
        )
    }
}

@Serializable
data class ChildProfile(
    val id: ChildProfileId,
    val displayName: String,
    val birthYear: Int,
    val avatarKey: String,
    val pinHash: ChildPinHash? = null,
)

@Serializable
data class ChildPinPolicy(val timeoutMinutes: Int = 15) {
    init {
        require(timeoutMinutes in 1..240) { "Child PIN timeout must be between 1 and 240 minutes." }
    }
}

@Serializable
data class ChildSession(val childProfileId: ChildProfileId, val unlockedAt: Instant, val expiresAt: Instant)

@Serializable
data class PairingSession(val childProfileId: ChildProfileId, val code: PairingCode, val expiresAt: Instant)

@Serializable
data class ChildDevice(
    val id: DeviceId,
    val childProfileId: ChildProfileId,
    val label: String,
    val pairingCode: PairingCode,
    val pairedAt: Instant,
    val lastSeenAt: Instant,
)

@Serializable
data class VirtualMoneyConsent(val acceptedAt: Instant, val disclosureVersion: String)

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

    @Serializable
    data object ManageSettings : FoundationAction

    @Serializable
    data object StartPurchase : FoundationAction

    @Serializable
    data object OpenExternalLink : FoundationAction

    @Serializable
    data object OpenParentZone : FoundationAction
}

@Serializable
data class ParentalGateRequest(val action: FoundationAction, val requestedAt: Instant = Clock.System.now())

@Serializable
data class SetupAction(val id: String, val label: String)
