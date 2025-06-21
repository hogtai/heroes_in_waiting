package com.lifechurch.heroesinwaiting.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lifechurch.heroesinwaiting.data.model.*

/**
 * Room entity for storing lesson data locally for offline access
 */
@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val lessonNumber: Int,
    val objectivesJson: String, // JSON array of objectives
    val keyTermsJson: String, // JSON array of key terms
    val estimatedDuration: Int,
    val targetGradesJson: String, // JSON array of grade strings
    val difficultyLevel: String,
    val category: String,
    val tagsJson: String, // JSON array of tags
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String?,
    // Content (simplified for local storage)
    val contentJson: String, // Serialized LessonContent
    val activitiesJson: String, // Serialized list of activities
    val assessmentsJson: String, // Serialized list of assessments
    val prerequisitesJson: String, // JSON array of prerequisite lesson IDs
    val nextLessonsJson: String, // JSON array of next lesson IDs
    // Accessibility and localization
    val accessibilityFeaturesJson: String, // Serialized AccessibilityFeatures
    val languageSupportJson: String, // JSON array of language codes
    // Local-only fields
    val lastSyncAt: Long = System.currentTimeMillis(),
    val isDownloaded: Boolean = false,
    val downloadSize: Long = 0L,
    val offlineAvailable: Boolean = false
) {
    
    /**
     * Converts entity to domain model
     */
    fun toDomainModel(): Lesson {
        // Parse JSON fields
        val objectives = parseJsonStringList(objectivesJson)
        val keyTerms = parseJsonStringList(keyTermsJson)
        val targetGradesStrings = parseJsonStringList(targetGradesJson)
        val tags = parseJsonStringList(tagsJson)
        val prerequisites = parseJsonStringList(prerequisitesJson)
        val nextLessons = parseJsonStringList(nextLessonsJson)
        val languageSupport = parseJsonStringList(languageSupportJson)
        
        // Convert grade strings to Grade enums
        val targetGrades = targetGradesStrings.mapNotNull { gradeString ->
            when (gradeString.lowercase()) {
                "grade_4" -> Grade.GRADE_4
                "grade_5" -> Grade.GRADE_5
                "grade_6" -> Grade.GRADE_6
                else -> null
            }
        }
        
        // Convert difficulty level
        val difficultyLevelEnum = when (difficultyLevel.lowercase()) {
            "beginner" -> DifficultyLevel.BEGINNER
            "intermediate" -> DifficultyLevel.INTERMEDIATE
            "advanced" -> DifficultyLevel.ADVANCED
            else -> DifficultyLevel.BEGINNER
        }
        
        // Convert category
        val categoryEnum = when (category.lowercase()) {
            "bullying_prevention" -> LessonCategory.BULLYING_PREVENTION
            "empathy_building" -> LessonCategory.EMPATHY_BUILDING
            "conflict_resolution" -> LessonCategory.CONFLICT_RESOLUTION
            "self_confidence" -> LessonCategory.SELF_CONFIDENCE
            "communication_skills" -> LessonCategory.COMMUNICATION_SKILLS
            "community_building" -> LessonCategory.COMMUNITY_BUILDING
            "leadership" -> LessonCategory.LEADERSHIP
            "respect_and_kindness" -> LessonCategory.RESPECT_AND_KINDNESS
            else -> LessonCategory.BULLYING_PREVENTION
        }
        
        // Parse complex objects (simplified for demo - in production would use proper JSON parsing)
        val content = parseContentJson(contentJson)
        val activities = parseActivitiesJson(activitiesJson)
        val assessments = parseAssessmentsJson(assessmentsJson)
        val accessibilityFeatures = parseAccessibilityFeaturesJson(accessibilityFeaturesJson)
        
        return Lesson(
            id = id,
            title = title,
            description = description,
            lessonNumber = lessonNumber,
            objectives = objectives,
            keyTerms = keyTerms,
            estimatedDuration = estimatedDuration,
            targetGrades = targetGrades,
            difficultyLevel = difficultyLevelEnum,
            category = categoryEnum,
            tags = tags,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt,
            content = content,
            activities = activities,
            assessments = assessments,
            prerequisites = prerequisites,
            nextLessons = nextLessons,
            accessibilityFeatures = accessibilityFeatures,
            languageSupport = languageSupport
        )
    }
    
    private fun parseJsonStringList(json: String): List<String> {
        return try {
            kotlinx.serialization.json.Json.decodeFromString<List<String>>(json)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun parseContentJson(json: String): LessonContent {
        // Simplified parsing - in production would use proper JSON serialization
        return try {
            kotlinx.serialization.json.Json.decodeFromString<LessonContent>(json)
        } catch (e: Exception) {
            // Return default content structure
            LessonContent(
                introduction = ContentSection(
                    id = "intro",
                    title = "Introduction",
                    content = "Welcome to this lesson",
                    contentType = ContentType.TEXT,
                    estimatedDuration = 2
                ),
                mainContent = emptyList(),
                conclusion = ContentSection(
                    id = "conclusion",
                    title = "Conclusion",
                    content = "Thank you for participating",
                    contentType = ContentType.TEXT,
                    estimatedDuration = 2
                )
            )
        }
    }
    
    private fun parseActivitiesJson(json: String): List<Activity> {
        return try {
            kotlinx.serialization.json.Json.decodeFromString<List<Activity>>(json)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun parseAssessmentsJson(json: String): List<Assessment> {
        return try {
            kotlinx.serialization.json.Json.decodeFromString<List<Assessment>>(json)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun parseAccessibilityFeaturesJson(json: String): AccessibilityFeatures {
        return try {
            kotlinx.serialization.json.Json.decodeFromString<AccessibilityFeatures>(json)
        } catch (e: Exception) {
            AccessibilityFeatures()
        }
    }
    
    companion object {
        /**
         * Creates entity from domain model
         */
        fun fromDomainModel(lesson: Lesson): LessonEntity {
            val objectivesJson = encodeJsonStringList(lesson.objectives)
            val keyTermsJson = encodeJsonStringList(lesson.keyTerms)
            val targetGradesJson = encodeJsonStringList(lesson.targetGrades.map { it.name })
            val tagsJson = encodeJsonStringList(lesson.tags)
            val prerequisitesJson = encodeJsonStringList(lesson.prerequisites)
            val nextLessonsJson = encodeJsonStringList(lesson.nextLessons)
            val languageSupportJson = encodeJsonStringList(lesson.languageSupport)
            
            // Serialize complex objects
            val contentJson = try {
                kotlinx.serialization.json.Json.encodeToString(lesson.content)
            } catch (e: Exception) {
                "{}"
            }
            
            val activitiesJson = try {
                kotlinx.serialization.json.Json.encodeToString(lesson.activities)
            } catch (e: Exception) {
                "[]"
            }
            
            val assessmentsJson = try {
                kotlinx.serialization.json.Json.encodeToString(lesson.assessments)
            } catch (e: Exception) {
                "[]"
            }
            
            val accessibilityFeaturesJson = try {
                kotlinx.serialization.json.Json.encodeToString(lesson.accessibilityFeatures)
            } catch (e: Exception) {
                "{}"
            }
            
            return LessonEntity(
                id = lesson.id,
                title = lesson.title,
                description = lesson.description,
                lessonNumber = lesson.lessonNumber,
                objectivesJson = objectivesJson,
                keyTermsJson = keyTermsJson,
                estimatedDuration = lesson.estimatedDuration,
                targetGradesJson = targetGradesJson,
                difficultyLevel = lesson.difficultyLevel.name,
                category = lesson.category.name,
                tagsJson = tagsJson,
                isActive = lesson.isActive,
                createdAt = lesson.createdAt,
                updatedAt = lesson.updatedAt,
                contentJson = contentJson,
                activitiesJson = activitiesJson,
                assessmentsJson = assessmentsJson,
                prerequisitesJson = prerequisitesJson,
                nextLessonsJson = nextLessonsJson,
                accessibilityFeaturesJson = accessibilityFeaturesJson,
                languageSupportJson = languageSupportJson
            )
        }
        
        private fun encodeJsonStringList(list: List<String>): String {
            return try {
                kotlinx.serialization.json.Json.encodeToString(list)
            } catch (e: Exception) {
                "[]"
            }
        }
    }
}