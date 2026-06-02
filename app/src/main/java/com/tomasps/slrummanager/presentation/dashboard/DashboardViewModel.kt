package com.tomasps.slrummanager.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasps.slrummanager.domain.model.Job
import com.tomasps.slrummanager.domain.model.JobState
import com.tomasps.slrummanager.domain.model.Server
import com.tomasps.slrummanager.domain.repository.JobRepository
import com.tomasps.slrummanager.domain.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class DashboardUiState(
    val jobs: List<Job> = emptyList(),
    val servers: List<Server> = emptyList(),
    val selectedServerId: String? = null,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    private val serverRepository: ServerRepository
) : ViewModel() {

    private val _selectedServerId = MutableStateFlow<String?>(null)
    private val _isRefreshing = MutableStateFlow(false)

    val uiState = combine(
        jobRepository.observeAll(),
        serverRepository.observeAll(),
        _selectedServerId,
        _isRefreshing
    ) { jobs, servers, selectedId, refreshing ->
        val filtered = if (selectedId == null) jobs
        else jobs.filter { it.serverId.toString() == selectedId }
        DashboardUiState(
            jobs = filtered.sortedWith(jobComparator),
            servers = servers,
            selectedServerId = selectedId,
            isRefreshing = refreshing
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun selectServer(serverId: String?) = _selectedServerId.update { serverId }

    private val jobComparator = compareBy<Job> { stateOrder(it.state) }.thenByDescending { it.submitTime }

    private fun stateOrder(state: JobState) = when (state) {
        JobState.RUNNING, JobState.COMPLETING -> 0
        JobState.PENDING -> 1
        JobState.COMPLETED -> 2
        else -> 3
    }
}
