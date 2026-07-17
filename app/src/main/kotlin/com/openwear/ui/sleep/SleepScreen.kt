package com.openwear.ui.sleep

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openwear.ui.components.*
import com.openwear.ui.theme.*
import com.openwear.viewmodel.SleepViewModel

@Composable
fun SleepScreen(
    viewModel: SleepViewModel = hiltViewModel()
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
                    "Sleep",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        if (uiState.lastNightHours > 0f) {
            // Last night sleep arc + details
            item {
                LastNightCard(uiState = uiState)
            }

            // Sleep stages breakdown
            item {
                SleepStagesCard(uiState = uiState)
            }

            // Weekly sleep chart
            item {
                WeeklySleepCard(
                    sleepHours = uiState.weeklySleepHours,
                    labels = uiState.weekLabels,
                    avgHours = uiState.avgSleepHours
                )
            }
        } else {
            item {
                SleepEmptyStateCard()
            }
        }

        // Sleep tips
        item {
            SleepTipsCard()
        }
    }
}

@Composable
private fun SleepEmptyStateCard() {
    StatCard(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(Icons.Default.Bedtime, null, tint = PurplePrimary, modifier = Modifier.size(22.dp))
            Text("Sleep Tracking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Text(
            text = "No sleep data has been synchronized yet.",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Because OpenWear is an open-source companion app built from reverse-engineering, some data formats (like multi-stage sleep logs) require active capture to decode.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "How to sync sleep data:",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("• Wear your watch to sleep tonight.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("• Keep the app running in the background.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("• Export the CSV logs tomorrow if sleep data doesn't load!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LastNightCard(uiState: com.openwear.viewmodel.SleepUiState) {
    StatCard(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)) {
        SectionHeader(title = "Last Night")
        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            SleepArcDial(
                totalHours = uiState.lastNightHours,
                deepHours = uiState.lastNightDeepHours,
                lightHours = uiState.lastNightLightHours,
                remHours = uiState.lastNightRemHours,
                awakeHours = uiState.lastNightAwakeHours,
                size = 220.dp
            )
        }

        Spacer(Modifier.height(16.dp))

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StageLegend("Deep", PurplePrimary)
            StageLegend("Light", TealPrimary)
            StageLegend("REM", BluePrimary)
            StageLegend("Awake", CoralPrimary.copy(alpha = 0.6f))
        }

        if (uiState.sleepScore > 0) {
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ScoreStat("Sleep Score", uiState.sleepScore.toString(), PurplePrimary)
                ScoreStat(
                    "Efficiency",
                    "${(uiState.sleepEfficiency * 100).toInt()}%",
                    TealPrimary
                )
            }
        }
    }
}

@Composable
private fun StageLegend(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ScoreStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SleepStagesCard(uiState: com.openwear.viewmodel.SleepUiState) {
    StatCard(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)) {
        SectionHeader(title = "Sleep Stages")
        Spacer(Modifier.height(16.dp))

        SleepStageRow("Deep Sleep", uiState.lastNightDeepHours, uiState.lastNightHours, PurplePrimary, Icons.Default.DarkMode)
        Spacer(Modifier.height(12.dp))
        SleepStageRow("Light Sleep", uiState.lastNightLightHours, uiState.lastNightHours, TealPrimary, Icons.Default.WbTwilight)
        Spacer(Modifier.height(12.dp))
        SleepStageRow("REM Sleep", uiState.lastNightRemHours, uiState.lastNightHours, BluePrimary, Icons.Default.Visibility)
        Spacer(Modifier.height(12.dp))
        SleepStageRow("Awake", uiState.lastNightAwakeHours, uiState.lastNightHours, CoralPrimary.copy(alpha = 0.6f), Icons.Default.WbSunny)

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            val h = uiState.lastNightHours.toInt()
            val m = ((uiState.lastNightHours - h) * 60).toInt()
            Text("${h}h ${m}m", style = MaterialTheme.typography.titleSmall, color = PurplePrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SleepStageRow(
    name: String,
    hours: Float,
    totalHours: Float,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val fraction = if (totalHours > 0) (hours / totalHours).coerceIn(0f, 1f) else 0f
    val h = hours.toInt()
    val m = ((hours - h) * 60).toInt()

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                Text(name, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = if (hours > 0) "${h}h ${m}m" else "--",
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(50)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun WeeklySleepCard(
    sleepHours: List<Float>,
    labels: List<String>,
    avgHours: Float
) {
    StatCard(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(title = "Last 7 Days", modifier = Modifier.weight(1f))
            if (avgHours > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    val h = avgHours.toInt()
                    val m = ((avgHours - h) * 60).toInt()
                    Text(
                        text = "${h}h ${m}m avg",
                        style = MaterialTheme.typography.labelMedium,
                        color = PurplePrimary
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (sleepHours.isNotEmpty()) {
            WeeklyBarChart(
                values = sleepHours,
                labels = labels,
                barColor = PurplePrimary,
                goalLine = 8f, // 8 hours goal
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                labels.forEach { label ->
                    Text(label.take(1), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                Text("No sleep data yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SleepTipsCard() {
    StatCard(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Lightbulb, null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
            Text("Sleep Tips", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
        listOf(
            "Aim for 7–9 hours per night for optimal recovery",
            "Keep a consistent bedtime and wake time",
            "Avoid screens 30 minutes before sleep"
        ).forEach { tip ->
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Circle, null, tint = PurplePrimary, modifier = Modifier.size(8.dp).padding(top = 4.dp))
                Text(tip, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
