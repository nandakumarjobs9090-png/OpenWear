package com.openwear.ble

import android.bluetooth.le.ScanResult
import android.bluetooth.BluetoothDevice

/**
 * Represents a raw BLE notification packet received from the watch.
 */
data class BlePacket(
    val timestamp: Long = System.currentTimeMillis(),
    val serviceUuid: String,
    val characteristicUuid: String,
    val data: ByteArray
) {
    val hexString: String get() = data.joinToString(" ") { "%02X".format(it) }
    val asciiString: String get() = data.map { if (it in 32..126) it.toInt().toChar() else '.' }.joinToString("")
    val length: Int get() = data.size

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BlePacket) return false
        return timestamp == other.timestamp &&
                serviceUuid == other.serviceUuid &&
                characteristicUuid == other.characteristicUuid &&
                data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + serviceUuid.hashCode()
        result = 31 * result + characteristicUuid.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

/**
 * BLE connection state machine states.
 */
sealed class BleConnectionState {
    object Disconnected : BleConnectionState()
    object Scanning : BleConnectionState()
    data class Found(val device: BluetoothDevice) : BleConnectionState()
    data class Connecting(val device: BluetoothDevice) : BleConnectionState()
    data class Connected(val device: BluetoothDevice, val deviceName: String) : BleConnectionState()
    data class Error(val message: String) : BleConnectionState()
}

/**
 * Known DIZO Watch R BLE UUIDs
 */
object DizoUuids {
    // Talk Go UUIDs observed in the user's BLE service discovery and packet capture.
    const val SERVICE_TALKGO_REALTIME = "000055ff-0000-1000-8000-00805f9b34fb"
    const val CHAR_TALKGO_REALTIME_NOTIFY = "000033f2-0000-1000-8000-00805f9b34fb"

    // Service UUIDs
    const val SERVICE_PRIMARY = "000001ff-3c17-d293-8e48-14fe2e4da212"
    const val SERVICE_SECONDARY = "0000d0ff-3c17-d293-8e48-14fe2e4da212"
    const val SERVICE_OTA = "00006287-3c17-d293-8e48-14fe2e4da212"
    const val SERVICE_FEE7 = "0000fee7-0000-1000-8000-00805f9b34fb"

    // Primary service characteristics
    const val CHAR_FF02_WRITE = "0000ff02-3c17-d293-8e48-14fe2e4da212"      // Write commands
    const val CHAR_FF03_NOTIFY = "0000ff03-3c17-d293-8e48-14fe2e4da212"     // Receive responses
    const val CHAR_FF04_RW = "0000ff04-3c17-d293-8e48-14fe2e4da212"        // Config/Commands

    // D0FF service characteristics
    const val CHAR_FFD1 = "0000ffd1-3c17-d293-8e48-14fe2e4da212"
    const val CHAR_FFD2 = "0000ffd2-3c17-d293-8e48-14fe2e4da212"
    const val CHAR_FFD8 = "0000ffd8-3c17-d293-8e48-14fe2e4da212"
    const val CHAR_FFF1 = "0000fff1-3c17-d293-8e48-14fe2e4da212"
    const val CHAR_FFF2 = "0000fff2-3c17-d293-8e48-14fe2e4da212"

    // FEE7 service characteristics (confirmed active notification)
    const val CHAR_FEA1_NOTIFY = "0000fea1-0000-1000-8000-00805f9b34fb"    // Active heartbeat/status
    const val CHAR_FEC9_READ = "0000fec9-0000-1000-8000-00805f9b34fb"

    // CCCD Descriptor UUID (standard BLE Client Characteristic Configuration)
    const val CCCD = "00002902-0000-1000-8000-00805f9b34fb"

    // Device name filter
    const val DEVICE_NAME_FILTER = "DIZO"
    const val DEVICE_NAME_FULL = "DIZO Watch R"
}
