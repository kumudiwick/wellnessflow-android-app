package com.kumud.wellnessflow.ui.profile

import com.kumud.wellnessflow.data.models.ProfileNote

data class ProfileUiState(
    val displayName: String,
    val memberSince: String,
    val bio: String,
    val moodTrend: List<MoodTrendPoint>,
    val weeklyHabitsCompleted: Int,
    val weeklyHabitsTotal: Int,
    val weeklyHydrationTotal: Int,
    val weeklyHydrationGoal: Int,
    val weeklyMoodsLogged: Int,
    val notes: List<ProfileNote>
)

data class MoodTrendPoint(
    val dayLabel: String,
    val averageEnergy: Float?
)
