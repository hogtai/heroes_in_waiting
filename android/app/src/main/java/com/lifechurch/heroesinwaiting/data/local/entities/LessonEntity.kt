package com.lifechurch.heroesinwaiting.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.lifechurch.heroesinwaiting.data.local.converters.StringListConverter

@Entity(tableName = "lessons")
@TypeConverters(StringListConverter::class)
data class LessonEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val orderIndex: Int,
    val estimatedDuration: Int,
    val objectives: List<String>,
    val category: String,
    val isActive: Boolean,
    val introduction: String? = null,
    val mainContent: String? = null,
    val keyPoints: List<String> = emptyList(),
    val cachedAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val isDownloaded: Boolean = false,
    val gradeLevel: String? = null,
    val difficultyLevel: String? = null,
    val targetGrades: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val isBookmarked: Boolean = false
) {
    fun toJson(): String {
        // Simple JSON serialization for metadata storage
        return """
        {
            "id": "$id",
            "title": "$title",
            "description": "$description",
            "orderIndex": $orderIndex,
            "estimatedDuration": $estimatedDuration,
            "category": "$category",
            "isActive": $isActive,
            "isDownloaded": $isDownloaded,
            "gradeLevel": "${gradeLevel ?: ""}",
            "difficultyLevel": "${difficultyLevel ?: ""}",
            "cachedAt": $cachedAt,
            "lastUpdated": $lastUpdated
        }
        """.trimIndent()
    }
}