package com.tomasps.slurmmanager.presentation.servers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.window.core.layout.WindowWidthSizeClass
import com.tomasps.slurmmanager.domain.model.Server
import com.tomasps.slurmmanager.domain.model.ServerStatus

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ServersScreen(
    onServerClick: (serverId: String) -> Unit,
    onAddServer: () -> Unit,
    showAddServer: Boolean = false,
    viewModel: ServersViewModel = hiltViewModel()
) {
    val servers by viewModel.servers.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var serverToDelete by remember { mutableStateOf<Server?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isCompact = windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Servers") },
                subtitle = if (servers.isNotEmpty()) ({
                    val online = servers.count { it.server.status == ServerStatus.ONLINE }
                    val unreachable = servers.count {
                        it.server.status == ServerStatus.UNREACHABLE || it.server.status == ServerStatus.OFFLINE
                    }
                    Text(buildString {
                        append("${servers.size} cluster${if (servers.size != 1) "s" else ""}")
                        if (online > 0) append(" · $online online")
                        if (unreachable > 0) append(" · $unreachable unreachable")
                    })
                }) else null,
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        if (servers.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(88.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                    Text(
                        "No clusters yet",
                        style = MaterialTheme.typography.headlineSmallEmphasized,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Connect to your first HPC cluster via SSH to start monitoring jobs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = onAddServer) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add your first cluster")
                    }
                }
            }
        } else if (isCompact) {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(servers, key = { it.server.id.toString() }) { item ->
                    SwipeToDismissServerCard(
                        item = item,
                        onDelete = { serverToDelete = item.server },
                        onClick = { onServerClick(item.server.id.toString()) }
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(servers, key = { it.server.id.toString() }) { item ->
                    SwipeToDismissServerCard(
                        item = item,
                        onDelete = { serverToDelete = item.server },
                        onClick = { onServerClick(item.server.id.toString()) }
                    )
                }
            }
        }
    }

    serverToDelete?.let { server ->
        AlertDialog(
            onDismissRequest = { serverToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Remove server?") },
            text = {
                Text(
                    "\"${server.name}\" (${server.hostname}) will be removed and polling will stop. This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteServer(server); serverToDelete = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { serverToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SwipeToDismissServerCard(
    item: ServerListItem,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onDelete()
            false
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier.fillMaxSize().padding(4.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxHeight().aspectRatio(1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
    ) {
        ServerCard(item = item, onClick = onClick)
    }
}

@Composable
private fun ServerCard(item: ServerListItem, onClick: () -> Unit) {
    val isUnreachable = item.server.status == ServerStatus.UNREACHABLE || item.server.status == ServerStatus.OFFLINE
    val (statusContainer, statusContent) = when (item.server.status) {
        ServerStatus.ONLINE -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        ServerStatus.UNREACHABLE, ServerStatus.OFFLINE -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        ServerStatus.UNKNOWN -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
    }

    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        // Inline error strip
        if (isUnreachable) {
            Surface(color = MaterialTheme.colorScheme.errorContainer) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.WifiOff, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
                    Text(
                        "Cannot reach ${item.server.hostname}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header: name + hostname + status badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.server.name, style = MaterialTheme.typography.titleMediumEmphasized)
                    Text(
                        "${item.server.username}@${item.server.hostname}:${item.server.port}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = statusContainer
                ) {
                    Text(
                        item.server.status.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusContent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Job stat tiles
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ServerStatTile(
                    label = "Running",
                    count = item.runningCount,
                    containerColor = if (item.runningCount > 0) MaterialTheme.colorScheme.primaryContainer
                                     else MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = if (item.runningCount > 0) MaterialTheme.colorScheme.onPrimaryContainer
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                ServerStatTile(
                    label = "Pending",
                    count = item.pendingCount,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                ServerStatTile(
                    label = "Failed",
                    count = item.failedCount,
                    containerColor = if (item.failedCount > 0) MaterialTheme.colorScheme.errorContainer
                                     else MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = if (item.failedCount > 0) MaterialTheme.colorScheme.onErrorContainer
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }

            // Last polled
            item.server.lastPolledAt?.let { ts ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Last polled ${formatRelativeTime(ts)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ServerStatTile(
    label: String,
    count: Int,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(shape = MaterialTheme.shapes.extraLarge, color = containerColor, modifier = modifier) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text("$count", style = MaterialTheme.typography.titleMediumEmphasized, color = contentColor)
            Text(label, style = MaterialTheme.typography.labelSmall, color = contentColor)
        }
    }
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
