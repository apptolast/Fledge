package com.apptolast.fledge.data.repository

import com.apptolast.customlogin.domain.AuthProvider
import com.apptolast.fledge.data.remote.firebase.FirestoreProvider
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.SavingsGoal
import com.apptolast.fledge.domain.model.SavingsGoalDraft
import com.apptolast.fledge.domain.model.SavingsGoalId
import com.apptolast.fledge.domain.model.toSavingsGoal
import com.apptolast.fledge.domain.repository.RepositorySyncStatus
import com.apptolast.fledge.domain.repository.SavingsGoalRepository
import com.apptolast.fledge.domain.repository.sortedForSavingsGoals
import kotlin.time.Instant
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
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

class FirestoreSavingsGoalRepository(
    private val firestoreProvider: FirestoreProvider,
    private val authProvider: AuthProvider,
) : SavingsGoalRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var syncJob: Job? = null
    private val mutableSyncStatus = MutableStateFlow<RepositorySyncStatus>(RepositorySyncStatus.Loading)
    private val mutableGoals = MutableStateFlow<List<SavingsGoal>>(emptyList())

    override val syncStatus: StateFlow<RepositorySyncStatus> = mutableSyncStatus
    override val goals: StateFlow<List<SavingsGoal>> = mutableGoals

    init {
        scope.launch {
            authProvider.authenticatedFamilyIds().collectLatest { familyId ->
                syncJob?.cancelAndJoin()
                if (familyId == null) {
                    mutableGoals.value = emptyList()
                    mutableSyncStatus.value = RepositorySyncStatus.Synced
                } else {
                    mutableSyncStatus.value = RepositorySyncStatus.Loading
                    syncJob = launch { bindSavingsGoals(familyId) }
                }
            }
        }
    }

    private suspend fun bindSavingsGoals(familyId: FamilyId) = coroutineScope {
        val jobs = listOf(
            launch {
                savingsGoalCollection(familyId).snapshots(includeMetadataChanges = true)
                    .catch { error ->
                        mutableSyncStatus.value = error.toRepositorySyncError()
                    }
                    .collect { snapshot ->
                        mutableSyncStatus.value = snapshot.metadata.toRepositorySyncStatus()
                        mutableGoals.value = snapshot.documents
                            .filter { it.exists }
                            .mapNotNull { runCatching { it.toSavingsGoal() }.getOrNull() }
                            .sortedForSavingsGoals()
                    }
            },
        )
        jobs.joinAll()
    }

    override suspend fun saveGoal(draft: SavingsGoalDraft, createdAt: Instant): SavingsGoal {
        require(authProvider.currentFamilyId() == draft.familyId) {
            "A signed-in parent can only save savings goals for the active family."
        }
        val ref = savingsGoalCollection(draft.familyId).document
        val goal = draft.toSavingsGoal(
            id = SavingsGoalId(ref.id),
            createdAt = createdAt,
        )
        ref.set(goal.toFirestoreMap())
        upsertLocal(goal)
        return goal
    }

    override fun goalsForChild(childProfileId: ChildProfileId): List<SavingsGoal> =
        goals.value.filter { it.childProfileId == childProfileId }.sortedForSavingsGoals()

    private fun savingsGoalCollection(familyId: FamilyId) = firestoreProvider
        .firestoreOrThrow()
        .collection(FAMILIES_COLLECTION)
        .document(familyId.value)
        .collection(SAVINGS_GOALS_COLLECTION)

    private fun upsertLocal(goal: SavingsGoal) {
        mutableGoals.value = (goals.value.filterNot { it.id == goal.id } + goal).sortedForSavingsGoals()
    }

    private companion object {
        const val SAVINGS_GOALS_COLLECTION = "savingsGoals"
    }
}
