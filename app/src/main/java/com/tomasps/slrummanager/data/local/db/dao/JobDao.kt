package com.tomasps.slrummanager.data.local.db.dao

import androidx.room.*
import com.tomasps.slrummanager.data.local.db.entity.JobEntity
import com.tomasps.slrummanager.domain.model.JobState
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {
    @Query("SELECT * FROM jobs WHERE serverId = :serverId ORDER BY priority DESC, submitTime DESC")
    fun observeByServer(serverId: String): Flow<List<JobEntity>>

    @Query("SELECT * FROM jobs ORDER BY submitTime DESC")
    fun observeAll(): Flow<List<JobEntity>>

    @Query("SELECT * FROM jobs WHERE jobId = :jobId AND serverId = :serverId")
    suspend fun getById(jobId: String, serverId: String): JobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(jobs: List<JobEntity>)

    @Query("SELECT * FROM jobs WHERE serverId = :serverId")
    suspend fun getSnapshot(serverId: String): List<JobEntity>

    @Query("SELECT * FROM jobs WHERE serverId = :serverId AND state = :state")
    suspend fun getByState(serverId: String, state: JobState): List<JobEntity>
}
