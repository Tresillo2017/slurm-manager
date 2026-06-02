package com.tomasps.slrummanager.presentation.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomasps.slrummanager.domain.model.Job
import com.tomasps.slrummanager.domain.model.JobState
import com.tomasps.slrummanager.presentation.submit.SubmitJobSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onJobClick: (jobId: String, serverId: String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showSubmit by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Dashboard") },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showSubmit = true }) {
                Icon(Icons.Default.Add, contentDescription = "Submit Job")
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = {},
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            Column {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = state.selectedServerId == null,
                            onClick = { viewModel.selectServer(null) },
                            label = { Text("All") }
                        )
                    }
                    items(state.servers) { server ->
                        FilterChip(
                            selected = state.selectedServerId == server.id.toString(),
                            onClick = { viewModel.selectServer(server.id.toString()) },
                            label = { Text(server.name) }
                        )
                    }
                }
                if (state.jobs.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No jobs", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.jobs, key = { "${it.serverId}:${it.jobId}" }) { job ->
                            val serverName = state.servers.find { it.id == job.serverId }?.name ?: ""
                            JobCard(job = job, serverName = serverName, onClick = {
                                onJobClick(job.jobId, job.serverId.toString())
                            })
                        }
                    }
                }
            }
        }
    }

    if (showSubmit) {
        SubmitJobSheet(
            serverId = state.selectedServerId,
            servers = state.servers,
            onDismiss = { showSubmit = false }
        )
    }
}

@Composable
fun JobCard(job: Job, serverName: String, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(job.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                JobStateChip(job.state)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(serverName, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 4.dp))
                }
                Text("ID: ${job.jobId}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(job.partition, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (job.state.isActive && job.startTime != null) {
                val elapsed = formatElapsed(System.currentTimeMillis() - job.startTime)
                Text("Elapsed: $elapsed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun JobStateChip(state: JobState) {
    val (container, onContainer) = when {
        state.isActive -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        state == JobState.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        state.isTerminal && state != JobState.COMPLETED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    AssistChip(
        onClick = {},
        label = { Text(state.name, style = MaterialTheme.typography.labelSmall) },
        colors = AssistChipDefaults.assistChipColors(containerColor = container, labelColor = onContainer)
    )
}

private fun formatElapsed(ms: Long): String {
    val s = ms / 1000; val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return if (h > 0) "%dh %02dm".format(h, m) else "%dm %02ds".format(m, sec)
}
