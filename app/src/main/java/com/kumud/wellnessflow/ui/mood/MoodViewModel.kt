package com.kumud.wellnessflow.ui.mood

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kumud.wellnessflow.data.models.MoodEntry
import com.kumud.wellnessflow.data.repository.PreferencesManager
import com.kumud.wellnessflow.utils.DateUtils

class MoodViewModel(private val preferencesManager: PreferencesManager) : ViewModel() {

    private val _entries = MutableLiveData<List<MoodEntry>>()
    val entries: LiveData<List<MoodEntry>> = _entries

    // Read the stored mood entries
    fun loadEntries() {
        val items = preferencesManager.loadMoodEntries().sortedByDescending { it.timestamp }
        _entries.value = items
    }

    // Create a new mood entry
    fun addMoodEntry(emoji: String, label: String, note: String, energyLevel: Int?) {
        val allEntries = preferencesManager.loadMoodEntries()
        val entry = MoodEntry(
            emoji = emoji,
            moodLabel = label,
            note = note,
            energyLevel = energyLevel,
            date = DateUtils.todayKey()
        )
        val updated = mutableListOf(entry).apply { addAll(allEntries) }
        preferencesManager.saveMoodEntries(updated)
        loadEntries()
    }

    // Delete a mood entry by id
    fun deleteMoodEntry(entryId: String) {
        preferencesManager.removeMoodEntry(entryId)
        loadEntries()
    }

    // Re-add a removed entry if it is missing
    fun restoreMoodEntry(entry: MoodEntry) {
        val entries = preferencesManager.loadMoodEntries()
        if (entries.none { it.id == entry.id }) {
            entries.add(entry)
        }
        val ordered = entries.sortedByDescending { it.timestamp }
        preferencesManager.saveMoodEntries(ordered)
        loadEntries()
    }

}


