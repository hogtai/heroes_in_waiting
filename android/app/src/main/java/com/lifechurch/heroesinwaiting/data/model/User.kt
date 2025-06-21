package com.lifechurch.heroesinwaiting.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * Represents different types of users in the Heroes in Waiting system
 */
enum class UserType {
    FACILITATOR,
    STUDENT
}

/**
 * Base user interface for common user properties
 */
interface User {
    val id: String
    val userType: UserType
    val isActive: Boolean
    val createdAt: String
    val updatedAt: String?
}

/**
 * Facilitator user data model
 * Represents adult users who manage classrooms and facilitate lessons
 */
@Parcelize
data class Facilitator(
    override val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val organization: String? = null,
    val role: String? = null,
    override val isActive: Boolean = true,
    override val createdAt: String,
    override val updatedAt: String? = null,
    // Additional facilitator-specific properties
    val emailVerified: Boolean = false,
    val lastLoginAt: String? = null,
    val classroomCount: Int = 0
) : User, Parcelable {
    override val userType: UserType = UserType.FACILITATOR
    
    val fullName: String
        get() = "$firstName $lastName"
    
    val displayName: String
        get() = if (organization.isNullOrBlank()) fullName else "$fullName ($organization)"
}

/**
 * Student user data model
 * Represents elementary students (grades 4-6) with COPPA-compliant data collection
 */
@Parcelize
data class Student(
    override val id: String,
    val sessionId: String, // Unique session identifier for anonymity
    val classroomId: String,
    val grade: Grade,
    val demographicInfo: DemographicInfo,
    override val isActive: Boolean = true,
    override val createdAt: String,
    override val updatedAt: String? = null,
    // Student-specific properties
    val currentLessonId: String? = null,
    val completedLessons: List<String> = emptyList(),
    val totalProgressPercentage: Float = 0f,
    val lastActiveAt: String? = null
) : User, Parcelable {
    override val userType: UserType = UserType.STUDENT
}

/**
 * Student grade levels supported by the system
 */
@Parcelize
enum class Grade : Parcelable {
    GRADE_4,
    GRADE_5,
    GRADE_6,
    OTHER;
    
    val displayName: String
        get() = when (this) {
            GRADE_4 -> "4th Grade"
            GRADE_5 -> "5th Grade" 
            GRADE_6 -> "6th Grade"
            OTHER -> "Other"
        }
    
    val numericValue: Int
        get() = when (this) {
            GRADE_4 -> 4
            GRADE_5 -> 5
            GRADE_6 -> 6
            OTHER -> 0
        }
}

/**
 * COPPA-compliant demographic information for students
 * No personally identifiable information is collected
 */
@Parcelize
data class DemographicInfo(
    val grade: Grade,
    val schoolType: SchoolType? = null,
    val region: String? = null, // General geographic region (e.g., "Southwest", "Northeast")
    val hasSpecialNeeds: Boolean? = null, // For accessibility accommodations
    val primaryLanguage: String? = null // For language support features
) : Parcelable

/**
 * Types of educational institutions
 */
@Parcelize
enum class SchoolType : Parcelable {
    PUBLIC,
    PRIVATE,
    CHARTER,
    HOMESCHOOL,
    OTHER;
    
    val displayName: String
        get() = when (this) {
            PUBLIC -> "Public School"
            PRIVATE -> "Private School"
            CHARTER -> "Charter School"
            HOMESCHOOL -> "Homeschool"
            OTHER -> "Other"
        }
}

/**
 * Authentication token response from the API
 */
@Parcelize
data class AuthToken(
    val token: String,
    val refreshToken: String? = null,
    val expiresAt: String,
    val tokenType: String = "Bearer"
) : Parcelable

/**
 * Authentication response containing user data and token
 */
@Parcelize
data class AuthResponse(
    val user: User,
    val token: AuthToken
) : Parcelable

/**
 * Login request for facilitators
 */
data class FacilitatorLoginRequest(
    val email: String,
    val password: String
)

/**
 * Registration request for facilitators
 */
data class FacilitatorRegistrationRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val organization: String? = null,
    val role: String? = null
)

/**
 * Student enrollment request using classroom code
 */
data class StudentEnrollmentRequest(
    val classroomCode: String,
    val grade: Grade,
    val demographicInfo: DemographicInfo
)

/**
 * Utility functions for user management
 */
object UserUtils {
    
    fun generateSessionId(): String = UUID.randomUUID().toString()
    
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    fun isValidPassword(password: String): Boolean {
        // Password requirements: At least 8 characters, one uppercase, one lowercase, one digit, one special character
        val passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,}\$"
        return password.matches(passwordPattern.toRegex())
    }
    
    fun getGradeFromString(gradeString: String): Grade {
        return when (gradeString.lowercase()) {
            "4", "grade_4", "4th grade" -> Grade.GRADE_4
            "5", "grade_5", "5th grade" -> Grade.GRADE_5
            "6", "grade_6", "6th grade" -> Grade.GRADE_6
            else -> Grade.OTHER
        }
    }
}