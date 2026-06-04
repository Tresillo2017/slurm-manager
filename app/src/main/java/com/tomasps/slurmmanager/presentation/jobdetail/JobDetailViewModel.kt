package com.tomasps.slurmmanager.presentation.jobdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasps.slurmmanager.data.credential.CredentialStore
import com.tomasps.slurmmanager.data.notification.NotificationEngine
import com.tomasps.slurmmanager.data.remote.ssh.SshClient
import com.tomasps.slurmmanager.domain.model.Job
import com.tomasps.slurmmanager.domain.model.Server
import com.tomasps.slurmmanager.domain.repository.JobRepository
import com.tomasps.slurmmanager.domain.repository.ServerRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class JobDetailUiState(
    val job: Job? = null,
    val server: Server? = null,
    val actionMessage: String? = null
)

@HiltViewModel(assistedFactory = JobDetailViewModel.Factory::class)
class JobDetailViewModel @AssistedInject constructor(
    @Assisted("jobId") private val jobId: String,
    @Assisted("serverId") private val serverIdStr: String,
    private val jobRepository: JobRepository,
    private val serverRepository: ServerRepository,
    private val credentialStore: CredentialStore,
    private val notificationEngine: NotificationEngine
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("jobId") jobId: String,
            @Assisted("serverId") serverId: String
        ): JobDetailViewModel
    }

    private val serverId = UUID.fromString(serverIdStr)
    private val _message = MutableStateFlow<String?>(null)

    val uiState = combine(
        jobRepository.observeByServer(serverId),
        serverRepository.observeAll(),
        _message
    ) { jobs, servers, message ->
        JobDetailUiState(
            job = jobs.find { it.jobId == jobId },
            server = servers.find { it.id == serverId },
            actionMessage = message
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JobDetailUiState())

    fun toggleWatch() {
        viewModelScope.launch {
            val job = uiState.value.job ?: return@launch
            val nowWatched = !job.watched
            jobRepository.setWatched(jobId, serverId, nowWatched)
            if (nowWatched) {
                notificationEngine.postLiveUpdateNotification(job.jobId, job.name, job.state)
                _message.update { "Watching ${job.name}" }
            } else {
                notificationEngine.cancelLiveNotification(jobId)
                _message.update { "Stopped watching ${job.name}" }
            }
        }
    }

    fun cancelJob() = runSshAction("scancel $jobId", "Job cancelled")
    fun requeueJob() = runSshAction("scontrol requeue $jobId", "Job requeued")

    private fun runSshAction(command: String, successMsg: String) {
        viewModelScope.launch {
            val server = uiState.value.server ?: return@launch
            val password = credentialStore.getPassword(serverIdStr)
            val privateKey = credentialStore.getSshPrivateKey(serverIdStr)
            try {
                withContext(Dispatchers.IO) { SshClient().executeCommands(server, password, privateKey, command) }
                _message.update { successMsg }
            } catch (e: Exception) {
                _message.update { "Error: ${e.message}" }
            }
        }
    }

    fun clearMessage() = _message.update { null }
}
