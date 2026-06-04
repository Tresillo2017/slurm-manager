package com.tomasps.slurmmanager.domain.model

import java.util.UUID

data class AlertEvent(
    val job: Job?,
    val alertType: AlertType,
    val serverId: UUID,
    val serverName: String,
    val message: String
)
