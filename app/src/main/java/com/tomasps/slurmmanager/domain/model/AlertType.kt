package com.tomasps.slurmmanager.domain.model

enum class AlertType {
    QUEUE_WAIT_EXCEEDED,
    RUNTIME_EXCEEDED,
    NODE_FAILURE,
    PARTITION_DOWN
}
