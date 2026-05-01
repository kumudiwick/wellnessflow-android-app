package com.kumud.wellnessflow.ui.hydration

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kumud.wellnessflow.data.models.HydrationDayLog
import com.kumud.wellnessflow.data.models.HydrationLogEntry
import com.kumud.wellnessflow.data.models.HydrationSettings
import com.kumud.wellnessflow.data.repository.PreferencesManager
import com.kumud.wellnessflow.utils.DateUtils

class HydrationViewModel(private val preferencesManager: PreferencesManager) : ViewModel() {

    private val _state = MutableLiveData<HydrationUiState>()
    val state: LiveData<HydrationUiState> = _state

    // Refresh the UI snapshot from storage
    fun loadState() {
        _state.value = buildState()
    }

    // Add a drinking entry for today
    fun logIntake(amountMl: Int) {
        val logs = preferencesManager.loadHydrationLogs()
        val dateKey = DateUtils.todayKey()
        val existing = logs[dateKey]
        val newEntry = HydrationLogEntry(System.currentTimeMillis(), amountMl)
        val updatedEntries = (existing?.entries ?: emptyList()) + newEntry
        val updatedDayLog = HydrationDayLog(entries = updatedEntries, totalMl = updatedEntries.sumOf { it.amountMl })
        logs[dateKey] = updatedDayLog
        preferencesManager.saveHydrationLogs(logs)
        loadState()
    }

    // Persist the latest reminder settings
    fun updateSettings(settings: HydrationSettings) {
        preferencesManager.saveHydrationSettings(settings)
        preferencesManager.setHydrationEnabled(settings.enabled)
        preferencesManager.saveHydrationInterval(settings.intervalMinutes)
        loadState()
    }

    // Delete all hydration records for a given day
    fun clearDailyLog(dateKey: String) {
        val logs = preferencesManager.loadHydrationLogs()
        logs.remove(dateKey)
        preferencesManager.saveHydrationLogs(logs)
        loadState()
    }

    private fun buildState(): HydrationUiState {
        val settings = preferencesManager.getHydrationSettings()
        val logs = preferencesManager.loadHydrationLogs()
        val todayKey = DateUtils.todayKey()
        val todayLog = logs[todayKey]
        val history = logs.entries
            .sortedByDescending { it.key }
            .take(7)
            .map { HydrationHistoryItem(it.key, it.value.totalMl) }

        val todayTotal = todayLog?.totalMl ?: 0
        val progress = if (settings.dailyGoalMl == 0) 0 else ((todayTotal.toFloat() / settings.dailyGoalMl.toFloat()) * 100).toInt()

        return HydrationUiState(
            settings = settings,
            todayTotal = todayTotal,
            progressPercent = progress.coerceIn(0, 100),
            history = history
        )
    }
}

