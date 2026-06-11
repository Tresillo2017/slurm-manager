package com.tomasps.slurmmanag.widget

// Purpose: shows job counts (running / queued / failed / done) across all servers.
// Compact (2×1): four pill badges in a row — number + one-word label.
// Medium (2×2): header + four equal cards each with a large count and state label.
// Large  (4×2): same card grid but with an extra "total" summary line at the bottom.

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
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.tomasps.slurmmanag.domain.model.JobState
import com.tomasps.slurmmanag.presentation.MainActivity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

class SlurmWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        // Three breakpoints so Glance always finds one that fits.
        // 50×50 covers any 1-cell placement; 180×120 = 2×2; 250×120 = wider/large.
        setOf(SMALL_SIZE, MEDIUM_SIZE, LARGE_SIZE)
    )

    companion object {
        val SMALL_SIZE  = DpSize(50.dp,  50.dp)
        val MEDIUM_SIZE = DpSize(180.dp, 120.dp)
        val LARGE_SIZE  = DpSize(250.dp, 120.dp)
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = EntryPointAccessors
            .fromApplication(context, SlurmWidgetEntryPoint::class.java)
            .jobRepository()

        val jobs      = repo.observeAll().first()
        val running   = jobs.count { it.state == JobState.RUNNING || it.state == JobState.COMPLETING }
        val pending   = jobs.count { it.state == JobState.PENDING }
        val failed    = jobs.count { it.state == JobState.FAILED || it.state == JobState.CANCELLED || it.state == JobState.TIMEOUT }
        val completed = jobs.count { it.state == JobState.COMPLETED }
        val total     = jobs.size

        provideContent {
            SlurmGlanceTheme {
                val size = LocalSize.current
                val launch = actionStartActivity(Intent(context, MainActivity::class.java))
                when {
                    size.width < MEDIUM_SIZE.width -> CompactLayout(running, pending, failed, completed, launch)
                    else                           -> ExpandedLayout(running, pending, failed, completed, total, launch)
                }
            }
        }
    }
}

// Compact: four pill badges in a horizontal row — most glanceable at a glance.
@Composable
private fun CompactLayout(
    running: Int, pending: Int, failed: Int, completed: Int,
    launch: androidx.glance.action.Action,
) {
    Scaffold(
        modifier = GlanceModifier.clickable(launch),
        horizontalPadding = 16.dp,
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CountPill(running,   "Run",   jobStateColor(JobState.RUNNING))
            Spacer(GlanceModifier.width(6.dp))
            CountPill(pending,   "Queue", jobStateColor(JobState.PENDING))
            Spacer(GlanceModifier.width(6.dp))
            CountPill(failed,    "Fail",  jobStateColor(JobState.FAILED))
            Spacer(GlanceModifier.width(6.dp))
            CountPill(completed, "Done",  jobStateColor(JobState.COMPLETED))
        }
    }
}

// Expanded: header + four equal-weight cards. Total line only when there are jobs.
@Composable
private fun ExpandedLayout(
    running: Int, pending: Int, failed: Int, completed: Int, total: Int,
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WidgetHeader("Job Counts")
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                // defaultWeight() distributes remaining width equally across the four cards.
                CountCard(GlanceModifier.defaultWeight().fillMaxHeight(), running,   "Running", jobStateColor(JobState.RUNNING))
                Spacer(GlanceModifier.width(8.dp))
                CountCard(GlanceModifier.defaultWeight().fillMaxHeight(), pending,   "Queued",  jobStateColor(JobState.PENDING))
                Spacer(GlanceModifier.width(8.dp))
                CountCard(GlanceModifier.defaultWeight().fillMaxHeight(), failed,    "Failed",  jobStateColor(JobState.FAILED))
                Spacer(GlanceModifier.width(8.dp))
                CountCard(GlanceModifier.defaultWeight().fillMaxHeight(), completed, "Done",    jobStateColor(JobState.COMPLETED))
            }
            if (total > 0) {
                Spacer(GlanceModifier.height(8.dp))
                Text(
                    text = "$total jobs total",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp),
                )
            }
        }
    }
}

@Composable
private fun CountPill(count: Int, label: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = GlanceModifier
            .background(ColorProvider(color.copy(alpha = 0.15f)))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .cornerRadius(WidgetShapes.Full),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = count.toString(),
                style = TextStyle(color = ColorProvider(color), fontSize = 16.sp, fontWeight = FontWeight.Bold),
            )
            Text(
                text = label,
                style = TextStyle(color = ColorProvider(color.copy(alpha = 0.8f)), fontSize = 9.sp),
            )
        }
    }
}

@Composable
private fun CountCard(modifier: GlanceModifier, count: Int, label: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = modifier
            .background(ColorProvider(color.copy(alpha = 0.12f)))
            .padding(vertical = 10.dp, horizontal = 6.dp)
            .cornerRadius(WidgetShapes.ExtraLarge),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = count.toString(),
                style = TextStyle(color = ColorProvider(color), fontSize = 26.sp, fontWeight = FontWeight.Bold),
            )
            Text(
                text = label,
                style = TextStyle(color = ColorProvider(color.copy(alpha = 0.8f)), fontSize = 10.sp),
            )
        }
    }
}

class SlurmWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SlurmWidget()
}
