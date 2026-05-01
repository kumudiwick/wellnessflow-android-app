package com.kumud.wellnessflow.data.models

import java.util.UUID

/**
 * Represents a checklist item the user can complete or revisit later.
 */
data class TodoItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val notes: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
