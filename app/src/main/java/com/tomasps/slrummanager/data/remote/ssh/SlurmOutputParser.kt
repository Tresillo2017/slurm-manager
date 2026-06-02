package com.tomasps.slrummanager.data.remote.ssh

import com.tomasps.slrummanager.domain.model.Job
import com.tomasps.slrummanager.domain.model.JobState
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

object SlurmOutputParser {

    private val squeueFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    private val sacctFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

    // squeue --format="%i|%j|%T|%P|%D|%C|%m|%V|%S|%e|%Z|%p|%Q"
    fun parseSqueue(output: String, serverId: UUID): List<Job> =
        output.lines().filter { it.isNotBlank() }.mapNotNull { line ->
            val p = line.split("|")
            if (p.size < 13) return@mapNotNull null
            runCatching {
                Job(
                    jobId = p[0].trim(),
                    serverId = serverId,
                    name = p[1].trim(),
                    state = parseState(p[2].trim()),
                    partition = p[3].trim(),
                    nodes = p[4].trim().toIntOrNull() ?: 1,
                    cpus = p[5].trim().toIntOrNull() ?: 1,
                    memoryMb = parseMemory(p[6].trim()),
                    submitTime = parseTime(p[7].trim(), squeueFmt),
                    startTime = p[8].trim().takeIf { it != "N/A" && it.isNotEmpty() }?.let { parseTime(it, squeueFmt) },
                    endTime = p[9].trim().takeIf { it != "N/A" && it.isNotEmpty() }?.let { parseTime(it, squeueFmt) },
                    workDir = p[10].trim(),
                    exitCode = null,
                    nodelist = p[11].trim(),
                    priority = p[12].trim().toIntOrNull() ?: 0,
                    queuePosition = null
                )
            }.getOrNull()
        }

    // sacct --format=JobID,JobName,State,Partition,NNodes,NCPUS,ReqMem,Submit,Start,End,WorkDir,ExitCode
    fun parseSacct(output: String, serverId: UUID): List<Job> =
        output.lines().filter { it.isNotBlank() && !it.trimStart().startsWith("JobID") }.mapNotNull { line ->
            val p = line.trim().split("\\s+".toRegex())
            if (p.size < 12) return@mapNotNull null
            // Skip job step lines (contain ".")
            if (p[0].contains(".")) return@mapNotNull null
            runCatching {
                Job(
                    jobId = p[0].trim(),
                    serverId = serverId,
                    name = p[1].trim(),
                    state = parseState(p[2].trim()),
                    partition = p[3].trim(),
                    nodes = p[4].trim().toIntOrNull() ?: 1,
                    cpus = p[5].trim().toIntOrNull() ?: 1,
                    memoryMb = parseMemory(p[6].trim()),
                    submitTime = parseTime(p[7].trim(), sacctFmt),
                    startTime = p[8].trim().takeIf { it != "Unknown" && it != "None" }?.let { parseTime(it, sacctFmt) },
                    endTime = p[9].trim().takeIf { it != "Unknown" && it != "None" }?.let { parseTime(it, sacctFmt) },
                    workDir = p[10].trim(),
                    exitCode = p[11].trim().split(":").firstOrNull()?.toIntOrNull(),
                    nodelist = "",
                    priority = 0,
                    queuePosition = null
                )
            }.getOrNull()
        }

    private fun parseState(raw: String): JobState = when (raw.uppercase().split(" ")[0]) {
        "PENDING", "PD" -> JobState.PENDING
        "RUNNING", "R" -> JobState.RUNNING
        "COMPLETING", "CG" -> JobState.COMPLETING
        "COMPLETED", "CD" -> JobState.COMPLETED
        "FAILED", "F" -> JobState.FAILED
        "CANCELLED", "CA" -> JobState.CANCELLED
        "TIMEOUT", "TO" -> JobState.TIMEOUT
        "NODE_FAIL", "NF" -> JobState.NODE_FAIL
        "PREEMPTED", "PR" -> JobState.PREEMPTED
        "SUSPENDED", "S" -> JobState.SUSPENDED
        else -> JobState.UNKNOWN
    }

    private fun parseMemory(raw: String): Long {
        if (raw.isBlank() || raw == "0") return 0L
        val unit = raw.last().uppercaseChar()
        val num = raw.dropLast(1).toLongOrNull() ?: return 0L
        return when (unit) {
            'K' -> num / 1024
            'M' -> num
            'G' -> num * 1024
            'T' -> num * 1024 * 1024
            else -> raw.toLongOrNull() ?: 0L
        }
    }

    private fun parseTime(raw: String, fmt: SimpleDateFormat): Long =
        runCatching { fmt.parse(raw)?.time ?: 0L }.getOrDefault(0L)
}
