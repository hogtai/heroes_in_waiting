package com.lifechurch.heroesinwaiting.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "student_progress")
data class ProgressEntity(
    @PrimaryKey
    val id: String, // Combination of studentId + itemId + itemType
    val studentId: String,
    val itemId: String,
    val itemType: String, // "lesson", "activity", "scenario"
    val completedAt: Long,
    val timeSpent: Int? = null, // in seconds
    val score: Double? = null,
    val responses: String? = null, // JSON string of responses
    val syncedToServer: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)