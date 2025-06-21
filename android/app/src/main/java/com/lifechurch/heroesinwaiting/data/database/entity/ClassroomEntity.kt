package com.lifechurch.heroesinwaiting.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lifechurch.heroesinwaiting.data.model.Classroom
import com.lifechurch.heroesinwaiting.data.model.Grade

/**
 * Room entity for storing classroom data locally
 */
@Entity(tableName = "classrooms")
data class ClassroomEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String?,
    val facilitatorId: String,
    val grade: String, // Stored as string for Room compatibility
    val classroomCode: String,
    val isActive: Boolean,
    val maxStudents: Int,
    val currentStudentCount: Int,
    val createdAt: String,
    val updatedAt: String?,
    // Session information
    val hasActiveSession: Boolean = false,
    val currentSessionId: String? = null,
    val currentLessonId: String? = null,
    val sessionStartTime: String? = null,
    // Settings
    val allowLateJoin: Boolean = true,
    val requiresApproval: Boolean = false,
    val isPublic: Boolean = true,
    val settingsJson: String = "{}", // JSON for additional settings
    // Local-only fields
    val lastSyncAt: Long = System.currentTimeMillis(),
    val isPendingSync: Boolean = false,
    val offlineChanges: String? = null
) {
    
    /**
     * Converts entity to domain model
     */
    fun toDomainModel(): Classroom {
        val gradeEnum = when (grade.lowercase()) {
            "grade_4" -> Grade.GRADE_4
            "grade_5" -> Grade.GRADE_5
            "grade_6" -> Grade.GRADE_6
            else -> Grade.OTHER
        }
        
        return Classroom(
            id = id,
            name = name,
            description = description,
            facilitatorId = facilitatorId,
            grade = gradeEnum,
            classroomCode = classroomCode,
            isActive = isActive,
            maxStudents = maxStudents,
            currentStudentCount = currentStudentCount,
            createdAt = createdAt,
            updatedAt = updatedAt,
            hasActiveSession = hasActiveSession,
            currentSessionId = currentSessionId,
            currentLessonId = currentLessonId,
            sessionStartTime = sessionStartTime,
            allowLateJoin = allowLateJoin,
            requiresApproval = requiresApproval,
            isPublic = isPublic
        )
    }
    
    companion object {
        /**
         * Creates entity from domain model
         */
        fun fromDomainModel(classroom: Classroom): ClassroomEntity {
            return ClassroomEntity(
                id = classroom.id,
                name = classroom.name,
                description = classroom.description,
                facilitatorId = classroom.facilitatorId,
                grade = classroom.grade.name,
                classroomCode = classroom.classroomCode,
                isActive = classroom.isActive,
                maxStudents = classroom.maxStudents,
                currentStudentCount = classroom.currentStudentCount,
                createdAt = classroom.createdAt,
                updatedAt = classroom.updatedAt,
                hasActiveSession = classroom.hasActiveSession,
                currentSessionId = classroom.currentSessionId,
                currentLessonId = classroom.currentLessonId,
                sessionStartTime = classroom.sessionStartTime,
                allowLateJoin = classroom.allowLateJoin,
                requiresApproval = classroom.requiresApproval,
                isPublic = classroom.isPublic
            )
        }
    }
}