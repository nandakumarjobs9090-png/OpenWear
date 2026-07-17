package com.openwear.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.openwear.ui.components.*
import com.openwear.ui.theme.*
import com.openwear.viewmodel.ActivityViewModel

@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel = hiltViewModel()
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
            // Header
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    "Fitness",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Today's stats
        item {
            TodayStepsCard(
                steps = uiState.todaySteps,
                goal = uiState.stepsGoal,
                calories = uiState.todayCalories,
                distanceKm = uiState.todayDistanceKm,
                activeMinutes = uiState.activeMinutes
            )
        }

        // Weekly chart
        item {
            WeeklyStepsCard(
                stepData = uiState.weeklyStepData,
                labels = uiState.weekLabels,
                goal = uiState.stepsGoal
            )
        }

        // Metrics grid
        item {
            MetricsGrid(
                calories = uiState.todayCalories,
                distanceKm = uiState.todayDistanceKm,
                activeMinutes = uiState.activeMinutes
            )
        }
    }
}

@Composable
private fun TodayStepsCard(
    steps: Int,
    goal: Int,
    calories: Int,
    distanceKm: Float,
    activeMinutes: Int
) {
    val progress = (steps.toFloat() / goal).coerceIn(0f, 1f)

    StatCard(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)) {
        SectionHeader(title = "Today's Steps")
        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            RingProgressIndicator(
                progress = progress,
                size = 180.dp,
                strokeWidth = 18.dp,
                primaryColor = GreenPrimary
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = steps.toString(),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "of $goal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StepSubStat(Icons.Default.LocalFireDepartment, "$calories", "Cal", OrangePrimary)
            StepSubStat(Icons.Default.Place, "${"%.2f".format(distanceKm)}", "km", TealPrimary)
            StepSubStat(Icons.Default.Timer, "$activeMinutes", "min", BluePrimary)
        }
    }
}

@Composable
private fun StepSubStat(icon: ImageVector, value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WeeklyStepsCard(
    stepData: List<Int>,
    labels: List<String>,
    goal: Int
) {
    StatCard(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)) {
        SectionHeader(title = "Last 7 Days")
        Spacer(Modifier.height(16.dp))
        if (stepData.isNotEmpty()) {
            WeeklyBarChart(
                values = stepData.map { it.toFloat() },
                labels = labels,
                barColor = GreenPrimary,
                goalLine = goal.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                labels.forEachIndexed { i, label ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            label.take(1),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Sync watch to see weekly data",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MetricsGrid(calories: Int, distanceKm: Float, activeMinutes: Int) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(title = "Key Metrics")
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MetricTile(
                label = "Calories",
                value = "$calories",
                unit = "kcal burned",
                icon = { Icon(Icons.Default.LocalFireDepartment, null, tint = OrangePrimary, modifier = Modifier.size(14.dp)) },
                color = OrangePrimary,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "Distance",
                value = "${"%.2f".format(distanceKm)}",
                unit = "kilometers",
                icon = { Icon(Icons.AutoMirrored.Filled.DirectionsWalk, null, tint = TealPrimary, modifier = Modifier.size(14.dp)) },
                color = TealPrimary,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MetricTile(
                label = "Active Time",
                value = "$activeMinutes",
                unit = "minutes",
                icon = { Icon(Icons.Default.Timer, null, tint = BluePrimary, modifier = Modifier.size(14.dp)) },
                color = BluePrimary,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "Goal",
                value = "8,000",
                unit = "steps target",
                icon = { Icon(Icons.Default.Flag, null, tint = GreenPrimary, modifier = Modifier.size(14.dp)) },
                color = GreenPrimary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
