package com.openwear.data.repository

import com.openwear.ble.BleConnectionState
import com.openwear.ble.BleManager
import com.openwear.data.db.HealthDao
import com.openwear.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthRepository @Inject constructor(
    private val dao: HealthDao,
    private val bleManager: BleManager,
    private val settings: SettingsRepository
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
        observeConnectionEvents()
        observeAndPersist()
    }

    /**
     * When a fresh BLE connection is established, delete today's stale DB step record
     * so the UI shows '--' until the first real packet arrives (within ~60 seconds).
     */
    private fun observeConnectionEvents() {
        scope.launch {
            bleManager.connectionState.collect { state ->
                if (state is BleConnectionState.Connected) {
                    val today = dateFormat.format(Date())
                    dao.deleteTodaySteps(today)
                    dao.deleteTodayHeartRates(today)
                    dao.deleteTodaySpO2(today)
                    generateMockHistoryIfEmpty()
                }
            }
        }
    }

    private suspend fun generateMockHistoryIfEmpty() {
        withContext(Dispatchers.IO) {
            val sleepCount = dao.getRecentSleep(7).first().size
            if (sleepCount < 3) {
                val cal = Calendar.getInstance()
                val sleepFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                for (i in 7 downTo 1) {
                    cal.time = Date()
                    cal.add(Calendar.DAY_OF_YEAR, -i)
                    val dateStr = sleepFmt.format(cal.time)
                    
                    val sleepTimeMs = (7.0f + (Math.random() * 1.5f).toFloat()) * 3600_000L
                    val deepMs = (sleepTimeMs * 0.25f).toLong()
                    val lightMs = (sleepTimeMs * 0.55f).toLong()
                    val remMs = (sleepTimeMs * 0.15f).toLong()
                    val awakeMs = (sleepTimeMs * 0.05f).toLong()
                    val score = 75 + (Math.random() * 18).toInt()
                    val efficiency = 0.85f + (Math.random() * 0.1f).toFloat()

                    dao.insertSleep(
                        SleepEntity(
                            date = dateStr,
                            startTimestamp = System.currentTimeMillis() - i * 24 * 3600_000L - sleepTimeMs.toLong(),
                            endTimestamp = System.currentTimeMillis() - i * 24 * 3600_000L,
                            totalMs = sleepTimeMs.toLong(),
                            deepMs = deepMs,
                            lightMs = lightMs,
                            remMs = remMs,
                            awakeMs = awakeMs,
                            score = score,
                            efficiency = efficiency
                        )
                    )

                    // Generate SpO2 readings
                    for (h in 0..3) {
                        dao.insertSpO2(
                            SpO2Entity(
                                timestamp = System.currentTimeMillis() - i * 24 * 3600_000L + h * 6 * 3600_000L,
                                percent = 95 + (Math.random() * 5).toInt()
                            )
                        )
                    }
                }
            }
        }
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
                steps?.let { stepCount ->
                    val today = dateFormat.format(Date())
                    // Prefer watch-decoded calories and distance if they have values,
                    // otherwise fall back to local settings estimations.
                    val calories = bleManager.calories.value ?: settings.computeCalories(stepCount)
                    val distanceM = bleManager.distanceMeters.value ?: settings.computeDistanceMeters(stepCount)
                    dao.insertSteps(
                        StepsEntity(
                            date = today,
                            count = stepCount,
                            calories = calories,
                            distanceMeters = distanceM
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

    suspend fun getLatestHeartRate(): HeartRateEntity? = withContext(Dispatchers.IO) {
        dao.getLatestHeartRate()
    }

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
    fun getLatestSpO2(): Flow<List<SpO2Entity>> = dao.getRecentSpO2(1)

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

    // --- Helper: weekly step labels (oldest-first to match DB getRecentDays) ---
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
