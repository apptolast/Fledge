package com.apptolast.fledge.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.apptolast.customlogin.presentation.navigation.AuthRoutesFlow
import com.apptolast.customlogin.presentation.navigation.authRoutesFlow
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FoundationAction
import com.apptolast.fledge.presentation.foundation.childhome.ChildHomeScreen
import com.apptolast.fledge.presentation.foundation.childpin.ChildPinResetScreen
import com.apptolast.fledge.presentation.foundation.childpin.ChildPinScreen
import com.apptolast.fledge.presentation.foundation.childsetup.ChildProfileSetupScreen
import com.apptolast.fledge.presentation.foundation.familysetup.FamilySetupScreen
import com.apptolast.fledge.presentation.foundation.onboarding.OnboardingScreen
import com.apptolast.fledge.presentation.foundation.pairing.PairingScreen
import com.apptolast.fledge.presentation.foundation.parentalgate.ParentalGateScreen
import com.apptolast.fledge.presentation.foundation.parenthome.ParentHomeScreen
import com.apptolast.fledge.presentation.foundation.roles.RoleSelectorScreen
import com.apptolast.fledge.presentation.foundation.virtualconsent.VirtualMoneyConsentScreen

@Composable
fun FledgeNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = OnboardingRoute,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        composable<OnboardingRoute> {
            OnboardingScreen(onContinue = { navController.navigate(RoleSelectorRoute) })
        }
        composable<RoleSelectorRoute> {
            RoleSelectorScreen(
                onNavigate = { target ->
                    when (target) {
                        FoundationNavigationTarget.ParentAuth -> navController.navigate(AuthRoutesFlow)
                        is FoundationNavigationTarget.ChildPin -> navController.navigate(
                            ChildPinRoute(target.childProfileId.value)
                        )
                    }
                }
            )
        }
        authRoutesFlow(
            navController = navController,
            onNavigateToHome = {
                navController.navigate(FamilySetupRoute) {
                    popUpTo(AuthRoutesFlow) { inclusive = true }
                }
            },
        )
        composable<FamilySetupRoute> {
            FamilySetupScreen(onFamilyCreated = { navController.navigate(VirtualMoneyConsentRoute) })
        }
        composable<VirtualMoneyConsentRoute> {
            VirtualMoneyConsentScreen(onConsentRecorded = { navController.navigate(ChildProfileSetupRoute) })
        }
        composable<ChildProfileSetupRoute> {
            ChildProfileSetupScreen(onChildCreated = { navController.navigate(ParentHomeRoute) })
        }
        composable<ParentHomeRoute> {
            ParentHomeScreen(
                onPairChild = { childId -> navController.navigate(PairingRoute(childId.value)) },
                onRequireParentalGate = { navController.navigate(ParentalGateRoute) },
            )
        }
        composable<ChildPinRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ChildPinRoute>()
            ChildPinScreen(
                childProfileId = ChildProfileId(route.childProfileId),
                onUnlocked = { navController.navigate(ChildHomeRoute(route.childProfileId)) },
                onParentalGateRequired = { navController.navigate(ParentalGateRoute) },
            )
        }
        composable<ChildPinResetRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ChildPinResetRoute>()
            ChildPinResetScreen(
                childProfileId = ChildProfileId(route.childProfileId),
                onPinSaved = {
                    navController.navigate(ChildPinRoute(route.childProfileId)) {
                        popUpTo(ChildPinResetRoute(route.childProfileId)) { inclusive = true }
                    }
                },
            )
        }
        composable<PairingRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<PairingRoute>()
            PairingScreen(childProfileId = ChildProfileId(route.childProfileId))
        }
        composable<ParentalGateRoute> {
            ParentalGateScreen(
                onConfirmed = { action ->
                    when (action) {
                        is FoundationAction.ResetChildPin -> {
                            navController.navigate(ChildPinResetRoute(action.childProfileId.value)) {
                                popUpTo(ParentalGateRoute) { inclusive = true }
                            }
                        }
                        FoundationAction.OpenParentZone -> {
                            navController.navigate(ParentHomeRoute) {
                                popUpTo(ParentalGateRoute) { inclusive = true }
                            }
                        }
                        else -> navController.popBackStack()
                    }
                }
            )
        }
        composable<ChildHomeRoute> {
            ChildHomeScreen(onParentalGateRequired = { navController.navigate(ParentalGateRoute) })
        }
    }
}
