package com.tomasps.slurmmanag.presentation.onboarding

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasps.slurmmanag.data.credential.CredentialStore
import com.tomasps.slurmmanag.data.remote.ssh.SshClient
import com.tomasps.slurmmanag.data.worker.PollWorker
import com.tomasps.slurmmanag.domain.model.AuthMethod
import com.tomasps.slurmmanag.domain.model.Server
import com.tomasps.slurmmanag.domain.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class OnboardingUiState(
    val name: String = "",
    val hostname: String = "",
    val port: String = "22",
    val username: String = "",
    val authMethod: AuthMethod = AuthMethod.PASSWORD,
    val password: String = "",
    val privateKeyPem: String = "",
    val step: Int = 0,
    val isTesting: Boolean = false,
    val testSuccess: Boolean? = null,
    val testLatencyMs: Long = 0L,
    val testError: String = ""
)

private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val credentialStore: CredentialStore,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state = _state.asStateFlow()

    fun update(block: OnboardingUiState.() -> OnboardingUiState) = _state.update(block)

    fun testConnection(context: Context) {
        val s = _state.value
        val server = buildServer(s)
        viewModelScope.launch {
            _state.update { it.copy(isTesting = true, testSuccess = null, testError = "") }
            try {
                val latency = withContext(Dispatchers.IO) {
                    SshClient().testConnection(
                        server,
                        s.password.takeIf { it.isNotBlank() },
                        s.privateKeyPem.takeIf { it.isNotBlank() }
                    )
                }
                _state.update { it.copy(isTesting = false, testSuccess = true, testLatencyMs = latency) }
            } catch (e: Exception) {
                _state.update { it.copy(isTesting = false, testSuccess = false, testError = e.message ?: "Connection failed") }
            }
        }
    }

    fun saveServer(context: Context, isFirstServer: Boolean = true, onDone: () -> Unit) {
        val s = _state.value
        val server = buildServer(s)
        viewModelScope.launch {
            serverRepository.insert(server)
            if (s.authMethod == AuthMethod.PASSWORD) {
                credentialStore.savePassword(server.id.toString(), s.password)
            } else {
                credentialStore.saveSshPrivateKey(server.id.toString(), s.privateKeyPem)
            }
            PollWorker.enqueue(context, server.id, server.pollingIntervalMinutes)
            PollWorker.enqueueImmediate(context, server.id)
            if (isFirstServer) {
                dataStore.edit { it[KEY_ONBOARDING_DONE] = true }
            }
            onDone()
        }
    }

    private fun buildServer(s: OnboardingUiState) = Server(
        name = s.name.ifBlank { s.hostname },
        hostname = s.hostname,
        port = s.port.toIntOrNull() ?: 22,
        username = s.username,
        authMethod = s.authMethod
    )
}
