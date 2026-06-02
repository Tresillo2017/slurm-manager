package com.tomasps.slrummanager.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tomasps.slrummanager.R
import com.tomasps.slrummanager.domain.model.AlertEvent
import com.tomasps.slrummanager.domain.model.JobState
import com.tomasps.slrummanager.domain.model.StateChangeEvent
import com.tomasps.slrummanager.presentation.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_JOB_STATE = "job_state_changes"
        const val CHANNEL_JOB_ALERTS = "job_alerts"
        const val CHANNEL_CLUSTER_EVENTS = "cluster_events"
        const val CHANNEL_LIVE_UPDATES = "live_updates"
    }

    init {
        createChannels()
    }

    private fun createChannels() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_JOB_STATE, "Job State Changes", NotificationManager.IMPORTANCE_HIGH)
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_JOB_ALERTS, "Job Alerts", NotificationManager.IMPORTANCE_DEFAULT)
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_CLUSTER_EVENTS, "Cluster Events", NotificationManager.IMPORTANCE_HIGH)
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_LIVE_UPDATES, "Running Jobs", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Live progress for running SLURM jobs"
            }
        )
    }

    fun notifyStateChange(event: StateChangeEvent) {
        val title = "Job ${event.job.name}: ${event.newState.name}"
        val text = "${event.serverName} — was ${event.previousState.name}"
        val notifId = event.job.jobId.hashCode()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("jobId", event.job.jobId)
            putExtra("serverId", event.serverId.toString())
        }
        val pi = PendingIntent.getActivity(
            context, notifId, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_JOB_STATE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pi)

        if (event.newState.isTerminal) {
            builder.addAction(0, "View Details", pi)
        }

        runCatching { NotificationManagerCompat.from(context).notify(notifId, builder.build()) }
    }

    fun notifyAlert(event: AlertEvent) {
        val notifId = ("alert_${event.job?.jobId}_${event.alertType}").hashCode()
        val builder = NotificationCompat.Builder(context, CHANNEL_JOB_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Alert: ${event.alertType.name.replace('_', ' ')}")
            .setContentText(event.message)
            .setAutoCancel(true)

        runCatching { NotificationManagerCompat.from(context).notify(notifId, builder.build()) }
    }

    fun notifyClusterEvent(serverId: String, serverName: String, message: String) {
        val notifId = ("cluster_$serverId").hashCode()
        val builder = NotificationCompat.Builder(context, CHANNEL_CLUSTER_EVENTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Cluster Event: $serverName")
            .setContentText(message)
            .setAutoCancel(true)

        runCatching { NotificationManagerCompat.from(context).notify(notifId, builder.build()) }
    }

    fun postLiveUpdateNotification(jobId: String, jobName: String, elapsedMs: Long, state: JobState) {
        val notifId = ("live_$jobId").hashCode()

        if (state.isTerminal) {
            NotificationManagerCompat.from(context).cancel(notifId)
            return
        }

        val elapsed = formatElapsed(elapsedMs)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("jobId", jobId)
        }
        val pi = PendingIntent.getActivity(
            context, notifId, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Live Update notification — ongoing pill in the notification shade, updated each poll.
        // Uses NotificationCompat for compatibility across all API levels.
        // When Notification.LiveUpdateExtras becomes stable in a future SDK release,
        // this can be enhanced with setLiveUpdateExtras() on API 36+.
        val isCompleting = state == JobState.COMPLETING
        val builder = NotificationCompat.Builder(context, CHANNEL_LIVE_UPDATES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (isCompleting) "Completing: $jobName" else "Running: $jobName")
            .setContentText("Elapsed: $elapsed")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(0, 0, !isCompleting)
            .setContentIntent(pi)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        runCatching { NotificationManagerCompat.from(context).notify(notifId, builder.build()) }
    }

    private fun formatElapsed(ms: Long): String {
        val s = ms / 1000
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) "%dh %02dm %02ds".format(h, m, sec) else "%dm %02ds".format(m, sec)
    }
}
