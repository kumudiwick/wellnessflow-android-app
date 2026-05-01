package com.kumud.wellnessflow.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kumud.wellnessflow.data.models.ProfileNote
import com.kumud.wellnessflow.data.models.UserProfile
import com.kumud.wellnessflow.data.repository.PreferencesManager
import com.kumud.wellnessflow.utils.DateUtils
import java.util.Locale

class ProfileViewModel(private val preferencesManager: PreferencesManager) : ViewModel() {

    private val _state = MutableLiveData<ProfileUiState>()
    val state: LiveData<ProfileUiState> = _state


    fun loadProfile() {
        val profile = preferencesManager.getUserProfile()
        val displayName = profile?.displayName?.trim()?.takeIf { it.isNotEmpty() } ?: DEFAULT_NAME
        val memberSinceTimestamp = profile?.createdAt ?: System.currentTimeMillis()
        val memberSince = DateUtils.formatTimestamp(memberSinceTimestamp)
        val bio = profile?.bio?.trim().orEmpty()

        val dateKeys = (6 downTo 0).map { DateUtils.dateKeyDaysAgo(it) }
        val dayLabels = dateKeys.map { DateUtils.formatDayLabel(it).uppercase(Locale.getDefault()) }

        val moodEntries = preferencesManager.loadMoodEntries()
        val moodEntriesByDay = moodEntries.groupBy { it.date }
        val moodTrend = dateKeys.mapIndexed { index, dateKey ->
            val energies = moodEntriesByDay[dateKey].orEmpty().mapNotNull { it.energyLevel }
            val average = if (energies.isNotEmpty()) energies.average().toFloat() else null
            MoodTrendPoint(dayLabels[index], average)
        }

        val habits = preferencesManager.loadHabits()
        val completions = preferencesManager.loadCompletions()
        val weeklyHabitsCompleted = dateKeys.sumOf { key ->
            completions[key]?.values?.count { it } ?: 0
        }
        val weeklyHabitsTotal = habits.size * dateKeys.size

        val hydrationLogs = preferencesManager.loadHydrationLogs()
        val weeklyHydrationTotal = dateKeys.sumOf { key -> hydrationLogs[key]?.totalMl ?: 0 }
        val hydrationGoalDaily = preferencesManager.getHydrationSettings().dailyGoalMl
        val weeklyHydrationGoal = hydrationGoalDaily * dateKeys.size

        val weeklyMoodsLogged = dateKeys.sumOf { key -> moodEntriesByDay[key]?.size ?: 0 }

        val storedNotes = preferencesManager.loadProfileNotes().sortedByDescending { it.createdAt }

        _state.value = ProfileUiState(
            displayName = displayName,
            memberSince = memberSince,
            bio = bio,
            moodTrend = moodTrend,
            weeklyHabitsCompleted = weeklyHabitsCompleted,
            weeklyHabitsTotal = weeklyHabitsTotal,
            weeklyHydrationTotal = weeklyHydrationTotal,
            weeklyHydrationGoal = weeklyHydrationGoal,
            weeklyMoodsLogged = weeklyMoodsLogged,
            notes = storedNotes
        )
    }
    fun updateBio(newBio: String) {
        val trimmed = newBio.trim()
        val currentState = _state.value ?: return
        val existingProfile = preferencesManager.getUserProfile()
        val updatedProfile = if (existingProfile != null) {
            existingProfile.copy(bio = trimmed)
        } else {
            UserProfile(
                displayName = currentState.displayName,
                createdAt = System.currentTimeMillis(),
                bio = trimmed
            )
        }
        preferencesManager.saveUserProfile(updatedProfile)
        _state.value = currentState.copy(bio = trimmed)
    }

    fun saveNote(content: String, noteId: String? = null) {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return
        val notes = preferencesManager.loadProfileNotes().toMutableList()
        if (noteId == null) {
            notes.add(ProfileNote(content = trimmed))
        } else {
            val index = notes.indexOfFirst { it.id == noteId }
            if (index >= 0) {
                val existing = notes[index]
                notes[index] = existing.copy(content = trimmed, createdAt = System.currentTimeMillis())
            } else {
                notes.add(ProfileNote(id = noteId, content = trimmed))
            }
        }
        val sorted = notes.sortedByDescending { it.createdAt }
        preferencesManager.saveProfileNotes(sorted)
        _state.value = _state.value?.copy(notes = sorted)
    }

    fun deleteNote(noteId: String) {
        val remaining = preferencesManager.loadProfileNotes().filterNot { it.id == noteId }
        preferencesManager.saveProfileNotes(remaining)
        _state.value = _state.value?.copy(notes = remaining)
    }


    companion object {
        private const val DEFAULT_NAME = "Wellness friend"
    }
}
