package com.tomasps.slurmmanager.presentation.jobdetail

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomasps.slurmmanager.domain.model.Job
import com.tomasps.slurmmanager.domain.model.JobState
import com.tomasps.slurmmanager.presentation.dashboard.JobStateChip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JobDetailScreen(
    jobId: String,
    serverId: String,
    onBack: () -> Unit,
    viewModel: JobDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val job = state.job
    var showCancelDialog by remember { mutableStateOf(false) }
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

    BackHandler(fabMenuExpanded) { fabMenuExpanded = false }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(job?.name ?: "Job Detail", maxLines = 2, overflow = TextOverflow.Ellipsis)
                },
                subtitle = job?.let { j -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Tag,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    j.jobId,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Category,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    j.partition,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                } },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {},
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = job != null,
            transitionSpec = { fadeIn(spring(stiffness = Spring.StiffnessLow)) togetherWith fadeOut() },
            modifier = Modifier.fillMaxSize()
        ) { hasJob ->
            if (!hasJob || job == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LoadingIndicator(modifier = Modifier.size(48.dp))
                        Text("Loading job details…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                return@AnimatedContent
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    // 120dp clears the FAB menu (96dp bottom offset + FAB height)
                    .padding(top = 4.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Status hero strip (untouched) ──────────────────────
                val (heroBg, heroContent) = when {
                    job.state.isActive -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                    job.state == JobState.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                    job.state.isTerminal -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                    job.state == JobState.PENDING -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurface
                }
                Surface(
                    color = heroBg,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val stateIcon = when (job.state) {
                                    JobState.RUNNING, JobState.COMPLETING -> Icons.Default.PlayArrow
                                    JobState.PENDING -> Icons.Default.HourglassEmpty
                                    JobState.COMPLETED -> Icons.Default.CheckCircle
                                    JobState.FAILED -> Icons.Default.Error
                                    JobState.CANCELLED -> Icons.Default.Cancel
                                    JobState.SUSPENDED -> Icons.Default.Pause
                                    else -> Icons.Default.Info
                                }
                                Icon(stateIcon, contentDescription = null, tint = heroContent, modifier = Modifier.size(20.dp))
                                Text(job.state.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium, color = heroContent)
                            }
                            if (job.state.isActive && job.startTime != null) {
                                val elapsed = System.currentTimeMillis() - job.startTime
                                Text(formatDuration(elapsed), style = MaterialTheme.typography.displaySmallEmphasized, color = heroContent)
                                if (job.nodelist.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Dns, contentDescription = null, tint = heroContent.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                                        Text(job.nodelist, style = MaterialTheme.typography.bodySmall, color = heroContent.copy(alpha = 0.7f))
                                    }
                                }
                            }
                            if (job.state == JobState.PENDING) {
                                if (job.queuePosition != null) Text("Position ${job.queuePosition} in queue", style = MaterialTheme.typography.titleLargeEmphasized, color = heroContent)
                                job.startTime?.let { Text("Est. start ${formatTimestamp(it)}", style = MaterialTheme.typography.bodySmall, color = heroContent.copy(alpha = 0.7f)) }
                            }
                            if (job.state.isTerminal && job.exitCode != null && job.exitCode != 0) {
                                Text("Exit code ${job.exitCode}", style = MaterialTheme.typography.bodyMedium, color = heroContent.copy(alpha = 0.8f))
                            }
                        }
                        if (job.state.isActive) {
                            Spacer(Modifier.width(16.dp))
                            ContainedLoadingIndicator(modifier = Modifier.size(56.dp), containerColor = heroBg, indicatorColor = heroContent)
                        }
                    }
                }

                // ── Resources card ─────────────────────────────────────
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("Resources", Icons.Default.Memory)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ResourceTile("Nodes", "${job.nodes}", Icons.Default.Dns,
                                MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Modifier.weight(1f))
                            ResourceTile("CPUs", "${job.cpus}", Icons.Default.Memory,
                                MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Modifier.weight(1f))
                            ResourceTile("Memory", formatMemory(job.memoryMb), Icons.Default.Storage,
                                MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ResourceTile("Priority", "${job.priority}", Icons.Default.PriorityHigh,
                                MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
                            ResourceTile("Partition", job.partition, Icons.Default.Category,
                                MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
                            ResourceTile("Exit code", "${job.exitCode ?: "—"}", Icons.Default.Code,
                                if (job.exitCode != null && job.exitCode != 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                if (job.exitCode != null && job.exitCode != 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                Modifier.weight(1f))
                        }
                    }
                }

                // ── Timeline card ──────────────────────────────────────
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("Timeline", Icons.Default.Schedule)
                        Column {
                            TimelineEvent(
                                icon = Icons.Default.Send,
                                label = "Submitted",
                                timestamp = job.submitTime,
                                nodeColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                nodeContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                isFirst = true,
                                isLast = job.startTime == null,
                                connectorDurationMs = job.startTime?.let { it - job.submitTime }
                            )
                            job.startTime?.let { startTime ->
                                TimelineEvent(
                                    icon = Icons.Default.PlayArrow,
                                    label = "Started",
                                    timestamp = startTime,
                                    nodeColor = MaterialTheme.colorScheme.primaryContainer,
                                    nodeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    isFirst = false,
                                    isLast = !(job.state.isTerminal && job.endTime != null),
                                    connectorDurationMs = job.endTime?.let { it - startTime }
                                )
                            }
                            if (job.state.isTerminal && job.endTime != null) {
                                val (nodeColor, nodeContentColor) = when (job.state) {
                                    JobState.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                                    else -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                                }
                                TimelineEvent(
                                    icon = if (job.state == JobState.COMPLETED) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    label = when (job.state) {
                                        JobState.COMPLETED -> "Completed"
                                        JobState.FAILED -> "Failed"
                                        JobState.CANCELLED -> "Cancelled"
                                        else -> "Ended"
                                    },
                                    timestamp = job.endTime,
                                    nodeColor = nodeColor,
                                    nodeContentColor = nodeContentColor,
                                    isFirst = false,
                                    isLast = true,
                                    connectorDurationMs = null
                                )
                            }
                        }
                        if (job.startTime != null) {
                            HorizontalDivider()
                            val endMs = if (job.state.isTerminal) job.endTime ?: System.currentTimeMillis() else System.currentTimeMillis()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Total runtime", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.primaryContainer) {
                                    Text(formatDuration(endMs - job.startTime), style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }
                }

                // ── Work directory card ────────────────────────────────
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("Work Directory", Icons.Default.Folder)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Folder, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp))
                                }
                            }
                            Text(
                                job.workDir,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 5,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // FAB menu — overlaid at bottom-end, above the floating pill nav bar
        job?.let { j ->
            FloatingActionButtonMenu(
                expanded = fabMenuExpanded,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = 96.dp),
                button = {
                    ToggleFloatingActionButton(
                        checked = fabMenuExpanded,
                        onCheckedChange = { fabMenuExpanded = !fabMenuExpanded },
                        modifier = Modifier.semantics {
                            stateDescription = if (fabMenuExpanded) "Expanded" else "Collapsed"
                            contentDescription = "Job actions"
                        }
                    ) {
                        val icon by remember {
                            derivedStateOf {
                                if (checkedProgress > 0.5f) Icons.Default.Close else Icons.Default.MoreVert
                            }
                        }
                        Icon(
                            painter = rememberVectorPainter(icon),
                            contentDescription = null,
                            modifier = Modifier.animateIcon({ checkedProgress })
                        )
                    }
                }
            ) {
                FloatingActionButtonMenuItem(
                    onClick = { fabMenuExpanded = false; viewModel.toggleWatch() },
                    icon = {
                        Icon(
                            if (j.watched) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                            contentDescription = null
                        )
                    },
                    text = { Text(if (j.watched) "Unwatch" else "Watch") }
                )
                if (!j.state.isTerminal) {
                    FloatingActionButtonMenuItem(
                        onClick = { fabMenuExpanded = false; showCancelDialog = true },
                        icon = { Icon(Icons.Default.Cancel, contentDescription = null) },
                        text = { Text("Cancel job") }
                    )
                    FloatingActionButtonMenuItem(
                        onClick = { fabMenuExpanded = false; viewModel.requeueJob() },
                        icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                        text = { Text("Requeue") }
                    )
                }
            }
        }
        } // end Box
    } // end Scaffold content

    // ── Cancel confirmation dialog ─────────────────────────────────────────
    if (showCancelDialog && job != null) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            icon = { Icon(Icons.Default.Cancel, contentDescription = null) },
            title = { Text("Cancel job?") },
            text = {
                Text(
                    "This will send scancel to job #${job.jobId} (${job.name}). The job will be terminated and cannot be resumed.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.cancelJob(); showCancelDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Cancel Job") }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Keep Running") }
            }
        )
    }
}

// ─── Section Header ──────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

// ─── Resource Tile ───────────────────────────────────────────────────────────

@Composable
private fun ResourceTile(
    label: String,
    value: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(shape = MaterialTheme.shapes.extraLarge, color = containerColor, modifier = modifier) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, color = contentColor)
            Text(label, style = MaterialTheme.typography.labelSmall, color = contentColor)
        }
    }
}

// ─── Timeline Event ──────────────────────────────────────────────────────────

@Composable
private fun TimelineEvent(
    icon: ImageVector,
    label: String,
    timestamp: Long,
    nodeColor: Color,
    nodeContentColor: Color,
    isFirst: Boolean,
    isLast: Boolean,
    connectorDurationMs: Long?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        // ── Track column: top connector → node → bottom connector ──────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            // Top connector (leading segment into this node)
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(8.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            } else {
                Spacer(Modifier.height(8.dp))
            }

            // Circular icon node
            Surface(
                shape = CircleShape,
                color = nodeColor,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = nodeContentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Bottom connector + optional duration badge
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(if (connectorDurationMs != null) 12.dp else 28.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                if (connectorDurationMs != null) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Text(
                            formatDuration(connectorDurationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(12.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                }
            }
        }

        // ── Content column ─────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 8.dp, bottom = if (isLast) 0.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                formatTimestamp(timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private val timestampFmt = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
private fun formatTimestamp(ms: Long) = timestampFmt.format(Date(ms))

private fun formatDuration(ms: Long): String {
    val s = ms / 1000; val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return if (h > 0) "%dh %02dm %02ds".format(h, m, sec) else "%dm %02ds".format(m, sec)
}

private fun formatMemory(mb: Long) = if (mb >= 1024) "${mb / 1024}GB" else "${mb}MB"
