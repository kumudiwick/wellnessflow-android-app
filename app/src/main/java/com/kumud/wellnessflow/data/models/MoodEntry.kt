package com.kumud.wellnessflow.data.models

import java.util.UUID

/**
 * Captures a single mood entry with optional notes and energy level.
 */
data class MoodEntry(
    val id: String = UUID.randomUUID().toString(),
    val emoji: String,
    val moodLabel: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val energyLevel: Int? = null,
    val date: String
)
