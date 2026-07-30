package com.apptolast.fledge.data.repository

import com.apptolast.customlogin.domain.AuthProvider
import com.apptolast.fledge.data.remote.firebase.FirestoreProvider
import com.apptolast.fledge.domain.model.ChildDevice
import com.apptolast.fledge.domain.model.ChildPin
import com.apptolast.fledge.domain.model.ChildPinHash
import com.apptolast.fledge.domain.model.ChildPinPolicy
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.ChildSession
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.DeviceId
import com.apptolast.fledge.domain.model.Family
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.FoundationAction
import com.apptolast.fledge.domain.model.InterestSettings
import com.apptolast.fledge.domain.model.InterestSettingsDraft
import com.apptolast.fledge.domain.model.PairingCode
import com.apptolast.fledge.domain.model.PairingSession
import com.apptolast.fledge.domain.model.ParentalGateRequest
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.domain.model.VirtualMoneyConsent
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository
import com.apptolast.fledge.domain.repository.RepositorySyncStatus
import com.apptolast.fledge.domain.security.Sha256
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

class FirestoreFamilyFoundationRepository(
    private val firestoreProvider: FirestoreProvider,
    private val authProvider: AuthProvider,
) : FamilyFoundationRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var syncJob: Job? = null

    private val mutableActiveFamily = MutableStateFlow<Family?>(null)
    private val mutableChildren = MutableStateFlow<List<ChildProfile>>(emptyList())
    private val mutableChildDevices = MutableStateFlow<List<ChildDevice>>(emptyList())
    private val mutableChildPinPolicy = MutableStateFlow(ChildPinPolicy())
    private val mutableVirtualMoneyConsent = MutableStateFlow<VirtualMoneyConsent?>(null)
    private val mutableParentalGateRequest = MutableStateFlow<ParentalGateRequest?>(null)
    private val mutableSyncStatus = MutableStateFlow<RepositorySyncStatus>(RepositorySyncStatus.Loading)
    private val familyDocumentSyncStatus = MutableStateFlow<RepositorySyncStatus>(RepositorySyncStatus.Loading)
    private val childrenSyncStatus = MutableStateFlow<RepositorySyncStatus>(RepositorySyncStatus.Loading)
    private val childDevicesSyncStatus = MutableStateFlow<RepositorySyncStatus>(RepositorySyncStatus.Loading)

    override val syncStatus: StateFlow<RepositorySyncStatus> = mutableSyncStatus
    override val activeFamily: StateFlow<Family?> = mutableActiveFamily
    override val children: StateFlow<List<ChildProfile>> = mutableChildren
    override val childDevices: StateFlow<List<ChildDevice>> = mutableChildDevices
    override val childPinPolicy: StateFlow<ChildPinPolicy> = mutableChildPinPolicy
    override val virtualMoneyConsent: StateFlow<VirtualMoneyConsent?> = mutableVirtualMoneyConsent
    override val parentalGateRequest: StateFlow<ParentalGateRequest?> = mutableParentalGateRequest

    init {
        scope.launch {
            combine(familyDocumentSyncStatus, childrenSyncStatus, childDevicesSyncStatus) { family, children, devices ->
                listOf(family, children, devices).aggregateRepositorySyncStatus()
            }.collect { status ->
                mutableSyncStatus.value = status
            }
        }
        scope.launch {
            authProvider.authenticatedFamilyIds().collectLatest { familyId ->
                syncJob?.cancelAndJoin()
                if (familyId == null) {
                    clearLocalState()
                } else {
                    resetSyncStatuses(RepositorySyncStatus.Loading)
                    syncJob = launch { bindFamily(familyId) }
                }
            }
        }
    }

    override suspend fun createFamily(name: String, currency: CurrencyCode, timeZone: TimeZoneId): Family {
        val familyId = authProvider.currentFamilyId()
        val ref = familyDoc(familyId)
        require(!ref.get().exists) {
            "Family money settings are locked after creation."
        }

        val family = Family(
            id = familyId,
            name = name.trim(),
            currency = currency,
            timeZone = timeZone,
        )
        ref.set(
            family.toFirestoreMap() + mapOf(
                "childPinTimeoutMinutes" to ChildPinPolicy().timeoutMinutes,
                "createdAt" to Clock.System.now().toFirestoreTimestamp(),
                "updatedAt" to Clock.System.now().toFirestoreTimestamp(),
            ),
        )
        mutableActiveFamily.value = family
        mutableChildPinPolicy.value = ChildPinPolicy()
        return family
    }

    override suspend fun addChildProfile(
        familyId: FamilyId,
        displayName: String,
        birthYear: Int,
        avatarKey: String,
        pin: ChildPin,
    ): ChildProfile {
        require(authProvider.currentFamilyId() == familyId) { "Family does not exist." }
        require(familyDoc(familyId).get().exists) { "Family does not exist." }
        require(familyDoc(familyId).get().toVirtualMoneyConsent() != null) {
            "Virtual money consent must be recorded first."
        }

        val childRef = familyDoc(familyId).collection(CHILDREN_COLLECTION).document
        val childProfileId = ChildProfileId(childRef.id)
        val child = ChildProfile(
            id = childProfileId,
            displayName = displayName.trim(),
            birthYear = birthYear,
            avatarKey = avatarKey,
            pinHash = pin.hashFor(childProfileId.value),
        )
        childRef.set(child.toFirestoreMap(familyId))
        mutableChildren.value = (mutableChildren.value.filterNot { it.id == child.id } + child)
            .sortedBy { it.displayName }
        return child
    }

    override suspend fun setChildPin(childProfileId: ChildProfileId, pin: ChildPin) {
        val familyId = authProvider.currentFamilyId()
        val child = children.value.firstOrNull { it.id == childProfileId }
            ?: familyDoc(familyId).collection(CHILDREN_COLLECTION).document(childProfileId.value)
                .get()
                .takeIf { it.exists }
                ?.toChildProfile()
        requireNotNull(child) { "Child profile does not exist." }

        val updated = child.copy(pinHash = pin.hashFor(child.id.value))
        familyDoc(familyId).collection(CHILDREN_COLLECTION).document(childProfileId.value)
            .set(updated.toFirestoreMap(familyId), merge = true)
        mutableChildren.value = mutableChildren.value.map { if (it.id == childProfileId) updated else it }
    }

    override suspend fun validateChildPin(childProfileId: ChildProfileId, pin: ChildPin): ChildSession? {
        val familyId = authProvider.currentFamilyId()
        val child = children.value.firstOrNull { it.id == childProfileId }
            ?: familyDoc(familyId).collection(CHILDREN_COLLECTION).document(childProfileId.value)
                .get()
                .takeIf { it.exists }
                ?.toChildProfile()
            ?: return null
        if (child.pinHash != pin.hashFor(child.id.value)) return null

        val unlockedAt = Clock.System.now()
        return ChildSession(
            childProfileId = childProfileId,
            unlockedAt = unlockedAt,
            expiresAt = unlockedAt.plus(childPinPolicy.value.timeoutMinutes.minutes),
        )
    }

    override suspend fun setChildPinTimeout(timeoutMinutes: Int): ChildPinPolicy {
        val familyId = authProvider.currentFamilyId()
        val policy = ChildPinPolicy(timeoutMinutes)
        familyDoc(familyId).set(
            mapOf(
                "childPinTimeoutMinutes" to policy.timeoutMinutes,
                "updatedAt" to Clock.System.now().toFirestoreTimestamp(),
            ),
            merge = true,
        )
        mutableChildPinPolicy.value = policy
        return policy
    }

    override suspend fun updateInterestSettings(draft: InterestSettingsDraft): InterestSettings {
        val familyId = authProvider.currentFamilyId()
        val currentFamily = activeFamily.value
            ?: familyDoc(familyId).get().takeIf { it.exists }?.toFamily()
        requireNotNull(currentFamily) { "Family does not exist." }

        val settings = InterestSettings(
            enabled = draft.enabled,
            annualRateBasisPoints = draft.annualRateBasisPoints,
            postingDayOfMonth = draft.postingDayOfMonth,
            lastPostedPeriodKey = currentFamily.interestSettings.lastPostedPeriodKey,
        )
        familyDoc(familyId).set(
            settings.toFirestorePatch() + mapOf("updatedAt" to Clock.System.now().toFirestoreTimestamp()),
            merge = true,
        )
        mutableActiveFamily.value = currentFamily.copy(interestSettings = settings)
        return settings
    }

    override suspend fun startPairing(childProfileId: ChildProfileId): PairingSession {
        val familyId = authProvider.currentFamilyId()
        require(
            children.value.any { it.id == childProfileId } ||
                familyDoc(familyId).collection(CHILDREN_COLLECTION).document(childProfileId.value).get().exists,
        ) {
            "Child profile does not exist."
        }

        val code = nextPairingCode()
        val session = PairingSession(
            childProfileId = childProfileId,
            code = code,
            expiresAt = Clock.System.now().plus(15.minutes),
        )
        familyDoc(familyId).collection(PAIRING_SESSIONS_COLLECTION)
            .document(code.value)
            .set(session.toFirestoreMap(familyId))
        return session
    }

    override suspend fun registerChildDevice(pairingCode: PairingCode, label: String): ChildDevice {
        require(label.isNotBlank()) { "Device label cannot be blank." }
        val familyId = authProvider.currentFamilyId()
        val now = Clock.System.now()
        val pairingSnapshot = familyDoc(familyId).collection(PAIRING_SESSIONS_COLLECTION)
            .document(pairingCode.value)
            .get()
        val pairingSession = pairingSnapshot.takeIf { it.exists }?.toPairingSession()
        require(pairingSession != null && pairingSession.expiresAt > now) {
            "Pairing code is invalid or expired."
        }

        val deviceRef = familyDoc(familyId).collection(CHILD_DEVICES_COLLECTION).document
        val device = ChildDevice(
            id = DeviceId(deviceRef.id),
            childProfileId = pairingSession.childProfileId,
            label = label.trim(),
            pairingCode = pairingCode,
            pairedAt = now,
            lastSeenAt = now,
        )
        deviceRef.set(device.toFirestoreMap(familyId))
        mutableChildDevices.value = (mutableChildDevices.value.filterNot { it.id == device.id } + device)
            .sortedBy { it.label }
        return device
    }

    override suspend fun touchChildDevice(deviceId: DeviceId): ChildDevice? {
        val familyId = authProvider.currentFamilyId()
        val device = childDevices.value.firstOrNull { it.id == deviceId }
            ?: familyDoc(familyId).collection(CHILD_DEVICES_COLLECTION).document(deviceId.value)
                .get()
                .takeIf { it.exists }
                ?.toChildDevice()
            ?: return null
        val touched = device.copy(lastSeenAt = Clock.System.now())
        familyDoc(familyId).collection(CHILD_DEVICES_COLLECTION).document(deviceId.value)
            .set(touched.toFirestoreMap(familyId), merge = true)
        mutableChildDevices.value = mutableChildDevices.value.map { if (it.id == deviceId) touched else it }
        return touched
    }

    override suspend fun recordVirtualMoneyConsent(): VirtualMoneyConsent {
        val familyId = authProvider.currentFamilyId()
        val consent = VirtualMoneyConsent(
            acceptedAt = Clock.System.now(),
            disclosureVersion = VIRTUAL_MONEY_DISCLOSURE_VERSION,
        )
        familyDoc(familyId).set(
            consent.toFirestorePatch() + mapOf("updatedAt" to Clock.System.now().toFirestoreTimestamp()),
            merge = true,
        )
        mutableVirtualMoneyConsent.value = consent
        return consent
    }

    override suspend fun requireParentalGate(action: FoundationAction): ParentalGateRequest {
        val familyId = authProvider.currentFamilyId()
        val request = ParentalGateRequest(action)
        familyDoc(familyId).set(request.toFirestorePatch(), merge = true)
        mutableParentalGateRequest.value = request
        return request
    }

    override suspend fun confirmParentalGate(): FoundationAction? {
        val familyId = authProvider.currentFamilyId()
        val action = parentalGateRequest.value?.action
            ?: familyDoc(familyId).get().takeIf { it.exists }?.toParentalGateRequest()?.action
        familyDoc(familyId).set(
            mapOf(
                "parentalGateActionType" to null,
                "parentalGateChildProfileId" to null,
                "parentalGateRequestedAt" to null,
            ),
            merge = true,
        )
        mutableParentalGateRequest.value = null
        return action
    }

    private suspend fun bindFamily(familyId: FamilyId) = coroutineScope {
        val familyRef = familyDoc(familyId)
        val jobs = listOf(
            launch {
                familyRef.snapshots(includeMetadataChanges = true).catch { error ->
                    familyDocumentSyncStatus.value = error.toRepositorySyncError()
                }.collect { snapshot ->
                    familyDocumentSyncStatus.value = snapshot.metadata.toRepositorySyncStatus()
                    if (snapshot.exists) {
                        mutableActiveFamily.value = snapshot.toFamily()
                        mutableChildPinPolicy.value =
                            ChildPinPolicy(snapshot.optionalInt("childPinTimeoutMinutes") ?: 15)
                        mutableVirtualMoneyConsent.value = snapshot.toVirtualMoneyConsent()
                        mutableParentalGateRequest.value = snapshot.toParentalGateRequest()
                    } else {
                        mutableActiveFamily.value = null
                        mutableChildPinPolicy.value = ChildPinPolicy()
                        mutableVirtualMoneyConsent.value = null
                        mutableParentalGateRequest.value = null
                    }
                }
            },
            launch {
                familyRef.collection(CHILDREN_COLLECTION).snapshots(includeMetadataChanges = true).catch { error ->
                    childrenSyncStatus.value = error.toRepositorySyncError()
                }.collect { snapshot ->
                    childrenSyncStatus.value = snapshot.metadata.toRepositorySyncStatus()
                    mutableChildren.value = snapshot.documents
                        .filter { it.exists }
                        .mapNotNull { runCatching { it.toChildProfile() }.getOrNull() }
                        .sortedBy { it.displayName }
                }
            },
            launch {
                familyRef.collection(CHILD_DEVICES_COLLECTION).snapshots(includeMetadataChanges = true).catch { error ->
                    childDevicesSyncStatus.value = error.toRepositorySyncError()
                }.collect { snapshot ->
                    childDevicesSyncStatus.value = snapshot.metadata.toRepositorySyncStatus()
                    mutableChildDevices.value = snapshot.documents
                        .filter { it.exists }
                        .mapNotNull { runCatching { it.toChildDevice() }.getOrNull() }
                        .sortedBy { it.label }
                }
            },
        )
        jobs.joinAll()
    }

    private fun clearLocalState() {
        mutableActiveFamily.value = null
        mutableChildren.value = emptyList()
        mutableChildDevices.value = emptyList()
        mutableChildPinPolicy.value = ChildPinPolicy()
        mutableVirtualMoneyConsent.value = null
        mutableParentalGateRequest.value = null
        resetSyncStatuses(RepositorySyncStatus.Synced)
    }

    private fun resetSyncStatuses(status: RepositorySyncStatus) {
        familyDocumentSyncStatus.value = status
        childrenSyncStatus.value = status
        childDevicesSyncStatus.value = status
        mutableSyncStatus.value = status
    }

    private fun familyDoc(familyId: FamilyId) = firestoreProvider.firestoreOrThrow()
        .collection(FAMILIES_COLLECTION)
        .document(familyId.value)

    private fun ChildPin.hashFor(salt: String): ChildPinHash =
        ChildPinHash(Sha256.hashHex("fledge-child-pin:$salt:$value"))

    private fun nextPairingCode(): PairingCode = PairingCode(
        (100_000 + Random.nextInt(900_000)).toString(),
    )

    private companion object {
        const val CHILDREN_COLLECTION = "childProfiles"
        const val CHILD_DEVICES_COLLECTION = "childDevices"
        const val PAIRING_SESSIONS_COLLECTION = "pairingSessions"
        const val VIRTUAL_MONEY_DISCLOSURE_VERSION = "virtual-money-v1"
    }
}
