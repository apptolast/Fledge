package com.apptolast.fledge.presentation

import com.apptolast.fledge.domain.model.AccountDeletionRequest
import com.apptolast.fledge.domain.model.AccountDeletionState
import com.apptolast.fledge.domain.model.AccountDeletionStatus
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.repository.AccountDeletionRepository
import com.apptolast.fledge.domain.repository.RepositorySyncStatus
import com.apptolast.fledge.presentation.foundation.accountdeletion.ACCOUNT_DELETION_CONFIRMATION_PHRASE
import com.apptolast.fledge.presentation.foundation.accountdeletion.AccountDeletionViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest

class AccountDeletionViewModelTest {

    @Test
    fun `AC-02 deletion is disabled unless confirmation is exact`() = runTest {
        // Given
        val viewModel = AccountDeletionViewModel(FakeAccountDeletionRepository())

        // When / Then
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.updateConfirmationInput("ELIM")
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.updateConfirmationInput(" $ACCOUNT_DELETION_CONFIRMATION_PHRASE ")
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.updateConfirmationInput(ACCOUNT_DELETION_CONFIRMATION_PHRASE)
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `AC-03 confirmed deletion request is submitted through repository`() = runTest {
        // Given
        val repository = FakeAccountDeletionRepository()
        val viewModel = AccountDeletionViewModel(repository)

        // When
        viewModel.updateConfirmationInput(ACCOUNT_DELETION_CONFIRMATION_PHRASE)
        val submitted = viewModel.submit()

        // Then
        assertTrue(submitted)
        assertEquals(1, repository.requestedAt.size)
        assertEquals(AccountDeletionStatus.Requested, viewModel.uiState.value.deletionState.status)
        assertTrue(viewModel.uiState.value.isSubmitted)
    }
}

private class FakeAccountDeletionRepository : AccountDeletionRepository {
    override val syncStatus: StateFlow<RepositorySyncStatus> = MutableStateFlow(RepositorySyncStatus.Synced)
    private val mutableDeletionState = MutableStateFlow(AccountDeletionState())
    override val deletionState: StateFlow<AccountDeletionState> = mutableDeletionState
    val requestedAt = mutableListOf<Instant>()

    override suspend fun requestAccountDeletion(requestedAt: Instant): AccountDeletionRequest {
        this.requestedAt += requestedAt
        mutableDeletionState.value = AccountDeletionState(
            status = AccountDeletionStatus.Requested,
            requestedAt = requestedAt,
            updatedAt = requestedAt,
        )
        return AccountDeletionRequest(FamilyId("family-1"), requestedAt)
    }
}
