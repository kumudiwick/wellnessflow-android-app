package com.kumud.wellnessflow.data.models

import java.util.UUID

/**
 * Lightweight personal note stored on the profile screen.
 */
data class ProfileNote(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
