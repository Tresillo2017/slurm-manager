package com.tomasps.slrummanager.domain.model

enum class JobState {
    PENDING, RUNNING, COMPLETING, COMPLETED,
    FAILED, CANCELLED, TIMEOUT, NODE_FAIL,
    PREEMPTED, SUSPENDED, UNKNOWN;

    val isTerminal: Boolean get() = this in setOf(COMPLETED, FAILED, CANCELLED, TIMEOUT, NODE_FAIL, PREEMPTED)
    val isActive: Boolean get() = this == RUNNING || this == COMPLETING
}
