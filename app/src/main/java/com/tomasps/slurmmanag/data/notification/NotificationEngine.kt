package com.tomasps.slurmmanag.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tomasps.slurmmanag.R
import com.tomasps.slurmmanag.domain.model.AlertEvent
import com.tomasps.slurmmanag.domain.model.AlertType
import com.tomasps.slurmmanag.domain.model.Job
import com.tomasps.slurmmanag.domain.model.JobState
import com.tomasps.slurmmanag.presentation.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_JOB_ALERTS = "job_alerts"
        const val CHANNEL_CLUSTER_EVENTS = "cluster_events"
        const val CHANNEL_LIVE_UPDATES = "live_updates"
    }

    init { createChannels() }

    private fun createChannels() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_JOB_ALERTS, "Job Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Threshold alerts: runtime exceeded, long queue wait, node failures"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_CLUSTER_EVENTS, "Cluster Events", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "SSH connection failures and cluster-level events"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_LIVE_UPDATES, "Watched Jobs", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Live progress for jobs you are watching"
            }
        )
    }

    fun cancelLiveNotification(jobId: String) {
        NotificationManagerCompat.from(context).cancel(("live_$jobId").hashCode())
    }

    fun notifyWatchedJobTerminal(job: Job, serverName: String) {
        val notifId = ("watched_done_${job.jobId}").hashCode()
        val pi = mainActivityIntent(notifId, job.jobId, job.serverId.toString())

        val (icon, title, bigText) = when (job.state) {
            JobState.COMPLETED -> Triple(
                R.drawable.ic_job_completed,
                "Job Completed",
                "${job.name} finished successfully on $serverName.\nJob #${job.jobId}"
            )
            JobState.FAILED -> Triple(
                R.drawable.ic_job_failed,
                "Job Failed",
                "${job.name} exited with an error on $serverName.\nJob #${job.jobId} — tap to view details."
            )
            JobState.CANCELLED -> Triple(
                R.drawable.ic_job_failed,
                "Job Cancelled",
                "${job.name} was cancelled on $serverName.\nJob #${job.jobId}"
            )
            else -> Triple(
                R.drawable.ic_job_running,
                "${job.state.name.lowercase().replaceFirstChar { it.uppercase() }}: ${job.name}",
                "${job.name} ended on $serverName with state ${job.state.name}."
            )
        }

        val notif = NotificationCompat.Builder(context, CHANNEL_LIVE_UPDATES)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(job.name)
            .setSubText(serverName)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .addAction(0, "View Details", pi)
            .build()

        runCatching { NotificationManagerCompat.from(context).notify(notifId, notif) }
    }

    fun notifyAlert(event: AlertEvent) {
        val notifId = ("alert_${event.job?.jobId}_${event.alertType}").hashCode()

        val (icon, title, bigText, priority) = when (event.alertType) {
            AlertType.RUNTIME_EXCEEDED -> AlertParams(
                icon = R.drawable.ic_alert,
                title = "Runtime exceeded — ${event.job?.name ?: "job"}",
                bigText = buildString {
                    appendLine("${event.serverName}: Job #${event.job?.jobId} has been running longer than the configured threshold.")
                    appendLine()
                    append(event.message)
                },
                priority = NotificationCompat.PRIORITY_HIGH
            )
            AlertType.QUEUE_WAIT_EXCEEDED -> AlertParams(
                icon = R.drawable.ic_alert,
                title = "Long queue wait — ${event.job?.name ?: "job"}",
                bigText = buildString {
                    appendLine("${event.serverName}: Job #${event.job?.jobId} has been pending longer than expected.")
                    appendLine()
                    append(event.message)
                },
                priority = NotificationCompat.PRIORITY_DEFAULT
            )
            AlertType.NODE_FAILURE -> AlertParams(
                icon = R.drawable.ic_cluster_event,
                title = "Node failure on ${event.serverName}",
                bigText = buildString {
                    appendLine("${event.serverName}: A compute node has failed or gone offline.")
                    appendLine()
                    append(event.message)
                },
                priority = NotificationCompat.PRIORITY_HIGH
            )
            AlertType.PARTITION_DOWN -> AlertParams(
                icon = R.drawable.ic_cluster_event,
                title = "Partition down on ${event.serverName}",
                bigText = buildString {
                    appendLine("${event.serverName}: A partition is unavailable. Pending jobs may be stuck.")
                    appendLine()
                    append(event.message)
                },
                priority = NotificationCompat.PRIORITY_HIGH
            )
        }

        val pi = event.job?.let { job ->
            mainActivityIntent(notifId, job.jobId, event.serverId.toString())
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_JOB_ALERTS)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(event.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(priority)
            .setAutoCancel(true)

        pi?.let {
            builder.setContentIntent(it)
            builder.addAction(0, "View Job", it)
        }

        runCatching { NotificationManagerCompat.from(context).notify(notifId, builder.build()) }
    }

    fun notifyClusterEvent(serverId: String, serverName: String, message: String) {
        val notifId = ("cluster_$serverId").hashCode()
        val notif = NotificationCompat.Builder(context, CHANNEL_CLUSTER_EVENTS)
            .setSmallIcon(R.drawable.ic_cluster_event)
            .setContentTitle("Cannot reach $serverName")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "$serverName: SSH connection failed.\n\n$message\n\nOpen the app to update credentials or check your network."
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(notifId, notif) }
    }

    fun postLiveUpdateNotification(jobId: String, jobName: String, state: JobState) {
        val notifId = ("live_$jobId").hashCode()
        val pi = mainActivityIntent(notifId, jobId, null)

        val (icon, stateLabel, color) = when (state) {
            JobState.RUNNING -> Triple(
                R.drawable.ic_job_running,
                "Running",
                android.graphics.Color.parseColor("#2E7D32")
            )
            JobState.COMPLETING -> Triple(
                R.drawable.ic_job_running,
                "Completing",
                android.graphics.Color.parseColor("#558B2F")
            )
            JobState.PENDING -> Triple(
                R.drawable.ic_alert,
                "Queued",
                android.graphics.Color.parseColor("#E65100")
            )
            JobState.SUSPENDED -> Triple(
                R.drawable.ic_alert,
                "Suspended",
                android.graphics.Color.parseColor("#616161")
            )
            JobState.FAILED -> Triple(
                R.drawable.ic_job_failed,
                "Failed",
                android.graphics.Color.parseColor("#B71C1C")
            )
            JobState.COMPLETED -> Triple(
                R.drawable.ic_job_completed,
                "Completed",
                android.graphics.Color.parseColor("#1B5E20")
            )
            JobState.CANCELLED -> Triple(
                R.drawable.ic_job_failed,
                "Cancelled",
                android.graphics.Color.parseColor("#37474F")
            )
            JobState.TIMEOUT -> Triple(
                R.drawable.ic_alert,
                "Timeout",
                android.graphics.Color.parseColor("#BF360C")
            )
            else -> Triple(
                R.drawable.ic_job_running,
                state.name.lowercase().replaceFirstChar { it.uppercase() },
                android.graphics.Color.parseColor("#455A64")
            )
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_LIVE_UPDATES)
            .setSmallIcon(icon)
            .setColor(color)
            .setContentTitle(jobName)
            .setContentText(stateLabel)
            .setSubText("#$jobId")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pi)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            builder.setRequestPromotedOngoing(true)
        }

        runCatching { NotificationManagerCompat.from(context).notify(notifId, builder.build()) }
    }

    private fun mainActivityIntent(requestCode: Int, jobId: String, serverId: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("jobId", jobId)
            serverId?.let { putExtra("serverId", it) }
        }
        return PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private data class AlertParams(val icon: Int, val title: String, val bigText: String, val priority: Int)
}
