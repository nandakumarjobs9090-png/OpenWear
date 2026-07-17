package com.openwear.ui.device

import android.bluetooth.le.ScanResult
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.openwear.ble.BleConnectionState
import com.openwear.ui.components.*
import com.openwear.ui.theme.*
import com.openwear.viewmodel.DeviceViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DeviceScreen(
    viewModel: DeviceViewModel = hiltViewModel(),
    onNavigateToDebug: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val bluetoothManager = context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
    val bluetoothAdapter = bluetoothManager?.adapter

    val enableBluetoothLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.startScan()
        }
    }

    val blePermissions = rememberMultiplePermissionsState(
        permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            listOf(
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            listOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    )

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
                    "Device",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Current connection status
        item {
            ConnectedDeviceCard(
                state = uiState.connectionState,
                deviceName = uiState.connectedDeviceName,
                battery = uiState.batteryLevel,
                onDisconnect = { viewModel.disconnect() }
            )
        }

        // Scan controls
        item {
            ScanControlCard(
                isScanning = uiState.isScanning,
                isConnected = uiState.connectionState is BleConnectionState.Connected,
                hasPermission = blePermissions.allPermissionsGranted,
                onScanStart = {
                    if (blePermissions.allPermissionsGranted) {
                        if (bluetoothAdapter?.isEnabled == true) {
                            viewModel.startScan()
                        } else {
                            enableBluetoothLauncher.launch(
                                android.content.Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE)
                            )
                        }
                    } else {
                        blePermissions.launchMultiplePermissionRequest()
                    }
                },
                onScanStop = { viewModel.stopScan() }
            )
        }

        // Found devices
        if (uiState.scannedDevices.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        "Found Devices (${uiState.scannedDevices.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(uiState.scannedDevices) { result ->
                DeviceListItem(
                    result = result,
                    isConnected = result.device.address == uiState.connectedDeviceAddress,
                    onClick = { viewModel.connect(result.device) }
                )
            }
        }

        // Device info
        if (uiState.connectionState is BleConnectionState.Connected) {
            item {
                DeviceInfoCard(
                    name = uiState.connectedDeviceName ?: "DIZO Watch R",
                    address = uiState.connectedDeviceAddress ?: "--",
                    battery = uiState.batteryLevel
                )
            }
        }

        // Developer debug tools card
        item {
            DeveloperToolsCard(onOpenDebug = onNavigateToDebug)
        }

        // Help
        item { BleHelpCard() }
    }
}

@Composable
private fun ConnectedDeviceCard(
    state: BleConnectionState,
    deviceName: String?,
    battery: Int?,
    onDisconnect: () -> Unit
) {
    StatCard(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            if (state is BleConnectionState.Connected) TealPrimary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Watch,
                        null,
                        tint = if (state is BleConnectionState.Connected) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column {
                    Text(
                        text = when (state) {
                            is BleConnectionState.Connected -> deviceName ?: "Connected Watch"
                            is BleConnectionState.Connecting -> "Connecting..."
                            is BleConnectionState.Scanning -> "Scanning..."
                            else -> "No device connected"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    ConnectionBadge(isConnected = state is BleConnectionState.Connected)
                }
            }
            if (state is BleConnectionState.Connected) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    battery?.let {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = when {
                                    it >= 75 -> Icons.Default.BatteryFull
                                    it >= 50 -> Icons.Default.Battery5Bar
                                    it >= 25 -> Icons.Default.Battery3Bar
                                    else -> Icons.Default.Battery1Bar
                                },
                                contentDescription = null,
                                tint = when { it >= 50 -> GreenPrimary; it >= 25 -> OrangePrimary; else -> CoralPrimary },
                                modifier = Modifier.size(18.dp)
                            )
                            Text("$it%", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    TextButton(onClick = onDisconnect, contentPadding = PaddingValues(4.dp)) {
                        Text("Disconnect", style = MaterialTheme.typography.labelSmall, color = CoralPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanControlCard(
    isScanning: Boolean,
    isConnected: Boolean,
    hasPermission: Boolean,
    onScanStart: () -> Unit,
    onScanStop: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearEasing)),
        label = "scan_rotation"
    )

    StatCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        if (isScanning) TealPrimary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable(enabled = !isConnected) { if (isScanning) onScanStop() else onScanStart() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isScanning) Icons.AutoMirrored.Filled.BluetoothSearching else Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = if (isScanning) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(36.dp)
                        .then(if (isScanning) Modifier.rotate(rotation) else Modifier)
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = when {
                    isConnected -> "Already Connected"
                    isScanning -> "Scanning for devices..."
                    !hasPermission -> "Tap to grant Bluetooth permission"
                    else -> "Tap to scan for watches"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            if (!isConnected) {
                Button(
                    onClick = { if (isScanning) onScanStop() else onScanStart() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isScanning) CoralPrimary else TealPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        if (isScanning) Icons.Default.Stop else Icons.Default.Search,
                        null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (isScanning) "Stop Scan" else "Start Scan")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DeviceListItem(
    result: ScanResult,
    isConnected: Boolean,
    onClick: () -> Unit
) {
    val name = result.device.name ?: "Unknown Device"
    val address = result.device.address
    val rssi = result.rssi
    val signalStrength = when {
        rssi >= -60 -> "Excellent"
        rssi >= -70 -> "Good"
        rssi >= -80 -> "Fair"
        else -> "Weak"
    }
    val signalColor = when {
        rssi >= -60 -> GreenPrimary
        rssi >= -70 -> TealPrimary
        rssi >= -80 -> OrangePrimary
        else -> CoralPrimary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable(enabled = !isConnected, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) TealPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(if (isConnected) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .background(if (isConnected) TealPrimary else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Watch, null,
                        tint = if (isConnected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp))
                }
                Column {
                    Text(name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    Text(address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(signalStrength, style = MaterialTheme.typography.labelSmall, color = signalColor)
                Text("$rssi dBm", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (isConnected) {
                    Text("Connected", style = MaterialTheme.typography.labelSmall, color = TealPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DeviceInfoCard(name: String, address: String, battery: Int?) {
    StatCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        SectionHeader(title = "Device Info")
        Spacer(Modifier.height(12.dp))
        DeviceInfoRow("Device Name", name)
        DeviceInfoRow("MAC Address", address)
        DeviceInfoRow("Driver", "DIZO Watch R")
        battery?.let { DeviceInfoRow("Battery", "$it%") }
        DeviceInfoRow("Status", "Open Source Driver")
    }
}

@Composable
private fun DeviceInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun BleHelpCard() {
    StatCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Info, null, tint = BluePrimary, modifier = Modifier.size(20.dp))
            Text("Connection Help", style = MaterialTheme.typography.titleSmall)
        }
        Spacer(Modifier.height(10.dp))
        listOf(
            "Make sure Bluetooth is enabled on your phone",
            "Keep the watch close (within 1 metre) during pairing",
            "If the watch doesn't appear, restart Bluetooth on your phone",
            "Wake the watch display before scanning"
        ).forEach { hint ->
            Row(modifier = Modifier.padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("•", color = BluePrimary, style = MaterialTheme.typography.bodySmall)
                Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DeveloperToolsCard(onOpenDebug: () -> Unit) {
    StatCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = OrangePrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Developer Debug Tools",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Button(
                onClick = onOpenDebug,
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Open Logs", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
