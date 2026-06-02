package com.tomasps.slrummanager.domain.repository

import com.tomasps.slrummanager.domain.model.Job
import com.tomasps.slrummanager.domain.model.JobState
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface JobRepository {
    fun observeByServer(serverId: UUID): Flow<List<Job>>
    fun observeAll(): Flow<List<Job>>
    suspend fun getById(jobId: String, serverId: UUID): Job?
    suspend fun upsertAll(jobs: List<Job>)
    suspend fun getPreviousSnapshot(serverId: UUID): List<Job>
    suspend fun getByState(serverId: UUID, state: JobState): List<Job>
}
