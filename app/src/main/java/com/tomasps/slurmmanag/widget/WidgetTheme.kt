package com.tomasps.slurmmanag.widget

import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.glance.LocalContext
import androidx.glance.GlanceComposable
import androidx.compose.runtime.Composable
import androidx.glance.GlanceTheme
import androidx.glance.material3.ColorProviders
import com.tomasps.slurmmanag.domain.model.JobState

private val SeedPurple = Color(0xFF6750A4)
private val FallbackLight = lightColorScheme(primary = SeedPurple)
private val FallbackDark  = darkColorScheme(primary = Color(0xFFD0BCFF))

@GlanceComposable
@Composable
fun SlurmGlanceTheme(content: @GlanceComposable @Composable () -> Unit) {
    val colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val ctx = LocalContext.current
        ColorProviders(
            light = dynamicLightColorScheme(ctx),
            dark  = dynamicDarkColorScheme(ctx)
        )
    } else {
        ColorProviders(light = FallbackLight, dark = FallbackDark)
    }
    GlanceTheme(colors = colors, content = content)
}

/** Returns a semantic colour for each job state, suitable for use in widget chips. */
fun jobStateColor(state: JobState): Color = when (state) {
    JobState.RUNNING    -> Color(0xFF2E7D32)
    JobState.COMPLETING -> Color(0xFF558B2F)
    JobState.PENDING    -> Color(0xFFE65100)
    JobState.SUSPENDED  -> Color(0xFF616161)
    JobState.FAILED     -> Color(0xFFB71C1C)
    JobState.COMPLETED  -> Color(0xFF1B5E20)
    JobState.CANCELLED  -> Color(0xFF37474F)
    JobState.TIMEOUT    -> Color(0xFFBF360C)
    JobState.NODE_FAIL  -> Color(0xFF880E4F)
    JobState.PREEMPTED  -> Color(0xFF4A148C)
    JobState.UNKNOWN    -> Color(0xFF455A64)
}

fun jobStateLabel(state: JobState): String = when (state) {
    JobState.RUNNING    -> "Running"
    JobState.COMPLETING -> "Completing"
    JobState.PENDING    -> "Queued"
    JobState.SUSPENDED  -> "Suspended"
    JobState.FAILED     -> "Failed"
    JobState.COMPLETED  -> "Done"
    JobState.CANCELLED  -> "Cancelled"
    JobState.TIMEOUT    -> "Timeout"
    JobState.NODE_FAIL  -> "Node Fail"
    JobState.PREEMPTED  -> "Preempted"
    JobState.UNKNOWN    -> "Unknown"
}
