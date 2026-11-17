package com.azxnism.waron.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azxnism.waron.ui.components.SeverityBadge
import com.azxnism.waron.ui.theme.*
import com.azxnism.waron.ui.viewmodel.ScanProgressViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanProgressScreen(
    onScanComplete: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ScanProgressViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val animatedProgress by animateFloatAsState(
        targetValue = uiState.progress,
        animationSpec = tween(durationMillis = 400),
        label = "progress"
    )

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isCompleted) "Scan Complete"
                        else if (uiState.isRunning) "Scanning…"
                        else if (uiState.errorMessage != null) "Scan Failed"
                        else "Preparing…",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Progress card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Status icon
                        Box(contentAlignment = Alignment.Center) {
                            if (uiState.isRunning) {
                                CircularProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier.size(100.dp),
                                    color = Cyan400,
                                    trackColor = DarkBorder,
                                    strokeWidth = 6.dp
                                )
                            }

                            val icon = when {
                                uiState.isCompleted -> Icons.Default.CheckCircle
                                uiState.errorMessage != null -> Icons.Default.Error
                                uiState.isRunning -> null
                                else -> null
                            }
                            val iconColor = when {
                                uiState.isCompleted -> AccentGreen
                                uiState.errorMessage != null -> AccentRed
                                else -> Cyan400
                            }

                            if (icon != null) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = iconColor,
                                    modifier = Modifier.size(64.dp)
                                )
                            } else if (uiState.isRunning) {
                                Text(
                                    text = "${(animatedProgress * 100).toInt()}%",
                                    color = Cyan400,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    color = Cyan400,
                                    strokeWidth = 3.dp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Target URL
                        uiState.scan?.let { scan ->
                            Text(
                                text = scan.targetUrl,
                                color = TextSecondary,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Current module
                        if (uiState.currentModule.isNotBlank()) {
                            Text(
                                text = uiState.currentModule,
                                color = Cyan400,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Module progress
                        if (uiState.totalModules > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${uiState.completedModules} / ${uiState.totalModules} modules",
                                color = TextTertiary,
                                fontSize = 12.sp
                            )
                        }

                        // Error message
                        uiState.errorMessage?.let { error ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = error,
                                color = AccentRed,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Linear progress bar
                        if (uiState.isRunning) {
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = Cyan400,
                                trackColor = DarkBorder
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action buttons
                        if (uiState.isRunning) {
                            OutlinedButton(
                                onClick = { viewModel.cancelScan() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = AccentRed
                                ),
                                border = BorderStroke(1.dp, AccentRed.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cancel Scan", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (uiState.isCompleted) {
                            Button(
                                onClick = { onScanComplete(viewModel.scanId) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Cyan500,
                                    contentColor = DarkBackground
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("View Results", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Live findings header
            if (uiState.findings.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Live Findings",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${uiState.findings.size} found",
                            color = Cyan400,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                items(
                    items = uiState.findings.reversed(),
                    key = { "${it.scanId}_${it.title}_${it.foundAt}" }
                ) { finding ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        border = BorderStroke(1.dp, DarkBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SeverityBadge(severity = finding.severity)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = finding.title,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = finding.category.label,
                                    color = TextTertiary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
