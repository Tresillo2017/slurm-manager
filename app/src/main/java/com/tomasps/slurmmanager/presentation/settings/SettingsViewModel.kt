package com.tomasps.slurmmanager.presentation.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DarkThemePreference { SYSTEM, LIGHT, DARK }

data class SettingsUiState(
    // Notifications
    val jobStateNotifs: Boolean = true,
    val alertNotifs: Boolean = true,
    val clusterNotifs: Boolean = true,
    val notifSound: Boolean = true,
    val notifVibration: Boolean = true,
    // Appearance
    val dynamicColor: Boolean = true,
    val darkTheme: DarkThemePreference = DarkThemePreference.SYSTEM,
    // Polling
    val defaultPollingInterval: Int = 15,
    val backgroundSync: Boolean = true,
    val batterySaver: Boolean = false,
    val amoledBlack: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    companion object {
        val KEY_JOB_NOTIFS = booleanPreferencesKey("job_state_notifs")
        val KEY_ALERT_NOTIFS = booleanPreferencesKey("alert_notifs")
        val KEY_CLUSTER_NOTIFS = booleanPreferencesKey("cluster_notifs")
        val KEY_NOTIF_SOUND = booleanPreferencesKey("notif_sound")
        val KEY_NOTIF_VIBRATION = booleanPreferencesKey("notif_vibration")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val KEY_DARK_THEME = stringPreferencesKey("dark_theme")
        val KEY_DEFAULT_POLLING = intPreferencesKey("default_polling_interval")
        val KEY_BACKGROUND_SYNC = booleanPreferencesKey("background_sync")
        val KEY_BATTERY_SAVER = booleanPreferencesKey("battery_saver")
        val KEY_AMOLED_BLACK = booleanPreferencesKey("amoled_black")
    }

    val state = dataStore.data.map { prefs ->
        SettingsUiState(
            jobStateNotifs = prefs[KEY_JOB_NOTIFS] != false,
            alertNotifs = prefs[KEY_ALERT_NOTIFS] != false,
            clusterNotifs = prefs[KEY_CLUSTER_NOTIFS] != false,
            notifSound = prefs[KEY_NOTIF_SOUND] != false,
            notifVibration = prefs[KEY_NOTIF_VIBRATION] != false,
            dynamicColor = prefs[KEY_DYNAMIC_COLOR] != false,
            darkTheme = prefs[KEY_DARK_THEME]?.let {
                runCatching { DarkThemePreference.valueOf(it) }.getOrNull()
            } ?: DarkThemePreference.SYSTEM,
            defaultPollingInterval = prefs[KEY_DEFAULT_POLLING] ?: 15,
            backgroundSync = prefs[KEY_BACKGROUND_SYNC] != false,
            batterySaver = prefs[KEY_BATTERY_SAVER] == true,
            amoledBlack = prefs[KEY_AMOLED_BLACK] == true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setJobStateNotifs(v: Boolean) = save { it[KEY_JOB_NOTIFS] = v }
    fun setAlertNotifs(v: Boolean) = save { it[KEY_ALERT_NOTIFS] = v }
    fun setClusterNotifs(v: Boolean) = save { it[KEY_CLUSTER_NOTIFS] = v }
    fun setNotifSound(v: Boolean) = save { it[KEY_NOTIF_SOUND] = v }
    fun setNotifVibration(v: Boolean) = save { it[KEY_NOTIF_VIBRATION] = v }
    fun setDynamicColor(v: Boolean) = save { it[KEY_DYNAMIC_COLOR] = v }
    fun setDarkTheme(v: DarkThemePreference) = save { it[KEY_DARK_THEME] = v.name }
    fun setDefaultPollingInterval(v: Int) = save { it[KEY_DEFAULT_POLLING] = v }
    fun setBackgroundSync(v: Boolean) = save { it[KEY_BACKGROUND_SYNC] = v }
    fun setBatterySaver(v: Boolean) = save { it[KEY_BATTERY_SAVER] = v }
    fun setAmoledBlack(v: Boolean) = save { it[KEY_AMOLED_BLACK] = v }

    private fun save(block: (MutablePreferences) -> Unit) =
        viewModelScope.launch { dataStore.edit(block) }
}
