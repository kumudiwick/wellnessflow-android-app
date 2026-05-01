package com.kumud.wellnessflow.data.models

/**
 * Stores the completion state of a habit for a given date.
 */
data class HabitCompletion(
    val habitId: String,
    val date: String,
    val completed: Boolean
)
