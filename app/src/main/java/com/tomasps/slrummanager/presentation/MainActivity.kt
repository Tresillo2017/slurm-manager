package com.tomasps.slrummanager.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.navigation.compose.rememberNavController
import com.tomasps.slrummanager.presentation.navigation.BottomNavBar
import com.tomasps.slrummanager.presentation.navigation.NavGraph
import com.tomasps.slrummanager.presentation.navigation.Screen
import com.tomasps.slrummanager.presentation.theme.SlrumManagerTheme
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
            val dynamicColorKey = booleanPreferencesKey("dynamic_color")
            val dynamicColor by dataStore.data
                .map { it[dynamicColorKey] != false }
                .collectAsState(initial = true)

            SlrumManagerTheme(dynamicColor = dynamicColor) {
                val navController = rememberNavController()
                val startDestination = if (isFirstLaunch) Screen.Onboarding.route else Screen.Dashboard.route
                val showBottomBar = remember { mutableStateOf(!isFirstLaunch) }

                LaunchedEffect(navController) {
                    navController.addOnDestinationChangedListener { _, dest, _ ->
                        showBottomBar.value = dest.route != Screen.Onboarding.route &&
                                dest.route != Screen.AddServer.route
                    }
                }

                Scaffold(
                    bottomBar = {
                        if (showBottomBar.value) BottomNavBar(navController)
                    }
                ) { _ ->
                    NavGraph(
                        navController = navController,
                        startDestination = startDestination,
                    )
                }
            }
        }
    }
}
