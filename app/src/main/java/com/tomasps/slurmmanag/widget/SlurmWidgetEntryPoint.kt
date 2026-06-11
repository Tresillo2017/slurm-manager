package com.tomasps.slurmmanag.widget

import com.tomasps.slurmmanag.domain.repository.JobRepository
import com.tomasps.slurmmanag.domain.repository.ServerRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SlurmWidgetEntryPoint {
    fun jobRepository(): JobRepository
    fun serverRepository(): ServerRepository
}
