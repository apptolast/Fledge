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
import com.apptolast.fledge.presentation.foundation.allowance.AllowanceRuleScreen
import com.apptolast.fledge.presentation.foundation.cashout.CashOutRequestScreen
import com.apptolast.fledge.presentation.foundation.childhome.ChildHomeScreen
import com.apptolast.fledge.presentation.foundation.childpin.ChildPinResetScreen
import com.apptolast.fledge.presentation.foundation.childpin.ChildPinScreen
import com.apptolast.fledge.presentation.foundation.childsetup.ChildProfileSetupScreen
import com.apptolast.fledge.presentation.foundation.familysetup.FamilySetupScreen
import com.apptolast.fledge.presentation.foundation.manualadjustment.ManualAdjustmentScreen
import com.apptolast.fledge.presentation.foundation.onboarding.OnboardingScreen
import com.apptolast.fledge.presentation.foundation.pairing.PairingScreen
import com.apptolast.fledge.presentation.foundation.parentalgate.ParentalGateScreen
import com.apptolast.fledge.presentation.foundation.parenthome.ParentHomeScreen
import com.apptolast.fledge.presentation.foundation.postlogin.PostLoginScreen
import com.apptolast.fledge.presentation.foundation.roles.RoleSelectorScreen
import com.apptolast.fledge.presentation.foundation.savingsgoal.SavingsGoalSetupScreen
import com.apptolast.fledge.presentation.foundation.taskassignment.TaskAssignmentScreen
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
                            ChildPinRoute(target.childProfileId.value),
                        )
                    }
                },
            )
        }
        authRoutesFlow(
            navController = navController,
            onNavigateToHome = {
                navController.navigate(PostLoginRoute) {
                    popUpTo(AuthRoutesFlow) { inclusive = true }
                }
            },
        )
        composable<PostLoginRoute> {
            PostLoginScreen(
                onNavigateToFamilySetup = {
                    navController.navigate(FamilySetupRoute) {
                        popUpTo(PostLoginRoute) { inclusive = true }
                    }
                },
                onNavigateToVirtualMoneyConsent = {
                    navController.navigate(VirtualMoneyConsentRoute) {
                        popUpTo(PostLoginRoute) { inclusive = true }
                    }
                },
                onNavigateToChildProfileSetup = {
                    navController.navigate(ChildProfileSetupRoute) {
                        popUpTo(PostLoginRoute) { inclusive = true }
                    }
                },
                onNavigateToParentHome = {
                    navController.navigate(ParentHomeRoute) {
                        popUpTo(PostLoginRoute) { inclusive = true }
                    }
                },
            )
        }
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
                onConfigureAllowance = { childId -> navController.navigate(AllowanceRuleRoute(childId.value)) },
                onAdjustChild = { childId -> navController.navigate(ManualAdjustmentRoute(childId.value)) },
                onCreateSavingsGoal = { childId -> navController.navigate(SavingsGoalSetupRoute(childId.value)) },
                onCreateTask = { navController.navigate(TaskAssignmentRoute) },
                onRequireParentalGate = { navController.navigate(ParentalGateRoute) },
            )
        }
        composable<TaskAssignmentRoute> {
            TaskAssignmentScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
        composable<SavingsGoalSetupRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<SavingsGoalSetupRoute>()
            SavingsGoalSetupScreen(
                childProfileId = ChildProfileId(route.childProfileId),
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
        composable<AllowanceRuleRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<AllowanceRuleRoute>()
            AllowanceRuleScreen(
                childProfileId = ChildProfileId(route.childProfileId),
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
        composable<ManualAdjustmentRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ManualAdjustmentRoute>()
            ManualAdjustmentScreen(
                childProfileId = ChildProfileId(route.childProfileId),
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
        composable<CashOutRequestRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<CashOutRequestRoute>()
            CashOutRequestScreen(
                childProfileId = ChildProfileId(route.childProfileId),
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
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
                },
            )
        }
        composable<ChildHomeRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ChildHomeRoute>()
            ChildHomeScreen(
                childProfileId = ChildProfileId(route.childProfileId),
                onRequestCashOut = { childId -> navController.navigate(CashOutRequestRoute(childId.value)) },
                onParentalGateRequired = { navController.navigate(ParentalGateRoute) },
            )
        }
    }
}
