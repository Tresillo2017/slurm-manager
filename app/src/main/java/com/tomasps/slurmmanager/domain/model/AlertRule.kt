package com.tomasps.slurmmanager.domain.model

data class AlertRule(
    val type: AlertType,
    val thresholdMinutes: Int? = null
)
