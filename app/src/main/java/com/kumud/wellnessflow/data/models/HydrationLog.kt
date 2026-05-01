package com.kumud.wellnessflow.data.models

/**
 * Represents a single hydration logging action.
 */
data class HydrationLogEntry(
    val timestamp: Long,
    val amountMl: Int
)

/**
 * Aggregates a day's worth of hydration entries for quick lookup.
 */
data class HydrationDayLog(
    val entries: List<HydrationLogEntry> = emptyList(),
    val totalMl: Int = 0
)
