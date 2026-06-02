package com.tomasps.slrummanager.data.local.db

import androidx.room.TypeConverter
import com.tomasps.slrummanager.domain.model.AlertRule
import com.tomasps.slrummanager.domain.model.AlertType
import com.tomasps.slrummanager.domain.model.AuthMethod
import com.tomasps.slrummanager.domain.model.JobState
import com.tomasps.slrummanager.domain.model.ServerStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter fun fromUUID(value: UUID?): String? = value?.toString()
    @TypeConverter fun toUUID(value: String?): UUID? = value?.let { UUID.fromString(it) }

    @TypeConverter fun fromJobState(value: JobState): String = value.name
    @TypeConverter fun toJobState(value: String): JobState = runCatching { JobState.valueOf(value) }.getOrDefault(JobState.UNKNOWN)

    @TypeConverter fun fromAuthMethod(value: AuthMethod): String = value.name
    @TypeConverter fun toAuthMethod(value: String): AuthMethod = AuthMethod.valueOf(value)

    @TypeConverter fun fromServerStatus(value: ServerStatus): String = value.name
    @TypeConverter fun toServerStatus(value: String): ServerStatus = runCatching { ServerStatus.valueOf(value) }.getOrDefault(ServerStatus.UNKNOWN)

    @TypeConverter fun fromAlertRules(value: List<AlertRule>): String = json.encodeToString(value.map { "${it.type.name}:${it.thresholdMinutes ?: ""}" })
    @TypeConverter fun toAlertRules(value: String): List<AlertRule> = runCatching {
        json.decodeFromString<List<String>>(value).mapNotNull { entry ->
            val parts = entry.split(":")
            val type = runCatching { AlertType.valueOf(parts[0]) }.getOrNull() ?: return@mapNotNull null
            val threshold = parts.getOrNull(1)?.toIntOrNull()
            AlertRule(type, threshold)
        }
    }.getOrDefault(emptyList())
}
