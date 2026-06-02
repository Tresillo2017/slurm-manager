package com.tomasps.slrummanager.domain.model

import java.util.UUID

data class StateChangeEvent(
    val job: Job,
    val previousState: JobState,
    val newState: JobState,
    val serverId: UUID,
    val serverName: String
)
