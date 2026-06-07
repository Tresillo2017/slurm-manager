package com.tomasps.slurmmanag.domain.model

data class AlertRule(
    val type: AlertType,
    val thresholdMinutes: Int? = null
)
