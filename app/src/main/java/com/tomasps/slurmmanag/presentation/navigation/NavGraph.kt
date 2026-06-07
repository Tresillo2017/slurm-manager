package com.tomasps.slurmmanag.presentation.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tomasps.slurmmanag.presentation.HomeScreen
import com.tomasps.slurmmanag.presentation.onboarding.OnboardingScreen
import com.tomasps.slurmmanag.presentation.serverdetail.ServerDetailScreen

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
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
    startDestination: String,
    tabIndex: Int = 0,
    onTabChange: (Int) -> Unit = {},
    showSubmitJob: Boolean = false,
    onDismissSubmitJob: () -> Unit = {},
    onAddServerNavigation: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) },
        exitTransition = { slideOutHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { -it } + fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) },
        popEnterTransition = { slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { -it } + fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) },
        popExitTransition = { slideOutHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) }
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(onFinished = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Home.route) {
            HomeScreen(
                tabIndex = tabIndex,
                onTabChange = onTabChange,
                showSubmitJob = showSubmitJob,
                onDismissSubmitJob = onDismissSubmitJob,
                onJobClick = { jobId, serverId ->
                    navController.navigate("job/$jobId/$serverId")
                },
                onServerClick = { serverId ->
                    navController.navigate(Screen.ServerDetail.route(serverId))
                },
                onAddServer = { navController.navigate(Screen.AddServer.route) },
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
        composable(
            route = "job/{jobId}/{serverId}",
            arguments = listOf(
                navArgument("jobId") { type = NavType.StringType },
                navArgument("serverId") { type = NavType.StringType }
            )
        ) { backStack ->
            val jobId = backStack.arguments?.getString("jobId") ?: return@composable
            val serverId = backStack.arguments?.getString("serverId") ?: return@composable
            com.tomasps.slurmmanag.presentation.jobdetail.JobDetailScreen(
                jobId = jobId,
                serverId = serverId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
