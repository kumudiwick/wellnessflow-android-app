package com.kumud.wellnessflow.data.models

/**
 * Configuration for hydration reminders and tracking.
 */
data class HydrationSettings(
    val enabled: Boolean = false,
    val intervalMinutes: Int = DEFAULT_INTERVAL_MINUTES,
    val startHour: Int = 8,
    val startMinute: Int = 0,
    val endHour: Int = 20,
    val endMinute: Int = 0,
    val activeDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
    val dailyGoalMl: Int = 2000,
    val notificationSound: Boolean = true,
    val vibrate: Boolean = true
) {
    companion object {
        const val DEFAULT_INTERVAL_MINUTES = 120
    }
}
