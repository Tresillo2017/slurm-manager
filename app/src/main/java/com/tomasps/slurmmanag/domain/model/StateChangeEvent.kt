package com.tomasps.slurmmanag.domain.model

import java.util.UUID

data class StateChangeEvent(
    val job: Job,
    val previousState: JobState,
    val newState: JobState,
    val serverId: UUID,
    val serverName: String
)
