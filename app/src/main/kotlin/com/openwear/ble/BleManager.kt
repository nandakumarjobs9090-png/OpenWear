package com.openwear.ble

import android.bluetooth.*
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "OpenWear:BleManager"

@Singleton
class BleManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var bluetoothGatt: BluetoothGatt? = null

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _packetFlow = MutableSharedFlow<BlePacket>(replay = 50, extraBufferCapacity = 200)
    val packetFlow: SharedFlow<BlePacket> = _packetFlow.asSharedFlow()

    private val _heartRate = MutableStateFlow<Int?>(null)
    val heartRate: StateFlow<Int?> = _heartRate.asStateFlow()

    private val _steps = MutableStateFlow<Int?>(null)
    val steps: StateFlow<Int?> = _steps.asStateFlow()

    private val _spO2 = MutableStateFlow<Int?>(null)
    val spO2: StateFlow<Int?> = _spO2.asStateFlow()

    private val _battery = MutableStateFlow<Int?>(null)
    val battery: StateFlow<Int?> = _battery.asStateFlow()

    private val _calories = MutableStateFlow<Int?>(null)
    val calories: StateFlow<Int?> = _calories.asStateFlow()

    private val _distanceMeters = MutableStateFlow<Float?>(null)
    val distanceMeters: StateFlow<Float?> = _distanceMeters.asStateFlow()

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "GATT connected. Discovering services...")
                    _connectionState.value = BleConnectionState.Connected(
                        gatt.device, gatt.device.name ?: "Unknown"
                    )
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "GATT disconnected (status=$status)")
                    _connectionState.value = BleConnectionState.Disconnected
                    bluetoothGatt = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Service discovery failed: $status")
                return
            }
            Log.d(TAG, "Services discovered. Enabling notifications...")
            logAllServices(gatt)
            enableNotifications(gatt)
            // Sync time after connect
            syncTime(gatt)
            // Request device info
            requestDeviceInfo(gatt)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handlePacket(characteristic.uuid.toString().lowercase(), value)
        }

        @Deprecated("Used for API < 33")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            @Suppress("DEPRECATION")
            handlePacket(characteristic.uuid.toString().lowercase(), characteristic.value ?: return)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handlePacket(characteristic.uuid.toString().lowercase(), value)
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Notification enabled for: ${descriptor.characteristic.uuid}")
                // Enable next notification in queue
                enableNextNotification(gatt)
            }
        }
    }

    private val notificationQueue = mutableListOf<Pair<String, String>>() // serviceUuid, charUuid
    private var notificationIndex = 0

    fun connect(device: BluetoothDevice) {
        Log.d(TAG, "Connecting to ${device.name} (${device.address})")
        _connectionState.value = BleConnectionState.Connecting(device)
        bluetoothGatt?.close()
        notificationIndex = 0
        notificationQueue.clear()
        bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting")
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _connectionState.value = BleConnectionState.Disconnected
    }

    fun getConnectedDevices(): List<BluetoothDevice> {
        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            bluetoothManager?.getConnectedDevices(BluetoothProfile.GATT) ?: emptyList()
        } catch (e: SecurityException) {
            Log.e(TAG, "Bluetooth permissions not granted yet for getConnectedDevices", e)
            emptyList()
        }
    }

    fun getBondedDevices(): List<BluetoothDevice> {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            adapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            Log.e(TAG, "Bluetooth permissions not granted yet for getBondedDevices", e)
            emptyList()
        }
    }

    private fun enableNotifications(gatt: BluetoothGatt) {
        // Build queue of all notification characteristics
        notificationQueue.clear()
        notificationIndex = 0

        val targets = listOf(
            DizoUuids.SERVICE_FEE7 to DizoUuids.CHAR_FEA1_NOTIFY,
            DizoUuids.SERVICE_PRIMARY to DizoUuids.CHAR_FF03_NOTIFY,
        )

        targets.forEach { (serviceUuid, charUuid) ->
            val service = gatt.getService(UUID.fromString(serviceUuid))
            val characteristic = service?.getCharacteristic(UUID.fromString(charUuid))
            if (characteristic != null) {
                notificationQueue.add(serviceUuid to charUuid)
            }
        }

        enableNextNotification(gatt)
    }

    private fun enableNextNotification(gatt: BluetoothGatt) {
        if (notificationIndex >= notificationQueue.size) {
            Log.d(TAG, "All notifications enabled (${notificationQueue.size} total)")
            return
        }

        val (serviceUuid, charUuid) = notificationQueue[notificationIndex++]
        val characteristic = gatt
            .getService(UUID.fromString(serviceUuid))
            ?.getCharacteristic(UUID.fromString(charUuid)) ?: run {
            enableNextNotification(gatt)
            return
        }

        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(UUID.fromString(DizoUuids.CCCD))
        descriptor?.let {
            it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            @Suppress("DEPRECATION")
            gatt.writeDescriptor(it)
        } ?: run {
            enableNextNotification(gatt)
        }
    }

    private fun handlePacket(charUuid: String, data: ByteArray) {
        val serviceUuid = getServiceForChar(charUuid)
        Log.d(TAG, "PKT [$charUuid] ${data.joinToString(" ") { "%02X".format(it) }}")

        val packet = BlePacket(
            serviceUuid = serviceUuid,
            characteristicUuid = charUuid,
            data = data
        )
        _packetFlow.tryEmit(packet)

        // Try to decode known packet types
        decodePacket(charUuid, data)
    }

    private fun decodePacket(charUuid: String, data: ByteArray) {
        if (data.isEmpty()) return

        when (charUuid) {
            DizoUuids.CHAR_FEA1_NOTIFY.lowercase() -> decodeFEA1(data)
            DizoUuids.CHAR_FF03_NOTIFY.lowercase() -> decodeFF03(data)
        }
    }

    /**
     * Decode FEA1 heartbeat/status packets.
     * Observed: 07 83 00 00 58 00 00 CC 13 00
     * Based on analysis:
     *   [0]    = 0x07: packet type (status frame)
     *   [1]    = flags / sub-type
     *   [4]    = heart rate (0x58 = 88 bpm) — if non-zero
     *   [7-8]  = step count little-endian (0xCC 0x13 = 5068 steps)
     */
    private fun decodeFEA1(data: ByteArray) {
        if (data.size < 6) return

        val packetType = data[0].toInt() and 0xFF

        when (packetType) {
            0x07 -> {
                // Status frame
                if (data.size >= 2) {
                    val hr = data[1].toInt() and 0xFF
                    if (hr in 30..220) {
                        _heartRate.value = hr
                        Log.d(TAG, "Heart rate decoded: $hr bpm")
                    }
                }
                if (data.size >= 5) {
                    val battery = data[4].toInt() and 0xFF
                    if (battery in 0..100) {
                        _battery.value = battery
                        Log.d(TAG, "Battery level decoded: $battery%")
                    }
                }
                if (data.size >= 9) {
                    val steps = ((data[8].toInt() and 0xFF) shl 8) or (data[7].toInt() and 0xFF)
                    if (steps >= 0) {
                        _steps.value = steps
                        Log.d(TAG, "Steps: $steps")
                    }
                }
            }
            0x05 -> {
                // Battery level packet (hypothesis)
                if (data.size >= 2) {
                    val battery = data[1].toInt() and 0xFF
                    if (battery in 0..100) {
                        _battery.value = battery
                        Log.d(TAG, "Battery: $battery%")
                    }
                }
            }
            0x0A -> {
                // SpO2 packet (hypothesis)
                if (data.size >= 3) {
                    val spo2 = data[2].toInt() and 0xFF
                    if (spo2 in 80..100) {
                        _spO2.value = spo2
                        Log.d(TAG, "SpO2: $spo2%")
                    }
                }
            }
        }
    }

    /**
     * Decode FF03 response packets from primary service.
     * Format TBD — log for analysis.
     */
    private fun decodeFF03(data: ByteArray) {
        if (data.isEmpty()) return
        val type = data[0].toInt() and 0xFF
        Log.d(TAG, "FF03 response type=0x${"%02X".format(type)} data=${data.size}bytes")
        // Protocol being decoded — expand as more packets are captured
    }

    /**
     * Write a command to the watch (FF02 characteristic).
     * Commands follow: [type][subtype][...payload]
     */
    fun writeCommand(serviceUuid: String, charUuid: String, data: ByteArray) {
        val gatt = bluetoothGatt ?: return
        val characteristic = gatt
            .getService(UUID.fromString(serviceUuid))
            ?.getCharacteristic(UUID.fromString(charUuid)) ?: return

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(
                characteristic,
                data,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            )
        } else {
            @Suppress("DEPRECATION")
            characteristic.value = data
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(characteristic)
        }
    }

    /**
     * Sync current time to the watch.
     * Standard BLE time sync command (placeholder — refine with actual DIZO protocol).
     */
    private fun syncTime(gatt: BluetoothGatt) {
        val now = System.currentTimeMillis() / 1000
        val year = (java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) - 1900).toByte()
        // Time sync command — 0x01 0x00 is a common smartwatch pattern
        val cmd = byteArrayOf(0x01, 0x00, year)
        Log.d(TAG, "Syncing time to watch")
        // writeCommand(DizoUuids.SERVICE_PRIMARY, DizoUuids.CHAR_FF02_WRITE, cmd)
    }

    /**
     * Request device info (battery, firmware version).
     */
    private fun requestDeviceInfo(gatt: BluetoothGatt) {
        // Try to read battery from d0ff service
        val service = gatt.getService(UUID.fromString(DizoUuids.SERVICE_SECONDARY))
        val ffd2 = service?.getCharacteristic(UUID.fromString(DizoUuids.CHAR_FFD2))
        ffd2?.let { gatt.readCharacteristic(it) }
    }

    private fun logAllServices(gatt: BluetoothGatt) {
        gatt.services.forEach { service ->
            Log.d(TAG, "Service: ${service.uuid}")
            service.characteristics.forEach { char ->
                Log.d(TAG, "  Char: ${char.uuid} props=${char.properties}")
            }
        }
    }

    private fun getServiceForChar(charUuid: String): String {
        return when (charUuid) {
            DizoUuids.CHAR_FEA1_NOTIFY.lowercase(), DizoUuids.CHAR_FEC9_READ.lowercase() -> DizoUuids.SERVICE_FEE7
            DizoUuids.CHAR_FF02_WRITE.lowercase(),
            DizoUuids.CHAR_FF03_NOTIFY.lowercase(),
            DizoUuids.CHAR_FF04_RW.lowercase() -> DizoUuids.SERVICE_PRIMARY
            DizoUuids.CHAR_FFD1.lowercase(),
            DizoUuids.CHAR_FFD2.lowercase(),
            DizoUuids.CHAR_FFD8.lowercase(),
            DizoUuids.CHAR_FFF1.lowercase(),
            DizoUuids.CHAR_FFF2.lowercase() -> DizoUuids.SERVICE_SECONDARY
            else -> "unknown"
        }
    }
}
