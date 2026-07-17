package com.openwear.data.db

import androidx.room.*
import com.openwear.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthDao {

    // --- Heart Rate ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeartRate(entity: HeartRateEntity)

    @Query("SELECT * FROM heart_rate ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHeartRates(limit: Int = 100): Flow<List<HeartRateEntity>>

    @Query("SELECT AVG(bpm) FROM heart_rate WHERE timestamp > :since")
    suspend fun getAvgHeartRateSince(since: Long): Float?

    @Query("SELECT * FROM heart_rate WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp ASC")
    suspend fun getHeartRatesInRange(start: Long, end: Long): List<HeartRateEntity>

    @Query("SELECT MIN(bpm) FROM heart_rate WHERE timestamp > :since")
    suspend fun getMinHeartRateSince(since: Long): Int?

    @Query("SELECT MAX(bpm) FROM heart_rate WHERE timestamp > :since")
    suspend fun getMaxHeartRateSince(since: Long): Int?

    // --- Steps ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(entity: StepsEntity)

    @Query("SELECT * FROM steps WHERE date = :date LIMIT 1")
    fun getStepsForDate(date: String): Flow<StepsEntity?>

    @Query("SELECT * FROM steps ORDER BY date DESC LIMIT :days")
    fun getRecentDays(days: Int = 7): Flow<List<StepsEntity>>

    @Query("SELECT SUM(count) FROM steps WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalStepsBetween(startDate: String, endDate: String): Int?

    // --- Sleep ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleep(entity: SleepEntity)

    @Query("SELECT * FROM sleep ORDER BY date DESC LIMIT 1")
    fun getLastNightSleep(): Flow<SleepEntity?>

    @Query("SELECT * FROM sleep ORDER BY date DESC LIMIT :days")
    fun getRecentSleep(days: Int = 7): Flow<List<SleepEntity>>

    @Query("SELECT AVG(totalMs) FROM sleep WHERE date > :since")
    suspend fun getAvgSleepMs(since: String): Long?

    // --- SpO2 ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpO2(entity: SpO2Entity)

    @Query("SELECT * FROM spo2 ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSpO2(limit: Int = 50): Flow<List<SpO2Entity>>

    @Query("SELECT AVG(percent) FROM spo2 WHERE timestamp > :since")
    suspend fun getAvgSpO2Since(since: Long): Float?

    // --- Workouts ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(entity: WorkoutEntity)

    @Query("SELECT * FROM workouts ORDER BY startTimestamp DESC LIMIT :limit")
    fun getRecentWorkouts(limit: Int = 20): Flow<List<WorkoutEntity>>

    // --- Devices ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(entity: DeviceEntity)

    @Query("SELECT * FROM devices")
    fun getAllDevices(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE macAddress = :mac LIMIT 1")
    suspend fun getDevice(mac: String): DeviceEntity?

    @Query("UPDATE devices SET batteryLevel = :battery, lastSyncTimestamp = :time WHERE macAddress = :mac")
    suspend fun updateDeviceStatus(mac: String, battery: Int, time: Long)
}
