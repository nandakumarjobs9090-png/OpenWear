package com.openwear.ble

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

private const val TAG = "OpenWear:BootReceiver"

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed — checking for last paired device")
            val prefs = context.getSharedPreferences("openwear_prefs", Context.MODE_PRIVATE)
            val lastAddress = prefs.getString("last_device_address", null)
            val lastName = prefs.getString("last_device_name", "DIZO Watch R")

            if (!lastAddress.isNullOrBlank()) {
                Log.d(TAG, "Auto-reconnecting to $lastAddress")
                BleConnectionService.startConnect(context, lastAddress, lastName ?: "Watch")
            }
        }
    }
}
