package com.tomasps.slrummanager.presentation.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Context.dataStore by preferencesDataStore("settings")

data class SettingsUiState(
    val jobStateNotifs: Boolean = true,
    val alertNotifs: Boolean = true,
    val clusterNotifs: Boolean = true,
    val dynamicColor: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val KEY_JOB_NOTIFS = booleanPreferencesKey("job_state_notifs")
    private val KEY_ALERT_NOTIFS = booleanPreferencesKey("alert_notifs")
    private val KEY_CLUSTER_NOTIFS = booleanPreferencesKey("cluster_notifs")
    private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")

    val state = context.dataStore.data.map { prefs ->
        SettingsUiState(
            jobStateNotifs = prefs[KEY_JOB_NOTIFS] != false,
            alertNotifs = prefs[KEY_ALERT_NOTIFS] != false,
            clusterNotifs = prefs[KEY_CLUSTER_NOTIFS] != false,
            dynamicColor = prefs[KEY_DYNAMIC_COLOR] != false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setJobStateNotifs(v: Boolean) = viewModelScope.launch { context.dataStore.edit { it[KEY_JOB_NOTIFS] = v } }
    fun setAlertNotifs(v: Boolean) = viewModelScope.launch { context.dataStore.edit { it[KEY_ALERT_NOTIFS] = v } }
    fun setClusterNotifs(v: Boolean) = viewModelScope.launch { context.dataStore.edit { it[KEY_CLUSTER_NOTIFS] = v } }
    fun setDynamicColor(v: Boolean) = viewModelScope.launch { context.dataStore.edit { it[KEY_DYNAMIC_COLOR] = v } }
}
