package com.tomasps.slurmmanager.data.worker

import com.tomasps.slurmmanager.domain.model.Job
import com.tomasps.slurmmanager.domain.model.Server
import com.tomasps.slurmmanager.domain.model.StateChangeEvent

object JobDiffer {
    fun diff(previous: List<Job>, current: List<Job>, server: Server): List<StateChangeEvent> {
        val previousMap = previous.associateBy { it.jobId }
        return current.mapNotNull { newJob ->
            val oldJob = previousMap[newJob.jobId]
            if (oldJob != null && oldJob.state != newJob.state) {
                StateChangeEvent(
                    job = newJob,
                    previousState = oldJob.state,
                    newState = newJob.state,
                    serverId = server.id,
                    serverName = server.name
                )
            } else null
        }
    }
}
