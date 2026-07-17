package com.openwear.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "OpenWear:BleScanner"

@Singleton
class BleScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter
) {
    private var scanCallback: ScanCallback? = null

    /**
     * Start scanning for BLE devices and emit found devices as a Flow.
     * Filters for DIZO Watch R devices by name.
     */
    fun scan(): Flow<ScanResult> = callbackFlow {
        if (!hasPermission()) {
            Log.e(TAG, "Missing BLE permissions")
            close(SecurityException("Missing BLE scan permission"))
            return@callbackFlow
        }

        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            Log.e(TAG, "BLE scanner not available (BT off?)")
            close(IllegalStateException("BLE not available"))
            return@callbackFlow
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        // Scan without name filter to catch all devices (filter by name in callback)
        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val name = result.device.name ?: ""
                Log.d(TAG, "Found device: $name (${result.device.address})")
                // Emit all devices — UI will filter/sort
                trySend(result)
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "Scan failed with error: $errorCode")
                close(Exception("BLE scan failed: $errorCode"))
            }
        }

        Log.d(TAG, "Starting BLE scan...")
        scanner.startScan(null, settings, scanCallback)

        awaitClose {
            Log.d(TAG, "Stopping BLE scan")
            try {
                scanner.stopScan(scanCallback)
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping scan: ${e.message}")
            }
        }
    }

    fun stopScan() {
        try {
            if (hasPermission()) {
                bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping scan: ${e.message}")
        }
    }

    private fun hasPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
