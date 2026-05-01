package com.kumud.wellnessflow.ui.home

import com.kumud.wellnessflow.data.models.MoodEntry

data class HomeUiState(
    val greeting: String,
    val userName: String,
    val dateDisplay: String,
    val habitCompleted: Int,
    val habitTotal: Int,
    val lastMood: MoodEntry?,
    val hydrationTotal: Int,
    val hydrationGoal: Int
) {
    val habitProgressPercent: Int = if (habitTotal == 0) 0 else ((habitCompleted.toFloat() / habitTotal.toFloat()) * 100).toInt()
    val hydrationProgressPercent: Int = if (hydrationGoal == 0) 0 else ((hydrationTotal.toFloat() / hydrationGoal.toFloat()) * 100).toInt().coerceIn(0, 100)
}
