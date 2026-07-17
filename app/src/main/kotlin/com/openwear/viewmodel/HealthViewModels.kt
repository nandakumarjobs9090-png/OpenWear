package com.openwear.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openwear.data.model.HeartRateEntity
import com.openwear.data.model.SpO2Entity
import com.openwear.data.repository.HealthRepository
import com.openwear.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HealthUiState(
    val liveHeartRate: Int? = null,
    val liveSpO2: Int? = null,
    val minHR: Int = 0,
    val maxHR: Int = 0,
    val avgHR: Float = 0f,
    val restingHR: Int = 0,
    val hrSamples: List<HeartRateEntity> = emptyList(),
    val spO2Samples: List<SpO2Entity> = emptyList(),
    val avgSpO2: Float = 0f
)

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val repository: HealthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HealthUiState())
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.liveHeartRate.collect { hr ->
                _uiState.value = _uiState.value.copy(liveHeartRate = hr)
            }
        }
        viewModelScope.launch {
            repository.liveSpO2.collect { spo2 ->
                _uiState.value = _uiState.value.copy(liveSpO2 = spo2)
            }
        }
        viewModelScope.launch {
            repository.getRecentHeartRates(200).collect { samples ->
                _uiState.value = _uiState.value.copy(hrSamples = samples)
                if (repository.liveHeartRate.value == null && samples.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(liveHeartRate = samples.first().bpm)
                }
            }
        }
        viewModelScope.launch {
            repository.getRecentSpO2().collect { samples ->
                _uiState.value = _uiState.value.copy(spO2Samples = samples)
                if (repository.liveSpO2.value == null && samples.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(liveSpO2 = samples.first().percent)
                }
            }
        }
        viewModelScope.launch {
            val (min, max, avg) = repository.getHRStats(24)
            _uiState.value = _uiState.value.copy(minHR = min, maxHR = max, avgHR = avg)
        }
    }
}

data class ActivityUiState(
    val todaySteps: Int = 0,
    val stepsGoal: Int = 8000,
    val todayCalories: Int = 0,
    val todayDistanceKm: Float = 0f,
    val activeMinutes: Int = 0,
    val weeklyStepData: List<Int> = emptyList(),
    val weekLabels: List<String> = emptyList()
)

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val repository: HealthRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settings.stepsGoal.collect { goal ->
                _uiState.value = _uiState.value.copy(stepsGoal = goal)
            }
        }
        viewModelScope.launch {
            repository.getTodaySteps().collect { today ->
                if (repository.liveSteps.value == null) {
                    _uiState.value = _uiState.value.copy(
                        todaySteps = today?.count ?: 0,
                        todayCalories = today?.calories ?: 0,
                        todayDistanceKm = (today?.distanceMeters ?: 0f) / 1000f
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.getWeeklySteps().collect { days ->
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                val labelFormat = java.text.SimpleDateFormat("EEE", java.util.Locale.US)
                
                val weeklyData = (6 downTo 0).map { offset ->
                    val cal = java.util.Calendar.getInstance()
                    cal.add(java.util.Calendar.DAY_OF_YEAR, -offset)
                    val dateStr = dateFormat.format(cal.time)
                    val label = labelFormat.format(cal.time)
                    
                    val count = days.find { it.date == dateStr }?.count ?: 0
                    Pair(label, count)
                }
                _uiState.value = _uiState.value.copy(
                    weeklyStepData = weeklyData.map { it.second },
                    weekLabels = weeklyData.map { it.first }
                )
            }
        }
        viewModelScope.launch {
            repository.liveSteps.collect { steps ->
                steps?.let { stepCount ->
                    _uiState.value = _uiState.value.copy(todaySteps = stepCount)
                    if (repository.liveCalories.value == null) {
                        _uiState.value = _uiState.value.copy(todayCalories = settings.computeCalories(stepCount))
                    }
                    if (repository.liveDistanceMeters.value == null) {
                        _uiState.value = _uiState.value.copy(todayDistanceKm = settings.computeDistanceMeters(stepCount) / 1000f)
                    }
                }
            }
        }
        viewModelScope.launch {
            repository.liveCalories.collect { calories ->
                calories?.let { _uiState.value = _uiState.value.copy(todayCalories = it) }
            }
        }
        viewModelScope.launch {
            repository.liveDistanceMeters.collect { distance ->
                distance?.let { _uiState.value = _uiState.value.copy(todayDistanceKm = it / 1000f) }
            }
        }
    }
}

data class SleepUiState(
    val lastNightHours: Float = 0f,
    val lastNightDeepHours: Float = 0f,
    val lastNightLightHours: Float = 0f,
    val lastNightRemHours: Float = 0f,
    val lastNightAwakeHours: Float = 0f,
    val sleepScore: Int = 0,
    val sleepEfficiency: Float = 0f,
    val weeklySleepHours: List<Float> = emptyList(),
    val weekLabels: List<String> = emptyList(),
    val avgSleepHours: Float = 0f
)

@HiltViewModel
class SleepViewModel @Inject constructor(
    private val repository: HealthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SleepUiState())
    val uiState: StateFlow<SleepUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getLastNightSleep().collect { sleep ->
                sleep?.let {
                    _uiState.value = _uiState.value.copy(
                        lastNightHours = it.totalMs / 3_600_000f,
                        lastNightDeepHours = it.deepMs / 3_600_000f,
                        lastNightLightHours = it.lightMs / 3_600_000f,
                        lastNightRemHours = it.remMs / 3_600_000f,
                        lastNightAwakeHours = it.awakeMs / 3_600_000f,
                        sleepScore = it.score,
                        sleepEfficiency = it.efficiency
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.getWeeklySleep().collect { days ->
                val labels = repository.getWeekDayLabels()
                val hours = days.map { it.totalMs / 3_600_000f }
                val avg = if (hours.isNotEmpty()) hours.average().toFloat() else 0f
                _uiState.value = _uiState.value.copy(
                    weeklySleepHours = hours,
                    weekLabels = labels,
                    avgSleepHours = avg
                )
            }
        }
    }
}
