package com.azxnism.waron.ui.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Dashboard : Screen("dashboard")
    data object NewScan : Screen("new_scan")
    data object ScanProgress : Screen("scan_progress/{scanId}") {
        fun createRoute(scanId: Long) = "scan_progress/$scanId"
    }
    data object ScanDetail : Screen("scan_detail/{scanId}") {
        fun createRoute(scanId: Long) = "scan_detail/$scanId"
    }
    data object Report : Screen("report/{scanId}") {
        fun createRoute(scanId: Long) = "report/$scanId"
    }
    data object Settings : Screen("settings")
}
