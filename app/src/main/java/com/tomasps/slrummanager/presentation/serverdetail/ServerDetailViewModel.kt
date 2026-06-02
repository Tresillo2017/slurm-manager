package com.tomasps.slrummanager.presentation.serverdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasps.slrummanager.data.credential.CredentialStore
import com.tomasps.slrummanager.data.remote.ssh.SshClient
import com.tomasps.slrummanager.data.worker.PollWorker
import com.tomasps.slrummanager.domain.model.AlertRule
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

data class ServerDetailUiState(
    val server: Server? = null,
    val jobs: List<Job> = emptyList(),
    val connectionTestResult: ConnectionTestResult? = null
)

sealed class ConnectionTestResult {
    data object Testing : ConnectionTestResult()
    data class Success(val latencyMs: Long) : ConnectionTestResult()
    data class Failure(val error: String) : ConnectionTestResult()
}

@HiltViewModel
class ServerDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val serverRepository: ServerRepository,
    private val jobRepository: JobRepository,
    private val credentialStore: CredentialStore
) : ViewModel() {

    private val serverId = UUID.fromString(savedStateHandle.get<String>("serverId")!!)
    private val _testResult = MutableStateFlow<ConnectionTestResult?>(null)

    val uiState = combine(
        serverRepository.observeAll(),
        jobRepository.observeByServer(serverId),
        _testResult
    ) { servers, jobs, testResult ->
        ServerDetailUiState(
            server = servers.find { it.id == serverId },
            jobs = jobs,
            connectionTestResult = testResult
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ServerDetailUiState())

    fun updatePollingInterval(minutes: Int) {
        viewModelScope.launch {
            val server = uiState.value.server ?: return@launch
            serverRepository.update(server.copy(pollingIntervalMinutes = minutes))
        }
    }

    fun updateAlertRules(rules: List<AlertRule>) {
        viewModelScope.launch {
            val server = uiState.value.server ?: return@launch
            serverRepository.update(server.copy(alertRules = rules))
        }
    }

    fun testConnection(context: android.content.Context) {
        viewModelScope.launch {
            _testResult.update { ConnectionTestResult.Testing }
            val server = uiState.value.server ?: return@launch
            val password = credentialStore.getPassword(serverId.toString())
            val privateKey = credentialStore.getSshPrivateKey(serverId.toString())
            try {
                val latency = withContext(Dispatchers.IO) {
                    SshClient().testConnection(server, password, privateKey)
                }
                _testResult.update { ConnectionTestResult.Success(latency) }
            } catch (e: Exception) {
                _testResult.update { ConnectionTestResult.Failure(e.message ?: "Unknown error") }
            }
        }
    }
}
