package com.lifechurch.heroesinwaiting.data.remote

import com.lifechurch.heroesinwaiting.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    // Authentication endpoints
    @POST("auth/facilitator/register")
    suspend fun registerFacilitator(@Body request: RegisterFacilitatorRequest): Response<AuthResponse>
    
    @POST("auth/facilitator/login")
    suspend fun loginFacilitator(@Body request: LoginRequest): Response<AuthResponse>
    
    @POST("auth/student/enroll")
    suspend fun enrollStudent(@Body request: StudentEnrollmentRequest): Response<StudentEnrollmentResponse>
    
    @GET("classrooms/{classroomCode}/preview")
    suspend fun getClassroomPreview(@Path("classroomCode") classroomCode: String): Response<ClassroomPreviewResponse>
    
    // Facilitator endpoints
    @GET("facilitator/profile")
    suspend fun getFacilitatorProfile(@Header("Authorization") token: String): Response<FacilitatorProfileResponse>
    
    @GET("facilitator/classrooms")
    suspend fun getFacilitatorClassrooms(@Header("Authorization") token: String): Response<ClassroomsResponse>
    
    @POST("facilitator/classrooms")
    suspend fun createClassroom(
        @Header("Authorization") token: String,
        @Body request: CreateClassroomRequest
    ): Response<ClassroomResponse>
    
    @GET("facilitator/classrooms/{classroomId}/students")
    suspend fun getClassroomStudents(
        @Header("Authorization") token: String,
        @Path("classroomId") classroomId: String
    ): Response<StudentsResponse>
    
    @GET("facilitator/classrooms/{classroomId}/analytics")
    suspend fun getClassroomAnalytics(
        @Header("Authorization") token: String,
        @Path("classroomId") classroomId: String
    ): Response<AnalyticsResponse>
    
    // Content endpoints
    @GET("content/curriculum")
    suspend fun getCurriculumOverview(): Response<CurriculumResponse>
    
    @GET("content/lessons")
    suspend fun getLessons(): Response<LessonsResponse>
    
    @GET("content/lessons/{lessonId}")
    suspend fun getLesson(@Path("lessonId") lessonId: String): Response<LessonDetailResponse>
    
    @GET("content/lessons/{lessonId}/mobile")
    suspend fun getMobileOptimizedLesson(@Path("lessonId") lessonId: String): Response<MobileLessonResponse>
    
    @GET("content/activities")
    suspend fun getActivities(): Response<ActivitiesResponse>
    
    @GET("content/activities/{activityId}")
    suspend fun getActivity(@Path("activityId") activityId: String): Response<ActivityDetailResponse>
    
    @GET("content/scenarios")
    suspend fun getScenarios(): Response<ScenariosResponse>
    
    @GET("content/scenarios/{scenarioId}")
    suspend fun getScenario(@Path("scenarioId") scenarioId: String): Response<ScenarioDetailResponse>
    
    // Student progress endpoints
    @POST("progress/lessons/{lessonId}/complete")
    suspend fun completeLessonForStudent(
        @Path("lessonId") lessonId: String,
        @Body request: StudentProgressRequest
    ): Response<ProgressResponse>
    
    @POST("progress/activities/{activityId}/complete")
    suspend fun completeActivityForStudent(
        @Path("activityId") activityId: String,
        @Body request: StudentProgressRequest
    ): Response<ProgressResponse>
    
    @POST("progress/scenarios/{scenarioId}/respond")
    suspend fun respondToScenario(
        @Path("scenarioId") scenarioId: String,
        @Body request: ScenarioResponseRequest
    ): Response<ScenarioResponseResponse>
    
    @GET("progress/student/{studentId}")
    suspend fun getStudentProgress(@Path("studentId") studentId: String): Response<StudentProgressResponse>
    
    @GET("progress/classroom/{classroomId}")
    suspend fun getClassroomProgress(@Path("classroomId") classroomId: String): Response<ClassroomProgressResponse>
    
    // Enhanced UX endpoints for mobile optimization
    @POST("enhanced/emotional-checkin")
    suspend fun submitEmotionalCheckin(@Body request: EmotionalCheckinRequest): Response<EmotionalCheckinResponse>
    
    @GET("enhanced/student-dashboard/{studentId}")
    suspend fun getStudentDashboard(@Path("studentId") studentId: String): Response<StudentDashboardResponse>
    
    @GET("enhanced/facilitator-insights/{facilitatorId}")
    suspend fun getFacilitatorInsights(
        @Header("Authorization") token: String,
        @Path("facilitatorId") facilitatorId: String
    ): Response<FacilitatorInsightsResponse>
    
    // Audit and compliance endpoints
    @GET("audit/facilitator-sessions")
    suspend fun getFacilitatorSessions(
        @Header("Authorization") token: String,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<AuditSessionsResponse>
    
    @GET("audit/student-activities/{studentId}")
    suspend fun getStudentAuditLog(
        @Path("studentId") studentId: String,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<StudentAuditResponse>
}