package com.tomasps.slrummanager.presentation.jobdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasps.slrummanager.data.credential.CredentialStore
import com.tomasps.slrummanager.data.remote.ssh.SshClient
import com.tomasps.slrummanager.domain.model.Job
import com.tomasps.slrummanager.domain.model.Server
import com.tomasps.slrummanager.domain.repository.JobRepository
import com.tomasps.slrummanager.domain.repository.ServerRepository
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
import javax.inject.Inject

data class JobDetailUiState(
    val job: Job? = null,
    val server: Server? = null,
    val actionMessage: String? = null
)

@HiltViewModel
class JobDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val jobRepository: JobRepository,
    private val serverRepository: ServerRepository,
    private val credentialStore: CredentialStore
) : ViewModel() {

    private val jobId = savedStateHandle.get<String>("jobId")!!
    private val serverId = UUID.fromString(savedStateHandle.get<String>("serverId")!!)
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

    fun cancelJob() = runSshAction("scancel $jobId", "Job cancelled")
    fun requeueJob() = runSshAction("scontrol requeue $jobId", "Job requeued")

    private fun runSshAction(command: String, successMsg: String) {
        viewModelScope.launch {
            val server = uiState.value.server ?: return@launch
            val password = credentialStore.getPassword(serverId.toString())
            val privateKey = credentialStore.getSshPrivateKey(serverId.toString())
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
