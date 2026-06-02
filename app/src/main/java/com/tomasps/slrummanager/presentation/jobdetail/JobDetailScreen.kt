package com.tomasps.slrummanager.presentation.jobdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomasps.slrummanager.domain.model.Job
import com.tomasps.slrummanager.presentation.dashboard.JobStateChip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailScreen(
    jobId: String,
    serverId: String,
    onBack: () -> Unit,
    viewModel: JobDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val job = state.job

    state.actionMessage?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(job?.name ?: "Job Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = {
            state.actionMessage?.let { msg ->
                SnackbarHost(hostState = remember { SnackbarHostState().also { host ->
                    // message shown via LaunchedEffect below
                } })
            }
        }
    ) { padding ->
        if (job == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            state.actionMessage?.let { msg ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Text(msg, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                JobStateChip(job.state)
                Text("ID: ${job.jobId}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Resource grid
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Resources", style = MaterialTheme.typography.titleSmall)
                    HorizontalDivider()
                    ResourceGrid(job)
                }
            }

            // Timeline
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Timeline", style = MaterialTheme.typography.titleSmall)
                    HorizontalDivider()
                    TimelineRow("Submitted", job.submitTime)
                    job.startTime?.let { TimelineRow("Started", it) }
                    job.endTime?.let { TimelineRow("Ended", it) }
                    if (job.startTime != null) {
                        val elapsed = (job.endTime ?: System.currentTimeMillis()) - job.startTime
                        Text("Runtime: ${formatDuration(elapsed)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Actions
            if (!job.state.isTerminal) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { viewModel.cancelJob() }, modifier = Modifier.weight(1f)) {
                        Text("Cancel Job")
                    }
                    FilledTonalButton(onClick = { viewModel.requeueJob() }, modifier = Modifier.weight(1f)) {
                        Text("Requeue")
                    }
                }
            }
            TextButton(onClick = {}) {
                Text("Work Dir: ${job.workDir}")
            }
        }
    }
}

@Composable
private fun ResourceGrid(job: Job) {
    val items = listOf(
        "Nodes" to "${job.nodes}",
        "CPUs" to "${job.cpus}",
        "Memory" to "${job.memoryMb} MB",
        "Partition" to job.partition,
        "Priority" to "${job.priority}",
        "Exit Code" to "${job.exitCode ?: "-"}"
    )
    items.chunked(2).forEach { row ->
        Row(modifier = Modifier.fillMaxWidth()) {
            row.forEach { (label, value) ->
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun TimelineRow(label: String, timestamp: Long) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatTimestamp(timestamp), style = MaterialTheme.typography.bodySmall)
    }
}

private val timestampFmt = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
private fun formatTimestamp(ms: Long) = timestampFmt.format(Date(ms))
private fun formatDuration(ms: Long): String {
    val s = ms / 1000; val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return if (h > 0) "%dh %02dm %02ds".format(h, m, sec) else "%dm %02ds".format(m, sec)
}
