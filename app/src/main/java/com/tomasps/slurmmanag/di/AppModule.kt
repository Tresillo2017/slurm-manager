package com.tomasps.slurmmanag.di

import android.content.Context
import androidx.room.Room
import com.tomasps.slurmmanag.data.local.db.AppDatabase
import com.tomasps.slurmmanag.data.local.db.dao.JobDao
import com.tomasps.slurmmanag.data.local.db.dao.ServerDao
import com.tomasps.slurmmanag.data.repository.JobRepositoryImpl
import com.tomasps.slurmmanag.data.repository.ServerRepositoryImpl
import com.tomasps.slurmmanag.domain.repository.JobRepository
import com.tomasps.slurmmanag.domain.repository.ServerRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "slrum_db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideServerDao(db: AppDatabase): ServerDao = db.serverDao()
    @Provides fun provideJobDao(db: AppDatabase): JobDao = db.jobDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindServerRepository(impl: ServerRepositoryImpl): ServerRepository

    @Binds @Singleton
    abstract fun bindJobRepository(impl: JobRepositoryImpl): JobRepository
}
