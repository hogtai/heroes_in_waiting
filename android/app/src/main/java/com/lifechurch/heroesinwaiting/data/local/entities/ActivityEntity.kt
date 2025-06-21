package com.lifechurch.heroesinwaiting.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.lifechurch.heroesinwaiting.data.local.converters.StringListConverter

@Entity(tableName = "activities")
@TypeConverters(StringListConverter::class)
data class ActivityEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val type: String, // "roleplay", "discussion", "creative", "reflection"
    val lessonId: String?,
    val estimatedDuration: Int,
    val materials: List<String>,
    val instructions: List<String>,
    val detailedInstructions: String? = null,
    val facilitatorGuide: String? = null,
    val isActive: Boolean,
    val cachedAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
)