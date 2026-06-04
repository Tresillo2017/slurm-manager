package com.tomasps.slurmmanager.presentation.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.window.core.layout.WindowWidthSizeClass
import com.tomasps.slurmmanager.domain.model.Job
import com.tomasps.slurmmanager.domain.model.JobState
import com.tomasps.slurmmanager.domain.model.ServerStatus
import com.tomasps.slurmmanager.presentation.submit.SubmitJobSheet

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DashboardScreen(
    onJobClick: (jobId: String, serverId: String) -> Unit,
    showSubmitJob: Boolean = false,
    onDismissSubmitJob: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val isExpanded = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED
    var selectedJob by remember { mutableStateOf<Job?>(null) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            val statsSubtitle = buildString {
                val s = state.stats
                if (s.running > 0) append("${s.running} running")
                if (s.pending > 0) { if (isNotEmpty()) append(" · "); append("${s.pending} pending") }
                if (s.failed > 0) { if (isNotEmpty()) append(" · "); append("${s.failed} failed") }
            }
            LargeFlexibleTopAppBar(
                title = { Text("Dashboard") },
                subtitle = if (statsSubtitle.isNotEmpty()) ({ Text(statsSubtitle) }) else null,
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        if (isExpanded) {
            val pullState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { viewModel.refresh() },
                state = pullState,
                modifier = Modifier.fillMaxSize(),
                indicator = {
                    PullToRefreshDefaults.LoadingIndicator(state = pullState, isRefreshing = state.isRefreshing,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = padding.calculateTopPadding()))
                }
            ) {
            Row(modifier = Modifier.fillMaxSize()) {
                DashboardListPane(
                    state = state,
                    viewModel = viewModel,
                    onJobClick = { job -> selectedJob = job },
                    modifier = Modifier.weight(0.4f),
                    topPadding = padding.calculateTopPadding(),
                )
                VerticalDivider()
                Box(Modifier.weight(0.6f).fillMaxHeight().padding(top = padding.calculateTopPadding())) {
                    if (selectedJob != null) {
                        com.tomasps.slurmmanager.presentation.jobdetail.JobDetailScreen(
                            jobId = selectedJob!!.jobId,
                            serverId = selectedJob!!.serverId.toString(),
                            onBack = { selectedJob = null },
                            isInlinePane = true
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.TouchApp, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                                Text("Select a job to see details",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
            } // PullToRefreshBox
        } else {
            val pullState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { viewModel.refresh() },
                state = pullState,
                modifier = Modifier.fillMaxSize().padding(padding),
                indicator = {
                    PullToRefreshDefaults.LoadingIndicator(state = pullState, isRefreshing = state.isRefreshing,
                        modifier = Modifier.align(Alignment.TopCenter))
                }
            ) {
                DashboardListPane(
                    state = state,
                    viewModel = viewModel,
                    onJobClick = { job -> onJobClick(job.jobId, job.serverId.toString()) },
                )
            }
        }
    }

    if (showSubmitJob) {
        SubmitJobSheet(
            serverId = state.selectedServerId,
            servers = state.servers,
            onDismiss = onDismissSubmitJob
        )
    }
}

// ─── Stats Strip ─────────────────────────────────────────────────────────────

@Composable
private fun StatsStrip(stats: DashboardStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatTile(
            label = "Running",
            value = "${stats.running}",
            icon = Icons.Default.PlayArrow,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f)
        )
        StatTile(
            label = "Pending",
            value = "${stats.pending}",
            icon = Icons.Default.HourglassEmpty,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f)
        )
        StatTile(
            label = "Done today",
            value = "${stats.completedToday}",
            icon = Icons.Default.CheckCircle,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.weight(1f)
        )
        StatTile(
            label = "Failed",
            value = "${stats.failed}",
            icon = Icons.Default.Error,
            containerColor = if (stats.failed > 0) MaterialTheme.colorScheme.errorContainer
                             else MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = if (stats.failed > 0) MaterialTheme.colorScheme.onErrorContainer
                           else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        modifier = modifier.fillMaxHeight()
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleLargeEmphasized, color = contentColor)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.8f),
                maxLines = 1)
        }
    }
}

// ─── List Pane ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DashboardListPane(
    state: DashboardUiState,
    viewModel: DashboardViewModel,
    onJobClick: (Job) -> Unit,
    modifier: Modifier = Modifier,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp,
) {
    val unreachable = state.servers.filter {
        it.status == ServerStatus.UNREACHABLE || it.status == ServerStatus.OFFLINE
    }
    val stateFilters = listOf(
        null to "All",
        JobState.RUNNING to "Running",
        JobState.PENDING to "Pending",
        JobState.COMPLETED to "Done",
        JobState.FAILED to "Failed",
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = topPadding, bottom = 160.dp),
    ) {
        // Unreachable banner
        if (unreachable.isNotEmpty()) {
            item(key = "banner") {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .animateItem()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.WifiOff, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(18.dp))
                        Text(
                            text = if (unreachable.size == 1) "${unreachable[0].name} is unreachable"
                            else "${unreachable.size} servers unreachable: ${unreachable.joinToString { it.name }}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Stats strip — scrolls away with content
        item(key = "stats") {
            StatsStrip(stats = state.stats)
        }

        // Sticky filter header — stays visible while scrolling the job list
        stickyHeader(key = "filters") {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = state.selectedServerId == null,
                                onClick = { viewModel.selectServer(null) },
                                label = { Text("All clusters") },
                                shapes = ChipShapes(
                                    shape = MaterialTheme.shapes.small,
                                    selectedShape = CircleShape,
                                    pressedShape = MaterialTheme.shapes.medium,
                                ),
                                colors = FilterChipDefaults.tonalFilterChipColors(),
                                border = null
                            )
                        }
                        items(state.servers) { server ->
                            val hasError = server.status == ServerStatus.UNREACHABLE || server.status == ServerStatus.OFFLINE
                            FilterChip(
                                selected = state.selectedServerId == server.id.toString(),
                                onClick = { viewModel.selectServer(server.id.toString()) },
                                label = { Text(server.name) },
                                leadingIcon = if (hasError) ({
                                    Icon(Icons.Default.WifiOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                }) else null,
                                shapes = ChipShapes(
                                    shape = MaterialTheme.shapes.small,
                                    selectedShape = CircleShape,
                                    pressedShape = MaterialTheme.shapes.medium,
                                ),
                                colors = FilterChipDefaults.tonalFilterChipColors(),
                                border = null
                            )
                        }
                    }
                    LazyRow(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(stateFilters, key = { it.first?.name ?: "all" }) { (jobState, label) ->
                            FilterChip(
                                selected = state.statusFilter == jobState,
                                onClick = { viewModel.setStatusFilter(jobState) },
                                label = { Text(label) },
                                shapes = ChipShapes(
                                    shape = MaterialTheme.shapes.small,
                                    selectedShape = CircleShape,
                                    pressedShape = MaterialTheme.shapes.medium,
                                ),
                                colors = FilterChipDefaults.tonalFilterChipColors(),
                                border = null
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }

        // Jobs section header with count
        item(key = "jobs_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.List, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text("Jobs", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                }
                if (state.jobs.isNotEmpty()) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                        Text(
                            "${state.jobs.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // Job items or empty state
        if (state.jobs.isEmpty()) {
            item(key = "empty") {
                EmptyState(state = state)
            }
        } else {
            items(state.jobs, key = { "${it.serverId}:${it.jobId}" }) { job ->
                val serverName = state.servers.find { it.id == job.serverId }?.name ?: ""
                JobCard(
                    job = job,
                    serverName = serverName,
                    onClick = { onJobClick(job) },
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                )
            }
        }
    }
}

// ─── Empty State ─────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(state: DashboardUiState) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 64.dp), contentAlignment = Alignment.Center) {
        val noServers = state.servers.isEmpty()
        val allUnreachable = state.servers.isNotEmpty() &&
            state.servers.all { it.status == ServerStatus.UNREACHABLE || it.status == ServerStatus.OFFLINE }
        val waitingForPoll = state.servers.isNotEmpty() &&
            state.servers.all { it.status == ServerStatus.UNKNOWN }
        val isFiltered = state.selectedServerId != null || state.statusFilter != null

        val (icon, iconTint, iconBg, title, subtitle) = when {
            noServers -> EmptyStateData(
                Icons.Default.Storage, MaterialTheme.colorScheme.onSecondaryContainer,
                MaterialTheme.colorScheme.secondaryContainer,
                "No clusters yet",
                "Go to Servers and add your first HPC cluster to start monitoring jobs."
            )
            allUnreachable -> EmptyStateData(
                Icons.Default.WifiOff, MaterialTheme.colorScheme.onErrorContainer,
                MaterialTheme.colorScheme.errorContainer,
                "All servers unreachable",
                "Check your SSH credentials and network. Jobs will appear once a server comes back online."
            )
            waitingForPoll -> EmptyStateData(
                Icons.Default.HourglassEmpty, MaterialTheme.colorScheme.onTertiaryContainer,
                MaterialTheme.colorScheme.tertiaryContainer,
                "Waiting for first poll",
                "The app is connecting to your cluster. This usually takes a few seconds."
            )
            isFiltered -> EmptyStateData(
                Icons.Default.FilterList, MaterialTheme.colorScheme.onSecondaryContainer,
                MaterialTheme.colorScheme.secondaryContainer,
                "No matching jobs",
                "Try clearing the filter or selecting a different server."
            )
            else -> EmptyStateData(
                Icons.Default.CheckCircle, MaterialTheme.colorScheme.onPrimaryContainer,
                MaterialTheme.colorScheme.primaryContainer,
                "No jobs running",
                "Your clusters are idle. Tap Submit Job to queue new work."
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = iconBg, modifier = Modifier.size(72.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(36.dp))
                }
            }
            Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Job Card ────────────────────────────────────────────────────────────────

@Composable
fun JobCard(job: Job, serverName: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accentColor = when {
        job.state.isActive -> MaterialTheme.colorScheme.primary
        job.state == JobState.COMPLETED -> MaterialTheme.colorScheme.tertiary
        job.state.isTerminal -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }

    ElevatedCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Colored left accent strip
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )

            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header: name + state chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        job.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    JobStateChip(job.state)
                }

                // Meta row: server · job ID · partition
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                serverName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    Text("·", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outlineVariant)
                    Text("#${job.jobId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("·", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outlineVariant)
                    Text(job.partition, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Resource pills
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ResourcePill(icon = Icons.Default.Dns, label = "${job.nodes}N")
                    ResourcePill(icon = Icons.Default.Memory, label = "${job.cpus} CPU")
                    ResourcePill(icon = Icons.Default.Storage, label = formatMemory(job.memoryMb))
                }

                // State-specific footer
                when {
                    job.state == JobState.PENDING && job.queuePosition != null -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Queue position ${job.queuePosition}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    job.state.isActive && job.startTime != null -> {
                        val elapsed = System.currentTimeMillis() - job.startTime
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(13.dp), tint = accentColor)
                            Text(formatElapsed(elapsed), style = MaterialTheme.typography.bodySmall, color = accentColor)
                            if (job.nodelist.isNotBlank()) {
                                Text("· ${job.nodelist}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    job.state.isTerminal && job.endTime != null -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Ended ${formatRelativeTime(job.endTime)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (job.exitCode != null && job.exitCode != 0) {
                                Text("· exit ${job.exitCode}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResourcePill(icon: ImageVector, label: String) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─── State Chip ──────────────────────────────────────────────────────────────

@Composable
fun JobStateChip(state: JobState) {
    val (container, onContainer) = when {
        state.isActive -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        state == JobState.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        state.isTerminal -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val icon = when (state) {
        JobState.RUNNING, JobState.COMPLETING -> Icons.Default.PlayArrow
        JobState.PENDING -> Icons.Default.HourglassEmpty
        JobState.COMPLETED -> Icons.Default.CheckCircle
        JobState.FAILED -> Icons.Default.Error
        JobState.CANCELLED -> Icons.Default.Cancel
        JobState.SUSPENDED -> Icons.Default.Pause
        else -> Icons.Default.Info
    }
    val label = state.name.lowercase().replaceFirstChar { it.uppercase() }
    SuggestionChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp)) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = container,
            labelColor = onContainer,
            iconContentColor = onContainer
        ),
        border = null
    )
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private data class EmptyStateData(
    val icon: ImageVector,
    val iconTint: Color,
    val iconBg: Color,
    val title: String,
    val subtitle: String
)

private fun formatElapsed(ms: Long): String {
    val s = ms / 1000; val h = s / 3600; val m = (s % 3600) / 60
    return if (h > 0) "%dh %02dm".format(h, m) else "%dm %02ds".format(m, s % 60)
}

private fun formatRelativeTime(ts: Long): String {
    val diff = System.currentTimeMillis() - ts
    val min = diff / 60_000
    return when {
        min < 1 -> "just now"
        min < 60 -> "${min}m ago"
        else -> "${min / 60}h ago"
    }
}

private fun formatMemory(mb: Long): String = when {
    mb >= 1024 -> "${mb / 1024}GB"
    else -> "${mb}MB"
}
