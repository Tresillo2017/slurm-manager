package com.tomasps.slurmmanag.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/**
 * Consistent title line shown at the top of every medium/large widget layout.
 * Keeps visual hierarchy uniform across all four widgets.
 */
@Composable
fun WidgetHeader(title: String) {
    Text(
        text = title,
        style = TextStyle(
            color = GlanceTheme.colors.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        ),
        modifier = GlanceModifier.padding(bottom = 10.dp),
    )
}

/**
 * Centered placeholder shown when the widget has no data.
 * Fills the remaining space so the widget doesn't look broken.
 */
@Composable
fun EmptyState(message: String) {
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
        )
    }
}

/**
 * Pill-shaped chip coloured by job state.
 * Low-alpha fill so the chip works on both light and dark surfaceVariant rows.
 */
@Composable
fun JobStateChip(label: String, color: Color) {
    Box(
        modifier = GlanceModifier
            .background(ColorProvider(color.copy(alpha = 0.18f)))
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .cornerRadius(WidgetShapes.Full),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = ColorProvider(color),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}
