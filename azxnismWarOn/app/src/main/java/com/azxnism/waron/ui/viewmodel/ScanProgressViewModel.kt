package com.azxnism.waron.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azxnism.waron.data.model.Finding
import com.azxnism.waron.data.model.ScanRecord
import com.azxnism.waron.data.model.ScanStatus
import com.azxnism.waron.data.model.Severity
import com.azxnism.waron.data.repository.ScanRepository
import com.azxnism.waron.scanner.ScanEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanProgressUiState(
    val scan: ScanRecord? = null,
    val findings: List<Finding> = emptyList(),
    val progress: Float = 0f,
    val completedModules: Int = 0,
    val totalModules: Int = 0,
    val currentModule: String = "",
    val isRunning: Boolean = false,
    val isCompleted: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ScanProgressViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ScanRepository,
    private val scanEngine: ScanEngine
) : ViewModel() {

    val scanId: Long = savedStateHandle.get<Long>("scanId") ?: 0L

    private val _uiState = MutableStateFlow(ScanProgressUiState())
    val uiState: StateFlow<ScanProgressUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null

    init {
        loadScan()
    }

    private fun loadScan() {
        viewModelScope.launch {
            val scan = repository.getScanById(scanId)
            if (scan != null) {
                _uiState.update { it.copy(scan = scan) }
                if (scan.status == ScanStatus.QUEUED) {
                    startScan(scan)
                }
            } else {
                _uiState.update { it.copy(errorMessage = "Scan not found") }
            }
        }
    }

    private fun startScan(scan: ScanRecord) {
        scanJob = viewModelScope.launch {
            _uiState.update { it.copy(isRunning = true) }

            // Update scan status to RUNNING
            val runningScan = scan.copy(status = ScanStatus.RUNNING)
            repository.updateScan(runningScan)
            _uiState.update { it.copy(scan = runningScan) }

            try {
                val findings = scanEngine.startScan(
                    targetUrl = scan.targetUrl,
                    modules = scan.enabledModules,
                    onProgress = { completed, total ->
                        val moduleNames = mapOf(
                            0 to "Header Security",
                            1 to "SSL/TLS",
                            2 to "Cookie Security",
                            3 to "CORS",
                            4 to "Info Disclosure",
                            5 to "Open Redirect"
                        )
                        _uiState.update {
                            it.copy(
                                completedModules = completed,
                                totalModules = total,
                                progress = if (total > 0) completed.toFloat() / total else 0f,
                                currentModule = if (completed < total) {
                                    moduleNames[completed] ?: "Module ${completed + 1}"
                                } else "Finalizing..."
                            )
                        }
                    },
                    onFinding = { finding ->
                        val savedFinding = finding.copy(scanId = scanId)
                        viewModelScope.launch {
                            repository.insertFinding(savedFinding)
                        }
                        _uiState.update {
                            it.copy(findings = it.findings + savedFinding)
                        }
                    }
                )

                // Count severities
                var critical = 0; var high = 0; var medium = 0; var low = 0; var info = 0
                findings.forEach { f ->
                    when (f.severity) {
                        Severity.CRITICAL -> critical++
                        Severity.HIGH -> high++
                        Severity.MEDIUM -> medium++
                        Severity.LOW -> low++
                        Severity.INFO -> info++
                    }
                }

                // Update scan as completed
                val completedScan = runningScan.copy(
                    status = ScanStatus.COMPLETED,
                    completedAt = System.currentTimeMillis(),
                    totalChecks = findings.size,
                    completedChecks = findings.size,
                    criticalCount = critical,
                    highCount = high,
                    mediumCount = medium,
                    lowCount = low,
                    infoCount = info
                )
                repository.updateScan(completedScan)

                _uiState.update {
                    it.copy(
                        scan = completedScan,
                        isRunning = false,
                        isCompleted = true,
                        progress = 1f,
                        currentModule = "Complete"
                    )
                }

            } catch (e: Exception) {
                val failedScan = runningScan.copy(
                    status = ScanStatus.FAILED,
                    completedAt = System.currentTimeMillis()
                )
                repository.updateScan(failedScan)

                _uiState.update {
                    it.copy(
                        scan = failedScan,
                        isRunning = false,
                        errorMessage = e.message ?: "Scan failed"
                    )
                }
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        scanJob = null
        viewModelScope.launch {
            val scan = _uiState.value.scan ?: return@launch
            val cancelledScan = scan.copy(
                status = ScanStatus.CANCELLED,
                completedAt = System.currentTimeMillis()
            )
            repository.updateScan(cancelledScan)
            _uiState.update {
                it.copy(
                    scan = cancelledScan,
                    isRunning = false,
                    currentModule = "Cancelled"
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
    }
}
