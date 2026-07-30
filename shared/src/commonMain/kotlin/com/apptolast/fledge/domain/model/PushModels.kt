package com.apptolast.fledge.domain.model

import kotlin.jvm.JvmInline
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class PushRegistrationId(val value: String) {
    init {
        require(value.isNotBlank()) { "Push registration id cannot be blank." }
        require("/" !in value) { "Push registration id cannot contain slashes." }
    }
}

@Serializable
@JvmInline
value class PushInstallationId(val value: String) {
    init {
        require(value.isNotBlank()) { "Push installation id cannot be blank." }
        require("/" !in value) { "Push installation id cannot contain slashes." }
    }
}

@Serializable
@JvmInline
value class PushToken(val value: String) {
    init {
        require(value.isNotBlank()) { "Push token cannot be blank." }
    }
}

@Serializable
enum class PushPlatform {
    Android,
    Ios,
}

@Serializable
enum class PushRegistrationStatus {
    Active,
    Inactive,
}

@Serializable
data class PushRegistration(
    val id: PushRegistrationId,
    val familyId: FamilyId,
    val installationId: PushInstallationId,
    val token: PushToken,
    val platform: PushPlatform,
    val role: SharedDeviceRole,
    val childProfileId: ChildProfileId? = null,
    val status: PushRegistrationStatus = PushRegistrationStatus.Active,
    val updatedAt: Instant,
) {
    init {
        requireValidPushRoleProfile(role, childProfileId)
    }

    fun deactivate(updatedAt: Instant): PushRegistration = copy(
        status = PushRegistrationStatus.Inactive,
        updatedAt = updatedAt,
    )
}

data class PushRegistrationDraft(
    val familyId: FamilyId,
    val installationId: PushInstallationId,
    val token: PushToken,
    val platform: PushPlatform,
    val role: SharedDeviceRole,
    val childProfileId: ChildProfileId? = null,
) {
    init {
        requireValidPushRoleProfile(role, childProfileId)
    }
}

fun PushRegistrationDraft.toPushRegistration(id: PushRegistrationId, updatedAt: Instant): PushRegistration =
    PushRegistration(
        id = id,
        familyId = familyId,
        installationId = installationId,
        token = token,
        platform = platform,
        role = role,
        childProfileId = childProfileId,
        updatedAt = updatedAt,
    )

internal fun PushRegistrationDraft.stableRegistrationId(): PushRegistrationId {
    val roleSegment = when (role) {
        SharedDeviceRole.Parent -> "parent"
        SharedDeviceRole.Child -> "child-${requireNotNull(childProfileId).value.stablePushIdSegment()}"
    }
    return PushRegistrationId(
        listOf(
            platform.name.stablePushIdSegment(),
            roleSegment,
            installationId.value.stablePushIdSegment(),
        ).joinToString("-"),
    )
}

private fun requireValidPushRoleProfile(role: SharedDeviceRole, childProfileId: ChildProfileId?) {
    when (role) {
        SharedDeviceRole.Parent -> require(childProfileId == null) {
            "Parent push registrations cannot include a child profile."
        }
        SharedDeviceRole.Child -> require(childProfileId != null) {
            "Child push registrations require a child profile."
        }
    }
}

private fun String.stablePushIdSegment(): String = map { char ->
    when {
        char.isLetterOrDigit() -> char.lowercaseChar()
        char == '-' || char == '_' || char == '.' -> char
        else -> '_'
    }
}.joinToString("")
