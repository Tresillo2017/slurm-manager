package com.tomasps.slurmmanag.widget

// Purpose: shows SLURM server health at a glance.
// Compact (2×1): dot row + "X / Y online" summary — answers "are my servers up?" instantly.
// Medium  (2×2): header + up to 4 server rows, each with name, last-polled time, status badge.
// Large   (4×2): same as medium but shows up to 6 rows.

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.tomasps.slurmmanag.domain.model.Server
import com.tomasps.slurmmanag.domain.model.ServerStatus
import com.tomasps.slurmmanag.presentation.MainActivity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.util.UUID

class ServerStatusWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Responsive(
        setOf(SMALL_SIZE, MEDIUM_SIZE, LARGE_SIZE)
    )

    companion object {
        val SMALL_SIZE  = DpSize(50.dp,  50.dp)
        val MEDIUM_SIZE = DpSize(180.dp, 120.dp)
        val LARGE_SIZE  = DpSize(180.dp, 250.dp)
        val KEY_SERVER_ID = stringPreferencesKey("server_id")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val ep = EntryPointAccessors.fromApplication(context, SlurmWidgetEntryPoint::class.java)
        val allServers = ep.serverRepository().observeAll().first()

        provideContent {
            SlurmGlanceTheme {
                val prefs    = currentState<androidx.datastore.preferences.core.Preferences>()
                val filterId = prefs[KEY_SERVER_ID]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                val servers  = if (filterId != null) allServers.filter { it.id == filterId } else allServers

                val launch = actionStartActivity(Intent(context, MainActivity::class.java))
                val size   = LocalSize.current

                when {
                    size.width < MEDIUM_SIZE.width -> CompactLayout(servers, launch)
                    size.height >= LARGE_SIZE.height -> LargeLayout(servers, launch)
                    else -> MediumLayout(servers, launch)
                }
            }
        }
    }
}

// Compact: dot row + "X / Y online" — one line answers the key question without listing every server.
@Composable
private fun CompactLayout(servers: List<Server>, launch: androidx.glance.action.Action) {
    val online = servers.count { it.status == ServerStatus.ONLINE }
    Scaffold(
        modifier = GlanceModifier.clickable(launch),
        horizontalPadding = 16.dp,
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Dot row: one coloured dot per server, visually scannable.
            servers.take(6).forEach { server ->
                StatusDot(status = server.status, size = 10.dp)
                Spacer(GlanceModifier.width(5.dp))
            }
            Spacer(GlanceModifier.width(6.dp))
            Column {
                Text(
                    text = "$online / ${servers.size} online",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                Text(
                    text = "Server Status",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp),
                )
            }
        }
    }
}

@Composable
private fun MediumLayout(servers: List<Server>, launch: androidx.glance.action.Action) {
    ServerListLayout(servers, maxRows = 3, launch)
}

@Composable
private fun LargeLayout(servers: List<Server>, launch: androidx.glance.action.Action) {
    ServerListLayout(servers, maxRows = 6, launch)
}

@Composable
private fun ServerListLayout(
    servers: List<Server>,
    maxRows: Int,
    launch: androidx.glance.action.Action,
) {
    Scaffold(
        modifier = GlanceModifier.clickable(launch),
        horizontalPadding = 16.dp,
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
        ) {
            WidgetHeader("Server Status")
            if (servers.isEmpty()) {
                EmptyState("No servers configured")
            } else {
                servers.take(maxRows).forEach { server ->
                    ServerRow(server)
                    Spacer(GlanceModifier.height(6.dp))
                }
                if (servers.size > maxRows) {
                    Text(
                        text = "+${servers.size - maxRows} more",
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ServerRow(server: Server) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .cornerRadius(WidgetShapes.Large),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(server.status, size = 9.dp)
        Spacer(GlanceModifier.width(8.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = server.name,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
            )
            Text(
                text = lastPolledText(server.lastPolledAt),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp),
            )
        }
        StatusBadge(server.status)
    }
}

@Composable
private fun StatusDot(status: ServerStatus, size: Dp) {
    // Circle uses Full cornerRadius; cornerRadius(size/2) would be Dp/Dp = Float not Dp,
    // so we use WidgetShapes.Full (9999.dp) which always produces a circle.
    val color = when (status) {
        ServerStatus.ONLINE      -> GlanceTheme.colors.primary
        ServerStatus.OFFLINE     -> GlanceTheme.colors.onSurfaceVariant
        ServerStatus.UNREACHABLE -> GlanceTheme.colors.error
        ServerStatus.UNKNOWN     -> GlanceTheme.colors.outline
    }
    Box(
        modifier = GlanceModifier
            .size(size)
            .background(color)
            .cornerRadius(WidgetShapes.Full),
    ) {}
}

@Composable
private fun StatusBadge(status: ServerStatus) {
    // M3 semantic container tokens — no hardcoded colours, adapts to dynamic colour scheme.
    val (label, container, content) = when (status) {
        ServerStatus.ONLINE      -> Triple("Online",      GlanceTheme.colors.primaryContainer,  GlanceTheme.colors.onPrimaryContainer)
        ServerStatus.OFFLINE     -> Triple("Offline",     GlanceTheme.colors.surfaceVariant,     GlanceTheme.colors.onSurfaceVariant)
        ServerStatus.UNREACHABLE -> Triple("Unreachable", GlanceTheme.colors.errorContainer,     GlanceTheme.colors.onErrorContainer)
        ServerStatus.UNKNOWN     -> Triple("Unknown",     GlanceTheme.colors.secondaryContainer, GlanceTheme.colors.onSecondaryContainer)
    }
    Box(
        modifier = GlanceModifier
            .background(container)
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .cornerRadius(WidgetShapes.Full),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = TextStyle(color = content, fontSize = 10.sp, fontWeight = FontWeight.Medium),
        )
    }
}

private fun lastPolledText(lastPolledAt: Long?): String {
    if (lastPolledAt == null) return "Never polled"
    val mins = (System.currentTimeMillis() - lastPolledAt) / 60_000
    return when {
        mins < 1  -> "Just now"
        mins < 60 -> "${mins}m ago"
        else      -> "${mins / 60}h ${mins % 60}m ago"
    }
}

class ServerStatusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ServerStatusWidget()
}
