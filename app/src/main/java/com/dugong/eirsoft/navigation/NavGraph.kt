package com.dugong.eirsoft.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dugong.eirsoft.ui.admin.AddEditPropertyScreen
import com.dugong.eirsoft.ui.admin.AdminDashboardScreen
import com.dugong.eirsoft.ui.admin.ApproveLoanScreen
import com.dugong.eirsoft.ui.admin.ManagePropertyScreen
import com.dugong.eirsoft.ui.admin.MemberDetailScreen
import com.dugong.eirsoft.ui.auth.LoginScreen
import com.dugong.eirsoft.ui.auth.RegisterScreen
import com.dugong.eirsoft.ui.member.LoanHistoryScreen
import com.dugong.eirsoft.ui.member.MemberDashboardScreen
import com.dugong.eirsoft.ui.member.ProfileScreen
import com.dugong.eirsoft.ui.member.PropertyDetailScreen
import com.dugong.eirsoft.ui.member.RequestLoanScreen

@Composable
fun NavGraph(navController: NavHostController, startDestination: String = Screen.Login.route) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        }
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { role ->
                    val route = if (role == "admin") Screen.AdminDashboard.route else Screen.MemberDashboard.route
                    navController.navigate(route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }
        
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
        
        // Admin Routes
        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(
                onManageProperties = { navController.navigate(Screen.ManageProperties.route) },
                onApproveLoans = { navController.navigate(Screen.ApproveLoans.route) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) // Menghapus semua screen
                    }
                }
            )
        }
        
        composable(Screen.ManageProperties.route) {
            ManagePropertyScreen(
                onBack = { navController.popBackStack() },
                onAddProperty = { navController.navigate(Screen.AddEditProperty.createRoute()) },
                onEditProperty = { id -> navController.navigate(Screen.AddEditProperty.createRoute(id)) }
            )
        }
        
        composable(
            route = Screen.AddEditProperty.route,
            arguments = listOf(navArgument("propertyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val propertyId = backStackEntry.arguments?.getString("propertyId")
            AddEditPropertyScreen(
                propertyId = if (propertyId == "new") null else propertyId,
                onBack = { navController.popBackStack() },
                onSuccess = { 
                    navController.navigate(Screen.ManageProperties.route) {
                        popUpTo(Screen.ManageProperties.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ApproveLoans.route) {
            ApproveLoanScreen(
                onBack = { navController.popBackStack() },
                onMemberClick = { userId -> navController.navigate(Screen.MemberDetail.createRoute(userId)) }
            )
        }
        
        composable(
            route = Screen.MemberDetail.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            MemberDetailScreen(
                userId = userId,
                onBack = { navController.popBackStack() }
            )
        }

        // Member Routes
        composable(Screen.MemberDashboard.route) {
            MemberDashboardScreen(
                onPropertyClick = { id -> navController.navigate(Screen.PropertyDetail.createRoute(id)) },
                onViewHistory = { navController.navigate(Screen.LoanHistory.route) },
                onProfileClick = { navController.navigate(Screen.Profile.route) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0)
                    }
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.PropertyDetail.route,
            arguments = listOf(navArgument("propertyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val propertyId = backStackEntry.arguments?.getString("propertyId") ?: ""
            PropertyDetailScreen(
                propertyId = propertyId,
                onBack = { navController.popBackStack() },
                onRequestLoan = { id -> navController.navigate(Screen.RequestLoan.createRoute(id)) }
            )
        }

        composable(
            route = Screen.RequestLoan.route,
            arguments = listOf(navArgument("propertyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val propertyId = backStackEntry.arguments?.getString("propertyId") ?: ""
            RequestLoanScreen(
                propertyId = propertyId,
                onBack = { navController.popBackStack() },
                onSuccess = { 
                    navController.navigate(Screen.LoanHistory.route) {
                        popUpTo(Screen.MemberDashboard.route)
                    }
                }
            )
        }

        composable(Screen.LoanHistory.route) {
            LoanHistoryScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
