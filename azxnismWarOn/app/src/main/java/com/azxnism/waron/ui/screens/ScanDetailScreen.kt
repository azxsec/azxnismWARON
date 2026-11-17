package com.azxnism.waron.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azxnism.waron.data.model.Severity
import com.azxnism.waron.ui.components.FindingCard
import com.azxnism.waron.ui.components.SeverityBar
import com.azxnism.waron.ui.components.StatusBadge
import com.azxnism.waron.ui.theme.*
import com.azxnism.waron.ui.viewmodel.ScanDetailViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToReport: (Long) -> Unit,
    viewModel: ScanDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Navigate back after delete
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onNavigateBack()
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Scan", color = TextPrimary) },
            text = { Text("Are you sure you want to delete this scan and all its findings?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteScan()
                }) {
                    Text("Delete", color = AccentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text("Scan Results", fontWeight = FontWeight.Bold, color = TextPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextSecondary)
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Cyan500, strokeWidth = 3.dp)
            }
            return@Scaffold
        }

        val scan = uiState.scan
        if (scan == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Scan not found", color = TextSecondary, fontSize = 16.sp)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Scan info card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = scan.targetUrl,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusBadge(status = scan.status)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Duration & date
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, null, tint = TextTertiary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            val duration = if (scan.completedAt != null) {
                                val secs = (scan.completedAt - scan.startedAt) / 1000
                                "${secs / 60}m ${secs % 60}s"
                            } else "In progress"
                            Text(text = duration, color = TextTertiary, fontSize = 12.sp)

                            Spacer(modifier = Modifier.width(16.dp))

                            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                            Text(
                                text = sdf.format(Date(scan.startedAt)),
                                color = TextTertiary,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Severity bar
                        SeverityBar(
                            critical = scan.criticalCount,
                            high = scan.highCount,
                            medium = scan.mediumCount,
                            low = scan.lowCount,
                            info = scan.infoCount
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Severity counts row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            SeverityCount("C", scan.criticalCount, SeverityCritical)
                            SeverityCount("H", scan.highCount, SeverityHigh)
                            SeverityCount("M", scan.mediumCount, SeverityMedium)
                            SeverityCount("L", scan.lowCount, SeverityLow)
                            SeverityCount("I", scan.infoCount, SeverityInfo)
                        }
                    }
                }
            }

            // Generate Report button
            item {
                Button(
                    onClick = { onNavigateToReport(viewModel.scanId) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Cyan500,
                        contentColor = DarkBackground
                    )
                ) {
                    Icon(Icons.Default.Description, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Report", fontWeight = FontWeight.Bold)
                }
            }

            // Filter chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.FilterList, null, tint = TextTertiary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Filter", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = uiState.selectedSeverity == null,
                            onClick = { viewModel.filterBySeverity(null) },
                            label = { Text("All (${uiState.findings.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Cyan500.copy(alpha = 0.2f),
                                selectedLabelColor = Cyan400,
                                labelColor = TextSecondary,
                                containerColor = DarkCard
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = DarkBorder,
                                selectedBorderColor = Cyan500,
                                enabled = true,
                                selected = uiState.selectedSeverity == null
                            )
                        )
                    }
                    items(Severity.entries.toList()) { severity ->
                        val count = uiState.findings.count { it.severity == severity }
                        if (count > 0) {
                            val chipColor = when (severity) {
                                Severity.CRITICAL -> SeverityCritical
                                Severity.HIGH -> SeverityHigh
                                Severity.MEDIUM -> SeverityMedium
                                Severity.LOW -> SeverityLow
                                Severity.INFO -> SeverityInfo
                            }
                            FilterChip(
                                selected = uiState.selectedSeverity == severity,
                                onClick = { viewModel.filterBySeverity(severity) },
                                label = { Text("${severity.label} ($count)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = chipColor.copy(alpha = 0.2f),
                                    selectedLabelColor = chipColor,
                                    labelColor = TextSecondary,
                                    containerColor = DarkCard
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = DarkBorder,
                                    selectedBorderColor = chipColor,
                                    enabled = true,
                                    selected = uiState.selectedSeverity == severity
                                )
                            )
                        }
                    }
                }
            }

            // Findings list
            if (uiState.filteredFindings.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Shield, null, tint = AccentGreen, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (uiState.findings.isEmpty()) "No vulnerabilities found!"
                                else "No findings match the filter",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                items(
                    items = uiState.filteredFindings,
                    key = { it.id }
                ) { finding ->
                    FindingCard(
                        finding = finding,
                        isExpanded = uiState.expandedFindingIds.contains(finding.id),
                        onToggle = { viewModel.toggleFindingExpanded(finding.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SeverityCount(label: String, count: Int, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "$count", color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}
