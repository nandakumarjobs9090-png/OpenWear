package com.openwear.ui.debug

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openwear.ble.BlePacket
import com.openwear.ble.PacketLogger
import com.openwear.data.model.ExportFormat
import com.openwear.ui.components.StatCard
import com.openwear.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val packetLogger: PacketLogger
) : ViewModel() {
    val packets: StateFlow<List<BlePacket>> = packetLogger.packets
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val isCapturing: StateFlow<Boolean> = packetLogger.isCapturing
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun clearLog() = packetLogger.clearLog()

    fun exportJson(): File? = packetLogger.exportLog(ExportFormat.JSON)
    fun exportCsv(): File? = packetLogger.exportLog(ExportFormat.CSV)
    fun toggleCapture() {
        viewModelScope.launch {
            if (isCapturing.value) packetLogger.stopCapture()
            else packetLogger.startCapture()
        }
    }
}

// UUID → display color mapping
private fun charColor(uuid: String): Color = when {
    uuid.contains("fea1") -> Color(0xFF7C4DFF)   // FEA1 = purple
    uuid.contains("ff03") -> Color(0xFF00BFA5)   // FF03 = teal
    uuid.contains("ff02") -> Color(0xFFFF6D6D)   // FF02 = coral (write)
    uuid.contains("ff04") -> Color(0xFF448AFF)   // FF04 = blue
    else -> Color(0xFF888888)
}

@Composable
fun DebugScreen(
    viewModel: DebugViewModel = hiltViewModel()
) {
    val packets by viewModel.packets.collectAsState()
    val isCapturing by viewModel.isCapturing.collectAsState()
    val context = LocalContext.current
    var exportSnackbar by remember { mutableStateOf<String?>(null) }
    val snackbarState = remember { SnackbarHostState() }

    LaunchedEffect(exportSnackbar) {
        exportSnackbar?.let {
            snackbarState.showSnackbar(it)
            exportSnackbar = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarState) },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallFloatingActionButton(
                    onClick = {
                        val file = viewModel.exportCsv()
                        exportSnackbar = if (file != null) "Exported CSV to ${file.name}" else "Nothing to export"
                    },
                    containerColor = GreenPrimary
                ) {
                    Icon(Icons.Default.FileDownload, "Export CSV", tint = Color.White)
                }
                FloatingActionButton(
                    onClick = {
                        val file = viewModel.exportJson()
                        exportSnackbar = if (file != null) "Exported JSON to ${file.name}" else "Nothing to export"
                    },
                    containerColor = TealPrimary
                ) {
                    Icon(Icons.Default.Share, "Export JSON", tint = Color.White)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            // Top bar
            DebugTopBar(
                isCapturing = isCapturing,
                packetCount = packets.size,
                onToggle = { viewModel.toggleCapture() },
                onClear = { viewModel.clearLog() }
            )

            // Legend
            CharLegend()

            // Packet list
            if (packets.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.BluetoothDisabled, null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (isCapturing) "Waiting for BLE packets..." else "Logger paused",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    reverseLayout = false
                ) {
                    items(packets) { packet ->
                        PacketRow(packet = packet)
                    }
                }
            }
        }
    }
}

@Composable
private fun DebugTopBar(
    isCapturing: Boolean,
    packetCount: Int,
    onToggle: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "BLE Packet Logger",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                "$packetCount packets captured",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onClear) {
                Icon(Icons.Default.ClearAll, "Clear", tint = CoralPrimary)
            }
            FilledIconButton(
                onClick = onToggle,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isCapturing) CoralPrimary else GreenPrimary
                )
            ) {
                Icon(
                    imageVector = if (isCapturing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun CharLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Legend:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        listOf(
            "FEA1" to Color(0xFF7C4DFF),
            "FF03" to Color(0xFF00BFA5),
            "FF04" to Color(0xFF448AFF),
            "Other" to Color(0xFF888888)
        ).forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun PacketRow(packet: BlePacket) {
    val color = charColor(packet.characteristicUuid)
    val charShort = packet.characteristicUuid.uppercase().takeLast(8)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.06f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Char badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(color.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = charShort,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        fontFamily = FontFamily.Monospace
                    )
                }
                // Timestamp
                val time = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US)
                    .format(java.util.Date(packet.timestamp))
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Length badge
                Text(
                    text = "${packet.length}B",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            // Hex line
            Text(
                text = packet.hexString,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = color,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            // ASCII line
            Text(
                text = packet.asciiString,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
