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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class ReportUiState(
    val scan: ScanRecord? = null,
    val findings: List<Finding> = emptyList(),
    val reportText: String = "",
    val isLoading: Boolean = true,
    val isExporting: Boolean = false
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ScanRepository
) : ViewModel() {

    val scanId: Long = savedStateHandle.get<Long>("scanId") ?: 0L

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init {
        loadReport()
    }

    private fun loadReport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val data = repository.getScanWithFindings(scanId)
            if (data != null) {
                val report = generateReport(data)
                _uiState.update {
                    it.copy(
                        scan = data.scan,
                        findings = data.findings,
                        reportText = report,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun getShareText(): String = _uiState.value.reportText

    private fun generateReport(data: ScanWithFindings): String {
        val scan = data.scan
        val findings = data.findings
        val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale.getDefault())

        val critical = findings.count { it.severity == Severity.CRITICAL }
        val high = findings.count { it.severity == Severity.HIGH }
        val medium = findings.count { it.severity == Severity.MEDIUM }
        val low = findings.count { it.severity == Severity.LOW }
        val info = findings.count { it.severity == Severity.INFO }

        val duration = if (scan.completedAt != null) {
            val secs = (scan.completedAt - scan.startedAt) / 1000
            "${secs / 60}m ${secs % 60}s"
        } else "N/A"

        return buildString {
            appendLine("╔══════════════════════════════════════════╗")
            appendLine("║          WARON SCAN REPORT               ║")
            appendLine("╚══════════════════════════════════════════╝")
            appendLine()
            appendLine("═══ EXECUTIVE SUMMARY ═══")
            appendLine()
            appendLine("Target     : ${scan.targetUrl}")
            appendLine("Status     : ${scan.status.name}")
            appendLine("Started    : ${sdf.format(Date(scan.startedAt))}")
            if (scan.completedAt != null) {
                appendLine("Completed  : ${sdf.format(Date(scan.completedAt))}")
            }
            appendLine("Duration   : $duration")
            appendLine("Total Findings: ${findings.size}")
            appendLine()
            appendLine("═══ FINDINGS BY SEVERITY ═══")
            appendLine()
            appendLine("  🔴 Critical : $critical")
            appendLine("  🟠 High     : $high")
            appendLine("  🟡 Medium   : $medium")
            appendLine("  🟢 Low      : $low")
            appendLine("  🔵 Info     : $info")
            appendLine()

            if (findings.isNotEmpty()) {
                appendLine("═══ DETAILED FINDINGS ═══")
                appendLine()

                val sortedFindings = findings.sortedBy { it.severity.weight }.reversed()
                sortedFindings.forEachIndexed { index, finding ->
                    appendLine("─── Finding #${index + 1} ───")
                    appendLine("Title    : ${finding.title}")
                    appendLine("Severity : ${finding.severity.label}")
                    appendLine("Category : ${finding.category.label}")
                    if (finding.affectedUrl.isNotBlank()) {
                        appendLine("URL      : ${finding.affectedUrl}")
                    }
                    appendLine("Description:")
                    appendLine("  ${finding.description}")
                    if (finding.evidence.isNotBlank()) {
                        appendLine("Evidence:")
                        appendLine("  ${finding.evidence}")
                    }
                    if (finding.remediation.isNotBlank()) {
                        appendLine("Remediation:")
                        appendLine("  ${finding.remediation}")
                    }
                    appendLine()
                }

                appendLine("═══ REMEDIATION SUMMARY ═══")
                appendLine()
                val remediations = sortedFindings
                    .filter { it.remediation.isNotBlank() }
                    .distinctBy { it.category }
                if (remediations.isNotEmpty()) {
                    remediations.forEachIndexed { i, f ->
                        appendLine("${i + 1}. [${f.severity.label}] ${f.category.label}")
                        appendLine("   → ${f.remediation}")
                        appendLine()
                    }
                } else {
                    appendLine("No specific remediation steps recorded.")
                    appendLine()
                }
            } else {
                appendLine("No vulnerabilities found. Target looks clean!")
                appendLine()
            }

            appendLine("────────────────────────────────────────────")
            appendLine("Report generated by WarOn Scanner v1.0.0")
            appendLine("by azxnism")
        }
    }
}
