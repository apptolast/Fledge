package com.apptolast.fledge.presentation.foundation.childhome

import androidx.lifecycle.ViewModel
import com.apptolast.fledge.domain.model.FoundationAction
import com.apptolast.fledge.domain.repository.FamilyFoundationRepository

class ChildHomeViewModel(
    private val repository: FamilyFoundationRepository,
) : ViewModel() {
    suspend fun requestProtectedAction(action: FoundationAction) {
        repository.requireParentalGate(action)
    }
}
