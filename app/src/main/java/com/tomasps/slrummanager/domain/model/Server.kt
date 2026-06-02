package com.tomasps.slrummanager.domain.model

import java.util.UUID

data class Server(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val hostname: String,
    val port: Int = 22,
    val username: String,
    val authMethod: AuthMethod,
    val pollingIntervalMinutes: Int = 5,
    val alertRules: List<AlertRule> = emptyList(),
    val status: ServerStatus = ServerStatus.UNKNOWN,
    val lastPolledAt: Long? = null
)

enum class ServerStatus { ONLINE, OFFLINE, UNREACHABLE, UNKNOWN }
