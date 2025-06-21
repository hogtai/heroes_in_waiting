package com.lifechurch.heroesinwaiting.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lifechurch.heroesinwaiting.data.model.Student
import com.lifechurch.heroesinwaiting.data.model.Grade
import com.lifechurch.heroesinwaiting.data.model.DemographicInfo
import com.lifechurch.heroesinwaiting.data.model.SchoolType

/**
 * Room entity for storing student data locally
 * Note: Only stores non-PII data in compliance with COPPA
 */
@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val classroomId: String,
    val grade: String, // Stored as string to handle enum conversion
    // Demographic information (non-PII)
    val schoolType: String?,
    val region: String?,
    val hasSpecialNeeds: Boolean?,
    val primaryLanguage: String?,
    // Student progress and activity
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String?,
    val currentLessonId: String?,
    val completedLessonsJson: String, // JSON array of lesson IDs
    val totalProgressPercentage: Float,
    val lastActiveAt: String?,
    // Local-only fields
    val lastSyncAt: Long = System.currentTimeMillis(),
    val isPendingSync: Boolean = false,
    val offlineChanges: String? = null // JSON of offline changes
) {
    
    /**
     * Converts entity to domain model
     */
    fun toDomainModel(): Student {
        val gradeEnum = when (grade.lowercase()) {
            "grade_4" -> Grade.GRADE_4
            "grade_5" -> Grade.GRADE_5
            "grade_6" -> Grade.GRADE_6
            else -> Grade.OTHER
        }
        
        val schoolTypeEnum = schoolType?.let { type ->
            when (type.lowercase()) {
                "public" -> SchoolType.PUBLIC
                "private" -> SchoolType.PRIVATE
                "charter" -> SchoolType.CHARTER
                "homeschool" -> SchoolType.HOMESCHOOL
                else -> SchoolType.OTHER
            }
        }
        
        val demographicInfo = DemographicInfo(
            grade = gradeEnum,
            schoolType = schoolTypeEnum,
            region = region,
            hasSpecialNeeds = hasSpecialNeeds,
            primaryLanguage = primaryLanguage
        )
        
        // Parse completed lessons from JSON
        val completedLessons = try {
            if (completedLessonsJson.isNotEmpty()) {
                kotlinx.serialization.json.Json.decodeFromString<List<String>>(completedLessonsJson)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList<String>()
        }
        
        return Student(
            id = id,
            sessionId = sessionId,
            classroomId = classroomId,
            grade = gradeEnum,
            demographicInfo = demographicInfo,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt,
            currentLessonId = currentLessonId,
            completedLessons = completedLessons,
            totalProgressPercentage = totalProgressPercentage,
            lastActiveAt = lastActiveAt
        )
    }
    
    companion object {
        /**
         * Creates entity from domain model
         */
        fun fromDomainModel(student: Student): StudentEntity {
            val completedLessonsJson = try {
                kotlinx.serialization.json.Json.encodeToString(student.completedLessons)
            } catch (e: Exception) {
                "[]"
            }
            
            return StudentEntity(
                id = student.id,
                sessionId = student.sessionId,
                classroomId = student.classroomId,
                grade = student.grade.name,
                schoolType = student.demographicInfo.schoolType?.name,
                region = student.demographicInfo.region,
                hasSpecialNeeds = student.demographicInfo.hasSpecialNeeds,
                primaryLanguage = student.demographicInfo.primaryLanguage,
                isActive = student.isActive,
                createdAt = student.createdAt,
                updatedAt = student.updatedAt,
                currentLessonId = student.currentLessonId,
                completedLessonsJson = completedLessonsJson,
                totalProgressPercentage = student.totalProgressPercentage,
                lastActiveAt = student.lastActiveAt
            )
        }
    }
}