package com.tomasps.slurmmanager.data.repository

import com.tomasps.slurmmanager.data.local.db.dao.JobDao
import com.tomasps.slurmmanager.data.local.db.entity.JobEntity
import com.tomasps.slurmmanager.domain.model.Job
import com.tomasps.slurmmanager.domain.model.JobState
import com.tomasps.slurmmanager.domain.repository.JobRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class JobRepositoryImpl @Inject constructor(
    private val dao: JobDao
) : JobRepository {

    override fun observeByServer(serverId: UUID): Flow<List<Job>> =
        dao.observeByServer(serverId.toString()).map { list -> list.map { it.toDomain() } }

    override fun observeAll(): Flow<List<Job>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(jobId: String, serverId: UUID): Job? =
        dao.getById(jobId, serverId.toString())?.toDomain()

    override suspend fun upsertAll(jobs: List<Job>) {
        // Preserve watched flag — SSH poll doesn't know about local watch state
        val watchedIds = dao.getWatchedSnapshot().map { it.jobId to it.serverId }.toSet()
        dao.upsertAll(jobs.map { job ->
            val isWatched = (job.jobId to job.serverId.toString()) in watchedIds
            JobEntity.fromDomain(job.copy(watched = isWatched))
        })
    }

    override suspend fun getPreviousSnapshot(serverId: UUID): List<Job> =
        dao.getSnapshot(serverId.toString()).map { it.toDomain() }

    override suspend fun getByState(serverId: UUID, state: JobState): List<Job> =
        dao.getByState(serverId.toString(), state).map { it.toDomain() }

    override fun observeWatched(): Flow<List<Job>> =
        dao.observeWatched().map { list -> list.map { it.toDomain() } }

    override suspend fun setWatched(jobId: String, serverId: UUID, watched: Boolean) =
        dao.setWatched(jobId, serverId.toString(), watched)

    override suspend fun getWatchedSnapshot(): List<Job> =
        dao.getWatchedSnapshot().map { it.toDomain() }
}
