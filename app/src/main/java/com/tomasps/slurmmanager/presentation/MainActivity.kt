package com.tomasps.slurmmanager.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tomasps.slurmmanager.presentation.navigation.NavGraph
import com.tomasps.slurmmanager.presentation.navigation.Screen
import com.tomasps.slurmmanager.presentation.theme.SlrumManagerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var dataStore: DataStore<Preferences>

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* proceed regardless */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val isFirstLaunch = runBlocking {
            val key = booleanPreferencesKey("onboarding_done")
            !dataStore.data.map { it[key] ?: false }.first()
        }

        setContent {
            val dynamicColor by dataStore.data
                .map { it[booleanPreferencesKey("dynamic_color")] != false }
                .collectAsState(initial = true)
            val darkThemeStr by dataStore.data
                .map { it[androidx.datastore.preferences.core.stringPreferencesKey("dark_theme")] }
                .collectAsState(initial = null)
            val darkTheme = when (darkThemeStr) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }
            val amoledBlack by dataStore.data
                .map { it[booleanPreferencesKey("amoled_black")] == true }
                .collectAsState(initial = false)

            SlrumManagerTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, amoledBlack = amoledBlack) {
                val navController = rememberNavController()
                val startDestination = if (isFirstLaunch) Screen.Onboarding.route else Screen.Home.route
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val showNav = currentRoute == Screen.Home.route

                val navItems = listOf(
                    NavItem(Screen.Dashboard.route, "Dashboard", Icons.Default.Dashboard),
                    NavItem(Screen.Servers.route, "Servers", Icons.Default.Storage),
                    NavItem(Screen.Settings.route, "Settings", Icons.Default.Settings),
                )

                var tabIndex by remember { mutableIntStateOf(0) }
                var showSubmitJob by remember { mutableStateOf(false) }

                // Map tab index to a "route" so the nav bar and FAB logic still work
                val tabRoutes = listOf(Screen.Dashboard.route, Screen.Servers.route, Screen.Settings.route)
                val effectiveRoute = if (showNav) tabRoutes.getOrElse(tabIndex) { Screen.Dashboard.route } else currentRoute

                val showFab = showNav && (tabIndex == 0 || tabIndex == 1)
                val fabLabel = if (tabIndex == 1) "Add Server" else "Submit Job"
                val fabIcon = Icons.Default.Add

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    NavGraph(
                        navController = navController,
                        startDestination = startDestination,
                        tabIndex = tabIndex,
                        onTabChange = { tabIndex = it },
                        showSubmitJob = showSubmitJob,
                        onDismissSubmitJob = { showSubmitJob = false },
                        onAddServerNavigation = { navController.navigate(Screen.AddServer.route) }
                    )

                    if (showNav) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (showFab) {
                                ExtendedFloatingActionButton(
                                    onClick = {
                                        if (tabIndex == 1) {
                                            navController.navigate(Screen.AddServer.route)
                                        } else {
                                            showSubmitJob = true
                                        }
                                    },
                                    icon = { Icon(fabIcon, contentDescription = null) },
                                    text = { Text(fabLabel) }
                                )
                            }

                            FloatingPillNavBar(
                                items = navItems,
                                currentRoute = effectiveRoute,
                                onItemClick = { route ->
                                    val idx = tabRoutes.indexOf(route)
                                    if (idx >= 0) tabIndex = idx
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FloatingPillNavBar(
    items: List<NavItem>,
    currentRoute: String?,
    onItemClick: (String) -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 6.dp,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                FloatingNavItem(
                    item = item,
                    selected = selected,
                    onClick = { onItemClick(item.route) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FloatingNavItem(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer
                      else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "nav_bg_${item.label}"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                      else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "nav_fg_${item.label}"
    )

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bgColor,
        contentColor = contentColor
    ) {
        AnimatedContent(
            targetState = selected,
            transitionSpec = {
                fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                        fadeOut(spring(stiffness = Spring.StiffnessMediumLow))
            },
            label = "nav_content_${item.label}"
        ) { isSelected ->
            if (isSelected) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(item.icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(item.label, style = MaterialTheme.typography.labelLargeEmphasized)
                }
            } else {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(item.icon, contentDescription = item.label, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
