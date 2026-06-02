package com.tomasps.slrummanager.data.repository

import com.tomasps.slrummanager.data.local.db.dao.JobDao
import com.tomasps.slrummanager.data.local.db.entity.JobEntity
import com.tomasps.slrummanager.domain.model.Job
import com.tomasps.slrummanager.domain.model.JobState
import com.tomasps.slrummanager.domain.repository.JobRepository
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

    override suspend fun upsertAll(jobs: List<Job>) =
        dao.upsertAll(jobs.map { JobEntity.fromDomain(it) })

    override suspend fun getPreviousSnapshot(serverId: UUID): List<Job> =
        dao.getSnapshot(serverId.toString()).map { it.toDomain() }

    override suspend fun getByState(serverId: UUID, state: JobState): List<Job> =
        dao.getByState(serverId.toString(), state).map { it.toDomain() }
}
