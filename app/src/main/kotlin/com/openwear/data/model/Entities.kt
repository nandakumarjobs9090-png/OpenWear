package com.openwear.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "heart_rate")
data class HeartRateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val bpm: Int,
    val deviceId: String = "dizo_watch_r",
    val source: String = "live" // "live", "historical"
)

@Entity(tableName = "steps")
data class StepsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // "yyyy-MM-dd"
    val count: Int,
    val distanceMeters: Float = 0f,
    val calories: Int = 0,
    val deviceId: String = "dizo_watch_r"
)

@Entity(tableName = "sleep")
data class SleepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // "yyyy-MM-dd" of the morning after
    val startTimestamp: Long,
    val endTimestamp: Long,
    val totalMs: Long,
    val deepMs: Long = 0L,
    val lightMs: Long = 0L,
    val remMs: Long = 0L,
    val awakeMs: Long = 0L,
    val score: Int = 0,       // 0-100
    val efficiency: Float = 0f, // 0.0-1.0
    val deviceId: String = "dizo_watch_r"
)

@Entity(tableName = "spo2")
data class SpO2Entity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val percent: Int,
    val deviceId: String = "dizo_watch_r"
)

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "walk", "run", "cycling", etc.
    val startTimestamp: Long,
    val endTimestamp: Long,
    val steps: Int = 0,
    val calories: Int = 0,
    val distanceMeters: Float = 0f,
    val avgHeartRate: Int = 0,
    val deviceId: String = "dizo_watch_r"
)

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val macAddress: String,
    val name: String,
    val driverType: String = "dizo_watch_r",
    val lastSyncTimestamp: Long = 0L,
    val firmwareVersion: String = "Unknown",
    val batteryLevel: Int = -1
)

// --- Data Models (non-Room, for UI) ---
data class DailyStats(
    val date: String,
    val steps: Int,
    val calories: Int,
    val distanceKm: Float,
    val avgHeartRate: Int,
    val sleepHours: Float
)

data class SleepSummary(
    val date: String,
    val durationHours: Float,
    val deepHours: Float,
    val lightHours: Float,
    val remHours: Float,
    val awakeHours: Float,
    val score: Int,
    val efficiency: Float
)

data class HeartRateSample(
    val timestamp: Long,
    val bpm: Int
)

data class WeeklyStepData(
    val dayLabel: String,    // "Mon", "Tue", etc.
    val steps: Int,
    val goalMet: Boolean
)

enum class ExportFormat { JSON, CSV }
