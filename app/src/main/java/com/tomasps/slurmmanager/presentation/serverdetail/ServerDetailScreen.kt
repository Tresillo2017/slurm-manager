package com.tomasps.slurmmanager.presentation.serverdetail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.window.core.layout.WindowWidthSizeClass
import com.tomasps.slurmmanager.domain.model.AlertType
import com.tomasps.slurmmanager.domain.model.AuthMethod
import com.tomasps.slurmmanager.domain.model.Job
import com.tomasps.slurmmanager.domain.model.JobState
import com.tomasps.slurmmanager.domain.model.ServerStatus
import com.tomasps.slurmmanager.presentation.dashboard.JobCard
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ServerDetailScreen(
    serverId: String,
    onBack: () -> Unit,
    onJobClick: (jobId: String) -> Unit,
    viewModel: ServerDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val server = state.server
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Jobs", "History", "Settings")
    val isMediumPlus = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT
    var selectedJobInDetail by remember { mutableStateOf<Job?>(null) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(server?.name ?: "Server") },
                subtitle = server?.let { s -> {
                    Text("${s.username}@${s.hostname}:${s.port}")
                } },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    server?.let {
                        val (container, content) = when (it.status) {
                            ServerStatus.ONLINE -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                            ServerStatus.UNREACHABLE, ServerStatus.OFFLINE -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                            ServerStatus.UNKNOWN -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Surface(
                            shape = CircleShape,
                            color = container,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                it.status.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = content,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Connection error / auth error banner ──────────────────
            if (server?.status == ServerStatus.UNREACHABLE || server?.status == ServerStatus.OFFLINE) {
                Surface(color = MaterialTheme.colorScheme.errorContainer) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (state.isAuthError) Icons.Default.Lock else Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (state.isAuthError) "Authentication failed" else "Cannot reach ${server.hostname}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                if (state.isAuthError) "SSH credentials were rejected. Tap Fix to update."
                                else "Data may be outdated. Check network connectivity.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        if (state.isAuthError) {
                            Button(
                                onClick = { viewModel.openCredentialEditor() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) { Text("Fix") }
                        }
                    }
                }
            }

            // Credential editor dialog
            if (state.showCredentialEditor && server != null) {
                CredentialEditorDialog(
                    server = server,
                    context = context,
                    onSave = { username, authMethod, password, key ->
                        viewModel.saveCredentials(username, authMethod, password, key, context)
                    },
                    onDismiss = { viewModel.closeCredentialEditor() }
                )
            }

            // ── Stats strip ───────────────────────────────────────────
            server?.let {
                val todayCutoff = System.currentTimeMillis() - 86_400_000L
                val running = state.jobs.count { j -> j.state.isActive }
                val pending = state.jobs.count { j -> j.state == JobState.PENDING }
                val doneToday = state.jobs.count { j ->
                    j.state == JobState.COMPLETED && (j.endTime ?: 0L) > todayCutoff
                }
                val failed = state.jobs.count { j ->
                    j.state == JobState.FAILED || j.state == JobState.CANCELLED
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatTile("Running", running, Icons.Default.PlayArrow,
                        if (running > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                        if (running > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        Modifier.weight(1f))
                    StatTile("Pending", pending, Icons.Default.HourglassEmpty,
                        MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer,
                        Modifier.weight(1f))
                    StatTile("Done today", doneToday, Icons.Default.CheckCircle,
                        MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer,
                        Modifier.weight(1f))
                    StatTile("Failed", failed, Icons.Default.Error,
                        if (failed > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                        if (failed > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        Modifier.weight(1f))
                }
            }

            // ── Tabs ──────────────────────────────────────────────────
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { i, title ->
                    Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(title) })
                }
            }

            // ── Content ───────────────────────────────────────────────
            if (selectedTab == 0 && isMediumPlus) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    val active = state.jobs.filter { !it.state.isTerminal }
                    LazyColumn(
                        modifier = Modifier.weight(0.45f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (active.isEmpty()) {
                            item { TabEmptyState(icon = Icons.Default.CheckCircle, message = "No active jobs") }
                        } else {
                            items(active, key = { it.jobId }) { job ->
                                JobCard(job = job, serverName = server?.name ?: "", onClick = { selectedJobInDetail = job })
                            }
                        }
                    }
                    VerticalDivider()
                    Box(Modifier.weight(0.55f).fillMaxHeight()) {
                        if (selectedJobInDetail != null) {
                            com.tomasps.slurmmanager.presentation.jobdetail.JobDetailScreen(
                                jobId = selectedJobInDetail!!.jobId,
                                serverId = serverId,
                                onBack = { selectedJobInDetail = null },
                                isInlinePane = true
                            )
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.TouchApp, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                                    Text("Select a job to see details", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            } else {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        (slideInHorizontally { it * dir } + fadeIn()) togetherWith (slideOutHorizontally { -it * dir } + fadeOut())
                    },
                    modifier = Modifier.weight(1f)
                ) { tab ->
                    when (tab) {
                        0 -> JobsTab(state = state, onJobClick = onJobClick)
                        1 -> HistoryTab(state = state, onJobClick = onJobClick)
                        2 -> SettingsTab(state = state, viewModel = viewModel, context = context)
                    }
                }
            }
        }
    }
}

// ─── Stat Tile ────────────────────────────────────────────────────────────────

@Composable
private fun StatTile(
    label: String,
    count: Int,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(shape = MaterialTheme.shapes.extraLarge, color = containerColor, modifier = modifier) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
            Text("$count", style = MaterialTheme.typography.titleMediumEmphasized, color = contentColor)
            Text(label, style = MaterialTheme.typography.labelSmall, color = contentColor)
        }
    }
}

// ─── Tab Empty State ──────────────────────────────────────────────────────────

@Composable
private fun TabEmptyState(icon: ImageVector, message: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

// ─── Jobs Tab ─────────────────────────────────────────────────────────────────

@Composable
private fun JobsTab(state: ServerDetailUiState, onJobClick: (String) -> Unit) {
    val active = state.jobs.filter { !it.state.isTerminal }
    if (active.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TabEmptyState(Icons.Default.CheckCircle, "No active jobs\nAll quiet on this cluster.")
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(active, key = { it.jobId }) { job ->
                JobCard(job = job, serverName = state.server?.name ?: "", onClick = { onJobClick(job.jobId) })
            }
        }
    }
}

// ─── History Tab ──────────────────────────────────────────────────────────────

@Composable
private fun HistoryTab(state: ServerDetailUiState, onJobClick: (String) -> Unit) {
    val history = state.jobs.filter { it.state.isTerminal }.sortedByDescending { it.endTime }
    if (history.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TabEmptyState(Icons.Default.History, "No job history yet\nCompleted jobs will appear here.")
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(history, key = { it.jobId }) { job ->
                JobCard(job = job, serverName = state.server?.name ?: "", onClick = { onJobClick(job.jobId) })
            }
        }
    }
}

// ─── Settings Tab ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsTab(
    state: ServerDetailUiState,
    viewModel: ServerDetailViewModel,
    context: android.content.Context
) {
    val server = state.server ?: return
    var pollingSlider by remember(server.pollingIntervalMinutes) {
        mutableFloatStateOf(server.pollingIntervalMinutes.toFloat())
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Polling section ──
        item {
            SectionLabel("Polling", Icons.Default.Sync)
            Spacer(Modifier.height(8.dp))
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Polling Interval", style = MaterialTheme.typography.bodyMedium)
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(
                                "${pollingSlider.roundToInt()} min",
                                style = MaterialTheme.typography.labelLargeEmphasized,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Slider(
                        value = pollingSlider,
                        onValueChange = { pollingSlider = it },
                        onValueChangeFinished = { viewModel.updatePollingInterval(pollingSlider.roundToInt()) },
                        valueRange = 1f..60f,
                        steps = 58,
                        modifier = Modifier.fillMaxWidth(),
                        track = { sliderState ->
                            LinearWavyProgressIndicator(
                                progress = { sliderState.value / sliderState.valueRange.endInclusive },
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

        // ── Alert rules section ──
        item {
            SectionLabel("Alert Rules", Icons.Default.NotificationsActive)
            Spacer(Modifier.height(8.dp))
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                AlertType.entries.forEachIndexed { index, type ->
                    val existing = server.alertRules.find { it.type == type }
                    var enabled by remember(existing) { mutableStateOf(existing != null) }
                    var threshold by remember(existing) { mutableStateOf(existing?.thresholdMinutes?.toString() ?: "") }

                    val hasThreshold = type != AlertType.NODE_FAILURE && type != AlertType.PARTITION_DOWN

                    Column {
                        ListItem(
                            headlineContent = {
                                Text(type.name.replace('_', ' ').lowercase()
                                    .split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) })
                            },
                            supportingContent = if (enabled && hasThreshold) ({
                                OutlinedTextField(
                                    value = threshold,
                                    onValueChange = { v ->
                                        threshold = v
                                        val newRules = AlertType.entries.mapNotNull { t ->
                                            val isEnabled = if (t == type) enabled else server.alertRules.any { it.type == t }
                                            if (isEnabled) com.tomasps.slurmmanager.domain.model.AlertRule(
                                                type = t,
                                                thresholdMinutes = if (t == type) v.toIntOrNull() else server.alertRules.find { it.type == t }?.thresholdMinutes
                                            ) else null
                                        }
                                        viewModel.updateAlertRules(newRules)
                                    },
                                    label = { Text("Threshold (min)") },
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    singleLine = true
                                )
                            }) else null,
                            trailingContent = {
                                Switch(
                                    checked = enabled,
                                    onCheckedChange = { checked ->
                                        enabled = checked
                                        val newRules = AlertType.entries.mapNotNull { t ->
                                            val isEnabled = if (t == type) checked else server.alertRules.any { it.type == t }
                                            if (isEnabled) com.tomasps.slurmmanager.domain.model.AlertRule(
                                                type = t,
                                                thresholdMinutes = if (t == type) threshold.toIntOrNull() else server.alertRules.find { it.type == t }?.thresholdMinutes
                                            ) else null
                                        }
                                        viewModel.updateAlertRules(newRules)
                                    }
                                )
                            }
                        )
                        if (index < AlertType.entries.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }

        // ── Connection section ──
        item {
            SectionLabel("Connection", Icons.Default.Wifi)
            Spacer(Modifier.height(8.dp))
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = { viewModel.testConnection(context) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Test")
                        }
                        Button(
                            onClick = { viewModel.forceFetch(context) },
                            enabled = !state.isFetching,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (state.isFetching) {
                                LoadingIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Fetching…")
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Fetch Now")
                            }
                        }
                    }
                    when (val result = state.connectionTestResult) {
                        is ConnectionTestResult.Testing -> LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                        is ConnectionTestResult.Success -> Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                                Text("Connected in ${result.latencyMs}ms",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        is ConnectionTestResult.Failure -> Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(Icons.Default.Error, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
                                Text("Failed: ${result.error}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                        null -> {}
                    }
                }
            }
        }
    }
}

// ─── Section Label ────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null,
            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    }
}

// ─── Credential Editor Dialog ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CredentialEditorDialog(
    server: com.tomasps.slurmmanager.domain.model.Server,
    context: android.content.Context,
    onSave: (String, AuthMethod, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var username by remember { mutableStateOf(server.username) }
    var authMethod by remember { mutableStateOf(server.authMethod) }
    var password by remember { mutableStateOf("") }
    var privateKeyPem by remember { mutableStateOf("") }
    val canSave = username.isNotBlank() && when (authMethod) {
        AuthMethod.PASSWORD -> password.isNotBlank()
        AuthMethod.SSH_KEY -> privateKeyPem.isNotBlank()
    }
    val keyPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val pem = context.contentResolver.openInputStream(it)?.bufferedReader()?.readText() ?: return@let
            privateKeyPem = pem
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Key, contentDescription = null) },
        title = { Text("Update credentials") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Update credentials for ${server.name} (${server.hostname})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = username, onValueChange = { username = it },
                    label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AuthMethod.entries.forEachIndexed { i, method ->
                        val label = if (method == AuthMethod.PASSWORD) "Password" else "SSH Key"
                        ToggleButton(
                            checked = authMethod == method,
                            onCheckedChange = { authMethod = method },
                            modifier = Modifier.weight(1f),
                            shapes = when (i) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                AuthMethod.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            }
                        ) { Text(label) }
                    }
                }
                if (authMethod == AuthMethod.PASSWORD) {
                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        label = { Text("New password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                } else {
                    OutlinedButton(onClick = { keyPicker.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (privateKeyPem.isBlank()) "Import private key file" else "Key loaded ✓")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(username, authMethod, password, privateKeyPem) }, enabled = canSave) {
                Text("Save & reconnect")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
