package com.tomasps.slurmmanager.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.tomasps.slurmmanager.domain.model.Job
import com.tomasps.slurmmanager.domain.model.JobState
import java.util.UUID

@Entity(tableName = "jobs", primaryKeys = ["jobId", "serverId"])
data class JobEntity(
    val jobId: String,
    val serverId: String,
    val name: String,
    val state: JobState,
    val partition: String,
    val nodes: Int,
    val cpus: Int,
    val memoryMb: Long,
    val submitTime: Long,
    val startTime: Long?,
    val endTime: Long?,
    val workDir: String,
    val exitCode: Int?,
    val nodelist: String,
    val priority: Int,
    val queuePosition: Int?,
    @ColumnInfo(defaultValue = "0") val watched: Boolean = false
) {
    fun toDomain() = Job(
        jobId = jobId,
        serverId = UUID.fromString(serverId),
        name = name,
        state = state,
        partition = partition,
        nodes = nodes,
        cpus = cpus,
        memoryMb = memoryMb,
        submitTime = submitTime,
        startTime = startTime,
        endTime = endTime,
        workDir = workDir,
        exitCode = exitCode,
        nodelist = nodelist,
        priority = priority,
        queuePosition = queuePosition,
        watched = watched
    )

    companion object {
        fun fromDomain(job: Job) = JobEntity(
            jobId = job.jobId,
            serverId = job.serverId.toString(),
            name = job.name,
            state = job.state,
            partition = job.partition,
            nodes = job.nodes,
            cpus = job.cpus,
            memoryMb = job.memoryMb,
            submitTime = job.submitTime,
            startTime = job.startTime,
            endTime = job.endTime,
            workDir = job.workDir,
            exitCode = job.exitCode,
            nodelist = job.nodelist,
            priority = job.priority,
            queuePosition = job.queuePosition,
            watched = job.watched
        )
    }
}
