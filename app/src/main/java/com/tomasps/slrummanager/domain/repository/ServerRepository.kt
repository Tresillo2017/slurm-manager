package com.tomasps.slrummanager.domain.repository

import com.tomasps.slrummanager.domain.model.Server
import com.tomasps.slrummanager.domain.model.ServerStatus
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface ServerRepository {
    fun observeAll(): Flow<List<Server>>
    suspend fun getById(id: UUID): Server?
    suspend fun insert(server: Server)
    suspend fun update(server: Server)
    suspend fun delete(id: UUID)
    suspend fun updateStatus(id: UUID, status: ServerStatus, lastPolledAt: Long)
}
