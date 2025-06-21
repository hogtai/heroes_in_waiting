package com.lifechurch.heroesinwaiting.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents a classroom where lessons are delivered
 */
@Parcelize
data class Classroom(
    val id: String,
    val facilitatorId: String,
    val name: String,
    val description: String? = null,
    val classroomCode: String, // 6-digit code for student enrollment
    val grade: Grade,
    val maxStudents: Int = 30,
    val currentStudentCount: Int = 0,
    val isActive: Boolean = true,
    val createdAt: String,
    val updatedAt: String? = null,
    // Session management
    val currentSessionId: String? = null,
    val isSessionActive: Boolean = false,
    val lastSessionAt: String? = null,
    // Progress tracking
    val currentLessonId: String? = null,
    val completedLessons: List<String> = emptyList(),
    val totalLessons: Int = 12,
    val progressPercentage: Float = 0f,
    // Settings
    val allowLateJoining: Boolean = true,
    val requireAttendanceCode: Boolean = false,
    val enableChatFeatures: Boolean = false,
    val enablePeerInteraction: Boolean = true
) : Parcelable {
    
    val isClassroomFull: Boolean
        get() = currentStudentCount >= maxStudents
    
    val hasStartedProgram: Boolean
        get() = completedLessons.isNotEmpty() || currentLessonId != null
    
    val nextLessonNumber: Int
        get() = completedLessons.size + 1
    
    val remainingLessons: Int
        get() = totalLessons - completedLessons.size
    
    val canStartNewSession: Boolean
        get() = isActive && !isSessionActive && currentStudentCount > 0
}

/**
 * Classroom session for real-time lesson delivery
 */
@Parcelize
data class ClassroomSession(
    val id: String,
    val classroomId: String,
    val facilitatorId: String,
    val lessonId: String,
    val sessionCode: String? = null, // Optional attendance code
    val startedAt: String,
    val endedAt: String? = null,
    val isActive: Boolean = true,
    val attendees: List<String> = emptyList(), // Student IDs
    val maxAttendees: Int = 30,
    val sessionType: SessionType = SessionType.REGULAR,
    val settings: SessionSettings = SessionSettings()
) : Parcelable {
    
    val duration: Long?
        get() = if (endedAt != null) {
            // Calculate duration in minutes
            // This would need proper date parsing in a real implementation
            0L
        } else null
    
    val attendeeCount: Int
        get() = attendees.size
    
    val canJoin: Boolean
        get() = isActive && attendeeCount < maxAttendees
}

/**
 * Types of classroom sessions
 */
@Parcelize
enum class SessionType : Parcelable {
    REGULAR,     // Standard lesson delivery
    REVIEW,      // Review of previous lessons
    MAKEUP,      // Makeup session for absent students
    ASSESSMENT,  // Assessment or feedback collection
    FREE_PLAY;   // Open discussion or activities
    
    val displayName: String
        get() = when (this) {
            REGULAR -> "Regular Lesson"
            REVIEW -> "Review Session"
            MAKEUP -> "Makeup Session"
            ASSESSMENT -> "Assessment"
            FREE_PLAY -> "Free Discussion"
        }
}

/**
 * Session configuration settings
 */
@Parcelize
data class SessionSettings(
    val allowLateJoining: Boolean = true,
    val enableChat: Boolean = false,
    val enableVideoSharing: Boolean = true,
    val enableScreenShare: Boolean = false,
    val muteStudentsOnJoin: Boolean = false,
    val enableHandRaising: Boolean = true,
    val enableBreakoutRooms: Boolean = false,
    val autoRecordSession: Boolean = false,
    val maxSessionDuration: Int = 60, // minutes
    val warningBeforeEnd: Int = 5 // minutes
) : Parcelable

/**
 * Classroom statistics and analytics
 */
@Parcelize
data class ClassroomStats(
    val classroomId: String,
    val totalStudents: Int,
    val activeStudents: Int,
    val completedSessions: Int,
    val averageAttendance: Float,
    val averageEngagement: Float,
    val lessonsCompleted: Int,
    val totalLessonTime: Long, // in minutes
    val lastActivityAt: String,
    val createdAt: String
) : Parcelable

/**
 * Request models for classroom operations
 */

data class CreateClassroomRequest(
    val name: String,
    val description: String? = null,
    val grade: Grade,
    val maxStudents: Int = 30,
    val allowLateJoining: Boolean = true,
    val requireAttendanceCode: Boolean = false,
    val enableChatFeatures: Boolean = false,
    val enablePeerInteraction: Boolean = true
)

data class UpdateClassroomRequest(
    val name: String? = null,
    val description: String? = null,
    val maxStudents: Int? = null,
    val allowLateJoining: Boolean? = null,
    val requireAttendanceCode: Boolean? = null,
    val enableChatFeatures: Boolean? = null,
    val enablePeerInteraction: Boolean? = null,
    val isActive: Boolean? = null
)

data class StartSessionRequest(
    val lessonId: String,
    val sessionType: SessionType = SessionType.REGULAR,
    val requireAttendanceCode: Boolean = false,
    val sessionSettings: SessionSettings = SessionSettings()
)

data class JoinClassroomRequest(
    val classroomCode: String,
    val attendanceCode: String? = null
)

/**
 * Response models
 */
@Parcelize
data class ClassroomListResponse(
    val classrooms: List<Classroom>,
    val totalCount: Int,
    val hasMore: Boolean
) : Parcelable

@Parcelize
data class ClassroomDetailsResponse(
    val classroom: Classroom,
    val students: List<Student>,
    val recentSessions: List<ClassroomSession>,
    val stats: ClassroomStats
) : Parcelable

/**
 * Utility functions for classroom management
 */
object ClassroomUtils {
    
    /**
     * Generates a 6-digit classroom code
     */
    fun generateClassroomCode(): String {
        return (100000..999999).random().toString()
    }
    
    /**
     * Validates classroom code format
     */
    fun isValidClassroomCode(code: String): Boolean {
        return code.matches("^[0-9]{6}$".toRegex())
    }
    
    /**
     * Generates a 4-digit attendance code
     */
    fun generateAttendanceCode(): String {
        return (1000..9999).random().toString()
    }
    
    /**
     * Validates classroom name
     */
    fun isValidClassroomName(name: String): Boolean {
        return name.trim().isNotEmpty() && name.length <= 100
    }
    
    /**
     * Gets classroom status display text
     */
    fun getClassroomStatusText(classroom: Classroom): String {
        return when {
            !classroom.isActive -> "Inactive"
            classroom.isSessionActive -> "In Session"
            classroom.currentStudentCount == 0 -> "No Students"
            classroom.hasStartedProgram -> "In Progress"
            else -> "Ready to Start"
        }
    }
    
    /**
     * Calculates progress percentage
     */
    fun calculateProgressPercentage(completedLessons: Int, totalLessons: Int): Float {
        return if (totalLessons > 0) {
            (completedLessons.toFloat() / totalLessons.toFloat()) * 100f
        } else {
            0f
        }
    }
}