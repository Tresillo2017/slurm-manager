package com.tomasps.slrummanager.di

import android.content.Context
import androidx.room.Room
import com.tomasps.slrummanager.data.local.db.AppDatabase
import com.tomasps.slrummanager.data.local.db.dao.JobDao
import com.tomasps.slrummanager.data.local.db.dao.ServerDao
import com.tomasps.slrummanager.data.repository.JobRepositoryImpl
import com.tomasps.slrummanager.data.repository.ServerRepositoryImpl
import com.tomasps.slrummanager.domain.repository.JobRepository
import com.tomasps.slrummanager.domain.repository.ServerRepository
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
