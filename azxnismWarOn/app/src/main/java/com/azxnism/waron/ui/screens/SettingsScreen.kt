package com.azxnism.waron.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.auth.FirebaseAuth
import com.azxnism.waron.ui.theme.*
import com.azxnism.waron.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showResetDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    // Reset confirmation
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Settings", color = TextPrimary) },
            text = { Text("Reset all settings to default values?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    viewModel.resetDefaults()
                }) { Text("Reset", color = AccentOrange) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Sign out confirmation
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out", color = TextPrimary) },
            text = { Text("Are you sure you want to sign out?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutDialog = false
                    FirebaseAuth.getInstance().signOut()
                    onSignOut()
                }) { Text("Sign Out", color = AccentRed) }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
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
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextSecondary)
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
            // Scan Configuration
            item { SectionHeader(icon = Icons.Default.Settings, title = "SCAN CONFIGURATION") }

            item {
                SettingsCard {
                    // Timeout
                    SettingsSliderItem(
                        label = "Request Timeout",
                        value = uiState.timeout,
                        valueLabel = "${uiState.timeout}s",
                        range = 5f..120f,
                        onValueChange = { viewModel.updateTimeout(it.toInt()) }
                    )

                    HorizontalDivider(color = DarkBorder, thickness = 1.dp)

                    // Max Redirects
                    SettingsSliderItem(
                        label = "Max Redirects",
                        value = uiState.maxRedirects,
                        valueLabel = "${uiState.maxRedirects}",
                        range = 0f..20f,
                        onValueChange = { viewModel.updateMaxRedirects(it.toInt()) }
                    )

                    HorizontalDivider(color = DarkBorder, thickness = 1.dp)

                    // Thread Count
                    SettingsSliderItem(
                        label = "Thread Count",
                        value = uiState.threadCount,
                        valueLabel = "${uiState.threadCount}",
                        range = 1f..8f,
                        onValueChange = { viewModel.updateThreadCount(it.toInt()) }
                    )
                }
            }

            // User Agent
            item { SectionHeader(icon = Icons.Default.Security, title = "USER AGENT") }

            item {
                SettingsCard {
                    OutlinedTextField(
                        value = uiState.userAgent,
                        onValueChange = { viewModel.updateUserAgent(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = Cyan400,
                            focusedBorderColor = Cyan500,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = DarkCardElevated,
                            unfocusedContainerColor = DarkCardElevated
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // App settings
            item { SectionHeader(icon = Icons.Default.Timer, title = "APPLICATION") }

            item {
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Auto-save Reports", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Save reports automatically after scan", color = TextTertiary, fontSize = 12.sp)
                        }
                        Switch(
                            checked = uiState.autoSaveReports,
                            onCheckedChange = { viewModel.toggleAutoSave() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Cyan400,
                                checkedTrackColor = Cyan700.copy(alpha = 0.4f),
                                uncheckedThumbColor = TextTertiary,
                                uncheckedTrackColor = DarkBorder
                            )
                        )
                    }
                }
            }

            // Account section
            item { SectionHeader(icon = Icons.Default.Logout, title = "ACCOUNT") }

            item {
                val user = FirebaseAuth.getInstance().currentUser
                SettingsCard {
                    Column(modifier = Modifier.padding(14.dp)) {
                        if (user != null) {
                            Text(
                                text = user.displayName ?: "User",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = user.email ?: "",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { showSignOutDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
                            border = BorderStroke(1.dp, AccentRed.copy(alpha = 0.4f))
                        ) {
                            Icon(Icons.Default.Logout, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign Out", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // About & Reset
            item { SectionHeader(icon = Icons.Default.Info, title = "ABOUT") }

            item {
                SettingsCard {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("WarOn Scanner", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Version 1.0.0", color = TextSecondary, fontSize = 13.sp)
                        Text("by azxnism", color = Cyan400, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentOrange),
                    border = BorderStroke(1.dp, AccentOrange.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Default.RestartAlt, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset to Defaults", fontWeight = FontWeight.SemiBold)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        Icon(icon, null, tint = Cyan400, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingsSliderItem(
    label: String,
    value: Int,
    valueLabel: String,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(valueLabel, color = Cyan400, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Cyan400,
                activeTrackColor = Cyan500,
                inactiveTrackColor = DarkBorder
            )
        )
    }
}
