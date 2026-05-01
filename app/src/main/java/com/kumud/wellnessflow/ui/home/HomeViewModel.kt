package com.kumud.wellnessflow.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kumud.wellnessflow.data.models.HydrationDayLog
import com.kumud.wellnessflow.data.models.HydrationLogEntry
import com.kumud.wellnessflow.data.models.MoodEntry
import com.kumud.wellnessflow.data.models.HydrationSettings
import com.kumud.wellnessflow.data.repository.PreferencesManager
import com.kumud.wellnessflow.utils.DateUtils
import java.util.Calendar

class HomeViewModel(private val preferencesManager: PreferencesManager) : ViewModel() {

    private val _state = MutableLiveData<HomeUiState>()
    val state: LiveData<HomeUiState> = _state

    fun loadState() {
        val todayKey = DateUtils.todayKey()
        val profile = preferencesManager.getUserProfile()
        val habits = preferencesManager.loadHabits()
        val completions = preferencesManager.loadCompletions()[todayKey]?.filterValues { it }?.size ?: 0
        val moodEntries = preferencesManager.loadMoodEntries()
        val lastMood = moodEntries.maxByOrNull { it.timestamp }
        val hydrationLogs = preferencesManager.loadHydrationLogs()
        val hydrationTotal = hydrationLogs[todayKey]?.totalMl ?: 0
        val hydrationSettings = preferencesManager.getHydrationSettings()

        _state.value = HomeUiState(
            greeting = buildGreeting(profile?.displayName),
            userName = profile?.displayName?.trim().orEmpty(),
            dateDisplay = DateUtils.formatDateKey(todayKey),
            habitCompleted = completions,
            habitTotal = habits.size,
            lastMood = lastMood,
            hydrationTotal = hydrationTotal,
            hydrationGoal = hydrationSettings.dailyGoalMl
        )
    }

    fun addMoodEntry(input: MoodEntry) {
        val entries = preferencesManager.loadMoodEntries().toMutableList()
        entries.add(0, input)
        preferencesManager.saveMoodEntries(entries)
        loadState()
    }

    fun logHydration(amountMl: Int) {
        val logs = preferencesManager.loadHydrationLogs()
        val dateKey = DateUtils.todayKey()
        val existing = logs[dateKey]
        val newEntry = HydrationLogEntry(System.currentTimeMillis(), amountMl)
        val updatedEntries = (existing?.entries ?: emptyList()) + newEntry
        logs[dateKey] = HydrationDayLog(updatedEntries, updatedEntries.sumOf { it.amountMl })
        preferencesManager.saveHydrationLogs(logs)
        loadState()
    }

    fun currentSettings(): HydrationSettings = preferencesManager.getHydrationSettings()

    private fun buildGreeting(name: String?): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greetingPrefix = when {
            hour in 5..11 -> "Good morning"
            hour in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
        val trimmedName = name?.takeIf { it.isNotBlank() }
        return if (trimmedName != null) {
            "$greetingPrefix, ${trimmedName}!"
        } else {
            "$greetingPrefix!"
        }
    }
}
