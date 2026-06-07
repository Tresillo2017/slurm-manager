package com.tomasps.slurmmanag.data.local.db.dao

import androidx.room.*
import com.tomasps.slurmmanag.data.local.db.entity.JobEntity
import com.tomasps.slurmmanag.domain.model.JobState
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

    @Query("UPDATE jobs SET watched = :watched WHERE jobId = :jobId AND serverId = :serverId")
    suspend fun setWatched(jobId: String, serverId: String, watched: Boolean)

    @Query("SELECT * FROM jobs WHERE watched = 1 ORDER BY submitTime DESC")
    fun observeWatched(): Flow<List<JobEntity>>

    @Query("SELECT * FROM jobs WHERE watched = 1")
    suspend fun getWatchedSnapshot(): List<JobEntity>
}
