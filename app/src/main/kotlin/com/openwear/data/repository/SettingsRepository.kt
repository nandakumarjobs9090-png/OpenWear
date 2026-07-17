package com.openwear.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("openwear_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_STEPS_GOAL = "steps_goal"
        private const val KEY_HEIGHT_CM = "height_cm"
        private const val KEY_WEIGHT_KG = "weight_kg"
        private const val KEY_UNIT_METRIC = "unit_metric"
        private const val KEY_NOTIFY_STEPS = "notify_steps"
        private const val KEY_NOTIFY_HR = "notify_hr"
        private const val DEFAULT_STEPS_GOAL = 8000
        private const val DEFAULT_HEIGHT_CM = 170
        private const val DEFAULT_WEIGHT_KG = 70
    }

    private val _stepsGoal = MutableStateFlow(prefs.getInt(KEY_STEPS_GOAL, DEFAULT_STEPS_GOAL))
    val stepsGoal: StateFlow<Int> = _stepsGoal.asStateFlow()

    private val _heightCm = MutableStateFlow(prefs.getInt(KEY_HEIGHT_CM, DEFAULT_HEIGHT_CM))
    val heightCm: StateFlow<Int> = _heightCm.asStateFlow()

    private val _weightKg = MutableStateFlow(prefs.getInt(KEY_WEIGHT_KG, DEFAULT_WEIGHT_KG))
    val weightKg: StateFlow<Int> = _weightKg.asStateFlow()

    private val _isMetric = MutableStateFlow(prefs.getBoolean(KEY_UNIT_METRIC, true))
    val isMetric: StateFlow<Boolean> = _isMetric.asStateFlow()

    private val _notifySteps = MutableStateFlow(prefs.getBoolean(KEY_NOTIFY_STEPS, true))
    val notifySteps: StateFlow<Boolean> = _notifySteps.asStateFlow()

    private val _notifyHr = MutableStateFlow(prefs.getBoolean(KEY_NOTIFY_HR, false))
    val notifyHr: StateFlow<Boolean> = _notifyHr.asStateFlow()

    /** Stride length in metres derived from height */
    val strideLengthM: Float get() = _heightCm.value * 0.415f / 100f

    fun setStepsGoal(goal: Int) {
        prefs.edit().putInt(KEY_STEPS_GOAL, goal).apply()
        _stepsGoal.value = goal
    }

    fun setHeightCm(height: Int) {
        prefs.edit().putInt(KEY_HEIGHT_CM, height).apply()
        _heightCm.value = height
    }

    fun setWeightKg(weight: Int) {
        prefs.edit().putInt(KEY_WEIGHT_KG, weight).apply()
        _weightKg.value = weight
    }

    fun setIsMetric(metric: Boolean) {
        prefs.edit().putBoolean(KEY_UNIT_METRIC, metric).apply()
        _isMetric.value = metric
    }

    fun setNotifySteps(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFY_STEPS, enabled).apply()
        _notifySteps.value = enabled
    }

    fun setNotifyHr(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFY_HR, enabled).apply()
        _notifyHr.value = enabled
    }

    /** Compute calories burned from step count using MET-based formula */
    fun computeCalories(steps: Int): Int {
        val weightKg = _weightKg.value.toFloat()
        return (steps * weightKg * 0.0005f).toInt()
    }

    /** Compute distance in metres from step count using stride length formula */
    fun computeDistanceMeters(steps: Int): Float {
        return steps * strideLengthM
    }
}
