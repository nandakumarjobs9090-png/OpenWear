package com.openwear.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Google Fit–style circular ring progress indicator.
 * Shows progress as an arc from top, with animated fill.
 */
@Composable
fun RingProgressIndicator(
    progress: Float, // 0.0f to 1.0f
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    strokeWidth: Dp = 14.dp,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    centerContent: @Composable BoxScope.() -> Unit = {}
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
        label = "ring_progress"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(
                this.size.width - strokePx,
                this.size.height - strokePx
            )
            val topLeft = Offset(inset, inset)

            // Track (background arc)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Progress arc
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.7f),
                        primaryColor
                    ),
                    center = Offset(this.size.width / 2f, this.size.height / 2f)
                ),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Box(content = centerContent)
    }
}

/**
 * A stat card in Google Fit card style.
 */
@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

/**
 * Google Fit–style metric tile (small 2-col tile).
 */
@Composable
fun MetricTile(
    label: String,
    value: String,
    unit: String,
    icon: @Composable () -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                icon()
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Animated live heart rate pulse dot.
 */
@Composable
fun PulseDot(color: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    Canvas(modifier = modifier.size(10.dp)) {
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = (size.minDimension / 2f) * scale
        )
    }
}

/**
 * Section header with optional action button.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Weekly bar chart rendered with Canvas (no Vico dependency for simple usage).
 */
@Composable
fun WeeklyBarChart(
    values: List<Float>,
    labels: List<String>,
    barColor: Color,
    goalLine: Float? = null,
    modifier: Modifier = Modifier
) {
    val animatedValues = values.map { value ->
        animateFloatAsState(
            targetValue = value,
            animationSpec = tween(800, easing = EaseOutCubic),
            label = "bar_$value"
        ).value
    }

    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas

        val maxVal = (values.maxOrNull() ?: 1f).coerceAtLeast(1f)
        val barWidth = size.width / (values.size * 2f)
        val spacing = barWidth
        val chartHeight = size.height - 30.dp.toPx()

        values.forEachIndexed { i, _ ->
            val barHeight = (animatedValues.getOrNull(i) ?: 0f) / maxVal * chartHeight
            val left = i * (barWidth + spacing) + spacing / 2f
            val top = chartHeight - barHeight

            // Bar background
            drawRoundRect(
                color = barColor.copy(alpha = 0.15f),
                topLeft = Offset(left, 0f),
                size = Size(barWidth, chartHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
            )

            // Filled bar
            if (barHeight > 0f) {
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                )
            }
        }

        // Goal line
        goalLine?.let {
            val lineY = chartHeight - (it / maxVal * chartHeight)
            drawLine(
                color = barColor.copy(alpha = 0.5f),
                start = Offset(0f, lineY),
                end = Offset(size.width, lineY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
            )
        }
    }
}

/**
 * Sleep arc similar to Google Fit sleep circle.
 */
@Composable
fun SleepArcDial(
    totalHours: Float,
    deepHours: Float,
    lightHours: Float,
    remHours: Float,
    awakeHours: Float,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp
) {
    val maxHours = 12f
    val animatedTotal by animateFloatAsState(
        targetValue = (totalHours / maxHours).coerceIn(0f, 1f),
        animationSpec = tween(1200, easing = EaseOutCubic),
        label = "sleep_arc"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = 18.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val offset = Offset(inset, inset)

            // Track
            drawArc(
                color = Color(0xFF7C4DFF).copy(alpha = 0.15f),
                startAngle = 150f, sweepAngle = 240f,
                useCenter = false, topLeft = offset, size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )

            // Deep sleep (purple)
            val deepSweep = 240f * (deepHours / maxHours.coerceAtLeast(1f)) * animatedTotal
            drawArc(
                color = Color(0xFF7C4DFF),
                startAngle = 150f, sweepAngle = deepSweep,
                useCenter = false, topLeft = offset, size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )

            // Light sleep (teal)
            val lightSweep = 240f * (lightHours / maxHours.coerceAtLeast(1f)) * animatedTotal
            drawArc(
                color = Color(0xFF00BFA5),
                startAngle = 150f + deepSweep, sweepAngle = lightSweep,
                useCenter = false, topLeft = offset, size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )

            // REM (blue)
            val remSweep = 240f * (remHours / maxHours.coerceAtLeast(1f)) * animatedTotal
            drawArc(
                color = Color(0xFF448AFF),
                startAngle = 150f + deepSweep + lightSweep, sweepAngle = remSweep,
                useCenter = false, topLeft = offset, size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val hours = totalHours.toInt()
            val minutes = ((totalHours - hours) * 60).toInt()
            Text(
                text = "${hours}h",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = "${minutes}m",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

/**
 * Connection status badge.
 */
@Composable
fun ConnectionBadge(isConnected: Boolean, modifier: Modifier = Modifier) {
    val color = if (isConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline
    val label = if (isConnected) "Connected" else "Disconnected"

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (isConnected) {
            PulseDot(color = color)
        } else {
            Canvas(Modifier.size(8.dp)) {
                drawCircle(color = color)
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

/**
 * Sparkline mini chart (for heart rate preview on home card).
 */
@Composable
fun Sparkline(
    values: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val max = values.maxOrNull() ?: 1f
        val min = values.minOrNull() ?: 0f
        val range = (max - min).coerceAtLeast(1f)

        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i / (values.size - 1f) * size.width
            val y = size.height - ((v - min) / range * size.height)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
