package com.tomasps.slrummanager.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.tomasps.slrummanager.data.credential.CredentialStore
import com.tomasps.slrummanager.data.notification.NotificationEngine
import com.tomasps.slrummanager.data.remote.ssh.SlurmOutputParser
import com.tomasps.slrummanager.data.remote.ssh.SshClient
import com.tomasps.slrummanager.domain.model.JobState
import com.tomasps.slrummanager.domain.model.ServerStatus
import com.tomasps.slrummanager.domain.repository.JobRepository
import com.tomasps.slrummanager.domain.repository.ServerRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.UUID
import java.util.concurrent.TimeUnit

@HiltWorker
class PollWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val serverRepository: ServerRepository,
    private val jobRepository: JobRepository,
    private val credentialStore: CredentialStore,
    private val notificationEngine: NotificationEngine
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_SERVER_ID = "server_id"
        const val TAG_PREFIX = "poll_"

        fun enqueue(context: Context, serverId: UUID, intervalMinutes: Int) {
            val tag = "$TAG_PREFIX$serverId"
            WorkManager.getInstance(context).cancelAllWorkByTag(tag)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val data = workDataOf(KEY_SERVER_ID to serverId.toString())

            if (intervalMinutes >= 15) {
                val request = PeriodicWorkRequestBuilder<PollWorker>(
                    intervalMinutes.toLong(), TimeUnit.MINUTES
                ).setConstraints(constraints)
                    .setInputData(data)
                    .addTag(tag)
                    .build()
                WorkManager.getInstance(context)
                    .enqueueUniquePeriodicWork(tag, ExistingPeriodicWorkPolicy.UPDATE, request)
            } else {
                enqueueOneTime(context, serverId, intervalMinutes, constraints, data, tag)
            }
        }

        private fun enqueueOneTime(
            context: Context, serverId: UUID, intervalMinutes: Int,
            constraints: Constraints, data: Data, tag: String
        ) {
            val request = OneTimeWorkRequestBuilder<PollWorker>()
                .setConstraints(constraints)
                .setInputData(data)
                .addTag(tag)
                .setInitialDelay(intervalMinutes.toLong(), TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(tag, ExistingWorkPolicy.REPLACE, request)
        }

        fun cancel(context: Context, serverId: UUID) {
            WorkManager.getInstance(context).cancelAllWorkByTag("$TAG_PREFIX$serverId")
        }
    }

    override suspend fun doWork(): Result {
        val serverId = inputData.getString(KEY_SERVER_ID)?.let { UUID.fromString(it) }
            ?: return Result.failure()

        val server = serverRepository.getById(serverId) ?: return Result.failure()
        val password = credentialStore.getPassword(serverId.toString())
        val privateKey = credentialStore.getSshPrivateKey(serverId.toString())

        return try {
            val sshClient = SshClient()
            val squeueCmd = "squeue --user=\$USER --noheader --format=\"%i|%j|%T|%P|%D|%C|%m|%V|%S|%e|%Z|%p|%Q\""
            val sacctCmd = "sacct --user=\$USER --starttime=now-7days --noheader --format=JobID,JobName,State,Partition,NNodes,NCPUS,ReqMem,Submit,Start,End,WorkDir,ExitCode"

            val outputs = sshClient.executeCommands(server, password, privateKey, squeueCmd, sacctCmd)

            val squeueJobs = SlurmOutputParser.parseSqueue(outputs[0], serverId)
            val sacctJobs = SlurmOutputParser.parseSacct(outputs[1], serverId)

            val mergedJobs = (squeueJobs + sacctJobs).distinctBy { it.jobId }

            val previous = jobRepository.getPreviousSnapshot(serverId)
            jobRepository.upsertAll(mergedJobs)

            val stateChanges = JobDiffer.diff(previous, mergedJobs, server)
            stateChanges.forEach { event ->
                notificationEngine.notifyStateChange(event)
            }

            val nowMs = System.currentTimeMillis()
            val alertEvents = AlertEngine.evaluate(mergedJobs, server, nowMs)
            alertEvents.forEach { notificationEngine.notifyAlert(it) }

            mergedJobs.filter { it.state.isActive }.forEach { job ->
                val elapsed = job.startTime?.let { nowMs - it } ?: 0L
                notificationEngine.postLiveUpdateNotification(job.jobId, job.name, elapsed, job.state)
            }
            mergedJobs.filter { it.state.isTerminal }.forEach { job ->
                notificationEngine.postLiveUpdateNotification(job.jobId, job.name, 0L, job.state)
            }

            serverRepository.updateStatus(serverId, ServerStatus.ONLINE, nowMs)

            if (server.pollingIntervalMinutes < 15) {
                enqueue(applicationContext, serverId, server.pollingIntervalMinutes)
            }

            Result.success()
        } catch (e: Exception) {
            serverRepository.updateStatus(serverId, ServerStatus.UNREACHABLE, System.currentTimeMillis())
            notificationEngine.notifyClusterEvent(
                serverId.toString(), server.name,
                "Cannot reach ${server.hostname}: ${e.message}"
            )
            Result.retry()
        }
    }
}
