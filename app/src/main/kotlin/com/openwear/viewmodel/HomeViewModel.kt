package com.openwear.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.bluetooth.BluetoothDevice
import com.openwear.ble.BleConnectionState
import com.openwear.ble.BleManager
import com.openwear.data.model.StepsEntity
import com.openwear.data.model.WeeklyStepData
import com.openwear.data.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class HomeUiState(
    val heartRate: Int? = null,
    val steps: Int = 0,
    val stepsGoal: Int = 8000,
    val calories: Int = 0,
    val distanceKm: Float = 0f,
    val battery: Int? = null,
    val spO2: Int? = null,
    val sleepHours: Float = 0f,
    val connectionState: BleConnectionState = BleConnectionState.Disconnected,
    val greetingText: String = "Good Morning",
    val weeklySteps: List<WeeklyStepData> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HealthRepository,
    private val bleManager: BleManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val connectionState: StateFlow<BleConnectionState> = bleManager.connectionState
        .stateIn(viewModelScope, SharingStarted.Lazily, BleConnectionState.Disconnected)

    init {
        observeLiveData()
        loadStoredData()
        setGreeting()
        checkAutoConnect()
    }

    private fun observeLiveData() {
        viewModelScope.launch {
            bleManager.heartRate.collect { hr ->
                _uiState.value = _uiState.value.copy(heartRate = hr)
            }
        }
        viewModelScope.launch {
            bleManager.steps.collect { steps ->
                steps?.let { _uiState.value = _uiState.value.copy(steps = it) }
            }
        }
        viewModelScope.launch {
            bleManager.battery.collect { bat ->
                _uiState.value = _uiState.value.copy(battery = bat)
            }
        }
        viewModelScope.launch {
            bleManager.spO2.collect { spo2 ->
                _uiState.value = _uiState.value.copy(spO2 = spo2)
            }
        }
        viewModelScope.launch {
            bleManager.calories.collect { cal ->
                cal?.let { _uiState.value = _uiState.value.copy(calories = it) }
            }
        }
        viewModelScope.launch {
            bleManager.distanceMeters.collect { dist ->
                dist?.let { _uiState.value = _uiState.value.copy(distanceKm = it / 1000f) }
            }
        }
        viewModelScope.launch {
            bleManager.connectionState.collect { state ->
                _uiState.value = _uiState.value.copy(connectionState = state)
            }
        }
    }

    private fun loadStoredData() {
        viewModelScope.launch {
            repository.getLastNightSleep().collect { sleep ->
                val hours = sleep?.totalMs?.let { it / 3_600_000f } ?: 0f
                _uiState.value = _uiState.value.copy(sleepHours = hours)
            }
        }
        viewModelScope.launch {
            repository.getWeeklySteps().collect { days ->
                val labels = repository.getWeekDayLabels()
                val weeklyData = labels.mapIndexed { index, label ->
                    val stepsDay = days.getOrNull(index)?.count ?: 0
                    WeeklyStepData(
                        dayLabel = label,
                        steps = stepsDay,
                        goalMet = stepsDay >= 8000
                    )
                }
                _uiState.value = _uiState.value.copy(weeklySteps = weeklyData)
            }
        }
        viewModelScope.launch {
            repository.getTodaySteps().collect { today ->
                today?.let {
                    _uiState.value = _uiState.value.copy(
                        steps = it.count,
                        calories = it.calories,
                        distanceKm = it.distanceMeters / 1000f
                    )
                }
            }
        }
    }

    private fun setGreeting() {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
        _uiState.value = _uiState.value.copy(greetingText = greeting)
    }

    private fun checkAutoConnect() {
        viewModelScope.launch {
            try {
                val connectedDevices = bleManager.getConnectedDevices()
                val bondedDevices = bleManager.getBondedDevices()
                val savedDevices = repository.getAllDevices().firstOrNull() ?: emptyList()
                
                val targetDevice = connectedDevices.find { connected ->
                    savedDevices.any { saved -> saved.macAddress == connected.address }
                } ?: connectedDevices.find { it.name?.contains("DIZO", ignoreCase = true) == true || it.name?.contains("Watch", ignoreCase = true) == true }
                  ?: bondedDevices.find { bonded ->
                    savedDevices.any { saved -> saved.macAddress == bonded.address }
                } ?: bondedDevices.find { it.name?.contains("DIZO", ignoreCase = true) == true || it.name?.contains("Watch", ignoreCase = true) == true }

                if (targetDevice != null && bleManager.connectionState.value is BleConnectionState.Disconnected) {
                    android.util.Log.d("OpenWear:HomeVM", "Auto-connecting to device: ${targetDevice.name}")
                    bleManager.connect(targetDevice)
                }
            } catch (e: Exception) {
                android.util.Log.e("OpenWear:HomeVM", "Error checking auto connect", e)
            }
        }
    }
}
