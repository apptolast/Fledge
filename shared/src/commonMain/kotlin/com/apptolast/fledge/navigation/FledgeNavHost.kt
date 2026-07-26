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
import com.apptolast.fledge.presentation.foundation.childsetup.ChildProfileSetupScreen
import com.apptolast.fledge.presentation.foundation.childhome.ChildHomeScreen
import com.apptolast.fledge.presentation.foundation.childpin.ChildPinScreen
import com.apptolast.fledge.presentation.foundation.familysetup.FamilySetupScreen
import com.apptolast.fledge.presentation.foundation.onboarding.OnboardingScreen
import com.apptolast.fledge.presentation.foundation.pairing.PairingScreen
import com.apptolast.fledge.presentation.foundation.parentalgate.ParentalGateScreen
import com.apptolast.fledge.presentation.foundation.parenthome.ParentHomeScreen
import com.apptolast.fledge.presentation.foundation.roles.RoleSelectorScreen

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
            FamilySetupScreen(onFamilyCreated = { navController.navigate(ChildProfileSetupRoute) })
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
                onUnlocked = { navController.navigate(ChildHomeRoute) },
                onParentalGateRequired = { navController.navigate(ParentalGateRoute) },
            )
        }
        composable<PairingRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<PairingRoute>()
            PairingScreen(childProfileId = ChildProfileId(route.childProfileId))
        }
        composable<ParentalGateRoute> {
            ParentalGateScreen(onConfirmed = { navController.popBackStack() })
        }
        composable<ChildHomeRoute> {
            ChildHomeScreen()
        }
    }
}
