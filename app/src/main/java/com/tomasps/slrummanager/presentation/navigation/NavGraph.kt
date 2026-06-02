package com.tomasps.slrummanager.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tomasps.slrummanager.presentation.dashboard.DashboardScreen
import com.tomasps.slrummanager.presentation.onboarding.OnboardingScreen
import com.tomasps.slrummanager.presentation.servers.ServersScreen
import com.tomasps.slrummanager.presentation.serverdetail.ServerDetailScreen
import com.tomasps.slrummanager.presentation.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Dashboard : Screen("dashboard")
    data object Servers : Screen("servers")
    data object Settings : Screen("settings")
    data object ServerDetail : Screen("server/{serverId}") {
        fun route(serverId: String) = "server/$serverId"
    }
    data object AddServer : Screen("add_server")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(onFinished = {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onJobClick = { jobId, serverId ->
                    navController.navigate("job/$jobId/$serverId")
                }
            )
        }
        composable(Screen.Servers.route) {
            ServersScreen(
                onServerClick = { serverId ->
                    navController.navigate(Screen.ServerDetail.route(serverId))
                },
                onAddServer = { navController.navigate(Screen.AddServer.route) }
            )
        }
        composable(
            route = Screen.ServerDetail.route,
            arguments = listOf(navArgument("serverId") { type = NavType.StringType })
        ) { backStack ->
            val serverId = backStack.arguments?.getString("serverId") ?: return@composable
            ServerDetailScreen(
                serverId = serverId,
                onBack = { navController.popBackStack() },
                onJobClick = { jobId ->
                    navController.navigate("job/$jobId/$serverId")
                }
            )
        }
        composable(Screen.AddServer.route) {
            OnboardingScreen(
                addServerOnly = true,
                onFinished = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) { SettingsScreen() }
        composable(
            route = "job/{jobId}/{serverId}",
            arguments = listOf(
                navArgument("jobId") { type = NavType.StringType },
                navArgument("serverId") { type = NavType.StringType }
            )
        ) { backStack ->
            val jobId = backStack.arguments?.getString("jobId") ?: return@composable
            val serverId = backStack.arguments?.getString("serverId") ?: return@composable
            com.tomasps.slrummanager.presentation.jobdetail.JobDetailScreen(
                jobId = jobId,
                serverId = serverId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
