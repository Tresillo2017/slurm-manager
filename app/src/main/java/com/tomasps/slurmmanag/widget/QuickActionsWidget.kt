package com.tomasps.slurmmanag.widget

// Purpose: one-tap shortcuts from the home screen — refresh all servers, open dashboard.
// Compact (2×1): two icon buttons side-by-side with a small "Slurm" label below each.
// Medium  (2×2): header "Quick Actions" + larger buttons with descriptive labels.

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.provideContent
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
import com.tomasps.slurmmanag.R
import com.tomasps.slurmmanag.presentation.MainActivity

class QuickActionsWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(SMALL_SIZE, MEDIUM_SIZE)
    )

    companion object {
        val SMALL_SIZE  = DpSize(50.dp,  50.dp)
        val MEDIUM_SIZE = DpSize(180.dp, 120.dp)
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            SlurmGlanceTheme {
                QuickActionsContent(context)
            }
        }
    }
}

@Composable
private fun QuickActionsContent(context: Context) {
    val refreshAction = actionSendBroadcast(
        Intent(WidgetRefreshReceiver.ACTION_REFRESH_ALL).apply {
            component = ComponentName(context, WidgetRefreshReceiver::class.java)
        }
    )
    val dashboardAction = actionStartActivity(Intent(context, MainActivity::class.java))

    val size = LocalSize.current
    // Show the "Quick Actions" title only when there is enough vertical room.
    val isExpanded = size.height >= QuickActionsWidget.MEDIUM_SIZE.height

    Scaffold(horizontalPadding = 16.dp) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isExpanded) {
                WidgetHeader("Quick Actions")
            }
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ActionButton(
                    icon = R.drawable.ic_job_running,
                    description = "Refresh all servers",
                    label = if (isExpanded) "Refresh" else null,
                    background = GlanceTheme.colors.primaryContainer,
                    tint = GlanceTheme.colors.onPrimaryContainer,
                    action = refreshAction,
                )
                Spacer(GlanceModifier.width(24.dp))
                ActionButton(
                    icon = R.drawable.ic_notification,
                    description = "Open dashboard",
                    label = if (isExpanded) "Dashboard" else null,
                    background = GlanceTheme.colors.secondaryContainer,
                    tint = GlanceTheme.colors.onSecondaryContainer,
                    action = dashboardAction,
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: Int,
    description: String,
    label: String?,
    background: androidx.glance.unit.ColorProvider,
    tint: androidx.glance.unit.ColorProvider,
    action: androidx.glance.action.Action,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircleIconButton(
            imageProvider = ImageProvider(icon),
            contentDescription = description,
            onClick = action,
            backgroundColor = background,
            contentColor = tint,
        )
        // Label below button is only shown in expanded mode for legibility.
        if (label != null) {
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = label,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}

class QuickActionsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickActionsWidget()
}
