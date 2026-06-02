package com.tomasps.slrummanager.di

import com.tomasps.slrummanager.data.worker.PollWorker
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object WorkerModule
// PollWorker is injected via @HiltWorker + HiltWorkerFactory bound in SlrumApplication
