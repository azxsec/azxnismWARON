package com.azxnism.waron.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azxnism.waron.data.model.Finding
import com.azxnism.waron.data.model.ScanRecord
import com.azxnism.waron.data.model.ScanWithFindings
import com.azxnism.waron.data.model.Severity
import com.azxnism.waron.data.repository.ScanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanDetailUiState(
    val scan: ScanRecord? = null,
    val findings: List<Finding> = emptyList(),
    val filteredFindings: List<Finding> = emptyList(),
    val selectedSeverity: Severity? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val expandedFindingIds: Set<Long> = emptySet()
)

@HiltViewModel
class ScanDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ScanRepository
) : ViewModel() {

    val scanId: Long = savedStateHandle.get<Long>("scanId") ?: 0L

    private val _uiState = MutableStateFlow(ScanDetailUiState())
    val uiState: StateFlow<ScanDetailUiState> = _uiState.asStateFlow()

    init {
        loadScan()
    }

    private fun loadScan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val scanWithFindings = repository.getScanWithFindings(scanId)
            if (scanWithFindings != null) {
                _uiState.update {
                    it.copy(
                        scan = scanWithFindings.scan,
                        findings = scanWithFindings.findings,
                        filteredFindings = scanWithFindings.findings,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun filterBySeverity(severity: Severity?) {
        _uiState.update { state ->
            val filtered = if (severity == null) {
                state.findings
            } else {
                state.findings.filter { it.severity == severity }
            }
            state.copy(
                selectedSeverity = severity,
                filteredFindings = filtered
            )
        }
    }

    fun toggleFindingExpanded(findingId: Long) {
        _uiState.update { state ->
            val expanded = state.expandedFindingIds.toMutableSet()
            if (expanded.contains(findingId)) {
                expanded.remove(findingId)
            } else {
                expanded.add(findingId)
            }
            state.copy(expandedFindingIds = expanded)
        }
    }

    fun deleteScan() {
        viewModelScope.launch {
            val scan = _uiState.value.scan ?: return@launch
            repository.deleteScan(scan)
            _uiState.update { it.copy(isDeleted = true) }
        }
    }

    fun refresh() {
        loadScan()
    }
}
