package com.openwear.ble

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.openwear.data.model.ExportFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "OpenWear:PacketLogger"
private const val MAX_LOG_SIZE = 1000

@Singleton
class PacketLogger @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bleManager: BleManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _packets = MutableStateFlow<List<BlePacket>>(emptyList())
    val packets: StateFlow<List<BlePacket>> = _packets.asStateFlow()

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val capturedPackets = mutableListOf<BlePacket>()

    fun startCapture() {
        _isCapturing.value = true
        scope.launch {
            bleManager.packetFlow.collect { packet ->
                if (_isCapturing.value) {
                    synchronized(capturedPackets) {
                        capturedPackets.add(0, packet) // newest first
                        if (capturedPackets.size > MAX_LOG_SIZE) {
                            capturedPackets.removeAt(capturedPackets.size - 1)
                        }
                        _packets.value = capturedPackets.toList()
                    }
                }
            }
        }
    }

    fun stopCapture() {
        _isCapturing.value = false
    }

    fun clearLog() {
        synchronized(capturedPackets) {
            capturedPackets.clear()
            _packets.value = emptyList()
        }
    }

    fun exportLog(format: ExportFormat = ExportFormat.JSON): File? {
        val packets = capturedPackets.toList()
        if (packets.isEmpty()) return null

        val dir = File(context.getExternalFilesDir(null), "OpenWear/logs").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

        return when (format) {
            ExportFormat.JSON -> {
                val file = File(dir, "openwear_log_$timestamp.json")
                val array = JSONArray()
                packets.forEach { p ->
                    array.put(JSONObject().apply {
                        put("timestamp", p.timestamp)
                        put("datetime", SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date(p.timestamp)))
                        put("service", p.serviceUuid)
                        put("characteristic", p.characteristicUuid)
                        put("hex", p.hexString)
                        put("ascii", p.asciiString)
                        put("length", p.length)
                    })
                }
                val root = JSONObject().apply {
                    put("device", "DIZO Watch R")
                    put("exported_at", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
                    put("packet_count", packets.size)
                    put("packets", array)
                }
                file.writeText(root.toString(2))
                Log.d(TAG, "Exported ${packets.size} packets to ${file.absolutePath}")
                file
            }
            ExportFormat.CSV -> {
                val file = File(dir, "openwear_log_$timestamp.csv")
                val sb = StringBuilder()
                sb.appendLine("timestamp,datetime,service,characteristic,hex,ascii,length")
                packets.forEach { p ->
                    val dt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date(p.timestamp))
                    sb.appendLine("${p.timestamp},\"$dt\",\"${p.serviceUuid}\",\"${p.characteristicUuid}\",\"${p.hexString}\",\"${p.asciiString}\",${p.length}")
                }
                file.writeText(sb.toString())
                Log.d(TAG, "Exported ${packets.size} packets to ${file.absolutePath}")
                file
            }
        }
    }
}
