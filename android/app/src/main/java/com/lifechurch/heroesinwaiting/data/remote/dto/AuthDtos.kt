package com.lifechurch.heroesinwaiting.data.remote.dto

import kotlinx.serialization.Serializable

// Authentication request/response DTOs
@Serializable
data class RegisterFacilitatorRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
    val organization: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val success: Boolean,
    val message: String,
    val data: AuthData? = null
)

@Serializable
data class AuthData(
    val token: String,
    val facilitator: FacilitatorData,
    val expiresIn: Long
)

@Serializable
data class FacilitatorData(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val organization: String,
    val role: String,
    val isActive: Boolean,
    val createdAt: String
)

@Serializable
data class StudentEnrollmentRequest(
    val classroomCode: String,
    val deviceId: String? = null
)

@Serializable
data class StudentEnrollmentResponse(
    val success: Boolean,
    val message: String,
    val data: StudentEnrollmentData? = null
)

@Serializable
data class StudentEnrollmentData(
    val studentId: String,
    val classroom: ClassroomData,
    val sessionToken: String? = null
)

@Serializable
data class ClassroomPreviewResponse(
    val success: Boolean,
    val message: String,
    val data: ClassroomPreviewData? = null
)

@Serializable
data class ClassroomPreviewData(
    val classroomCode: String,
    val teacherName: String,
    val schoolName: String,
    val gradeLevel: String,
    val studentCount: Int,
    val isActive: Boolean
)

@Serializable
data class FacilitatorProfileResponse(
    val success: Boolean,
    val message: String,
    val data: FacilitatorProfileData? = null
)

@Serializable
data class FacilitatorProfileData(
    val facilitator: FacilitatorData,
    val classroomCount: Int,
    val totalStudents: Int,
    val completedSessions: Int
)