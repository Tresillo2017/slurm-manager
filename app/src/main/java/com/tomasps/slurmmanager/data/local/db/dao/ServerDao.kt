package com.tomasps.slurmmanager.data.local.db.dao

import androidx.room.*
import com.tomasps.slurmmanager.data.local.db.entity.ServerEntity
import com.tomasps.slurmmanager.domain.model.ServerStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
    @Query("SELECT * FROM servers ORDER BY name ASC")
    fun observeAll(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM servers WHERE id = :id")
    suspend fun getById(id: String): ServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(server: ServerEntity)

    @Update
    suspend fun update(server: ServerEntity)

    @Query("DELETE FROM servers WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE servers SET status = :status, lastPolledAt = :lastPolledAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: ServerStatus, lastPolledAt: Long)
}
