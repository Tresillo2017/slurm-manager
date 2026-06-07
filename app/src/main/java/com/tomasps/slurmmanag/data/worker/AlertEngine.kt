package com.tomasps.slurmmanag.data.worker

import com.tomasps.slurmmanag.domain.model.AlertEvent
import com.tomasps.slurmmanag.domain.model.AlertType
import com.tomasps.slurmmanag.domain.model.Job
import com.tomasps.slurmmanag.domain.model.JobState
import com.tomasps.slurmmanag.domain.model.Server

object AlertEngine {

    fun evaluate(jobs: List<Job>, server: Server, nowMs: Long): List<AlertEvent> {
        val events = mutableListOf<AlertEvent>()
        for (rule in server.alertRules) {
            when (rule.type) {
                AlertType.QUEUE_WAIT_EXCEEDED -> {
                    val threshold = (rule.thresholdMinutes ?: 30) * 60_000L
                    jobs.filter { it.state == JobState.PENDING }
                        .filter { nowMs - it.submitTime > threshold }
                        .forEach { job ->
                            events += AlertEvent(
                                job = job,
                                alertType = AlertType.QUEUE_WAIT_EXCEEDED,
                                serverId = server.id,
                                serverName = server.name,
                                message = "Job ${job.name} has been queued for over ${rule.thresholdMinutes ?: 30} minutes"
                            )
                        }
                }
                AlertType.RUNTIME_EXCEEDED -> {
                    val threshold = (rule.thresholdMinutes ?: 60) * 60_000L
                    jobs.filter { it.state.isActive && it.startTime != null }
                        .filter { nowMs - (it.startTime ?: 0L) > threshold }
                        .forEach { job ->
                            events += AlertEvent(
                                job = job,
                                alertType = AlertType.RUNTIME_EXCEEDED,
                                serverId = server.id,
                                serverName = server.name,
                                message = "Job ${job.name} has been running for over ${rule.thresholdMinutes ?: 60} minutes"
                            )
                        }
                }
                AlertType.NODE_FAILURE -> {
                    jobs.filter { it.state == JobState.NODE_FAIL }.forEach { job ->
                        events += AlertEvent(
                            job = job,
                            alertType = AlertType.NODE_FAILURE,
                            serverId = server.id,
                            serverName = server.name,
                            message = "Node failure detected for job ${job.name}"
                        )
                    }
                }
                AlertType.PARTITION_DOWN -> {
                    // Detected externally via squeue error; placeholder for partition-level events
                }
            }
        }
        return events
    }
}
