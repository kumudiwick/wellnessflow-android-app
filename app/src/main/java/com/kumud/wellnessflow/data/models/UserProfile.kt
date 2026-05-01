package com.kumud.wellnessflow.data.models

/**
 * Basic details captured during onboarding to personalise the experience.
 */
data class UserProfile(
    val displayName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val bio: String = ""
)
