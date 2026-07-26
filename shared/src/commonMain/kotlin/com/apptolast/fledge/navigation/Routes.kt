package com.apptolast.fledge.navigation

import kotlinx.serialization.Serializable

@Serializable
data object OnboardingRoute

@Serializable
data object RoleSelectorRoute

@Serializable
data object FamilySetupRoute

@Serializable
data object ChildProfileSetupRoute

@Serializable
data object ParentHomeRoute

@Serializable
data class ChildPinRoute(val childProfileId: String)

@Serializable
data class PairingRoute(val childProfileId: String)

@Serializable
data object ParentalGateRoute

@Serializable
data object ChildHomeRoute
