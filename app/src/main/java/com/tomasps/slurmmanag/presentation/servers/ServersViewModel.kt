package com.tomasps.slurmmanag.presentation.servers

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasps.slurmmanag.data.worker.PollWorker
import com.tomasps.slurmmanag.domain.model.Job
import com.tomasps.slurmmanag.domain.model.JobState
import com.tomasps.slurmmanag.domain.model.Server
import com.tomasps.slurmmanag.domain.repository.JobRepository
import com.tomasps.slurmmanag.domain.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServerListItem(
    val server: Server,
    val runningCount: Int,
    val pendingCount: Int,
    val failedCount: Int
)

@HiltViewModel
class ServersViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val jobRepository: JobRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)

    val servers = combine(
        serverRepository.observeAll(),
        jobRepository.observeAll()
    ) { servers, jobs ->
        servers.map { server ->
            val serverJobs = jobs.filter { it.serverId == server.id }
            ServerListItem(
                server = server,
                runningCount = serverJobs.count { it.state.isActive },
                pendingCount = serverJobs.count { it.state == JobState.PENDING },
                failedCount = serverJobs.count { it.state == JobState.FAILED }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val errorMessage = _errorMessage.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), null
    )

    fun deleteServer(server: Server) {
        viewModelScope.launch {
            try {
                PollWorker.cancel(context, server.id)
                serverRepository.delete(server.id)
            } catch (e: Exception) {
                _errorMessage.update { "Failed to delete ${server.name}: ${e.message}" }
            }
        }
    }

    fun clearError() = _errorMessage.update { null }
}
