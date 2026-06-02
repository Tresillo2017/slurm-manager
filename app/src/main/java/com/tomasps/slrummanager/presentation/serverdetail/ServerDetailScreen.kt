package com.tomasps.slrummanager.presentation.serverdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomasps.slrummanager.domain.model.AlertRule
import com.tomasps.slrummanager.domain.model.AlertType
import com.tomasps.slrummanager.domain.model.JobState
import com.tomasps.slrummanager.domain.model.ServerStatus
import com.tomasps.slrummanager.presentation.dashboard.JobCard
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text(server?.name ?: "Server") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val color = when (server?.status) {
                        ServerStatus.ONLINE -> MaterialTheme.colorScheme.primary
                        ServerStatus.UNREACHABLE, ServerStatus.OFFLINE -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Badge(containerColor = color) {
                        Text(server?.status?.name ?: "", modifier = Modifier.padding(horizontal = 4.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Summary row
            server?.let { s ->
                val today = System.currentTimeMillis() - 86_400_000L
                val todayJobs = state.jobs.filter { (it.endTime ?: 0L) > today || it.state.isActive }
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryCard("Running", state.jobs.count { it.state.isActive }, Modifier.weight(1f))
                    SummaryCard("Pending", state.jobs.count { it.state == JobState.PENDING }, Modifier.weight(1f))
                    SummaryCard("Done Today", todayJobs.count { it.state == JobState.COMPLETED }, Modifier.weight(1f))
                }
            }

            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { i, title ->
                    Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(title) })
                }
            }

            when (selectedTab) {
                0 -> JobsTab(state = state, onJobClick = onJobClick)
                1 -> HistoryTab(state = state, onJobClick = onJobClick)
                2 -> SettingsTab(state = state, viewModel = viewModel, context = context)
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, count: Int, modifier: Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$count", style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun JobsTab(state: ServerDetailUiState, onJobClick: (String) -> Unit) {
    val active = state.jobs.filter { !it.state.isTerminal }
    if (active.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active jobs", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(active, key = { it.jobId }) { job ->
                JobCard(job = job, serverName = state.server?.name ?: "", onClick = { onJobClick(job.jobId) })
            }
        }
    }
}

@Composable
private fun HistoryTab(state: ServerDetailUiState, onJobClick: (String) -> Unit) {
    val history = state.jobs.filter { it.state.isTerminal }.sortedByDescending { it.endTime }
    if (history.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No history", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(history, key = { it.jobId }) { job ->
                JobCard(job = job, serverName = state.server?.name ?: "", onClick = { onJobClick(job.jobId) })
            }
        }
    }
}

@Composable
private fun SettingsTab(state: ServerDetailUiState, viewModel: ServerDetailViewModel, context: android.content.Context) {
    val server = state.server ?: return
    var pollingSlider by remember(server.pollingIntervalMinutes) { mutableFloatStateOf(server.pollingIntervalMinutes.toFloat()) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Polling Interval: ${pollingSlider.roundToInt()} min", style = MaterialTheme.typography.titleSmall)
            Slider(
                value = pollingSlider,
                onValueChange = { pollingSlider = it },
                onValueChangeFinished = { viewModel.updatePollingInterval(pollingSlider.roundToInt()) },
                valueRange = 1f..60f,
                steps = 58
            )
        }
        item {
            Text("Alert Rules", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider()
        }
        items(AlertType.entries) { type ->
            val existing = server.alertRules.find { it.type == type }
            var enabled by remember(existing) { mutableStateOf(existing != null) }
            var threshold by remember(existing) { mutableStateOf(existing?.thresholdMinutes?.toString() ?: "") }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(type.name.replace('_', ' '), style = MaterialTheme.typography.bodyMedium)
                    if (enabled && type != AlertType.NODE_FAILURE && type != AlertType.PARTITION_DOWN) {
                        OutlinedTextField(
                            value = threshold,
                            onValueChange = { threshold = it },
                            label = { Text("Threshold (min)") },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        )
                    }
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { checked ->
                        enabled = checked
                        val newRules = AlertType.entries.mapNotNull { t ->
                            val isEnabled = if (t == type) checked else server.alertRules.any { it.type == t }
                            if (isEnabled) com.tomasps.slrummanager.domain.model.AlertRule(
                                type = t,
                                thresholdMinutes = if (t == type) threshold.toIntOrNull() else server.alertRules.find { it.type == t }?.thresholdMinutes
                            ) else null
                        }
                        viewModel.updateAlertRules(newRules)
                    }
                )
            }
        }
        item {
            FilledTonalButton(onClick = { viewModel.testConnection(context) }, modifier = Modifier.fillMaxWidth()) {
                Text("Test Connection")
            }
            when (val result = state.connectionTestResult) {
                is ConnectionTestResult.Testing -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                is ConnectionTestResult.Success -> Text("Connected in ${result.latencyMs}ms ✓", color = MaterialTheme.colorScheme.primary)
                is ConnectionTestResult.Failure -> Text("Failed: ${result.error}", color = MaterialTheme.colorScheme.error)
                null -> {}
            }
        }
    }
}
