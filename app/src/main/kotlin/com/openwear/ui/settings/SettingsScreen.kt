package com.openwear.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openwear.ui.components.StatCard
import com.openwear.ui.theme.*
import com.openwear.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val stepsGoal by viewModel.stepsGoal.collectAsState()
    val heightCm by viewModel.heightCm.collectAsState()
    val weightKg by viewModel.weightKg.collectAsState()
    val isMetric by viewModel.isMetric.collectAsState()
    val notifySteps by viewModel.notifySteps.collectAsState()
    val notifyHr by viewModel.notifyHr.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    "Settings",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Personalise your OpenWear experience",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Activity Goals
        item {
            SettingsSection(title = "Activity Goals", icon = Icons.Default.Flag, tint = TealPrimary) {
                NumberStepSetting(
                    label = "Daily Steps Goal",
                    value = stepsGoal,
                    step = 500,
                    min = 1000,
                    max = 30000,
                    unit = "steps",
                    onValueChange = viewModel::setStepsGoal
                )
            }
        }

        // User Profile
        item {
            SettingsSection(title = "User Profile", icon = Icons.Default.Person, tint = BluePrimary) {
                Text(
                    "Used to calculate calories burned and distance walked accurately.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                NumberStepSetting(
                    label = "Height",
                    value = heightCm,
                    step = 1,
                    min = 100,
                    max = 250,
                    unit = "cm",
                    onValueChange = viewModel::setHeightCm
                )
                Spacer(Modifier.height(12.dp))
                NumberStepSetting(
                    label = "Weight",
                    value = weightKg,
                    step = 1,
                    min = 30,
                    max = 200,
                    unit = "kg",
                    onValueChange = viewModel::setWeightKg
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Units", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            if (isMetric) "Metric (km, °C)" else "Imperial (mi, °F)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isMetric,
                        onCheckedChange = viewModel::setIsMetric,
                        colors = SwitchDefaults.colors(checkedThumbColor = TealPrimary, checkedTrackColor = TealPrimary.copy(alpha = 0.4f))
                    )
                }
            }
        }

        // Notifications
        item {
            SettingsSection(title = "Notifications", icon = Icons.Default.Notifications, tint = OrangePrimary) {
                ToggleSetting(
                    label = "Daily Step Goal Alert",
                    description = "Notify when you reach your step goal",
                    checked = notifySteps,
                    onCheckedChange = viewModel::setNotifySteps
                )
                Spacer(Modifier.height(12.dp))
                ToggleSetting(
                    label = "Heart Rate Alert",
                    description = "Notify on abnormal heart rate readings",
                    checked = notifyHr,
                    onCheckedChange = viewModel::setNotifyHr
                )
            }
        }

        // App Info
        item {
            SettingsSection(title = "About OpenWear", icon = Icons.Default.Info, tint = PurplePrimary) {
                InfoRow("Version", "1.0.0-alpha")
                InfoRow("Watch Driver", "DIZO Watch R")
                InfoRow("Protocol", "BLE GATT (Vendor FEA1)")
                InfoRow("License", "Apache 2.0")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Open-source companion app for abandoned smartwatches.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    content: @Composable ColumnScope.() -> Unit
) {
    StatCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 14.dp)
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        content()
    }
}

@Composable
private fun NumberStepSetting(
    label: String,
    value: Int,
    step: Int,
    min: Int,
    max: Int,
    unit: String,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                "$value $unit",
                style = MaterialTheme.typography.bodySmall,
                color = TealPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = { if (value - step >= min) onValueChange(value - step) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp))
            }
            FilledTonalIconButton(
                onClick = { if (value + step <= max) onValueChange(value + step) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun ToggleSetting(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = TealPrimary, checkedTrackColor = TealPrimary.copy(alpha = 0.4f))
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}
