package com.openwear.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.openwear.data.model.*

@Database(
    entities = [
        HeartRateEntity::class,
        StepsEntity::class,
        SleepEntity::class,
        SpO2Entity::class,
        WorkoutEntity::class,
        DeviceEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun healthDao(): HealthDao
}
