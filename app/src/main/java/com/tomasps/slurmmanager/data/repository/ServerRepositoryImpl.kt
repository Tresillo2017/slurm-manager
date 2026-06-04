package com.tomasps.slurmmanager.data.repository

import com.tomasps.slurmmanager.data.local.db.dao.ServerDao
import com.tomasps.slurmmanager.data.local.db.entity.ServerEntity
import com.tomasps.slurmmanager.domain.model.Server
import com.tomasps.slurmmanager.domain.model.ServerStatus
import com.tomasps.slurmmanager.domain.repository.ServerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class ServerRepositoryImpl @Inject constructor(
    private val dao: ServerDao
) : ServerRepository {

    override fun observeAll(): Flow<List<Server>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: UUID): Server? =
        dao.getById(id.toString())?.toDomain()

    override suspend fun insert(server: Server) =
        dao.insert(ServerEntity.fromDomain(server))

    override suspend fun update(server: Server) =
        dao.update(ServerEntity.fromDomain(server))

    override suspend fun delete(id: UUID) =
        dao.deleteById(id.toString())

    override suspend fun updateStatus(id: UUID, status: ServerStatus, lastPolledAt: Long) =
        dao.updateStatus(id.toString(), status, lastPolledAt)
}
