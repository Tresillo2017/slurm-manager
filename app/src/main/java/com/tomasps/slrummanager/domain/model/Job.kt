package com.tomasps.slrummanager.domain.model

import java.util.UUID

data class Job(
    val jobId: String,
    val serverId: UUID,
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
    val queuePosition: Int?
)
