package com.openwear.ui.health

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openwear.data.model.HeartRateEntity
import com.openwear.ui.components.*
import com.openwear.ui.theme.*
import com.openwear.viewmodel.HealthViewModel

@Composable
fun HealthScreen(
    viewModel: HealthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                    "Health",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Vitals overview
        item {
            VitalsOverviewCard(
                heartRate = uiState.liveHeartRate,
                spO2 = uiState.liveSpO2,
                minHR = uiState.minHR,
                maxHR = uiState.maxHR,
                avgHR = uiState.avgHR
            )
        }

        // Heart rate chart
        item {
            HeartRateChartCard(samples = uiState.hrSamples)
        }

        // SpO2 overview
        item {
            SpO2OverviewCard(
                current = uiState.liveSpO2,
                avgSpO2 = uiState.avgSpO2,
                samples = uiState.spO2Samples.map { it.percent.toFloat() }.reversed()
            )
        }

        // Health tips
        item {
            HealthStatusCard(
                heartRate = uiState.liveHeartRate,
                spO2 = uiState.liveSpO2
            )
        }
    }
}

@Composable
private fun VitalsOverviewCard(
    heartRate: Int?,
    spO2: Int?,
    minHR: Int,
    maxHR: Int,
    avgHR: Float
) {
    StatCard(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)) {
        SectionHeader(title = "Vitals")
        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Heart Rate big tile
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = CoralPrimary.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Favorite, null, tint = CoralPrimary, modifier = Modifier.size(16.dp))
                        if (heartRate != null) PulseDot(CoralPrimary)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = heartRate?.toString() ?: "--",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text("bpm", style = MaterialTheme.typography.labelSmall, color = CoralPrimary)
                    Spacer(Modifier.height(8.dp))
                    Text("Heart Rate", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // SpO2 big tile
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = BluePrimary.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.Air, null, tint = BluePrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = spO2?.let { "$it%" } ?: "--%",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text("SpO₂", style = MaterialTheme.typography.labelSmall, color = BluePrimary)
                    Spacer(Modifier.height(8.dp))
                    Text("Blood Oxygen", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (avgHR > 0) {
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HRStatItem("Min", if (minHR > 0) "$minHR" else "--", "bpm")
                VerticalDivider()
                HRStatItem("Avg", if (avgHR > 0) "${avgHR.toInt()}" else "--", "bpm")
                VerticalDivider()
                HRStatItem("Max", if (maxHR > 0) "$maxHR" else "--", "bpm")
            }
        }
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    )
}

@Composable
private fun HRStatItem(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface)
        Text(unit, style = MaterialTheme.typography.labelSmall, color = CoralPrimary)
    }
}

@Composable
private fun HeartRateChartCard(samples: List<HeartRateEntity>) {
    StatCard(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)) {
        SectionHeader(title = "Heart Rate (24h)")
        Spacer(Modifier.height(16.dp))
        if (samples.isNotEmpty()) {
            val values = samples.take(60).map { it.bpm.toFloat() }.reversed()
            val min = values.minOrNull()?.toInt() ?: 0
            val max = values.maxOrNull()?.toInt() ?: 0
            Sparkline(
                values = values,
                color = CoralPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Min: $min bpm", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Max: $max bpm", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                Text("Awaiting live heart rate packets...", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SpO2OverviewCard(current: Int?, avgSpO2: Float, samples: List<Float>) {
    StatCard(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(title = "Blood Oxygen (SpO₂)")
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = current?.let { "$it%" } ?: "--%",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text("Current", style = MaterialTheme.typography.labelSmall, color = BluePrimary)
            }
            if (avgSpO2 > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${"%.1f".format(avgSpO2)}%",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text("Avg (24h)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (samples.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Sparkline(
                values = samples,
                color = BluePrimary,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            )
        } else {
            Box(Modifier.fillMaxWidth().height(50.dp), contentAlignment = Alignment.Center) {
                Text("Measure SpO2 on watch to see history", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Normal range: 95–100%. Values below 90% may need attention.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HealthStatusCard(heartRate: Int?, spO2: Int?) {
    val status = when {
        heartRate == null && spO2 == null -> "Connect your watch to see health status"
        (heartRate ?: 0) > 100 -> "Elevated heart rate detected. Try relaxing."
        (spO2 ?: 100) < 90 -> "Low blood oxygen detected. Consider consulting a doctor."
        else -> "Your vitals are in the normal range. Keep it up!"
    }

    StatCard(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.HealthAndSafety, null, tint = GreenPrimary, modifier = Modifier.size(22.dp))
            Text("Health Status", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
