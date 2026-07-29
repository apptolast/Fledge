package com.apptolast.fledge.navigation

import kotlinx.serialization.Serializable

@Serializable
data object OnboardingRoute

@Serializable
data object RoleSelectorRoute

@Serializable
data object FamilySetupRoute

@Serializable
data object PostLoginRoute

@Serializable
data object VirtualMoneyConsentRoute

@Serializable
data object ChildProfileSetupRoute

@Serializable
data object ParentHomeRoute

@Serializable
data class AllowanceRuleRoute(val childProfileId: String)

@Serializable
data class ManualAdjustmentRoute(val childProfileId: String)

@Serializable
data class CashOutRequestRoute(val childProfileId: String)

@Serializable
data class ChildPinRoute(val childProfileId: String)

@Serializable
data class ChildPinResetRoute(val childProfileId: String)

@Serializable
data class PairingRoute(val childProfileId: String)

@Serializable
data object ParentalGateRoute

@Serializable
data class ChildHomeRoute(val childProfileId: String)
