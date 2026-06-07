package com.tomasps.slurmmanag.presentation.settings

import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.roundToInt

// ─── Sub-page enum ──────────────────────────────────────────────────────────

private enum class SettingsPage { HUB, NOTIFICATIONS, APPEARANCE, POLLING, ABOUT }

// ─── Entry point ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var page by remember { mutableStateOf(SettingsPage.HUB) }
    val forward = remember { mutableStateOf(true) }

    fun navigate(target: SettingsPage, isForward: Boolean = true) {
        forward.value = isForward
        page = target
    }

    val isExpanded = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT

    if (isExpanded) {
        // Outer shell has no topBar — left pane (SettingsHubList) owns its own Scaffold+topBar
        // Right pane sub-pages each own their own Scaffold, filling full height from y=0
        Row(modifier = Modifier.fillMaxSize()) {
            SettingsHubList(
                state = state,
                selectedPage = page,
                onNavigate = { navigate(it) },
                modifier = Modifier.weight(0.4f)
            )
            VerticalDivider()
            Box(modifier = Modifier.weight(0.6f).fillMaxHeight()) {
                when (page) {
                    SettingsPage.HUB -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.TouchApp, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                            Text("Select a category", color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    SettingsPage.NOTIFICATIONS -> NotificationsPage(state = state, viewModel = viewModel, onBack = { navigate(SettingsPage.HUB, false) }, isInlinePane = true)
                    SettingsPage.APPEARANCE -> AppearancePage(state = state, viewModel = viewModel, onBack = { navigate(SettingsPage.HUB, false) }, isInlinePane = true)
                    SettingsPage.POLLING -> PollingPage(state = state, viewModel = viewModel, onBack = { navigate(SettingsPage.HUB, false) }, isInlinePane = true)
                    SettingsPage.ABOUT -> AboutPage(onBack = { navigate(SettingsPage.HUB, false) }, isInlinePane = true)
                }
            }
        }
        return
    } else {
        val swipeOffset = remember { Animatable(0f) }
        PredictiveBackHandler(enabled = page != SettingsPage.HUB) { flow ->
            try {
                flow.collect { event: BackEventCompat ->
                    swipeOffset.snapTo(event.progress)
                }
                swipeOffset.snapTo(0f)
                navigate(SettingsPage.HUB, isForward = false)
            } catch (_: Exception) {
                swipeOffset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
            }
        }

        val enterSpec = spring<Float>(stiffness = Spring.StiffnessMediumLow)
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                if (forward.value) {
                    (slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn(enterSpec)) togetherWith
                            (slideOutHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { -it } + fadeOut())
                } else {
                    (slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { -it } + fadeIn(enterSpec)) togetherWith
                            (slideOutHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeOut())
                }
            },
            label = "settings_page"
        ) { currentPage ->
            when (currentPage) {
                SettingsPage.HUB -> SettingsHub(state = state, onNavigate = { navigate(it) })
                SettingsPage.NOTIFICATIONS -> NotificationsPage(state = state, viewModel = viewModel, onBack = { navigate(SettingsPage.HUB, false) })
                SettingsPage.APPEARANCE -> AppearancePage(state = state, viewModel = viewModel, onBack = { navigate(SettingsPage.HUB, false) })
                SettingsPage.POLLING -> PollingPage(state = state, viewModel = viewModel, onBack = { navigate(SettingsPage.HUB, false) })
                SettingsPage.ABOUT -> AboutPage(onBack = { navigate(SettingsPage.HUB, false) })
            }
        }
    }
}

// ─── Hub ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsHubList(
    state: SettingsUiState,
    selectedPage: SettingsPage,
    onNavigate: (SettingsPage) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Settings") },
                subtitle = { Text("Customize your experience") },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsNavCard(
                    icon = Icons.Default.Notifications,
                    iconContainerColor = if (selectedPage == SettingsPage.NOTIFICATIONS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                    iconContentColor = if (selectedPage == SettingsPage.NOTIFICATIONS) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                    title = "Notifications",
                    subtitle = buildString {
                        val enabled = listOfNotNull("Jobs".takeIf { state.jobStateNotifs }, "Alerts".takeIf { state.alertNotifs }, "Cluster".takeIf { state.clusterNotifs })
                        append(if (enabled.isEmpty()) "All disabled" else enabled.joinToString(" · "))
                    },
                    onClick = { onNavigate(SettingsPage.NOTIFICATIONS) }
                )
            }
            item {
                SettingsNavCard(
                    icon = Icons.Default.Palette,
                    iconContainerColor = if (selectedPage == SettingsPage.APPEARANCE) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondaryContainer,
                    iconContentColor = if (selectedPage == SettingsPage.APPEARANCE) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSecondaryContainer,
                    title = "Appearance",
                    subtitle = buildString {
                        append(state.darkTheme.name.lowercase().replaceFirstChar { it.uppercase() })
                        if (state.dynamicColor) append(" · Material You")
                    },
                    onClick = { onNavigate(SettingsPage.APPEARANCE) }
                )
            }
            item {
                SettingsNavCard(
                    icon = Icons.Default.Sync,
                    iconContainerColor = if (selectedPage == SettingsPage.POLLING) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.tertiaryContainer,
                    iconContentColor = if (selectedPage == SettingsPage.POLLING) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onTertiaryContainer,
                    title = "Polling & Sync",
                    subtitle = buildString {
                        append("Every ${state.defaultPollingInterval}m")
                        if (state.batterySaver) append(" · Battery saver on")
                        if (!state.backgroundSync) append(" · Background off")
                    },
                    onClick = { onNavigate(SettingsPage.POLLING) }
                )
            }
            item {
                SettingsNavCard(
                    icon = Icons.Default.Info,
                    iconContainerColor = if (selectedPage == SettingsPage.ABOUT) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerHighest,
                    iconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    title = "About",
                    subtitle = "SLURM Manager v1.0",
                    onClick = { onNavigate(SettingsPage.ABOUT) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsHub(
    state: SettingsUiState,
    onNavigate: (SettingsPage) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Settings") },
                subtitle = { Text("Customize your experience") },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsNavCard(icon = Icons.Default.Notifications,
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    title = "Notifications",
                    subtitle = buildString {
                        val enabled = listOfNotNull("Jobs".takeIf { state.jobStateNotifs }, "Alerts".takeIf { state.alertNotifs }, "Cluster".takeIf { state.clusterNotifs })
                        append(if (enabled.isEmpty()) "All disabled" else enabled.joinToString(" · "))
                    },
                    onClick = { onNavigate(SettingsPage.NOTIFICATIONS) })
            }
            item {
                SettingsNavCard(icon = Icons.Default.Palette,
                    iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    title = "Appearance",
                    subtitle = buildString {
                        append(state.darkTheme.name.lowercase().replaceFirstChar { it.uppercase() })
                        if (state.dynamicColor) append(" · Material You")
                    },
                    onClick = { onNavigate(SettingsPage.APPEARANCE) })
            }
            item {
                SettingsNavCard(icon = Icons.Default.Sync,
                    iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    title = "Polling & Sync",
                    subtitle = buildString {
                        append("Every ${state.defaultPollingInterval}m")
                        if (state.batterySaver) append(" · Battery saver on")
                        if (!state.backgroundSync) append(" · Background off")
                    },
                    onClick = { onNavigate(SettingsPage.POLLING) })
            }
            item {
                SettingsNavCard(icon = Icons.Default.Info,
                    iconContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    iconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    title = "About", subtitle = "SLURM Manager v1.0",
                    onClick = { onNavigate(SettingsPage.ABOUT) })
            }
        }
    }
}

@Composable
private fun SettingsNavCard(
    icon: ImageVector,
    iconContainerColor: androidx.compose.ui.graphics.Color,
    iconContentColor: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = iconContainerColor,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconContentColor, modifier = Modifier.size(24.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ─── Notifications sub-page ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NotificationsPage(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    isInlinePane: Boolean = false,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        contentWindowInsets = if (isInlinePane) WindowInsets(0) else ScaffoldDefaults.contentWindowInsets,
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text("Notifications") },
                subtitle = { Text("Control how and when you're alerted") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SectionLabel("Event Types") }
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    SwitchListItem(
                        title = "Job State Changes",
                        subtitle = "Notify when jobs complete, fail, or are cancelled",
                        checked = state.jobStateNotifs,
                        onCheckedChange = { viewModel.setJobStateNotifs(it) }
                    )
                    SettingsDivider()
                    SwitchListItem(
                        title = "Threshold Alerts",
                        subtitle = "Queue wait time and runtime exceeded",
                        checked = state.alertNotifs,
                        onCheckedChange = { viewModel.setAlertNotifs(it) }
                    )
                    SettingsDivider()
                    SwitchListItem(
                        title = "Cluster Events",
                        subtitle = "Node failures and partition outages",
                        checked = state.clusterNotifs,
                        onCheckedChange = { viewModel.setClusterNotifs(it) }
                    )
                }
            }
            item { SectionLabel("Delivery") }
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    SwitchListItem(
                        title = "Sound",
                        subtitle = "Play notification sound",
                        checked = state.notifSound,
                        onCheckedChange = { viewModel.setNotifSound(it) }
                    )
                    SettingsDivider()
                    SwitchListItem(
                        title = "Vibration",
                        subtitle = "Vibrate on notification",
                        checked = state.notifVibration,
                        onCheckedChange = { viewModel.setNotifVibration(it) }
                    )
                }
            }
        }
    }
}

// ─── Appearance sub-page ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppearancePage(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    isInlinePane: Boolean = false,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        contentWindowInsets = if (isInlinePane) WindowInsets(0) else ScaffoldDefaults.contentWindowInsets,
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text("Appearance") },
                subtitle = { Text("Colors, theme, and display") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SectionLabel("Color") }
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    SwitchListItem(
                        title = "Material You",
                        subtitle = "Adapt colors to your wallpaper (Android 12+)",
                        checked = state.dynamicColor,
                        onCheckedChange = { viewModel.setDynamicColor(it) }
                    )
                }
            }
            item { SectionLabel("Theme") }
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Dark mode", style = MaterialTheme.typography.titleSmall)
                        val darkOptions = DarkThemePreference.entries
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            darkOptions.forEachIndexed { i, pref ->
                                val label = when (pref) {
                                    DarkThemePreference.SYSTEM -> "System"
                                    DarkThemePreference.LIGHT -> "Light"
                                    DarkThemePreference.DARK -> "Dark"
                                }
                                ToggleButton(
                                    checked = state.darkTheme == pref,
                                    onCheckedChange = { viewModel.setDarkTheme(pref) },
                                    modifier = Modifier.weight(1f),
                                    shapes = when (i) {
                                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        darkOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    }
                                ) { Text(label) }
                            }
                        }
                    }
                    SettingsDivider()
                    SwitchListItem(
                        title = "AMOLED Black",
                        subtitle = "Pure black background — saves power on OLED screens",
                        checked = state.amoledBlack,
                        onCheckedChange = { viewModel.setAmoledBlack(it) },
                        enabled = state.darkTheme != DarkThemePreference.LIGHT
                    )
                }
            }
        }
    }
}

// ─── Polling sub-page ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PollingPage(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    isInlinePane: Boolean = false,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var sliderValue by remember(state.defaultPollingInterval) {
        mutableFloatStateOf(state.defaultPollingInterval.toFloat())
    }

    Scaffold(
        contentWindowInsets = if (isInlinePane) WindowInsets(0) else ScaffoldDefaults.contentWindowInsets,
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text("Polling & Sync") },
                subtitle = { Text("Background refresh and battery settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SectionLabel("Default Polling Interval") }
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Interval", style = MaterialTheme.typography.bodyMedium)
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                                Text(
                                    "${sliderValue.roundToInt()} min",
                                    style = MaterialTheme.typography.labelLargeEmphasized,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                        Slider(
                            value = sliderValue,
                            onValueChange = { sliderValue = it },
                            onValueChangeFinished = { viewModel.setDefaultPollingInterval(sliderValue.roundToInt()) },
                            valueRange = 1f..60f,
                            steps = 58,
                            modifier = Modifier.fillMaxWidth(),
                            track = { sliderState ->
                                LinearWavyProgressIndicator(
                                    progress = { (sliderState.value - 1f) / 59f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("1 min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("60 min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item { SectionLabel("Background") }
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    SwitchListItem(
                        title = "Background Sync",
                        subtitle = "Keep polling when the app is not open",
                        checked = state.backgroundSync,
                        onCheckedChange = { viewModel.setBackgroundSync(it) }
                    )
                    SettingsDivider()
                    SwitchListItem(
                        title = "Battery Saver",
                        subtitle = "Reduce polling frequency when battery is low",
                        checked = state.batterySaver,
                        onCheckedChange = { viewModel.setBatterySaver(it) }
                    )
                }
            }
            item {
                if (!state.backgroundSync) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(20.dp))
                            Text(
                                "Background sync is off. Notifications will only arrive while the app is open.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── About sub-page ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AboutPage(onBack: () -> Unit, isInlinePane: Boolean = false) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "–"
        } catch (_: Exception) { "–" }
    }
    val versionCode = remember {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toString()
            }
        } catch (_: Exception) { "–" }
    }

    Scaffold(
        contentWindowInsets = if (isInlinePane) WindowInsets(0) else ScaffoldDefaults.contentWindowInsets,
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text("About") },
                subtitle = { Text("App info & developer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero card
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(32.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(96.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Terminal, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(48.dp))
                            }
                        }
                        Text("SLURM Manager", style = MaterialTheme.typography.headlineSmallEmphasized)
                        Text(
                            "SSH-based HPC job manager for researchers and engineers",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        // Version pill
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                Text("v$versionName (build $versionCode)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }
            }

            // Tech stack
            item { SectionLabel("Stack") }
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    InfoListItem("Platform", "Android 11+ (API 30)")
                    SettingsDivider()
                    InfoListItem("UI", "Jetpack Compose · Material You 3 Expressive")
                    SettingsDivider()
                    InfoListItem("SSH", "sshj 0.38 · BouncyCastle 1.78")
                    SettingsDivider()
                    InfoListItem("Storage", "Room · DataStore")
                    SettingsDivider()
                    InfoListItem("DI", "Hilt")
                }
            }

            // Developer
            item { SectionLabel("Developer") }
            item {
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                ElevatedCard(
                    onClick = { uriHandler.openUri("https://github.com/Tresillo2017") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(28.dp))
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tomás Palma", style = MaterialTheme.typography.titleMediumEmphasized)
                            Text("@Tresillo2017 · github.com", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Feedback & support
            item { SectionLabel("Feedback & Support") }
            item {
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        leadingContent = {
                            Icon(Icons.Default.BugReport, contentDescription = null,
                                tint = MaterialTheme.colorScheme.error)
                        },
                        headlineContent = { Text("Report a Bug", style = MaterialTheme.typography.bodyLarge) },
                        supportingContent = { Text("Open a GitHub issue", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        trailingContent = {
                            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null,
                                modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://github.com/Tresillo2017/slurm-manager/issues/new?template=bug_report.yml")
                        }
                    )
                    SettingsDivider()
                    ListItem(
                        leadingContent = {
                            Icon(Icons.Default.Lightbulb, contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary)
                        },
                        headlineContent = { Text("Request a Feature", style = MaterialTheme.typography.bodyLarge) },
                        supportingContent = { Text("Suggest an improvement", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        trailingContent = {
                            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null,
                                modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://github.com/Tresillo2017/slurm-manager/issues/new?template=feature_request.yml")
                        }
                    )
                    SettingsDivider()
                    ListItem(
                        leadingContent = {
                            Icon(Icons.Default.Feedback, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary)
                        },
                        headlineContent = { Text("Send Feedback", style = MaterialTheme.typography.bodyLarge) },
                        supportingContent = { Text("General thoughts or UX comments", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        trailingContent = {
                            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null,
                                modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://github.com/Tresillo2017/slurm-manager/issues/new?template=feedback.yml")
                        }
                    )
                    SettingsDivider()
                    ListItem(
                        leadingContent = {
                            Icon(Icons.Default.Forum, contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary)
                        },
                        headlineContent = { Text("Discussions", style = MaterialTheme.typography.bodyLarge) },
                        supportingContent = { Text("Ask questions, share ideas", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        trailingContent = {
                            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null,
                                modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://github.com/Tresillo2017/slurm-manager/discussions")
                        }
                    )
                }
            }

            // Open source
            item { SectionLabel("Open Source") }
            item {
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    InfoListItem("License", "MIT")
                    SettingsDivider()
                    ListItem(
                        headlineContent = { Text("Source", style = MaterialTheme.typography.bodyLarge) },
                        trailingContent = {
                            TextButton(onClick = { uriHandler.openUri("https://github.com/Tresillo2017/slurm-manager") }) {
                                Text("GitHub", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoListItem(label: String, value: String) {
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        trailingContent = {
            Text(value, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    )
}

// ─── Shared primitives ──────────────────────────────────────────────────────

@Composable
private fun SwitchListItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    ListItem(
        headlineContent = {
            Text(title, style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
        },
        supportingContent = {
            Text(subtitle,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f))
        },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        },
        modifier = if (enabled) Modifier.clickable { onCheckedChange(!checked) } else Modifier
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}
