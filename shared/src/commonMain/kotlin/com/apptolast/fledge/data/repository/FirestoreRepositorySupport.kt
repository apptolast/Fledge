package com.apptolast.fledge.data.repository

import com.apptolast.customlogin.domain.AuthProvider
import com.apptolast.customlogin.domain.model.AuthState
import com.apptolast.customlogin.domain.model.UserSession
import com.apptolast.fledge.data.remote.firebase.FirestoreProvider
import com.apptolast.fledge.data.remote.firebase.GitLiveFirestoreProvider
import com.apptolast.fledge.domain.model.AllowanceDay
import com.apptolast.fledge.domain.model.AllowanceFrequency
import com.apptolast.fledge.domain.model.AllowanceRule
import com.apptolast.fledge.domain.model.AllowanceRuleId
import com.apptolast.fledge.domain.model.CashOutSettlement
import com.apptolast.fledge.domain.model.ChildDevice
import com.apptolast.fledge.domain.model.ChildPinHash
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.DeviceId
import com.apptolast.fledge.domain.model.Family
import com.apptolast.fledge.domain.model.FamilyAdminInvite
import com.apptolast.fledge.domain.model.FamilyAdminInviteStatus
import com.apptolast.fledge.domain.model.FamilyAdminRole
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.FoundationAction
import com.apptolast.fledge.domain.model.InterestSettings
import com.apptolast.fledge.domain.model.LedgerActor
import com.apptolast.fledge.domain.model.LedgerConcept
import com.apptolast.fledge.domain.model.LedgerTransaction
import com.apptolast.fledge.domain.model.LedgerTransactionType
import com.apptolast.fledge.domain.model.LedgerTransferGroupId
import com.apptolast.fledge.domain.model.MatchSettings
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.MoneyPotType
import com.apptolast.fledge.domain.model.PairingCode
import com.apptolast.fledge.domain.model.PairingSession
import com.apptolast.fledge.domain.model.ParentalGateRequest
import com.apptolast.fledge.domain.model.PushInstallationId
import com.apptolast.fledge.domain.model.PushPlatform
import com.apptolast.fledge.domain.model.PushRegistration
import com.apptolast.fledge.domain.model.PushRegistrationId
import com.apptolast.fledge.domain.model.PushRegistrationStatus
import com.apptolast.fledge.domain.model.PushToken
import com.apptolast.fledge.domain.model.SavingsGoal
import com.apptolast.fledge.domain.model.SavingsGoalId
import com.apptolast.fledge.domain.model.SavingsGoalStatus
import com.apptolast.fledge.domain.model.SettlementId
import com.apptolast.fledge.domain.model.SettlementStatus
import com.apptolast.fledge.domain.model.SharedDeviceRole
import com.apptolast.fledge.domain.model.TaskAssignment
import com.apptolast.fledge.domain.model.TaskAssignmentId
import com.apptolast.fledge.domain.model.TaskInstance
import com.apptolast.fledge.domain.model.TaskInstanceId
import com.apptolast.fledge.domain.model.TaskInstanceStatus
import com.apptolast.fledge.domain.model.TaskRecurrence
import com.apptolast.fledge.domain.model.TaskTemplate
import com.apptolast.fledge.domain.model.TaskTemplateId
import com.apptolast.fledge.domain.model.TaskTemplateSource
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.domain.model.TransactionId
import com.apptolast.fledge.domain.model.VirtualAccountType
import com.apptolast.fledge.domain.model.VirtualMoneyConsent
import com.apptolast.fledge.domain.model.normalizeFamilyAdminEmail
import com.apptolast.fledge.domain.model.toMoneyPotType
import com.apptolast.fledge.domain.repository.RepositorySyncStatus
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.SnapshotMetadata
import dev.gitlive.firebase.firestore.Timestamp
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest

internal const val FAMILIES_COLLECTION = "families"
internal const val FAMILY_ADMIN_INVITES_COLLECTION = "familyAdminInvites"

internal fun FirestoreProvider.firestoreOrThrow(): FirebaseFirestore {
    check(isAvailable) { "Firebase is not configured; Firestore persistence is unavailable." }
    val gitLiveProvider = this as? GitLiveFirestoreProvider
    checkNotNull(gitLiveProvider) { "Production Firestore provider is required for persistence." }
    return gitLiveProvider.firestore()
}

internal data class AuthenticatedFamilyAccess(val familyId: FamilyId, val role: FamilyAdminRole)

internal suspend fun AuthProvider.currentOwnerFamilyId(): FamilyId {
    val session = getCurrentSession()
    checkNotNull(session) { "A signed-in parent is required before using Firestore persistence." }
    return FamilyId(session.userId)
}

internal suspend fun AuthProvider.currentFamilyAccess(firestoreProvider: FirestoreProvider): AuthenticatedFamilyAccess {
    val session = getCurrentSession()
    checkNotNull(session) { "A signed-in parent is required before using Firestore persistence." }
    return firestoreProvider.resolveFamilyAccess(session)
}

internal suspend fun AuthProvider.currentFamilyId(firestoreProvider: FirestoreProvider): FamilyId =
    currentFamilyAccess(firestoreProvider).familyId

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
internal fun AuthProvider.authenticatedFamilyAccesses(
    firestoreProvider: FirestoreProvider,
): Flow<AuthenticatedFamilyAccess?> = observeAuthState()
    .mapLatest { state ->
        (state as? AuthState.Authenticated)?.session?.let { session ->
            firestoreProvider.resolveFamilyAccess(session)
        }
    }
    .distinctUntilChanged()
    .catch { emit(null) }

internal fun AuthProvider.authenticatedFamilyIds(firestoreProvider: FirestoreProvider): Flow<FamilyId?> =
    authenticatedFamilyAccesses(firestoreProvider)
        .map { access -> access?.familyId }
        .distinctUntilChanged()

private suspend fun FirestoreProvider.resolveFamilyAccess(session: UserSession): AuthenticatedFamilyAccess {
    val firestore = firestoreOrThrow()
    val ownerFamilyId = FamilyId(session.userId)
    val ownerFamily = firestore.collection(FAMILIES_COLLECTION).document(ownerFamilyId.value).get()
    if (ownerFamily.exists) {
        return AuthenticatedFamilyAccess(ownerFamilyId, FamilyAdminRole.Owner)
    }

    val normalizedEmail = session.email?.let { runCatching { normalizeFamilyAdminEmail(it) }.getOrNull() }
    val invite = normalizedEmail?.let { email ->
        runCatching {
            firestore.collection(FAMILY_ADMIN_INVITES_COLLECTION)
                .document(email)
                .get()
                .takeIf { it.exists }
                ?.toFamilyAdminInvite()
        }.getOrNull()
    }
    if (invite?.status == FamilyAdminInviteStatus.Active) {
        return AuthenticatedFamilyAccess(invite.familyId, FamilyAdminRole.Admin)
    }

    return AuthenticatedFamilyAccess(ownerFamilyId, FamilyAdminRole.Owner)
}

internal fun Instant.toFirestoreTimestamp(): Timestamp = Timestamp(epochSeconds, nanosecondsOfSecond)

internal fun Timestamp.toKotlinInstant(): Instant = Instant.fromEpochSeconds(seconds, nanoseconds)

internal fun SnapshotMetadata.toRepositorySyncStatus(): RepositorySyncStatus = when {
    hasPendingWrites -> RepositorySyncStatus.PendingWrites
    isFromCache -> RepositorySyncStatus.FromCache
    else -> RepositorySyncStatus.Synced
}

internal fun Iterable<RepositorySyncStatus>.aggregateRepositorySyncStatus(): RepositorySyncStatus = when {
    any { it is RepositorySyncStatus.Error } -> first { it is RepositorySyncStatus.Error }
    any { it == RepositorySyncStatus.PendingWrites } -> RepositorySyncStatus.PendingWrites
    any { it == RepositorySyncStatus.Loading } -> RepositorySyncStatus.Loading
    any { it == RepositorySyncStatus.FromCache } -> RepositorySyncStatus.FromCache
    else -> RepositorySyncStatus.Synced
}

internal fun Throwable.toRepositorySyncError(): RepositorySyncStatus.Error = RepositorySyncStatus.Error(message)

internal fun DocumentSnapshot.requiredString(field: String): String = get(field)

internal fun DocumentSnapshot.requiredStringList(field: String): List<String> = get(field)

internal fun DocumentSnapshot.optionalString(field: String): String? =
    if (contains(field)) get<String?>(field) else null

internal fun DocumentSnapshot.optionalStringList(field: String): List<String> =
    if (contains(field)) get<List<String>?>(field).orEmpty() else emptyList()

internal fun DocumentSnapshot.requiredLong(field: String): Long = get(field)

internal fun DocumentSnapshot.optionalLong(field: String): Long? = if (contains(field)) get<Long?>(field) else null

internal fun DocumentSnapshot.requiredInt(field: String): Int = get(field)

internal fun DocumentSnapshot.optionalInt(field: String): Int? = if (contains(field)) get<Int?>(field) else null

internal fun DocumentSnapshot.optionalBoolean(field: String): Boolean? =
    if (contains(field)) get<Boolean?>(field) else null

internal fun DocumentSnapshot.requiredBoolean(field: String): Boolean = get(field)

internal fun DocumentSnapshot.requiredTimestamp(field: String): Instant = get<Timestamp>(field).toKotlinInstant()

internal fun DocumentSnapshot.optionalTimestamp(field: String): Instant? =
    if (contains(field)) get<Timestamp?>(field)?.toKotlinInstant() else null

internal fun Family.toFirestoreMap(): Map<String, Any?> = mapOf(
    "familyId" to id.value,
    "name" to name,
    "currency" to currency.value,
    "timeZone" to timeZone.value,
    "ownerUid" to ownerUid,
    "adminEmails" to adminEmails,
    "moneySettingsLocked" to moneySettingsLocked,
    "interestEnabled" to interestSettings.enabled,
    "interestAnnualRateBasisPoints" to interestSettings.annualRateBasisPoints,
    "interestPostingDayOfMonth" to interestSettings.postingDayOfMonth,
    "interestLastPostedPeriodKey" to interestSettings.lastPostedPeriodKey,
    "matchEnabled" to matchSettings.enabled,
    "matchBasisPoints" to matchSettings.matchBasisPoints,
    "matchMaxCents" to matchSettings.maxMatchCents,
)

internal fun DocumentSnapshot.toFamily(): Family = Family(
    id = FamilyId(id),
    name = requiredString("name"),
    currency = CurrencyCode(requiredString("currency")),
    timeZone = TimeZoneId(requiredString("timeZone")),
    ownerUid = optionalString("ownerUid") ?: id,
    adminEmails = optionalStringList("adminEmails").map(::normalizeFamilyAdminEmail).distinct().sorted(),
    moneySettingsLocked = optionalBoolean("moneySettingsLocked") ?: true,
    interestSettings = toInterestSettings(),
    matchSettings = toMatchSettings(),
)

internal fun FamilyAdminInvite.toFirestoreMap(): Map<String, Any?> = mapOf(
    "familyId" to familyId.value,
    "email" to email,
    "role" to role.name,
    "status" to status.name,
    "invitedAt" to invitedAt?.toFirestoreTimestamp(),
    "invitedByUid" to invitedByUid,
    "revokedAt" to revokedAt?.toFirestoreTimestamp(),
)

internal fun DocumentSnapshot.toFamilyAdminInvite(): FamilyAdminInvite = FamilyAdminInvite(
    familyId = FamilyId(requiredString("familyId")),
    email = normalizeFamilyAdminEmail(requiredString("email")),
    role = FamilyAdminRole.valueOf(optionalString("role") ?: FamilyAdminRole.Admin.name),
    status = FamilyAdminInviteStatus.valueOf(optionalString("status") ?: FamilyAdminInviteStatus.Active.name),
    invitedAt = optionalTimestamp("invitedAt"),
    invitedByUid = optionalString("invitedByUid"),
    revokedAt = optionalTimestamp("revokedAt"),
)

internal fun InterestSettings.toFirestorePatch(): Map<String, Any?> = mapOf(
    "interestEnabled" to enabled,
    "interestAnnualRateBasisPoints" to annualRateBasisPoints,
    "interestPostingDayOfMonth" to postingDayOfMonth,
    "interestLastPostedPeriodKey" to lastPostedPeriodKey,
)

internal fun DocumentSnapshot.toInterestSettings(): InterestSettings = InterestSettings(
    enabled = optionalBoolean("interestEnabled") ?: false,
    annualRateBasisPoints = optionalInt("interestAnnualRateBasisPoints") ?: 0,
    postingDayOfMonth = optionalInt("interestPostingDayOfMonth") ?: 1,
    lastPostedPeriodKey = optionalString("interestLastPostedPeriodKey"),
)

internal fun MatchSettings.toFirestorePatch(): Map<String, Any?> = mapOf(
    "matchEnabled" to enabled,
    "matchBasisPoints" to matchBasisPoints,
    "matchMaxCents" to maxMatchCents,
)

internal fun DocumentSnapshot.toMatchSettings(): MatchSettings = MatchSettings(
    enabled = optionalBoolean("matchEnabled") ?: false,
    matchBasisPoints = optionalInt("matchBasisPoints") ?: 0,
    maxMatchCents = optionalLong("matchMaxCents") ?: 0L,
)

internal fun ChildProfile.toFirestoreMap(familyId: FamilyId): Map<String, Any?> = mapOf(
    "familyId" to familyId.value,
    "childProfileId" to id.value,
    "displayName" to displayName,
    "birthYear" to birthYear,
    "avatarKey" to avatarKey,
    "pinHash" to pinHash?.value,
)

internal fun DocumentSnapshot.toChildProfile(): ChildProfile = ChildProfile(
    id = ChildProfileId(id),
    displayName = requiredString("displayName"),
    birthYear = requiredInt("birthYear"),
    avatarKey = requiredString("avatarKey"),
    pinHash = optionalString("pinHash")?.let(::ChildPinHash),
)

internal fun ChildDevice.toFirestoreMap(familyId: FamilyId): Map<String, Any?> = mapOf(
    "familyId" to familyId.value,
    "deviceId" to id.value,
    "childProfileId" to childProfileId.value,
    "label" to label,
    "pairingCode" to pairingCode.value,
    "pairedAt" to pairedAt.toFirestoreTimestamp(),
    "lastSeenAt" to lastSeenAt.toFirestoreTimestamp(),
)

internal fun DocumentSnapshot.toChildDevice(): ChildDevice = ChildDevice(
    id = DeviceId(id),
    childProfileId = ChildProfileId(requiredString("childProfileId")),
    label = requiredString("label"),
    pairingCode = PairingCode(requiredString("pairingCode")),
    pairedAt = requiredTimestamp("pairedAt"),
    lastSeenAt = requiredTimestamp("lastSeenAt"),
)

internal fun PairingSession.toFirestoreMap(familyId: FamilyId): Map<String, Any?> = mapOf(
    "familyId" to familyId.value,
    "childProfileId" to childProfileId.value,
    "code" to code.value,
    "expiresAt" to expiresAt.toFirestoreTimestamp(),
)

internal fun DocumentSnapshot.toPairingSession(): PairingSession = PairingSession(
    childProfileId = ChildProfileId(requiredString("childProfileId")),
    code = PairingCode(id),
    expiresAt = requiredTimestamp("expiresAt"),
)

internal fun LedgerTransaction.toFirestoreMap(): Map<String, Any?> = mapOf(
    "familyId" to familyId.value,
    "childProfileId" to childProfileId.value,
    "accountType" to accountType.name,
    "type" to type.name,
    "amountCents" to amountCents.value,
    "concept" to concept.value,
    "createdBy" to createdBy.name,
    "createdAt" to createdAt.toFirestoreTimestamp(),
    "reversesTransactionId" to reversesTransactionId?.value,
    "transferGroupId" to transferGroupId?.value,
)

internal fun DocumentSnapshot.toLedgerTransaction(): LedgerTransaction = LedgerTransaction(
    id = TransactionId(id),
    familyId = FamilyId(requiredString("familyId")),
    childProfileId = ChildProfileId(requiredString("childProfileId")),
    accountType = VirtualAccountType.valueOf(requiredString("accountType")),
    type = LedgerTransactionType.valueOf(requiredString("type")),
    amountCents = MoneyCents(requiredLong("amountCents")),
    concept = LedgerConcept(requiredString("concept")),
    createdBy = LedgerActor.valueOf(requiredString("createdBy")),
    createdAt = requiredTimestamp("createdAt"),
    reversesTransactionId = optionalString("reversesTransactionId")?.let(::TransactionId),
    transferGroupId = optionalString("transferGroupId")?.let(::LedgerTransferGroupId),
)

internal fun AllowanceRule.toFirestoreMap(): Map<String, Any?> = mapOf(
    "familyId" to familyId.value,
    "childProfileId" to childProfileId.value,
    "accountType" to accountType.name,
    "frequency" to frequency.name,
    "day" to day.value,
    "amountCents" to amountCents.value,
    "concept" to concept.value,
    "timeZone" to timeZone.value,
    "nextRunAt" to nextRunAt.toFirestoreTimestamp(),
    "active" to active,
    "createdAt" to createdAt.toFirestoreTimestamp(),
    "updatedAt" to updatedAt.toFirestoreTimestamp(),
)

internal fun DocumentSnapshot.toAllowanceRule(): AllowanceRule = AllowanceRule(
    id = AllowanceRuleId(id),
    familyId = FamilyId(requiredString("familyId")),
    childProfileId = ChildProfileId(requiredString("childProfileId")),
    accountType = VirtualAccountType.valueOf(requiredString("accountType")),
    frequency = AllowanceFrequency.valueOf(requiredString("frequency")),
    day = AllowanceDay(requiredInt("day")),
    amountCents = MoneyCents(requiredLong("amountCents")),
    concept = LedgerConcept(requiredString("concept")),
    timeZone = TimeZoneId(requiredString("timeZone")),
    nextRunAt = requiredTimestamp("nextRunAt"),
    active = optionalBoolean("active") ?: true,
    createdAt = requiredTimestamp("createdAt"),
    updatedAt = requiredTimestamp("updatedAt"),
)

internal fun SavingsGoal.toFirestoreMap(): Map<String, Any?> = mapOf(
    "familyId" to familyId.value,
    "childProfileId" to childProfileId.value,
    "title" to title,
    "targetCents" to targetCents.value,
    "accountType" to accountType.name,
    "potType" to potType.name,
    "iconKey" to iconKey,
    "imageUri" to imageUri,
    "status" to status.name,
    "createdAt" to createdAt.toFirestoreTimestamp(),
    "updatedAt" to updatedAt.toFirestoreTimestamp(),
)

internal fun DocumentSnapshot.toSavingsGoal(): SavingsGoal {
    val accountType = VirtualAccountType.valueOf(requiredString("accountType"))
    return SavingsGoal(
        id = SavingsGoalId(id),
        familyId = FamilyId(requiredString("familyId")),
        childProfileId = ChildProfileId(requiredString("childProfileId")),
        title = requiredString("title"),
        targetCents = MoneyCents(requiredLong("targetCents")),
        accountType = accountType,
        potType = optionalString("potType")?.let(MoneyPotType::valueOf) ?: accountType.toMoneyPotType(),
        iconKey = optionalString("iconKey"),
        imageUri = optionalString("imageUri"),
        status = SavingsGoalStatus.valueOf(requiredString("status")),
        createdAt = requiredTimestamp("createdAt"),
        updatedAt = requiredTimestamp("updatedAt"),
    )
}

internal fun CashOutSettlement.toFirestoreMap(): Map<String, Any?> = mapOf(
    "familyId" to familyId.value,
    "childProfileId" to childProfileId.value,
    "amountCents" to amountCents.value,
    "concept" to concept.value,
    "status" to status.name,
    "requestedAt" to requestedAt.toFirestoreTimestamp(),
    "paidByParentAt" to paidByParentAt?.toFirestoreTimestamp(),
    "confirmedByChildAt" to confirmedByChildAt?.toFirestoreTimestamp(),
    "settlementTransactionId" to settlementTransactionId?.value,
)

internal fun DocumentSnapshot.toCashOutSettlement(): CashOutSettlement = CashOutSettlement(
    id = SettlementId(id),
    familyId = FamilyId(requiredString("familyId")),
    childProfileId = ChildProfileId(requiredString("childProfileId")),
    amountCents = MoneyCents(requiredLong("amountCents")),
    concept = LedgerConcept(requiredString("concept")),
    status = SettlementStatus.valueOf(requiredString("status")),
    requestedAt = requiredTimestamp("requestedAt"),
    paidByParentAt = optionalTimestamp("paidByParentAt"),
    confirmedByChildAt = optionalTimestamp("confirmedByChildAt"),
    settlementTransactionId = optionalString("settlementTransactionId")?.let(::TransactionId),
)

internal fun PushRegistration.toFirestoreMap(): Map<String, Any?> = mapOf(
    "familyId" to familyId.value,
    "childProfileId" to childProfileId?.value,
    "installationId" to installationId.value,
    "token" to token.value,
    "platform" to platform.name,
    "role" to role.name,
    "status" to status.name,
    "updatedAt" to updatedAt.toFirestoreTimestamp(),
)

internal fun DocumentSnapshot.toPushRegistration(): PushRegistration = PushRegistration(
    id = PushRegistrationId(id),
    familyId = FamilyId(requiredString("familyId")),
    childProfileId = optionalString("childProfileId")?.let(::ChildProfileId),
    installationId = PushInstallationId(requiredString("installationId")),
    token = PushToken(requiredString("token")),
    platform = PushPlatform.valueOf(requiredString("platform")),
    role = SharedDeviceRole.valueOf(requiredString("role")),
    status = PushRegistrationStatus.valueOf(requiredString("status")),
    updatedAt = requiredTimestamp("updatedAt"),
)

internal fun TaskTemplate.toFirestoreMap(): Map<String, Any?> = mapOf(
    "familyId" to familyId.value,
    "title" to title,
    "description" to description,
    "iconKey" to iconKey,
    "defaultValueCents" to defaultValueCents.value,
    "requiresPhoto" to requiresPhoto,
    "suggestedMinAge" to suggestedMinAge,
    "suggestedMaxAge" to suggestedMaxAge,
    "source" to source.name,
    "sourceChildProfileId" to sourceChildProfileId?.value,
    "sourceKey" to sourceKey,
    "archived" to archived,
    "createdAt" to createdAt.toFirestoreTimestamp(),
    "updatedAt" to updatedAt.toFirestoreTimestamp(),
)

internal fun DocumentSnapshot.toTaskTemplate(): TaskTemplate = TaskTemplate(
    id = TaskTemplateId(id),
    familyId = FamilyId(requiredString("familyId")),
    title = requiredString("title"),
    description = requiredString("description"),
    iconKey = requiredString("iconKey"),
    defaultValueCents = MoneyCents(requiredLong("defaultValueCents")),
    requiresPhoto = requiredBoolean("requiresPhoto"),
    suggestedMinAge = optionalInt("suggestedMinAge"),
    suggestedMaxAge = optionalInt("suggestedMaxAge"),
    source = TaskTemplateSource.valueOf(requiredString("source")),
    sourceChildProfileId = optionalString("sourceChildProfileId")?.let(::ChildProfileId),
    sourceKey = optionalString("sourceKey"),
    archived = optionalBoolean("archived") ?: false,
    createdAt = requiredTimestamp("createdAt"),
    updatedAt = requiredTimestamp("updatedAt"),
)

internal fun TaskAssignment.toFirestoreMap(): Map<String, Any?> = mapOf(
    "familyId" to familyId.value,
    "taskTemplateId" to taskTemplateId.value,
    "title" to title,
    "rewardCents" to rewardCents.value,
    "requiresPhoto" to requiresPhoto,
    "childProfileIds" to childProfileIds.map { it.value },
    "recurrence" to recurrence.name,
    "dueAt" to dueAt.toFirestoreTimestamp(),
    "customIntervalDays" to customIntervalDays,
    "active" to active,
    "createdAt" to createdAt.toFirestoreTimestamp(),
    "updatedAt" to updatedAt.toFirestoreTimestamp(),
)

internal fun DocumentSnapshot.toTaskAssignment(templateFallback: TaskTemplate? = null): TaskAssignment {
    val taskTemplateId = TaskTemplateId(requiredString("taskTemplateId"))
    val template = templateFallback?.takeIf { it.id == taskTemplateId }
    return TaskAssignment(
        id = TaskAssignmentId(id),
        familyId = FamilyId(requiredString("familyId")),
        taskTemplateId = taskTemplateId,
        title = optionalString("title")?.takeIf { it.isNotBlank() }
            ?: requireNotNull(template) { "Legacy task assignment requires its source template." }.title,
        rewardCents = MoneyCents(
            optionalLong("rewardCents") ?: requireNotNull(template) {
                "Legacy task assignment requires its source template."
            }.defaultValueCents.value,
        ),
        requiresPhoto = optionalBoolean("requiresPhoto") ?: template?.requiresPhoto ?: requiredBoolean("requiresPhoto"),
        childProfileIds = requiredStringList("childProfileIds").map(::ChildProfileId),
        recurrence = TaskRecurrence.valueOf(requiredString("recurrence")),
        dueAt = requiredTimestamp("dueAt"),
        customIntervalDays = optionalInt("customIntervalDays"),
        active = optionalBoolean("active") ?: true,
        createdAt = requiredTimestamp("createdAt"),
        updatedAt = requiredTimestamp("updatedAt"),
    )
}

internal fun TaskInstance.toFirestoreMap(): Map<String, Any?> = mapOf(
    "familyId" to familyId.value,
    "taskAssignmentId" to taskAssignmentId.value,
    "taskTemplateId" to taskTemplateId.value,
    "childProfileId" to childProfileId.value,
    "title" to title,
    "rewardCents" to rewardCents.value,
    "requiresPhoto" to requiresPhoto,
    "status" to status.name,
    "dueAt" to dueAt.toFirestoreTimestamp(),
    "periodKey" to periodKey,
    "createdAt" to createdAt.toFirestoreTimestamp(),
    "updatedAt" to updatedAt.toFirestoreTimestamp(),
    "submittedAt" to submittedAt?.toFirestoreTimestamp(),
    "reviewedAt" to reviewedAt?.toFirestoreTimestamp(),
    "expiredAt" to expiredAt?.toFirestoreTimestamp(),
    "photoEvidenceUri" to photoEvidenceUri,
    "approvedRewardCents" to approvedRewardCents?.value,
    "approvalTransactionId" to approvalTransactionId?.value,
    "rejectionReason" to rejectionReason,
)

internal fun DocumentSnapshot.toTaskInstance(): TaskInstance = TaskInstance(
    id = TaskInstanceId(id),
    familyId = FamilyId(requiredString("familyId")),
    taskAssignmentId = TaskAssignmentId(requiredString("taskAssignmentId")),
    taskTemplateId = TaskTemplateId(requiredString("taskTemplateId")),
    childProfileId = ChildProfileId(requiredString("childProfileId")),
    title = requiredString("title"),
    rewardCents = MoneyCents(requiredLong("rewardCents")),
    requiresPhoto = requiredBoolean("requiresPhoto"),
    status = TaskInstanceStatus.valueOf(requiredString("status")),
    dueAt = requiredTimestamp("dueAt"),
    periodKey = requiredString("periodKey"),
    createdAt = requiredTimestamp("createdAt"),
    updatedAt = requiredTimestamp("updatedAt"),
    submittedAt = optionalTimestamp("submittedAt"),
    reviewedAt = optionalTimestamp("reviewedAt"),
    expiredAt = optionalTimestamp("expiredAt"),
    photoEvidenceUri = optionalString("photoEvidenceUri"),
    approvedRewardCents = optionalLong("approvedRewardCents")?.let(::MoneyCents),
    approvalTransactionId = optionalString("approvalTransactionId")?.let(::TransactionId),
    rejectionReason = optionalString("rejectionReason"),
)

internal fun VirtualMoneyConsent.toFirestorePatch(): Map<String, Any?> = mapOf(
    "virtualMoneyConsentAcceptedAt" to acceptedAt.toFirestoreTimestamp(),
    "virtualMoneyConsentDisclosureVersion" to disclosureVersion,
)

internal fun DocumentSnapshot.toVirtualMoneyConsent(): VirtualMoneyConsent? {
    val acceptedAt = optionalTimestamp("virtualMoneyConsentAcceptedAt") ?: return null
    val version = optionalString("virtualMoneyConsentDisclosureVersion") ?: return null
    return VirtualMoneyConsent(acceptedAt = acceptedAt, disclosureVersion = version)
}

internal fun ParentalGateRequest.toFirestorePatch(): Map<String, Any?> = mapOf(
    "parentalGateActionType" to action.actionType,
    "parentalGateChildProfileId" to (
        (action as? FoundationAction.ResetChildPin)?.childProfileId?.value
            ?: (action as? FoundationAction.PairChildDevice)?.childProfileId?.value
        ),
    "parentalGateRequestedAt" to requestedAt.toFirestoreTimestamp(),
)

internal fun DocumentSnapshot.toParentalGateRequest(): ParentalGateRequest? {
    val type = optionalString("parentalGateActionType") ?: return null
    val requestedAt = optionalTimestamp("parentalGateRequestedAt") ?: return null
    return ParentalGateRequest(
        action = foundationActionOf(type, optionalString("parentalGateChildProfileId")),
        requestedAt = requestedAt,
    )
}

internal val FoundationAction.actionType: String
    get() = when (this) {
        FoundationAction.ManageSettings -> "ManageSettings"
        FoundationAction.OpenExternalLink -> "OpenExternalLink"
        FoundationAction.OpenParentZone -> "OpenParentZone"
        is FoundationAction.PairChildDevice -> "PairChildDevice"
        is FoundationAction.ResetChildPin -> "ResetChildPin"
        FoundationAction.StartPurchase -> "StartPurchase"
    }

private fun foundationActionOf(type: String, childProfileId: String?): FoundationAction = when (type) {
    "ManageSettings" -> FoundationAction.ManageSettings
    "OpenExternalLink" -> FoundationAction.OpenExternalLink
    "OpenParentZone" -> FoundationAction.OpenParentZone
    "PairChildDevice" -> FoundationAction.PairChildDevice(ChildProfileId(requireNotNull(childProfileId)))
    "ResetChildPin" -> FoundationAction.ResetChildPin(ChildProfileId(requireNotNull(childProfileId)))
    "StartPurchase" -> FoundationAction.StartPurchase
    else -> error("Unknown parental gate action: $type")
}
