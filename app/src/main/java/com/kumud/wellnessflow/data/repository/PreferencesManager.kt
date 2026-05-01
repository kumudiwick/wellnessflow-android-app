package com.kumud.wellnessflow.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kumud.wellnessflow.data.models.Habit
import com.kumud.wellnessflow.data.models.HydrationDayLog
import com.kumud.wellnessflow.data.models.HydrationSettings
import com.kumud.wellnessflow.data.models.MoodEntry
import com.kumud.wellnessflow.data.models.ProfileNote
import com.kumud.wellnessflow.data.models.TodoItem
import com.kumud.wellnessflow.data.models.UserProfile

private typealias HabitCompletionMap = MutableMap<String, MutableMap<String, Boolean>>

/**
 * Central point for persisting and retrieving app data stored in SharedPreferences.
 * All complex structures are serialised using Gson to keep the storage layer simple.
 */
class PreferencesManager(context: Context) {

    private val gson = Gson()
    private val prefs: SharedPreferences = createPreferences(context.applicationContext)

    fun saveHabits(habits: List<Habit>) {
        write(KEY_HABITS, habits)
    }

    fun loadHabits(): MutableList<Habit> = read(KEY_HABITS, mutableListOf<Habit>())

    fun saveCompletions(completions: HabitCompletionMap) {
        write(KEY_HABIT_COMPLETIONS, completions)
    }

    fun loadCompletions(): HabitCompletionMap =
        read(KEY_HABIT_COMPLETIONS, mutableMapOf<String, MutableMap<String, Boolean>>())

    fun saveMoodEntries(entries: List<MoodEntry>) {
        write(KEY_MOOD_ENTRIES, entries)
    }

    fun loadMoodEntries(): MutableList<MoodEntry> = read(KEY_MOOD_ENTRIES, mutableListOf<MoodEntry>())

    fun saveTodoItems(items: List<TodoItem>) {
        write(KEY_TODO_ITEMS, items)
    }

    fun loadTodoItems(): MutableList<TodoItem> = read(KEY_TODO_ITEMS, mutableListOf<TodoItem>())

    fun saveUserProfile(profile: UserProfile) {
        write(KEY_USER_PROFILE, profile)
    }

    fun getUserProfile(): UserProfile? = readOrNull(KEY_USER_PROFILE)

    fun saveProfileNotes(notes: List<ProfileNote>) {
        write(KEY_PROFILE_NOTES, notes)
    }

    fun loadProfileNotes(): MutableList<ProfileNote> = read(KEY_PROFILE_NOTES, mutableListOf<ProfileNote>())

    fun saveHydrationSettings(settings: HydrationSettings) {
        write(KEY_HYDRATION_SETTINGS, settings)
    }

    fun getHydrationSettings(): HydrationSettings = read(KEY_HYDRATION_SETTINGS, HydrationSettings())

    fun saveHydrationLogs(logs: Map<String, HydrationDayLog>) {
        write(KEY_HYDRATION_LOG, logs)
    }

    fun loadHydrationLogs(): MutableMap<String, HydrationDayLog> =
        read(KEY_HYDRATION_LOG, mutableMapOf<String, HydrationDayLog>())

    fun saveHydrationInterval(minutes: Int) {
        prefs.edit().putInt(KEY_HYDRATION_INTERVAL, minutes).apply()
    }

    fun getHydrationInterval(defaultMinutes: Int = HydrationSettings.DEFAULT_INTERVAL_MINUTES): Int =
        prefs.getInt(KEY_HYDRATION_INTERVAL, defaultMinutes)

    fun setHydrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HYDRATION_ENABLED, enabled).apply()
    }

    fun isHydrationEnabled(): Boolean = prefs.getBoolean(KEY_HYDRATION_ENABLED, false)

    fun updateHabitCompletion(date: String, habitId: String, completed: Boolean) {
        val completions = loadCompletions()
        val dayRecord = completions.getOrPut(date) { mutableMapOf() }
        dayRecord[habitId] = completed
        saveCompletions(completions)
    }

    fun removeHabitData(habitId: String) {
        val completions = loadCompletions()
        completions.values.forEach { it.remove(habitId) }
        saveCompletions(completions)
    }

    fun removeMoodEntry(entryId: String) {
        val entries = loadMoodEntries()
        val updated = entries.filterNot { it.id == entryId }
        saveMoodEntries(updated)
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun createPreferences(context: Context): SharedPreferences {
        return runCatching {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.getOrElse {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private inline fun <reified T> readOrNull(key: String): T? {
        val raw = prefs.getString(key, null) ?: return null
        return runCatching {
            val type = object : TypeToken<T>() {}.type
            gson.fromJson<T>(raw, type)
        }.getOrNull()
    }

    private inline fun <reified T> read(key: String, defaultValue: T): T {
        val raw = prefs.getString(key, null) ?: return defaultValue
        return runCatching {
            val type = object : TypeToken<T>() {}.type
            gson.fromJson<T>(raw, type)
        }.getOrDefault(defaultValue)
    }

    private fun write(key: String, value: Any?) {
        if (value == null) {
            prefs.edit().remove(key).apply()
        } else {
            prefs.edit().putString(key, gson.toJson(value)).apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "wellnessflow_prefs"
        private const val KEY_HABITS = "habits_json"
        private const val KEY_HABIT_COMPLETIONS = "completions_json"
        private const val KEY_MOOD_ENTRIES = "moods_json"
        private const val KEY_TODO_ITEMS = "todo_items_json"
        private const val KEY_USER_PROFILE = "user_profile_json"
        private const val KEY_PROFILE_NOTES = "profile_notes_json"
        private const val KEY_HYDRATION_SETTINGS = "hydration_settings_json"
        private const val KEY_HYDRATION_LOG = "hydration_log_json"
        private const val KEY_HYDRATION_INTERVAL = "hydration_interval"
        private const val KEY_HYDRATION_ENABLED = "hydration_enabled"
    }
}








