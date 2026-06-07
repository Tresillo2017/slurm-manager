package com.tomasps.slurmmanag.domain.repository

import com.tomasps.slurmmanag.domain.model.Job
import com.tomasps.slurmmanag.domain.model.JobState
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface JobRepository {
    fun observeByServer(serverId: UUID): Flow<List<Job>>
    fun observeAll(): Flow<List<Job>>
    fun observeWatched(): Flow<List<Job>>
    suspend fun getById(jobId: String, serverId: UUID): Job?
    suspend fun upsertAll(jobs: List<Job>)
    suspend fun getPreviousSnapshot(serverId: UUID): List<Job>
    suspend fun getByState(serverId: UUID, state: JobState): List<Job>
    suspend fun setWatched(jobId: String, serverId: UUID, watched: Boolean)
    suspend fun getWatchedSnapshot(): List<Job>
}
