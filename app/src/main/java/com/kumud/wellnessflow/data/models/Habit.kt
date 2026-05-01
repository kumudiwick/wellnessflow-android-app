package com.kumud.wellnessflow.data.models

import java.util.UUID

/**
 * Represents a user-defined habit with configuration for scheduling and visual identity.
 */
data class Habit(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val category: String,
    val color: Int,
    val iconResId: Int,
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val targetDays: List<Int> = emptyList(),
    val createdDate: Long = System.currentTimeMillis()
)

enum class HabitFrequency {
    DAILY,
    WEEKLY
}
