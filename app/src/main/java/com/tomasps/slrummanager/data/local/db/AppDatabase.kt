package com.tomasps.slrummanager.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tomasps.slrummanager.data.local.db.dao.JobDao
import com.tomasps.slrummanager.data.local.db.dao.ServerDao
import com.tomasps.slrummanager.data.local.db.entity.JobEntity
import com.tomasps.slrummanager.data.local.db.entity.ServerEntity

@Database(
    entities = [ServerEntity::class, JobEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
    abstract fun jobDao(): JobDao
}
