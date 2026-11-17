package com.azxnism.waron.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azxnism.waron.data.repository.ScanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanModule(
    val key: String,
    val name: String,
    val description: String,
    val isEnabled: Boolean = true
)

data class NewScanUiState(
    val targetUrl: String = "",
    val modules: List<ScanModule> = defaultModules(),
    val isValid: Boolean = false,
    val errorMessage: String? = null,
    val isStarting: Boolean = false,
    val startedScanId: Long? = null
)

private fun defaultModules() = listOf(
    ScanModule("header_security", "Header Security", "Check HTTP security headers"),
    ScanModule("ssl_tls", "SSL/TLS", "Analyze certificate & encryption"),
    ScanModule("cookie_security", "Cookie Security", "Inspect cookie flags & attributes"),
    ScanModule("cors", "CORS", "Test cross-origin policies"),
    ScanModule("info_disclosure", "Info Disclosure", "Detect leaked server information"),
    ScanModule("redirect", "Open Redirect", "Find unvalidated redirects")
)

@HiltViewModel
class NewScanViewModel @Inject constructor(
    private val repository: ScanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewScanUiState())
    val uiState: StateFlow<NewScanUiState> = _uiState.asStateFlow()

    fun updateUrl(url: String) {
        _uiState.update {
            it.copy(
                targetUrl = url,
                isValid = validateUrl(url) && it.modules.any { m -> m.isEnabled },
                errorMessage = null
            )
        }
    }

    fun toggleModule(key: String) {
        _uiState.update { state ->
            val updated = state.modules.map { module ->
                if (module.key == key) module.copy(isEnabled = !module.isEnabled)
                else module
            }
            state.copy(
                modules = updated,
                isValid = validateUrl(state.targetUrl) && updated.any { it.isEnabled }
            )
        }
    }

    fun startScan() {
        val state = _uiState.value
        if (!validateUrl(state.targetUrl)) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid URL") }
            return
        }

        val enabledModules = state.modules.filter { it.isEnabled }.map { it.key }
        if (enabledModules.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Select at least one module") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isStarting = true, errorMessage = null) }
            try {
                val scanId = repository.createScan(state.targetUrl, enabledModules)
                _uiState.update {
                    it.copy(isStarting = false, startedScanId = scanId)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isStarting = false,
                        errorMessage = e.message ?: "Failed to start scan"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetStartedScanId() {
        _uiState.update { it.copy(startedScanId = null) }
    }

    private fun validateUrl(url: String): Boolean {
        if (url.isBlank()) return false
        val trimmed = url.trim()
        val withScheme = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            "https://$trimmed"
        } else {
            trimmed
        }
        return try {
            val parsed = java.net.URL(withScheme)
            !parsed.host.isNullOrBlank() && !parsed.host.contains(" ")
        } catch (e: Exception) {
            false
        }
    }
}
