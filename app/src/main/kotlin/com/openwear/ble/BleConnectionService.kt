package com.openwear.ble

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.openwear.MainActivity
import com.openwear.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "OpenWear:BleService"
private const val NOTIFICATION_ID = 1001
private const val CHANNEL_ID = "openwear_ble"
private const val PREF_LAST_DEVICE = "last_device_address"

@AndroidEntryPoint
class BleConnectionService : Service() {

    @Inject lateinit var bleManager: BleManager
    @Inject lateinit var packetLogger: PacketLogger

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var connectedDeviceName = "Watch"

    companion object {
        const val ACTION_CONNECT = "com.openwear.action.CONNECT"
        const val ACTION_DISCONNECT = "com.openwear.action.DISCONNECT"
        const val EXTRA_DEVICE_ADDRESS = "device_address"
        const val EXTRA_DEVICE_NAME = "device_name"

        fun startConnect(context: Context, address: String, name: String) {
            val intent = Intent(context, BleConnectionService::class.java).apply {
                action = ACTION_CONNECT
                putExtra(EXTRA_DEVICE_ADDRESS, address)
                putExtra(EXTRA_DEVICE_NAME, name)
            }
            context.startForegroundService(intent)
        }

        fun disconnect(context: Context) {
            val intent = Intent(context, BleConnectionService::class.java).apply {
                action = ACTION_DISCONNECT
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Searching for watch..."))
        packetLogger.startCapture()
        observeConnectionState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val address = intent.getStringExtra(EXTRA_DEVICE_ADDRESS) ?: return START_STICKY
                val name = intent.getStringExtra(EXTRA_DEVICE_NAME) ?: "Watch"
                connectedDeviceName = name
                saveLastDevice(address)
                try {
                    val adapter = BluetoothAdapter.getDefaultAdapter()
                    val device: BluetoothDevice = adapter.getRemoteDevice(address)
                    bleManager.connect(device)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to connect: ${e.message}")
                }
            }
            ACTION_DISCONNECT -> {
                bleManager.disconnect()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        packetLogger.stopCapture()
        bleManager.disconnect()
        serviceScope.cancel()
    }

    private fun observeConnectionState() {
        serviceScope.launch {
            bleManager.connectionState.collect { state ->
                val notifText = when (state) {
                    is BleConnectionState.Connected -> "Connected to ${state.deviceName} 🟢"
                    is BleConnectionState.Connecting -> "Connecting to $connectedDeviceName..."
                    is BleConnectionState.Disconnected -> "Disconnected"
                    is BleConnectionState.Scanning -> "Scanning..."
                    is BleConnectionState.Error -> "Error: ${state.message}"
                    else -> "Searching..."
                }
                updateNotification(notifText)
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "OpenWear Connection",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows the current connection status to your smartwatch"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String) = run {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OpenWear")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun saveLastDevice(address: String) {
        getSharedPreferences("openwear_prefs", Context.MODE_PRIVATE)
            .edit().putString(PREF_LAST_DEVICE, address).apply()
    }
}
