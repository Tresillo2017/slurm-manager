package com.tomasps.slurmmanag.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tomasps.slurmmanag.domain.model.AlertRule
import com.tomasps.slurmmanag.domain.model.AuthMethod
import com.tomasps.slurmmanag.domain.model.Server
import com.tomasps.slurmmanag.domain.model.ServerStatus
import java.util.UUID

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val hostname: String,
    val port: Int,
    val username: String,
    val authMethod: AuthMethod,
    val pollingIntervalMinutes: Int,
    val alertRules: List<AlertRule>,
    val status: ServerStatus,
    val lastPolledAt: Long?
) {
    fun toDomain() = Server(
        id = UUID.fromString(id),
        name = name,
        hostname = hostname,
        port = port,
        username = username,
        authMethod = authMethod,
        pollingIntervalMinutes = pollingIntervalMinutes,
        alertRules = alertRules,
        status = status,
        lastPolledAt = lastPolledAt
    )

    companion object {
        fun fromDomain(server: Server) = ServerEntity(
            id = server.id.toString(),
            name = server.name,
            hostname = server.hostname,
            port = server.port,
            username = server.username,
            authMethod = server.authMethod,
            pollingIntervalMinutes = server.pollingIntervalMinutes,
            alertRules = server.alertRules,
            status = server.status,
            lastPolledAt = server.lastPolledAt
        )
    }
}
