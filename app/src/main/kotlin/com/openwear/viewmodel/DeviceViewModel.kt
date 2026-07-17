package com.openwear.viewmodel

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openwear.ble.BleConnectionState
import com.openwear.ble.BleManager
import com.openwear.ble.BleScanner
import com.openwear.data.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceUiState(
    val connectionState: BleConnectionState = BleConnectionState.Disconnected,
    val scannedDevices: List<ScanResult> = emptyList(),
    val isScanning: Boolean = false,
    val connectedDeviceName: String? = null,
    val connectedDeviceAddress: String? = null,
    val batteryLevel: Int? = null
)

@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val bleManager: BleManager,
    private val bleScanner: BleScanner,
    private val repository: HealthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceUiState())
    val uiState: StateFlow<DeviceUiState> = _uiState.asStateFlow()

    private val _scanResults = mutableMapOf<String, ScanResult>()
    private var scanJob: Job? = null

    init {
        viewModelScope.launch {
            bleManager.connectionState.collect { state ->
                val deviceName = (state as? BleConnectionState.Connected)?.deviceName
                val deviceAddress = (state as? BleConnectionState.Connected)?.device?.address
                _uiState.value = _uiState.value.copy(
                    connectionState = state,
                    connectedDeviceName = deviceName,
                    connectedDeviceAddress = deviceAddress
                )
                if (state is BleConnectionState.Connected) {
                    stopScan()
                    deviceName?.let { deviceAddress?.let { addr ->
                        repository.saveDevice(addr, it)
                    }}
                }
            }
        }
        viewModelScope.launch {
            bleManager.battery.collect { bat ->
                _uiState.value = _uiState.value.copy(batteryLevel = bat)
            }
        }
    }

    fun startScan() {
        _scanResults.clear()
        _uiState.value = _uiState.value.copy(isScanning = true, scannedDevices = emptyList())

        scanJob = viewModelScope.launch {
            // First check if the device is already bonded and connected in GATT
            val connectedDevices = bleManager.getConnectedDevices()
            val bondedDevices = bleManager.getBondedDevices()
            val savedDevices = repository.getAllDevices().firstOrNull() ?: emptyList()
            
            // Priority: Previously saved device that is currently connected
            val targetDevice = connectedDevices.find { connected ->
                savedDevices.any { saved -> saved.macAddress == connected.address }
            } ?: connectedDevices.find { it.name?.contains("DIZO", ignoreCase = true) == true || it.name?.contains("Watch", ignoreCase = true) == true }
              ?: bondedDevices.find { bonded ->
                savedDevices.any { saved -> saved.macAddress == bonded.address }
            } ?: bondedDevices.find { it.name?.contains("DIZO", ignoreCase = true) == true || it.name?.contains("Watch", ignoreCase = true) == true }

            if (targetDevice != null) {
                // Auto-connect and abort scan
                connect(targetDevice)
                return@launch
            }

            // Normal scanning fallback
            bleScanner.scan()
                .catch { /* handle errors */ }
                .collect { result ->
                    val name = result.device.name
                    if (!name.isNullOrBlank()) {
                        _scanResults[result.device.address] = result
                        _uiState.value = _uiState.value.copy(
                            scannedDevices = _scanResults.values.sortedByDescending { it.rssi }
                        )
                    }
                }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        bleScanner.stopScan()
        _uiState.value = _uiState.value.copy(isScanning = false)
    }

    fun connect(device: BluetoothDevice) {
        stopScan()
        bleManager.connect(device)
    }

    fun disconnect() {
        bleManager.disconnect()
    }
}
