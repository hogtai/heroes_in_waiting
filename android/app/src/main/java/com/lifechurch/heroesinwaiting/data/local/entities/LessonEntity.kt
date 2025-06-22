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
    
    /**
     * Converts to domain model with lazy loading (content loaded on demand)
     */
    fun toDomainModelLazy(): com.lifechurch.heroesinwaiting.data.model.Lesson {
        return com.lifechurch.heroesinwaiting.data.model.Lesson(
            id = id,
            title = title,
            description = description,
            lessonNumber = orderIndex,
            objectives = emptyList(), // Will be loaded on demand
            keyTerms = emptyList(),
            estimatedDuration = estimatedDuration,
            targetGrades = targetGrades.map { com.lifechurch.heroesinwaiting.data.model.Grade.valueOf(it) },
            difficultyLevel = difficultyLevel?.let { com.lifechurch.heroesinwaiting.data.model.DifficultyLevel.valueOf(it) } 
                ?: com.lifechurch.heroesinwaiting.data.model.DifficultyLevel.BEGINNER,
            category = com.lifechurch.heroesinwaiting.data.model.LessonCategory.valueOf(category),
            tags = tags,
            isActive = isActive,
            createdAt = cachedAt.toString(),
            updatedAt = lastUpdated.toString(),
            content = com.lifechurch.heroesinwaiting.data.model.LessonContent(
                introduction = com.lifechurch.heroesinwaiting.data.model.ContentSection(
                    id = "intro",
                    title = "Introduction",
                    content = "Loading...", // Will be loaded on demand
                    contentType = com.lifechurch.heroesinwaiting.data.model.ContentType.TEXT,
                    estimatedDuration = 5
                ),
                mainContent = emptyList(), // Will be loaded on demand
                conclusion = com.lifechurch.heroesinwaiting.data.model.ContentSection(
                    id = "conclusion",
                    title = "Conclusion",
                    content = "Loading...", // Will be loaded on demand
                    contentType = com.lifechurch.heroesinwaiting.data.model.ContentType.TEXT,
                    estimatedDuration = 5
                )
            ),
            activities = emptyList(), // Will be loaded on demand
            assessments = emptyList(),
            prerequisites = emptyList(),
            nextLessons = emptyList(),
            accessibilityFeatures = com.lifechurch.heroesinwaiting.data.model.AccessibilityFeatures(),
            languageSupport = listOf("en"),
            isDownloaded = isDownloaded,
            isBookmarked = isBookmarked
        )
    }
    
    /**
     * Converts to domain model with metadata only (for list views)
     */
    fun toDomainModelMetadata(): com.lifechurch.heroesinwaiting.data.model.Lesson {
        return com.lifechurch.heroesinwaiting.data.model.Lesson(
            id = id,
            title = title,
            description = description,
            lessonNumber = orderIndex,
            objectives = emptyList(),
            keyTerms = emptyList(),
            estimatedDuration = estimatedDuration,
            targetGrades = targetGrades.map { com.lifechurch.heroesinwaiting.data.model.Grade.valueOf(it) },
            difficultyLevel = difficultyLevel?.let { com.lifechurch.heroesinwaiting.data.model.DifficultyLevel.valueOf(it) } 
                ?: com.lifechurch.heroesinwaiting.data.model.DifficultyLevel.BEGINNER,
            category = com.lifechurch.heroesinwaiting.data.model.LessonCategory.valueOf(category),
            tags = tags,
            isActive = isActive,
            createdAt = cachedAt.toString(),
            updatedAt = lastUpdated.toString(),
            content = com.lifechurch.heroesinwaiting.data.model.LessonContent(
                introduction = com.lifechurch.heroesinwaiting.data.model.ContentSection(
                    id = "intro",
                    title = "Introduction",
                    content = "",
                    contentType = com.lifechurch.heroesinwaiting.data.model.ContentType.TEXT,
                    estimatedDuration = 5
                ),
                mainContent = emptyList(),
                conclusion = com.lifechurch.heroesinwaiting.data.model.ContentSection(
                    id = "conclusion",
                    title = "Conclusion",
                    content = "",
                    contentType = com.lifechurch.heroesinwaiting.data.model.ContentType.TEXT,
                    estimatedDuration = 5
                )
            ),
            activities = emptyList(),
            assessments = emptyList(),
            prerequisites = emptyList(),
            nextLessons = emptyList(),
            accessibilityFeatures = com.lifechurch.heroesinwaiting.data.model.AccessibilityFeatures(),
            languageSupport = listOf("en"),
            isDownloaded = isDownloaded,
            isBookmarked = isBookmarked
        )
    }
}