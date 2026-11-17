package com.azxnism.waron.ui.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val timeout: Int = 30,
    val maxRedirects: Int = 5,
    val userAgent: String = "WarOn Scanner/1.0",
    val threadCount: Int = 2,
    val autoSaveReports: Boolean = true,
    val isLoading: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    companion object {
        val KEY_TIMEOUT = intPreferencesKey("scan_timeout")
        val KEY_MAX_REDIRECTS = intPreferencesKey("max_redirects")
        val KEY_USER_AGENT = stringPreferencesKey("user_agent")
        val KEY_THREAD_COUNT = intPreferencesKey("thread_count")
        val KEY_AUTO_SAVE = booleanPreferencesKey("auto_save_reports")
    }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val prefs = dataStore.data.first()
            _uiState.update {
                it.copy(
                    timeout = prefs[KEY_TIMEOUT] ?: 30,
                    maxRedirects = prefs[KEY_MAX_REDIRECTS] ?: 5,
                    userAgent = prefs[KEY_USER_AGENT] ?: "WarOn Scanner/1.0",
                    threadCount = prefs[KEY_THREAD_COUNT] ?: 2,
                    autoSaveReports = prefs[KEY_AUTO_SAVE] ?: true,
                    isLoading = false
                )
            }
        }
    }

    fun updateTimeout(value: Int) {
        val clamped = value.coerceIn(5, 120)
        _uiState.update { it.copy(timeout = clamped) }
        viewModelScope.launch {
            dataStore.edit { it[KEY_TIMEOUT] = clamped }
        }
    }

    fun updateMaxRedirects(value: Int) {
        val clamped = value.coerceIn(0, 20)
        _uiState.update { it.copy(maxRedirects = clamped) }
        viewModelScope.launch {
            dataStore.edit { it[KEY_MAX_REDIRECTS] = clamped }
        }
    }

    fun updateUserAgent(value: String) {
        _uiState.update { it.copy(userAgent = value) }
        viewModelScope.launch {
            dataStore.edit { it[KEY_USER_AGENT] = value }
        }
    }

    fun updateThreadCount(value: Int) {
        val clamped = value.coerceIn(1, 8)
        _uiState.update { it.copy(threadCount = clamped) }
        viewModelScope.launch {
            dataStore.edit { it[KEY_THREAD_COUNT] = clamped }
        }
    }

    fun toggleAutoSave() {
        val newValue = !_uiState.value.autoSaveReports
        _uiState.update { it.copy(autoSaveReports = newValue) }
        viewModelScope.launch {
            dataStore.edit { it[KEY_AUTO_SAVE] = newValue }
        }
    }

    fun resetDefaults() {
        _uiState.update {
            SettingsUiState(isLoading = false)
        }
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_TIMEOUT] = 30
                prefs[KEY_MAX_REDIRECTS] = 5
                prefs[KEY_USER_AGENT] = "WarOn Scanner/1.0"
                prefs[KEY_THREAD_COUNT] = 2
                prefs[KEY_AUTO_SAVE] = true
            }
        }
    }
}
