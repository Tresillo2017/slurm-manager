package com.tomasps.slrummanager.domain.model

data class AlertRule(
    val type: AlertType,
    val thresholdMinutes: Int? = null
)
