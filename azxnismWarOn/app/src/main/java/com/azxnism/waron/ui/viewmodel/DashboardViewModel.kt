package com.azxnism.waron.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azxnism.waron.data.model.DashboardStats
import com.azxnism.waron.data.model.ScanRecord
import com.azxnism.waron.data.repository.ScanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val stats: DashboardStats = DashboardStats(),
    val recentScans: List<ScanRecord> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: ScanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            launch {
                repository.getDashboardStats().collect { stats ->
                    _uiState.update {
                        it.copy(stats = stats, isLoading = false)
                    }
                }
            }
            launch {
                repository.getRecentScans(5).collect { scans ->
                    _uiState.update {
                        it.copy(recentScans = scans, isLoading = false)
                    }
                }
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        loadDashboard()
    }
}
