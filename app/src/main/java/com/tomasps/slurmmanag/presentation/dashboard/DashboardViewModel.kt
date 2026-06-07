package com.tomasps.slurmmanag.presentation.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
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

data class DashboardStats(
    val running: Int = 0,
    val pending: Int = 0,
    val completedToday: Int = 0,
    val failed: Int = 0,
)

data class DashboardUiState(
    val jobs: List<Job> = emptyList(),
    val servers: List<Server> = emptyList(),
    val selectedServerId: String? = null,
    val statusFilter: JobState? = null,
    val stats: DashboardStats = DashboardStats(),
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val jobRepository: JobRepository,
    private val serverRepository: ServerRepository
) : ViewModel() {

    private val _selectedServerId = MutableStateFlow<String?>(null)
    private val _statusFilter = MutableStateFlow<JobState?>(null)
    private val _isRefreshing = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState = combine(
        jobRepository.observeAll(),
        serverRepository.observeAll(),
        _selectedServerId,
        _statusFilter,
        combine(_isRefreshing, _errorMessage) { r, e -> r to e }
    ) { jobs, servers, selectedId, statusFilter, (refreshing, error) ->
        val todayCutoff = System.currentTimeMillis() - 86_400_000L
        val serverFiltered = if (selectedId == null) jobs
            else jobs.filter { it.serverId.toString() == selectedId }

        val displayed = serverFiltered
            .let { list -> if (statusFilter == null) list else list.filter { it.state == statusFilter } }
            .sortedWith(jobComparator)

        val stats = DashboardStats(
            running = serverFiltered.count { it.state.isActive },
            pending = serverFiltered.count { it.state == JobState.PENDING },
            completedToday = serverFiltered.count {
                it.state == JobState.COMPLETED && (it.endTime ?: 0L) > todayCutoff
            },
            failed = serverFiltered.count {
                it.state == JobState.FAILED || it.state == JobState.CANCELLED
            },
        )

        DashboardUiState(
            jobs = displayed,
            servers = servers,
            selectedServerId = selectedId,
            statusFilter = statusFilter,
            stats = stats,
            isRefreshing = refreshing,
            errorMessage = error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun selectServer(serverId: String?) = _selectedServerId.update { serverId }
    fun setStatusFilter(state: JobState?) = _statusFilter.update { state }

    fun refresh() {
        _isRefreshing.update { true }
        WorkManager.getInstance(context)
            .enqueue(OneTimeWorkRequestBuilder<PollWorker>().build())
        // isRefreshing resets when the DB flow emits new data (observed via uiState)
        viewModelScope.launch {
            uiState.collect { _isRefreshing.update { false } }
        }
    }

    fun clearError() = _errorMessage.update { null }

    private val jobComparator = compareBy<Job> { stateOrder(it.state) }.thenByDescending { it.submitTime }

    private fun stateOrder(state: JobState) = when (state) {
        JobState.RUNNING, JobState.COMPLETING -> 0
        JobState.PENDING -> 1
        JobState.COMPLETED -> 2
        else -> 3
    }
}
