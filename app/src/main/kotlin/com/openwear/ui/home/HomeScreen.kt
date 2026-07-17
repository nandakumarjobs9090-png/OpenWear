package com.openwear.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openwear.ble.BleConnectionState
import com.openwear.ui.components.*
import com.openwear.ui.theme.*
import com.openwear.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigateToDevice: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val today = SimpleDateFormat("EEEE, MMMM d", Locale.US).format(Date())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Top Bar
        item {
            HomeTopBar(
                greeting = uiState.greetingText,
                dateText = today,
                isConnected = uiState.connectionState is BleConnectionState.Connected,
                battery = uiState.battery,
                onSettingsClick = onNavigateToSettings
            )
        }

        // Main Activity Ring + Stats
        item {
            MainActivityCard(uiState = uiState)
        }

        // Quick Stats Row
        item {
            QuickStatsRow(uiState = uiState)
        }

        // Heart Rate Card
        item {
            HeartRateCard(
                heartRate = uiState.heartRate,
                hrSamples = uiState.hrSamples
            )
        }

        // Weather Card
        if (uiState.weatherTemp != null) {
            item {
                WeatherCard(
                    temp = uiState.weatherTemp,
                    condition = uiState.weatherCondition,
                    emoji = uiState.weatherEmoji
                )
            }
        }

        // Sleep Card
        item {
            SleepSummaryCard(sleepHours = uiState.sleepHours)
        }

        // SpO2 Card — always show
        item {
            SpO2Card(percent = uiState.spO2)
        }

        // Weekly activity bar chart
        if (uiState.weeklySteps.isNotEmpty()) {
            item {
                WeeklyActivityCard(weeklySteps = uiState.weeklySteps)
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    greeting: String,
    dateText: String,
    isConnected: Boolean,
    battery: Int?,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = dateText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ConnectionBadge(isConnected = isConnected)
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun MainActivityCard(uiState: com.openwear.viewmodel.HomeUiState) {
    val progress = (uiState.steps.toFloat() / uiState.stepsGoal).coerceIn(0f, 1f)

    StatCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Ring
            RingProgressIndicator(
                progress = progress,
                size = 150.dp,
                strokeWidth = 16.dp,
                primaryColor = TealPrimary
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.steps.toString(),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "/ ${uiState.stepsGoal}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "steps",
                        style = MaterialTheme.typography.labelSmall,
                        color = TealPrimary
                    )
                }
            }

            // Right side stats
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(start = 24.dp)
            ) {
                SideStatItem(
                    icon = Icons.Default.LocalFireDepartment,
                    value = "${uiState.calories}",
                    label = "Cal",
                    color = OrangePrimary
                )
                SideStatItem(
                    icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                    value = "${"%.1f".format(uiState.distanceKm)}",
                    label = "km",
                    color = TealPrimary
                )
                SideStatItem(
                    icon = Icons.Default.Bedtime,
                    value = "${"%.1f".format(uiState.sleepHours)}h",
                    label = "Sleep",
                    color = PurplePrimary
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Progress bar
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
            color = TealPrimary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Text(
            text = "${(progress * 100).toInt()}% of daily goal",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun SideStatItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickStatsRow(uiState: com.openwear.viewmodel.HomeUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricTile(
            label = "Heart Rate",
            value = uiState.heartRate?.toString() ?: "--",
            unit = "bpm",
            icon = {
                Icon(Icons.Default.Favorite, null, tint = CoralPrimary, modifier = Modifier.size(14.dp))
            },
            color = CoralPrimary,
            modifier = Modifier.weight(1f)
        )
        MetricTile(
            label = "SpO₂",
            value = uiState.spO2?.let { "$it%" } ?: "--%",
            unit = "oxygen",
            icon = {
                Icon(Icons.Default.Air, null, tint = BluePrimary, modifier = Modifier.size(14.dp))
            },
            color = BluePrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HeartRateCard(
    heartRate: Int?,
    hrSamples: List<Int>
) {
    StatCard(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Favorite, null, tint = CoralPrimary, modifier = Modifier.size(20.dp))
                Text(
                    "Heart Rate",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (heartRate != null) PulseDot(color = CoralPrimary)
            }
            Text(
                text = "LIVE",
                style = MaterialTheme.typography.labelSmall,
                color = CoralPrimary
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = heartRate?.toString() ?: "--",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "bpm",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        if (hrSamples.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Sparkline(
                values = hrSamples.map { it.toFloat() },
                color = CoralPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            )
        }
    }
}

@Composable
private fun SleepSummaryCard(sleepHours: Float) {
    StatCard(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Bedtime, null, tint = PurplePrimary, modifier = Modifier.size(20.dp))
            Text("Last Night", style = MaterialTheme.typography.titleSmall)
        }
        Spacer(Modifier.height(8.dp))
        val hours = sleepHours.toInt()
        val minutes = ((sleepHours - hours) * 60).toInt()
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = if (sleepHours > 0) "${hours}h ${minutes}m" else "No data",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (sleepHours > 0) {
            val quality = when {
                sleepHours >= 7.5 -> "Good" to GreenPrimary
                sleepHours >= 6 -> "Fair" to OrangePrimary
                else -> "Poor" to CoralPrimary
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = quality.first,
                style = MaterialTheme.typography.labelMedium,
                color = quality.second
            )
        }
    }
}

@Composable
private fun SpO2Card(percent: Int?) {
    StatCard(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Air, null, tint = BluePrimary, modifier = Modifier.size(20.dp))
                Text("Blood Oxygen (SpO₂)", style = MaterialTheme.typography.titleSmall)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = percent?.toString() ?: "--",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "%",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        if (percent != null) {
            val status = when {
                percent >= 95 -> "Normal" to GreenPrimary
                percent >= 90 -> "Low" to OrangePrimary
                else -> "Very Low" to CoralPrimary
            }
            Text(status.first, style = MaterialTheme.typography.labelMedium, color = status.second)
        } else {
            Text("Measure on your watch", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WeeklyActivityCard(weeklySteps: List<com.openwear.data.model.WeeklyStepData>) {
    StatCard(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)) {
        SectionHeader(title = "This Week")
        Spacer(Modifier.height(16.dp))
        WeeklyBarChart(
            values = weeklySteps.map { it.steps.toFloat() },
            labels = weeklySteps.map { it.dayLabel },
            barColor = TealPrimary,
            goalLine = 8000f,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            weeklySteps.forEach { day ->
                Text(
                    text = day.dayLabel.take(1),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (day.goalMet) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WeatherCard(
    temp: Float?,
    condition: String?,
    emoji: String?
) {
    if (temp == null) return
    StatCard(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(emoji ?: "⛅", style = androidx.compose.ui.text.TextStyle(fontSize = 28.sp))
                Column {
                    Text("Weather", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(condition ?: "Cloudy", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                text = "${temp.toInt()}°C",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
