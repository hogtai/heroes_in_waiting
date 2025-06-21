package com.lifechurch.heroesinwaiting.data.api

import com.lifechurch.heroesinwaiting.data.api.response.*
import com.lifechurch.heroesinwaiting.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Main API Service interface for Heroes in Waiting backend
 * Integration with enhanced backend API running at localhost:3000
 */
interface ApiService {
    
    // ================== Authentication Endpoints ==================
    
    @POST("auth/facilitator/login")
    suspend fun loginFacilitator(
        @Body request: FacilitatorLoginRequest
    ): Response<AuthResponse>
    
    @POST("auth/facilitator/register")
    suspend fun registerFacilitator(
        @Body request: FacilitatorRegistrationRequest
    ): Response<AuthResponse>
    
    @POST("auth/facilitator/refresh")
    suspend fun refreshToken(
        @Header("Authorization") token: String
    ): Response<AuthToken>
    
    @POST("auth/logout")
    suspend fun logout(
        @Header("Authorization") token: String
    ): Response<Unit>
    
    // ================== Enhanced Student Endpoints ==================
    
    @GET("enhanced-students/classroom-preview/{code}")
    suspend fun getClassroomPreview(
        @Path("code") classroomCode: String
    ): Response<ClassroomPreviewResponse>
    
    @POST("enhanced-students/join")
    suspend fun joinClassroom(
        @Body request: StudentEnrollmentRequest
    ): Response<StudentEnrollmentResponse>
    
    @POST("enhanced-students/emotional-checkin")
    suspend fun submitEmotionalCheckin(
        @Header("Authorization") token: String,
        @Body request: EmotionalCheckinRequest
    ): Response<EmotionalCheckinResponse>
    
    @GET("enhanced-students/lessons/{id}/mobile-optimized")
    suspend fun getMobileOptimizedLesson(
        @Path("id") lessonId: String,
        @Header("Authorization") token: String,
        @Query("device_type") deviceType: String = "mobile",
        @Query("screen_density") screenDensity: String? = null
    ): Response<MobileOptimizedLessonResponse>
    
    // ================== Facilitator Endpoints ==================
    
    @GET("facilitators/profile")
    suspend fun getFacilitatorProfile(
        @Header("Authorization") token: String
    ): Response<Facilitator>
    
    @PUT("facilitators/profile")
    suspend fun updateFacilitatorProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateFacilitatorRequest
    ): Response<Facilitator>
    
    // ================== Classroom Endpoints ==================
    
    @GET("classrooms")
    suspend fun getClassrooms(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ClassroomListResponse>
    
    @GET("classrooms/{id}")
    suspend fun getClassroomDetails(
        @Path("id") classroomId: String,
        @Header("Authorization") token: String
    ): Response<ClassroomDetailsResponse>
    
    @POST("classrooms")
    suspend fun createClassroom(
        @Header("Authorization") token: String,
        @Body request: CreateClassroomRequest
    ): Response<Classroom>
    
    @PUT("classrooms/{id}")
    suspend fun updateClassroom(
        @Path("id") classroomId: String,
        @Header("Authorization") token: String,
        @Body request: UpdateClassroomRequest
    ): Response<Classroom>
    
    @DELETE("classrooms/{id}")
    suspend fun deleteClassroom(
        @Path("id") classroomId: String,
        @Header("Authorization") token: String
    ): Response<Unit>
    
    @POST("classrooms/{id}/sessions")
    suspend fun startClassroomSession(
        @Path("id") classroomId: String,
        @Header("Authorization") token: String,
        @Body request: StartSessionRequest
    ): Response<ClassroomSession>
    
    @PUT("classrooms/{id}/sessions/{sessionId}/end")
    suspend fun endClassroomSession(
        @Path("id") classroomId: String,
        @Path("sessionId") sessionId: String,
        @Header("Authorization") token: String
    ): Response<ClassroomSession>
    
    // ================== Lesson Endpoints ==================
    
    @GET("lessons")
    suspend fun getLessons(
        @Query("grade") grade: String? = null,
        @Query("category") category: String? = null,
        @Query("difficulty") difficulty: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<LessonListResponse>
    
    @GET("lessons/{id}")
    suspend fun getLessonDetails(
        @Path("id") lessonId: String,
        @Header("Authorization") token: String
    ): Response<Lesson>
    
    @GET("lessons/recommended")
    suspend fun getRecommendedLessons(
        @Header("Authorization") token: String,
        @Query("grade") grade: String,
        @Query("completed_lessons") completedLessons: String? = null
    ): Response<LessonListResponse>
    
    // ================== Student Progress Endpoints ==================
    
    @GET("students/{id}/progress")
    suspend fun getStudentProgress(
        @Path("id") studentId: String,
        @Header("Authorization") token: String
    ): Response<StudentProgressResponse>
    
    @POST("students/{id}/progress")
    suspend fun updateStudentProgress(
        @Path("id") studentId: String,
        @Header("Authorization") token: String,
        @Body request: ProgressUpdateRequest
    ): Response<StudentProgressResponse>
    
    @POST("students/{id}/feedback")
    suspend fun submitStudentFeedback(
        @Path("id") studentId: String,
        @Header("Authorization") token: String,
        @Body request: StudentFeedbackRequest
    ): Response<Unit>
    
    // ================== Analytics Endpoints ==================
    
    @POST("analytics/events")
    suspend fun logAnalyticsEvent(
        @Header("Authorization") token: String,
        @Body request: AnalyticsEventRequest
    ): Response<Unit>
    
    @GET("analytics/classroom/{id}/summary")
    suspend fun getClassroomAnalytics(
        @Path("id") classroomId: String,
        @Header("Authorization") token: String,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): Response<ClassroomAnalyticsResponse>
}

/**
 * API Service specifically for student operations (no JWT required)
 */
interface StudentApiService {
    
    @GET("enhanced-students/classroom-preview/{code}")
    suspend fun getClassroomPreview(
        @Path("code") classroomCode: String
    ): Response<ClassroomPreviewResponse>
    
    @POST("enhanced-students/join")
    suspend fun joinClassroom(
        @Body request: StudentEnrollmentRequest
    ): Response<StudentEnrollmentResponse>
    
    @GET("lessons/{id}/student-view")
    suspend fun getStudentLesson(
        @Path("id") lessonId: String,
        @Query("session_id") sessionId: String
    ): Response<StudentLessonResponse>
    
    @POST("students/session/{sessionId}/emotional-checkin")
    suspend fun submitEmotionalCheckin(
        @Path("sessionId") sessionId: String,
        @Body request: EmotionalCheckinRequest
    ): Response<EmotionalCheckinResponse>
    
    @POST("students/session/{sessionId}/activity-response")
    suspend fun submitActivityResponse(
        @Path("sessionId") sessionId: String,
        @Body request: ActivityResponseRequest
    ): Response<Unit>
}