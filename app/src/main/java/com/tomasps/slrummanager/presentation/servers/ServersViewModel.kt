package com.tomasps.slrummanager.presentation.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasps.slrummanager.domain.model.Job
import com.tomasps.slrummanager.domain.model.JobState
import com.tomasps.slrummanager.domain.model.Server
import com.tomasps.slrummanager.domain.repository.JobRepository
import com.tomasps.slrummanager.domain.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    private val jobRepository: JobRepository
) : ViewModel() {

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

    suspend fun deleteServer(server: Server) {
        serverRepository.delete(server.id)
    }
}
