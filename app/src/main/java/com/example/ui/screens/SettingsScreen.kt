package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemeSetting
import com.example.data.model.InterestPeriod
import com.example.data.model.InterestType
import com.example.ui.components.RestoreConfirmationDialog
import com.example.ui.viewmodel.MainViewModel
import com.example.util.AppStrings

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by viewModel.appLanguage.collectAsState()
    val theme by viewModel.appTheme.collectAsState()
    val isPinEnabled by viewModel.isPinLockEnabled.collectAsState()
    val defaultInterestType by viewModel.defaultInterestType.collectAsState()
    val defaultInterestPeriod by viewModel.defaultInterestPeriod.collectAsState()

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonInput by remember { mutableStateOf("") }
    var showPasteRestoreDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = AppStrings.get("settings_title", language),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Section 1: Appearance & Localization
        item {
            SettingsSectionHeader(title = "Appearance & Language")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Language
                    SettingsActionRow(
                        icon = Icons.Default.Translate,
                        title = "App Language",
                        subtitle = if (language == AppLanguage.ENGLISH) "English" else "বাংলা (Bengali)",
                        onClick = {
                            viewModel.setLanguage(if (language == AppLanguage.ENGLISH) AppLanguage.BENGALI else AppLanguage.ENGLISH)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    // Theme
                    SettingsActionRow(
                        icon = Icons.Default.Palette,
                        title = "Theme Mode",
                        subtitle = theme.name.lowercase().replaceFirstChar { it.uppercase() },
                        onClick = {
                            val next = when (theme) {
                                AppThemeSetting.SYSTEM -> AppThemeSetting.LIGHT
                                AppThemeSetting.LIGHT -> AppThemeSetting.DARK
                                AppThemeSetting.DARK -> AppThemeSetting.SYSTEM
                            }
                            viewModel.setTheme(next)
                        }
                    )
                }
            }
        }

        // Section 2: Security & App Lock
        item {
            SettingsSectionHeader(title = "Security & Protection")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("4-Digit PIN Lock", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Protect loan records with PIN", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Switch(
                            checked = isPinEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    showPinSetupDialog = true
                                } else {
                                    viewModel.setPin(false)
                                }
                            }
                        )
                    }

                    if (isPinEnabled) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                        TextButton(
                            onClick = { showPinSetupDialog = true },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Change PIN Code")
                        }
                    }
                }
            }
        }

        // Section 3: Loan Calculation Defaults
        item {
            SettingsSectionHeader(title = "Loan Calculation Defaults")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    SettingsActionRow(
                        icon = Icons.Default.Settings,
                        title = "Default Interest Type",
                        subtitle = defaultInterestType.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                        onClick = {
                            val next = when (defaultInterestType) {
                                InterestType.SIMPLE -> InterestType.COMPOUND
                                InterestType.COMPOUND -> InterestType.FLAT
                                InterestType.FLAT -> InterestType.REDUCING_BALANCE
                                InterestType.REDUCING_BALANCE -> InterestType.SIMPLE
                            }
                            viewModel.setDefaultInterestType(next)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    SettingsActionRow(
                        icon = Icons.Default.Settings,
                        title = "Default Interest Period",
                        subtitle = defaultInterestPeriod.name.lowercase().replaceFirstChar { it.uppercase() },
                        onClick = {
                            val next = when (defaultInterestPeriod) {
                                InterestPeriod.MONTHLY -> InterestPeriod.YEARLY
                                InterestPeriod.YEARLY -> InterestPeriod.DAILY
                                InterestPeriod.DAILY -> InterestPeriod.WEEKLY
                                InterestPeriod.WEEKLY -> InterestPeriod.MONTHLY
                            }
                            viewModel.setDefaultInterestPeriod(next)
                        }
                    )
                }
            }
        }

        // Section 4: Backup & Restore
        item {
            SettingsSectionHeader(title = "Backup & Data Export")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    SettingsActionRow(
                        icon = Icons.Default.Backup,
                        title = "Create JSON Backup",
                        subtitle = "Save a secure offline snapshot of all records",
                        onClick = { viewModel.createBackup(context) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    SettingsActionRow(
                        icon = Icons.Default.Restore,
                        title = "Restore from JSON",
                        subtitle = "Restore database from previous JSON file",
                        onClick = { showPasteRestoreDialog = true }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    SettingsActionRow(
                        icon = Icons.Default.FileDownload,
                        title = "Export All Transactions (CSV)",
                        subtitle = "Spreadsheet compatible CSV ledger",
                        onClick = { viewModel.exportTransactionsCsv(context) }
                    )
                }
            }
        }

        // Section 5: About App
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("SuDhan – Smart Interest & Lending Manager", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("“Your Money. Your Interest. Your Control.”", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Version 1.0.0 • 100% Offline & Private (Room Database)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Set PIN Dialog
    if (showPinSetupDialog) {
        var newPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinSetupDialog = false },
            title = { Text("Set 4-Digit Security PIN") },
            text = {
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it },
                    label = { Text("Enter 4 digits") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPin.length == 4) {
                            viewModel.setPin(true, newPin)
                            showPinSetupDialog = false
                        }
                    }
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinSetupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Paste / Enter Restore JSON Dialog
    if (showPasteRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showPasteRestoreDialog = false },
            title = { Text("Restore Database from Backup") },
            text = {
                Column {
                    Text("Paste your SuDhan JSON backup content below:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = restoreJsonInput,
                        onValueChange = { restoreJsonInput = it },
                        placeholder = { Text("{\"version\":1, \"appName\":\"SuDhan\", ...}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreJsonInput.isNotBlank()) {
                            showPasteRestoreDialog = false
                            showRestoreDialog = true
                        }
                    }
                ) {
                    Text("Next")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteRestoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Restore Warning Confirmation Dialog
    if (showRestoreDialog) {
        RestoreConfirmationDialog(
            onConfirm = {
                viewModel.restoreBackup(restoreJsonInput)
                showRestoreDialog = false
                restoreJsonInput = ""
            },
            onDismiss = { showRestoreDialog = false }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(text = subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
