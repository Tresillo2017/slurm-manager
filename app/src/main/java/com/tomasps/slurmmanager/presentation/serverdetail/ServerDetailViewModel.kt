package com.tomasps.slurmmanager.presentation.serverdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasps.slurmmanager.data.credential.CredentialStore
import com.tomasps.slurmmanager.data.remote.ssh.SshClient
import com.tomasps.slurmmanager.data.worker.PollWorker
import com.tomasps.slurmmanager.domain.model.AlertRule
import com.tomasps.slurmmanager.domain.model.AuthMethod
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

data class ServerDetailUiState(
    val server: Server? = null,
    val jobs: List<Job> = emptyList(),
    val connectionTestResult: ConnectionTestResult? = null,
    val isAuthError: Boolean = false,
    val showCredentialEditor: Boolean = false,
    val isFetching: Boolean = false,
)

sealed class ConnectionTestResult {
    data object Testing : ConnectionTestResult()
    data class Success(val latencyMs: Long) : ConnectionTestResult()
    data class Failure(val error: String) : ConnectionTestResult()
}

@HiltViewModel(assistedFactory = ServerDetailViewModel.Factory::class)
class ServerDetailViewModel @AssistedInject constructor(
    @Assisted private val serverIdStr: String,
    private val serverRepository: ServerRepository,
    private val jobRepository: JobRepository,
    private val credentialStore: CredentialStore
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(serverId: String): ServerDetailViewModel
    }

    private val serverId = UUID.fromString(serverIdStr)

    private data class Flags(
        val testResult: ConnectionTestResult? = null,
        val isAuthError: Boolean = false,
        val showCredentialEditor: Boolean = false,
        val isFetching: Boolean = false,
    )

    private val _flags = MutableStateFlow(Flags())

    val uiState = combine(
        serverRepository.observeAll(),
        jobRepository.observeByServer(serverId),
        _flags
    ) { servers, jobs, flags ->
        ServerDetailUiState(
            server = servers.find { it.id == serverId },
            jobs = jobs,
            connectionTestResult = flags.testResult,
            isAuthError = flags.isAuthError,
            showCredentialEditor = flags.showCredentialEditor,
            isFetching = flags.isFetching,
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
            _flags.update { it.copy(testResult = ConnectionTestResult.Testing, isAuthError = false) }
            val server = uiState.value.server ?: return@launch
            val password = credentialStore.getPassword(serverIdStr)
            val privateKey = credentialStore.getSshPrivateKey(serverIdStr)
            try {
                val latency = withContext(Dispatchers.IO) {
                    SshClient().testConnection(server, password, privateKey)
                }
                _flags.update { it.copy(testResult = ConnectionTestResult.Success(latency)) }
            } catch (e: Exception) {
                val msg = e.message ?: "Unknown error"
                _flags.update { it.copy(testResult = ConnectionTestResult.Failure(msg), isAuthError = isAuthFailure(msg)) }
            }
        }
    }

    fun forceFetch(context: android.content.Context) {
        _flags.update { it.copy(isFetching = true) }
        PollWorker.enqueueImmediate(context, serverId)
        viewModelScope.launch {
            kotlinx.coroutines.delay(3_000)
            _flags.update { it.copy(isFetching = false) }
        }
    }

    fun openCredentialEditor() = _flags.update { it.copy(showCredentialEditor = true) }
    fun closeCredentialEditor() = _flags.update { it.copy(showCredentialEditor = false) }

    fun saveCredentials(
        username: String,
        authMethod: AuthMethod,
        password: String,
        privateKeyPem: String,
        context: android.content.Context
    ) {
        viewModelScope.launch {
            val server = uiState.value.server ?: return@launch
            if (authMethod == AuthMethod.PASSWORD) {
                credentialStore.savePassword(serverIdStr, password)
            } else {
                credentialStore.saveSshPrivateKey(serverIdStr, privateKeyPem)
            }
            serverRepository.update(server.copy(
                username = username.ifBlank { server.username },
                authMethod = authMethod
            ))
            _flags.update { it.copy(showCredentialEditor = false, isAuthError = false) }
            PollWorker.enqueue(context, serverId, server.pollingIntervalMinutes)
            testConnection(context)
        }
    }

    private fun isAuthFailure(message: String): Boolean {
        val lower = message.lowercase()
        return lower.contains("auth") ||
            lower.contains("permission denied") ||
            lower.contains("publickey") ||
            lower.contains("password") ||
            lower.contains("credential") ||
            lower.contains("no supported authentication")
    }
}
