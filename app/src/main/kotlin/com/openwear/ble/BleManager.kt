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

    /** Add readable GATT diagnostics to the in-app packet logger. */
    private fun diagnostic(message: String) {
        Log.i(TAG, "DIAG: $message")
        _packetFlow.tryEmit(
            BlePacket(
                serviceUuid = "diagnostic",
                characteristicUuid = "diagnostic",
                data = message.toByteArray(Charsets.UTF_8)
            )
        )
    }

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
                    diagnostic("GATT CONNECTED; starting service discovery")
                    _connectionState.value = BleConnectionState.Connected(
                        gatt.device, gatt.device.name ?: "Unknown"
                    )
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    diagnostic("GATT DISCONNECTED; status=$status")
                    Log.d(TAG, "GATT disconnected (status=$status)")
                    _connectionState.value = BleConnectionState.Disconnected
                    bluetoothGatt = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            diagnostic("SERVICE DISCOVERY callback status=$status (${if (status == BluetoothGatt.GATT_SUCCESS) "SUCCESS" else "FAILED"})")
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Service discovery failed: $status")
                diagnostic("SERVICE DISCOVERY FAILED; disconnect and retry")
                return
            }
            Log.d(TAG, "Services discovered. Enabling notifications...")
            logAllServices(gatt)
            enableNotifications(gatt)
            // Diagnostic-only build: do not send unverified vendor commands.
            diagnostic("Automatic time sync and historical sync commands disabled for safe diagnosis")
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handlePacket(characteristic.service.uuid.toString().lowercase(), characteristic.uuid.toString().lowercase(), value)
        }

        @Deprecated("Used for API < 33")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            @Suppress("DEPRECATION")
            handlePacket(characteristic.service.uuid.toString().lowercase(), characteristic.uuid.toString().lowercase(), characteristic.value ?: return)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handlePacket(characteristic.service.uuid.toString().lowercase(), characteristic.uuid.toString().lowercase(), value)
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            val charUuid = descriptor.characteristic.uuid.toString().lowercase()
            diagnostic("CCCD WRITE $charUuid status=$status (${if (status == BluetoothGatt.GATT_SUCCESS) "SUCCESS" else "FAILED"})")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Notification enabled for: ${descriptor.characteristic.uuid}")
            }
            // Continue even after a failed descriptor write so all targets are tested.
            enableNextNotification(gatt)
        }
    }

    private val notificationQueue = mutableListOf<Pair<String, String>>() // serviceUuid, charUuid
    private var notificationIndex = 0

    fun connect(device: BluetoothDevice) {
        Log.d(TAG, "Connecting to ${device.name} (${device.address})")
        resetDailyData() // Clear stale data before new connection
        _connectionState.value = BleConnectionState.Connecting(device)
        bluetoothGatt?.close()
        notificationIndex = 0
        notificationQueue.clear()
        bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    /** Clear all live data flows to null so the UI shows '--' until fresh BLE packets arrive. */
    fun resetDailyData() {
        _heartRate.value = null
        _steps.value = null
        _spO2.value = null
        _calories.value = null
        _distanceMeters.value = null
        Log.d(TAG, "Live data cache cleared — awaiting fresh BLE packets")
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
        // Talk Go exposes a different GATT layout from the original Watch R.
        // Passively subscribe to discovered NOTIFY/INDICATE characteristics,
        // but deliberately exclude the Realtek DFU service and HID service.
        // No vendor commands or firmware-update writes are sent by this diagnostic.
        notificationQueue.clear()
        notificationIndex = 0

        val excludedServices = setOf(
            "00006287-3c17-d293-8e48-14fe2e4da212", // Realtek DFU service
            "00001812-0000-1000-8000-00805f9b34fb"  // HID service
        )

        gatt.services.forEach { service ->
            val serviceUuid = service.uuid.toString().lowercase()
            if (serviceUuid in excludedServices) {
                diagnostic("PASSIVE SUBSCRIBE SKIPPED service=$serviceUuid (DFU/HID safety exclusion)")
                return@forEach
            }
            service.characteristics.forEach { characteristic ->
                val props = characteristic.properties
                val notify = (props and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
                val indicate = (props and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                if (notify || indicate) {
                    val charUuid = characteristic.uuid.toString().lowercase()
                    val hasCccd = characteristic.getDescriptor(UUID.fromString(DizoUuids.CCCD)) != null
                    diagnostic("PASSIVE NOTIFY CANDIDATE service=$serviceUuid char=$charUuid notify=$notify indicate=$indicate cccd=$hasCccd")
                    if (hasCccd) notificationQueue.add(serviceUuid to charUuid)
                    else diagnostic("SKIP candidate without CCCD char=$charUuid")
                }
            }
        }
        diagnostic("PASSIVE notification targets queued=${notificationQueue.size}; no protocol writes will be sent")
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

        val localEnabled = gatt.setCharacteristicNotification(characteristic, true)
        diagnostic("setCharacteristicNotification char=$charUuid result=$localEnabled")
        val descriptor = characteristic.getDescriptor(UUID.fromString(DizoUuids.CCCD))
        descriptor?.let {
            it.value = if ((characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0 &&
                (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0) {
                BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            } else {
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            }
            @Suppress("DEPRECATION")
            val started = gatt.writeDescriptor(it)
            diagnostic("writeDescriptor started=$started char=$charUuid")
            if (!started) enableNextNotification(gatt)
        } ?: run {
            diagnostic("CCCD 0x2902 MISSING for $charUuid")
            enableNextNotification(gatt)
        }
    }

    private fun handlePacket(serviceUuid: String, charUuid: String, data: ByteArray) {
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
     * Decode FEA1 live status packets using the layout documented in README.md.
     * For a 10-byte 0x07 frame:
     *   [0]    = 0x07 packet type
     *   [1]    = heart rate (bpm)
     *   [4]    = battery percentage
     *   [7-8]  = steps, unsigned 16-bit little-endian
     *
     * Keep this decoder conservative: only publish fields documented by the
     * current protocol notes. Other packet types remain best-effort until
     * confirmed from a Talk Go packet capture.
     */
    private fun decodeFEA1(data: ByteArray) {
        if (data.isEmpty()) return

        val packetType = data[0].toInt() and 0xFF
        when (packetType) {
            0x07 -> {
                if (data.size >= 2) {
                    val hr = data[1].toInt() and 0xFF
                    if (hr in 30..220) {
                        _heartRate.value = hr
                        Log.d(TAG, "Heart rate decoded from status frame: $hr bpm")
                    }
                }
                if (data.size >= 5) {
                    val battery = data[4].toInt() and 0xFF
                    if (battery in 0..100) {
                        _battery.value = battery
                        Log.d(TAG, "Battery decoded from status frame: $battery%")
                    }
                }
                if (data.size >= 9) {
                    val steps = (data[7].toInt() and 0xFF) or
                        ((data[8].toInt() and 0xFF) shl 8)
                    _steps.value = steps
                    Log.d(TAG, "Steps decoded from status frame: $steps")
                }
            }
            0x08 -> {
                // Retain the existing possible standalone HR frame as a fallback.
                if (data.size >= 2) {
                    val hr = data[1].toInt() and 0xFF
                    if (hr in 30..220) {
                        _heartRate.value = hr
                        Log.d(TAG, "Heart rate decoded from 0x08 frame: $hr bpm")
                    }
                }
            }
            0x05 -> {
                // Unverified packet hypothesis; retain for diagnostics only.
                if (data.size >= 2) {
                    val battery = data[1].toInt() and 0xFF
                    if (battery in 0..100) {
                        _battery.value = battery
                        Log.d(TAG, "Possible battery packet: $battery%")
                    }
                }
            }
            0x0A -> {
                // Unverified packet hypothesis; retain for diagnostics only.
                if (data.size >= 3) {
                    val spo2 = data[2].toInt() and 0xFF
                    if (spo2 in 80..100) {
                        _spO2.value = spo2
                        Log.d(TAG, "Possible SpO2 packet: $spo2%")
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
     * Standard BLE time sync command following the format:
     * [0x01] [year_high] [year_low] [month] [day] [hour] [minute] [second] [day_of_week]
     */
    private fun syncTime(gatt: BluetoothGatt) {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = (calendar.get(java.util.Calendar.MONTH) + 1).toByte()
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH).toByte()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY).toByte()
        val minute = calendar.get(java.util.Calendar.MINUTE).toByte()
        val second = calendar.get(java.util.Calendar.SECOND).toByte()
        val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK).toByte() // 1 = Sunday, 2 = Monday...

        val cmd = byteArrayOf(
            0x01,
            ((year shr 8) and 0xFF).toByte(),
            (year and 0xFF).toByte(),
            month,
            day,
            hour,
            minute,
            second,
            dayOfWeek
        )
        Log.d(TAG, "Syncing time to watch: Year=$year, Month=$month, Day=$day, Time=$hour:$minute:$second")
        writeCommand(DizoUuids.SERVICE_PRIMARY, DizoUuids.CHAR_FF02_WRITE, cmd)
    }

    /**
     * Request historical data sync from the watch.
     */
    fun requestSync() {
        Log.d(TAG, "Requesting historical data sync from watch (steps, sleep, heart rate, SpO2)")
        // 0x03 0x01 -> sync steps/activity history
        writeCommand(DizoUuids.SERVICE_PRIMARY, DizoUuids.CHAR_FF02_WRITE, byteArrayOf(0x03, 0x01))
        // 0x03 0x02 -> sync sleep history
        writeCommand(DizoUuids.SERVICE_PRIMARY, DizoUuids.CHAR_FF02_WRITE, byteArrayOf(0x03, 0x02))
        // 0x03 0x03 -> sync heart rate history
        writeCommand(DizoUuids.SERVICE_PRIMARY, DizoUuids.CHAR_FF02_WRITE, byteArrayOf(0x03, 0x03))
        // 0x03 0x04 -> sync SpO2 history
        writeCommand(DizoUuids.SERVICE_PRIMARY, DizoUuids.CHAR_FF02_WRITE, byteArrayOf(0x03, 0x04))
    }

    /**
     * Request device info (battery, firmware version).
     */
    private fun requestDeviceInfo(gatt: BluetoothGatt) {
        val service = gatt.getService(UUID.fromString(DizoUuids.SERVICE_SECONDARY))
        val ffd2 = service?.getCharacteristic(UUID.fromString(DizoUuids.CHAR_FFD2))
        ffd2?.let { gatt.readCharacteristic(it) }
    }

    private fun logAllServices(gatt: BluetoothGatt) {
        diagnostic("DISCOVERED SERVICES count=${gatt.services.size}")
        gatt.services.forEach { service ->
            Log.d(TAG, "Service: ${service.uuid}")
            diagnostic("SERVICE ${service.uuid}")
            service.characteristics.forEach { char ->
                val p = char.properties
                val flags = buildList {
                    if (p and BluetoothGattCharacteristic.PROPERTY_READ != 0) add("READ")
                    if (p and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) add("WRITE")
                    if (p and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) add("WRITE_NO_RESPONSE")
                    if (p and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) add("NOTIFY")
                    if (p and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) add("INDICATE")
                }.joinToString("|").ifEmpty { "NONE" }
                Log.d(TAG, "  Char: ${char.uuid} props=${char.properties}")
                diagnostic("CHAR ${char.uuid} properties=$p flags=$flags cccd=${char.getDescriptor(UUID.fromString(DizoUuids.CCCD)) != null}")
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
