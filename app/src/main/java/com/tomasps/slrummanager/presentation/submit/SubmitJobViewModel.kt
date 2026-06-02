package com.tomasps.slrummanager.presentation.submit

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasps.slrummanager.data.credential.CredentialStore
import com.tomasps.slrummanager.data.remote.ssh.SshClient
import com.tomasps.slrummanager.domain.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

data class SubmitJobUiState(
    val selectedServerId: String = "",
    val scriptContent: String = "",
    val partition: String = "",
    val nodes: String = "",
    val cpus: String = "",
    val timeLimit: String = "",
    val isSubmitting: Boolean = false,
    val submitResult: String? = null
)

@HiltViewModel
class SubmitJobViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val credentialStore: CredentialStore
) : ViewModel() {

    private val _state = MutableStateFlow(SubmitJobUiState())
    val state = _state.asStateFlow()

    fun update(block: SubmitJobUiState.() -> SubmitJobUiState) = _state.update(block)

    fun submit(context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, submitResult = null) }
            try {
                val serverId = UUID.fromString(_state.value.selectedServerId)
                val server = serverRepository.getById(serverId) ?: error("Server not found")
                val password = credentialStore.getPassword(serverId.toString())
                val privateKey = credentialStore.getSshPrivateKey(serverId.toString())
                val s = _state.value

                val sbatchArgs = buildList {
                    if (s.partition.isNotBlank()) add("--partition=${s.partition}")
                    if (s.nodes.isNotBlank()) add("--nodes=${s.nodes}")
                    if (s.cpus.isNotBlank()) add("--cpus-per-task=${s.cpus}")
                    if (s.timeLimit.isNotBlank()) add("--time=${s.timeLimit}")
                }

                val script = s.scriptContent
                val escapedScript = script.replace("'", "'\\''")
                val argsStr = sbatchArgs.joinToString(" ")
                val cmd = "echo '$escapedScript' | sbatch $argsStr"

                val output = withContext(Dispatchers.IO) {
                    SshClient().executeCommands(server, password, privateKey, cmd).first()
                }
                val jobId = Regex("Submitted batch job (\\d+)").find(output)?.groupValues?.get(1)
                if (jobId != null) {
                    _state.update { it.copy(isSubmitting = false, submitResult = "Submitted: Job $jobId") }
                    kotlinx.coroutines.delay(1500)
                    onSuccess()
                } else {
                    _state.update { it.copy(isSubmitting = false, submitResult = "Error: $output") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isSubmitting = false, submitResult = "Error: ${e.message}") }
            }
        }
    }
}
