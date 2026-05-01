package com.kumud.wellnessflow.ui.hydration

import com.kumud.wellnessflow.data.models.HydrationSettings

data class HydrationUiState(
    val settings: HydrationSettings,
    val todayTotal: Int,
    val progressPercent: Int,
    val history: List<HydrationHistoryItem>
) {
    val dailyGoal: Int = settings.dailyGoalMl
    val remindersEnabled: Boolean = settings.enabled
}
