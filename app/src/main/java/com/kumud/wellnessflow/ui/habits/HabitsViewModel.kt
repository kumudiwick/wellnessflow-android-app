package com.kumud.wellnessflow.ui.habits

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kumud.wellnessflow.data.models.Habit
import com.kumud.wellnessflow.data.models.HabitFrequency
import com.kumud.wellnessflow.data.repository.PreferencesManager
import com.kumud.wellnessflow.utils.DateUtils
import java.util.UUID

class HabitsViewModel(private val preferencesManager: PreferencesManager) : ViewModel() {

    private val _habits = MutableLiveData<List<Habit>>()
    val habits: LiveData<List<Habit>> = _habits

    private val _completionState = MutableLiveData<Map<String, Boolean>>()
    val completionState: LiveData<Map<String, Boolean>> = _completionState

    private val _todayKey = MutableLiveData(DateUtils.todayKey())
    val todayKey: LiveData<String> = _todayKey

    // Pull latest habits and completions from storage
    fun refresh() {
        val dateKey = _todayKey.value ?: DateUtils.todayKey()
        _habits.value = preferencesManager.loadHabits()
        val completions = preferencesManager.loadCompletions()
        _completionState.value = completions[dateKey]?.toMap() ?: emptyMap()
    }

    // Add a new habit to the saved list
    fun addHabit(
        name: String,
        description: String,
        category: String,
        color: Int,
        iconResId: Int,
        frequency: HabitFrequency,
        targetDays: List<Int>
    ) {
        val newHabit = Habit(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            category = category,
            color = color,
            iconResId = iconResId,
            frequency = frequency,
            targetDays = if (frequency == HabitFrequency.WEEKLY) targetDays else emptyList()
        )
        val updated = preferencesManager.loadHabits().apply { add(newHabit) }
        preferencesManager.saveHabits(updated)
        refresh()
    }

    // Update an existing habit in place
    fun updateHabit(updatedHabit: Habit) {
        val habits = preferencesManager.loadHabits()
        val index = habits.indexOfFirst { it.id == updatedHabit.id }
        if (index != -1) {
            habits[index] = updatedHabit
            preferencesManager.saveHabits(habits)
            refresh()
        }
    }

    // Remove a habit and its completion history
    fun deleteHabit(habitId: String) {
        val updated = preferencesManager.loadHabits().filterNot { it.id == habitId }
        preferencesManager.saveHabits(updated)
        preferencesManager.removeHabitData(habitId)
        refresh()
    }

    // Flip the done state for today
    fun toggleCompletion(habitId: String, completed: Boolean) {
        val dateKey = _todayKey.value ?: DateUtils.todayKey()
        preferencesManager.updateHabitCompletion(dateKey, habitId, completed)
        val completions = preferencesManager.loadCompletions()
        _completionState.value = completions[dateKey]?.toMap() ?: emptyMap()
    }

    fun setDate(dateKey: String) {
        _todayKey.value = dateKey
        refresh()
    }

    fun completionPercentage(): Int {
        val habitsForProgress = _habits.value.orEmpty()
        if (habitsForProgress.isEmpty()) return 0
        val completions = _completionState.value.orEmpty()
        val completedCount = habitsForProgress.count { completions[it.id] == true }
        return ((completedCount.toFloat() / habitsForProgress.size.toFloat()) * 100).toInt()
    }
}


