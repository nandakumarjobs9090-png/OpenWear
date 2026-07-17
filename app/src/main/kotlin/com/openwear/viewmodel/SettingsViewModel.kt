package com.openwear.viewmodel

import androidx.lifecycle.ViewModel
import com.openwear.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository
) : ViewModel() {

    val stepsGoal: StateFlow<Int> = settings.stepsGoal
    val heightCm: StateFlow<Int> = settings.heightCm
    val weightKg: StateFlow<Int> = settings.weightKg
    val isMetric: StateFlow<Boolean> = settings.isMetric
    val notifySteps: StateFlow<Boolean> = settings.notifySteps
    val notifyHr: StateFlow<Boolean> = settings.notifyHr

    fun setStepsGoal(goal: Int) = settings.setStepsGoal(goal)
    fun setHeightCm(h: Int) = settings.setHeightCm(h)
    fun setWeightKg(w: Int) = settings.setWeightKg(w)
    fun setIsMetric(m: Boolean) = settings.setIsMetric(m)
    fun setNotifySteps(e: Boolean) = settings.setNotifySteps(e)
    fun setNotifyHr(e: Boolean) = settings.setNotifyHr(e)
}
