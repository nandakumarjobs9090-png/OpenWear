package com.openwear.data.repository

import com.openwear.ble.BleManager
import com.openwear.data.db.HealthDao
import com.openwear.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthRepository @Inject constructor(
    private val dao: HealthDao,
    private val bleManager: BleManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    val liveHeartRate: StateFlow<Int?> = bleManager.heartRate
    val liveSteps: StateFlow<Int?> = bleManager.steps
    val liveSpO2: StateFlow<Int?> = bleManager.spO2
    val liveBattery: StateFlow<Int?> = bleManager.battery
    val liveCalories: StateFlow<Int?> = bleManager.calories
    val liveDistanceMeters: StateFlow<Float?> = bleManager.distanceMeters

    init {
        // Persist live data to database
        observeAndPersist()
    }

    private fun observeAndPersist() {
        scope.launch {
            bleManager.heartRate.collect { hr ->
                hr?.let {
                    dao.insertHeartRate(
                        HeartRateEntity(timestamp = System.currentTimeMillis(), bpm = it)
                    )
                }
            }
        }
        scope.launch {
            bleManager.steps.collect { steps ->
                steps?.let {
                    val today = dateFormat.format(Date())
                    dao.insertSteps(
                        StepsEntity(
                            date = today,
                            count = it,
                            calories = bleManager.calories.value ?: 0,
                            distanceMeters = bleManager.distanceMeters.value ?: 0f
                        )
                    )
                }
            }
        }
        scope.launch {
            bleManager.spO2.collect { spo2 ->
                spo2?.let {
                    dao.insertSpO2(
                        SpO2Entity(timestamp = System.currentTimeMillis(), percent = it)
                    )
                }
            }
        }
    }

    // --- Heart Rate ---
    fun getRecentHeartRates(limit: Int = 100): Flow<List<HeartRateEntity>> =
        dao.getRecentHeartRates(limit)

    suspend fun getHRStats(hoursBack: Int = 24): Triple<Int, Int, Float> {
        val since = System.currentTimeMillis() - hoursBack * 3600_000L
        val min = dao.getMinHeartRateSince(since) ?: 0
        val max = dao.getMaxHeartRateSince(since) ?: 0
        val avg = dao.getAvgHeartRateSince(since) ?: 0f
        return Triple(min, max, avg)
    }

    // --- Steps ---
    fun getTodaySteps(): Flow<StepsEntity?> = dao.getStepsForDate(dateFormat.format(Date()))
    fun getWeeklySteps(): Flow<List<StepsEntity>> = dao.getRecentDays(7)

    // --- Sleep ---
    fun getLastNightSleep(): Flow<SleepEntity?> = dao.getLastNightSleep()
    fun getWeeklySleep(): Flow<List<SleepEntity>> = dao.getRecentSleep(7)

    // --- SpO2 ---
    fun getRecentSpO2(): Flow<List<SpO2Entity>> = dao.getRecentSpO2(50)

    // --- Workouts ---
    fun getRecentWorkouts(): Flow<List<WorkoutEntity>> = dao.getRecentWorkouts(20)

    // --- Devices ---
    fun getAllDevices(): Flow<List<DeviceEntity>> = dao.getAllDevices()

    suspend fun saveDevice(mac: String, name: String) {
        dao.insertDevice(DeviceEntity(macAddress = mac, name = name))
    }

    suspend fun updateDeviceStatus(mac: String, battery: Int) {
        dao.updateDeviceStatus(mac, battery, System.currentTimeMillis())
    }

    // --- Helper: weekly step labels ---
    fun getWeekDayLabels(): List<String> {
        val days = mutableListOf<String>()
        val cal = Calendar.getInstance()
        val fmt = SimpleDateFormat("EEE", Locale.US)
        for (i in 6 downTo 0) {
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            days.add(fmt.format(cal.time))
        }
        return days
    }
}
