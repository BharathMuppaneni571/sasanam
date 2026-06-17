package com.bh571.sasanam.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    biometricEnabled: Boolean,
    onBiometricToggle: (Boolean) -> Unit,
    primaryColorHex: String,
    onColorSelected: (String) -> Unit,
    onClearData: () -> Unit
) {
    var darkModeEnabled by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var customHexCode by remember { mutableStateOf(primaryColorHex) }

    val presetColors = listOf("#673AB7", "#3F51B5", "#2196F3", "#009688", "#4CAF50", "#FFC107", "#FF5722", "#F44336", "#E91E63")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSection(title = "Appearance") {
                SettingsToggleItem(
                    icon = Icons.Default.DarkMode,
                    title = "Dark Mode",
                    subtitle = "Use a darker theme for the app",
                    checked = darkModeEnabled,
                    onCheckedChange = { darkModeEnabled = it }
                )
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = "Theme Color",
                    subtitle = "Change the primary color of the app",
                    onClick = { showColorPicker = true }
                )
                // Show current color indicator
                Box(
                    modifier = Modifier
                        .padding(start = 56.dp, bottom = 16.dp)
                        .size(40.dp)
                        .background(
                            color = try { Color(android.graphics.Color.parseColor(primaryColorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary },
                            shape = CircleShape
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
            }

            SettingsSection(title = "Privacy & Security") {
                SettingsToggleItem(
                    icon = Icons.Default.Fingerprint,
                    title = "Biometric Lock",
                    subtitle = "Require fingerprint to open Sasanam",
                    checked = biometricEnabled,
                    onCheckedChange = onBiometricToggle
                )
                SettingsItem(
                    icon = Icons.Default.Key,
                    title = "Encryption Key",
                    subtitle = "Manage your database encryption key",
                    onClick = { /* TODO */ }
                )
            }

            SettingsSection(title = "Data Management") {
                SettingsItem(
                    icon = Icons.Default.Download,
                    title = "Export Data",
                    subtitle = "Download all memories as JSON",
                    onClick = { /* TODO */ }
                )
                SettingsItem(
                    icon = Icons.Default.DeleteForever,
                    title = "Clear All Data",
                    subtitle = "Permanently delete all stored memories",
                    color = MaterialTheme.colorScheme.error,
                    onClick = { showClearDataDialog = true }
                )
            }

            SettingsSection(title = "About") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Version",
                    subtitle = "Sasanam 1.0.2 (2026-06-16)",
                    onClick = { /* TODO */ }
                )
                SettingsItem(
                    icon = Icons.Default.Help,
                    title = "Help & Support",
                    subtitle = "FAQs and contact information",
                    onClick = { /* TODO */ }
                )
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showColorPicker) {
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text("Select Theme Color") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Presets", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        presetColors.take(5).forEach { hex ->
                            ColorItem(hex = hex) {
                                onColorSelected(hex)
                                showColorPicker = false
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        presetColors.drop(5).forEach { hex ->
                            ColorItem(hex = hex) {
                                onColorSelected(hex)
                                showColorPicker = false
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    Text("Custom Hex Code", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        value = customHexCode,
                        onValueChange = { 
                            customHexCode = it
                            if (it.length == 7 && it.startsWith("#")) {
                                try {
                                    android.graphics.Color.parseColor(it)
                                    onColorSelected(it)
                                } catch (e: Exception) {}
                            }
                        },
                        placeholder = { Text("#673AB7") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorPicker = false }) {
                    Text("Done")
                }
            }
        )
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear All Data?") },
            text = { Text("This action cannot be undone. All your offline memories and extracted facts will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = { 
                        onClearData()
                        showClearDataDialog = false 
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
        HorizontalDivider(modifier = Modifier.padding(top = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun ColorItem(hex: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(44.dp)
            .clickable { onClick() },
        shape = CircleShape,
        color = Color(android.graphics.Color.parseColor(hex)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {}
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (color == MaterialTheme.colorScheme.onSurface) MaterialTheme.colorScheme.onSurfaceVariant else color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = color)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
