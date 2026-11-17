package com.azxnism.waron.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.azxnism.waron.ui.screens.DashboardScreen
import com.azxnism.waron.ui.screens.LoginScreen
import com.azxnism.waron.ui.screens.NewScanScreen
import com.azxnism.waron.ui.screens.ReportScreen
import com.azxnism.waron.ui.screens.ScanDetailScreen
import com.azxnism.waron.ui.screens.ScanProgressScreen
import com.azxnism.waron.ui.screens.SettingsScreen

@Composable
fun WarOnNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        // Login
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Dashboard
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToNewScan = {
                    navController.navigate(Screen.NewScan.route)
                },
                onNavigateToScanDetail = { scanId ->
                    navController.navigate(Screen.ScanDetail.createRoute(scanId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // New Scan
        composable(Screen.NewScan.route) {
            NewScanScreen(
                onNavigateBack = { navController.popBackStack() },
                onScanStarted = { scanId ->
                    navController.navigate(Screen.ScanProgress.createRoute(scanId)) {
                        popUpTo(Screen.NewScan.route) { inclusive = true }
                    }
                }
            )
        }

        // Scan Progress
        composable(
            route = Screen.ScanProgress.route,
            arguments = listOf(navArgument("scanId") { type = NavType.LongType })
        ) {
            ScanProgressScreen(
                onScanComplete = { scanId ->
                    navController.navigate(Screen.ScanDetail.createRoute(scanId)) {
                        popUpTo(Screen.Dashboard.route)
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Scan Detail
        composable(
            route = Screen.ScanDetail.route,
            arguments = listOf(navArgument("scanId") { type = NavType.LongType })
        ) {
            ScanDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToReport = { scanId ->
                    navController.navigate(Screen.Report.createRoute(scanId))
                }
            )
        }

        // Report
        composable(
            route = Screen.Report.route,
            arguments = listOf(navArgument("scanId") { type = NavType.LongType })
        ) {
            ReportScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Settings
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onSignOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
