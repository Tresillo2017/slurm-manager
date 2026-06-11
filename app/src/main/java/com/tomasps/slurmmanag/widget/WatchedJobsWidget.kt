package com.tomasps.slurmmanag.widget

// Purpose: lists jobs the user is watching with their live state and runtime.
// Medium (3×2): header + up to 3 job rows — name, server, state chip, runtime.
// Large  (3×4): same but up to 6 rows — full watch list visible without scrolling.

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.tomasps.slurmmanag.domain.model.Job
import com.tomasps.slurmmanag.presentation.MainActivity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.util.UUID

class WatchedJobsWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(MEDIUM_SIZE, LARGE_SIZE)
    )

    companion object {
        private val MEDIUM_SIZE = DpSize(180.dp, 120.dp)
        private val LARGE_SIZE  = DpSize(180.dp, 280.dp)
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val ep          = EntryPointAccessors.fromApplication(context, SlurmWidgetEntryPoint::class.java)
        val watched     = ep.jobRepository().observeWatched().first()
        val serverNames = ep.serverRepository().observeAll().first().associate { it.id to it.name }

        provideContent {
            SlurmGlanceTheme {
                val launch  = actionStartActivity(Intent(context, MainActivity::class.java))
                val size    = LocalSize.current
                // Large layout shows up to 6 rows; medium is capped at 3 to avoid overflow.
                val maxRows = if (size.height >= LARGE_SIZE.height) 6 else 3
                WatchedLayout(watched, serverNames, maxRows, launch)
            }
        }
    }
}

@Composable
private fun WatchedLayout(
    jobs: List<Job>,
    serverNames: Map<UUID, String>,
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
            WidgetHeader("Watched Jobs")
            if (jobs.isEmpty()) {
                EmptyState("No watched jobs")
            } else {
                jobs.take(maxRows).forEach { job ->
                    JobRow(job, serverNames[job.serverId] ?: "Unknown")
                    Spacer(GlanceModifier.height(6.dp))
                }
                if (jobs.size > maxRows) {
                    Text(
                        text = "+${jobs.size - maxRows} more",
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp),
                        modifier = GlanceModifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun JobRow(job: Job, serverName: String) {
    val color = jobStateColor(job.state)
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .cornerRadius(WidgetShapes.Large),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left: job name + server sub-label.
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = job.name,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
            )
            Text(
                text = serverName,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp),
            )
        }
        Spacer(GlanceModifier.width(6.dp))
        // Right: state chip stacked above runtime — most important info closest to the edge.
        Column(horizontalAlignment = Alignment.End) {
            JobStateChip(label = jobStateLabel(job.state), color = color)
            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = runtimeText(job),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp),
            )
        }
    }
}

private fun runtimeText(job: Job): String {
    val startMs  = job.startTime ?: return "Waiting"
    val endMs    = job.endTime ?: System.currentTimeMillis()
    val diffMins = (endMs - startMs) / 60_000
    return when {
        diffMins < 1  -> "<1 min"
        diffMins < 60 -> "${diffMins}m"
        else          -> "${diffMins / 60}h ${diffMins % 60}m"
    }
}

class WatchedJobsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WatchedJobsWidget()
}
