package com.tomasps.slrummanager.presentation.servers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomasps.slrummanager.domain.model.Server
import com.tomasps.slrummanager.domain.model.ServerStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen(
    onServerClick: (serverId: String) -> Unit,
    onAddServer: () -> Unit,
    viewModel: ServersViewModel = hiltViewModel()
) {
    val servers by viewModel.servers.collectAsState()
    val scope = rememberCoroutineScope()
    var serverToDelete by remember { mutableStateOf<Server?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Servers") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddServer) {
                Icon(Icons.Default.Add, contentDescription = "Add Server")
            }
        }
    ) { padding ->
        if (servers.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No servers added yet", style = MaterialTheme.typography.titleMedium)
                    Text("Tap + to add your first cluster", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(servers, key = { it.server.id.toString() }) { item ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                serverToDelete = item.server
                            }
                            false // don't auto-dismiss — wait for confirmation
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    ) {
                        ServerCard(item = item, onClick = { onServerClick(item.server.id.toString()) })
                    }
                }
            }
        }
    }

    serverToDelete?.let { server ->
        AlertDialog(
            onDismissRequest = { serverToDelete = null },
            title = { Text("Delete server?") },
            text = { Text("Remove \"${server.name}\" (${server.hostname})? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteServer(server)
                        serverToDelete = null
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { serverToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ServerCard(item: ServerListItem, onClick: () -> Unit) {
    val statusColor = when (item.server.status) {
        ServerStatus.ONLINE -> MaterialTheme.colorScheme.primary
        ServerStatus.UNREACHABLE, ServerStatus.OFFLINE -> MaterialTheme.colorScheme.error
        ServerStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.server.name, style = MaterialTheme.typography.titleMedium)
                    Text(item.server.hostname, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Badge(containerColor = statusColor) {
                    Text(item.server.status.name, modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatLabel("Running", item.runningCount, MaterialTheme.colorScheme.primary)
                StatLabel("Pending", item.pendingCount, MaterialTheme.colorScheme.onSurfaceVariant)
                StatLabel("Failed", item.failedCount, MaterialTheme.colorScheme.error)
            }
            item.server.lastPolledAt?.let { ts ->
                Text("Last polled: ${formatRelativeTime(ts)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StatLabel(label: String, count: Int, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$count", style = MaterialTheme.typography.titleMedium, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
