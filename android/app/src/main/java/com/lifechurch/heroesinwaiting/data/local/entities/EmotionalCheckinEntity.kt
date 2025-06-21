package com.lifechurch.heroesinwaiting.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emotional_checkins")
data class EmotionalCheckinEntity(
    @PrimaryKey
    val id: String,
    val studentId: String,
    val emotion: String,
    val intensity: Int, // 1-5 scale
    val context: String? = null,
    val encouragement: String? = null,
    val recommendedActivity: String? = null,
    val emoji: String? = null,
    val timestamp: Long,
    val syncedToServer: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)