package com.tomasps.slurmmanager.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tomasps.slurmmanager.data.local.db.dao.JobDao
import com.tomasps.slurmmanager.data.local.db.dao.ServerDao
import com.tomasps.slurmmanager.data.local.db.entity.JobEntity
import com.tomasps.slurmmanager.data.local.db.entity.ServerEntity

@Database(
    entities = [ServerEntity::class, JobEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
    abstract fun jobDao(): JobDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE jobs ADD COLUMN watched INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
