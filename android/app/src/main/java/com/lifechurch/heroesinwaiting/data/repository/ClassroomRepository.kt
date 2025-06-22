package com.lifechurch.heroesinwaiting.data.repository

import com.lifechurch.heroesinwaiting.data.api.ApiService
import com.lifechurch.heroesinwaiting.data.local.dao.ClassroomDao
import com.lifechurch.heroesinwaiting.data.local.entities.ClassroomEntity
import com.lifechurch.heroesinwaiting.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for classroom data with offline-first approach
 * Handles both facilitator classroom management and student classroom access
 */
@Singleton
class ClassroomRepository @Inject constructor(
    private val apiService: ApiService,
    private val classroomDao: ClassroomDao,
    private val authRepository: AuthRepository
) {
    
    /**
     * Gets all classrooms for the current facilitator (offline-first)
     */
    fun getFacilitatorClassrooms(): Flow<List<Classroom>> {
        return flow {
            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                // Start with cached data
                classroomDao.getClassroomsByFacilitator(userId).collect { entities ->
                    emit(entities.map { it.toDomainModel() })
                }
                
                // Try to sync with server
                try {
                    val token = authRepository.getAuthHeader()
                    if (token != null) {
                        syncFacilitatorClassrooms(token, userId)
                    }
                } catch (e: Exception) {
                    // Continue with cached data if sync fails
                }
            } else {
                emit(emptyList())
            }
        }.catch { 
            // Fallback to cached data on any error
            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                classroomDao.getClassroomsByFacilitator(userId).collect { entities ->
                    emit(entities.map { it.toDomainModel() })
                }
            } else {
                emit(emptyList())
            }
        }
    }
    
    /**
     * Gets classroom details by ID
     */
    suspend fun getClassroomById(classroomId: String): Result<Classroom> {
        return try {
            // Try local first
            val cachedClassroom = classroomDao.getClassroomById(classroomId)
            if (cachedClassroom != null) {
                // Check if we need to refresh from server
                val token = authRepository.getAuthHeader()
                if (token != null) {
                    try {
                        val response = apiService.getClassroomDetails(classroomId, token)
                        if (response.isSuccessful && response.body() != null) {
                            val classroomDetails = response.body()!!.classroom
                            cacheClassroom(classroomDetails)
                            return Result.success(classroomDetails)
                        }
                    } catch (e: Exception) {
                        // Continue with cached data
                    }
                }
                Result.success(cachedClassroom.toDomainModel())
            } else {
                Result.failure(Exception("Classroom not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Creates a new classroom
     */
    suspend fun createClassroom(
        name: String,
        description: String?,
        grade: Grade,
        maxStudents: Int = 30
    ): Result<Classroom> {
        return try {
            val token = authRepository.getAuthHeader()
            if (token == null) {
                return Result.failure(Exception("Authentication required"))
            }
            
            val request = CreateClassroomRequest(
                name = name,
                description = description,
                grade = grade,
                maxStudents = maxStudents
            )
            
            val response = apiService.createClassroom(token, request)
            if (response.isSuccessful && response.body() != null) {
                val classroom = response.body()!!
                cacheClassroom(classroom)
                Result.success(classroom)
            } else {
                Result.failure(Exception("Failed to create classroom: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Gets student's current classroom
     */
    fun getStudentClassroom(): Flow<Classroom?> {
        return classroomDao.getEnrolledClassrooms().map { entities ->
            entities.firstOrNull()?.toDomainModel()
        }
    }
    
    /**
     * Gets classroom by enrollment code
     */
    suspend fun getClassroomByCode(classroomCode: String): Result<Classroom> {
        return try {
            // Try local cache first
            val cachedClassroom = classroomDao.getClassroomByCode(classroomCode)
            if (cachedClassroom != null) {
                return Result.success(cachedClassroom.toDomainModel())
            }
            
            // If not cached, this would typically be handled by the preview endpoint
            // in the student enrollment flow
            Result.failure(Exception("Classroom not found locally"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Gets classroom statistics for dashboard
     */
    suspend fun getClassroomStats(classroomId: String): Result<ClassroomStats> {
        return try {
            val token = authRepository.getAuthHeader()
            if (token != null) {
                val response = apiService.getClassroomDetails(classroomId, token)
                if (response.isSuccessful && response.body() != null) {
                    val stats = response.body()!!.stats
                    Result.success(stats)
                } else {
                    // Generate basic stats from local data
                    val classroom = classroomDao.getClassroomById(classroomId)
                    if (classroom != null) {
                        val stats = ClassroomStats(
                            classroomId = classroomId,
                            totalStudents = classroom.studentCount,
                            activeStudents = classroom.studentCount,
                            completedSessions = 0,
                            averageAttendance = 0f,
                            averageEngagement = 0f,
                            lessonsCompleted = 0,
                            totalLessonTime = 0L,
                            lastActivityAt = classroom.lastUpdated.toString(),
                            createdAt = classroom.cachedAt.toString()
                        )
                        Result.success(stats)
                    } else {
                        Result.failure(Exception("Classroom not found"))
                    }
                }
            } else {
                Result.failure(Exception("Authentication required"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Gets aggregated stats for all facilitator classrooms
     */
    suspend fun getFacilitatorDashboardStats(): Result<FacilitatorDashboardStats> {
        return try {
            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                val classrooms = classroomDao.getClassroomsByFacilitator(userId).map { entities ->
                    entities.map { it.toDomainModel() }
                }
                
                // Collect the current snapshot
                var totalStudents = 0
                var activeClassrooms = 0
                var activeSessions = 0
                
                classrooms.collect { classroomList ->
                    totalStudents = classroomList.sumOf { it.currentStudentCount }
                    activeClassrooms = classroomList.count { it.isActive }
                    activeSessions = classroomList.count { it.hasActiveSession }
                    
                    val stats = FacilitatorDashboardStats(
                        totalClassrooms = classroomList.size,
                        activeClassrooms = activeClassrooms,
                        totalStudents = totalStudents,
                        activeSessions = activeSessions,
                        completedLessons = 0, // Would need lesson progress data
                        lastActivity = System.currentTimeMillis().toString()
                    )
                    
                    return Result.success(stats)
                }
                
                Result.failure(Exception("No data available"))
            } else {
                Result.failure(Exception("User not authenticated"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Synchronizes facilitator classrooms from server
     */
    private suspend fun syncFacilitatorClassrooms(token: String, facilitatorId: String) {
        try {
            val response = apiService.getClassrooms(token)
            if (response.isSuccessful && response.body() != null) {
                val classrooms = response.body()!!.classrooms
                val entities = classrooms.map { classroom ->
                    ClassroomEntity(
                        id = classroom.id,
                        name = classroom.name,
                        code = classroom.classroomCode,
                        gradeLevel = classroom.grade.name,
                        schoolName = "", // Would need from API
                        facilitatorId = classroom.facilitatorId,
                        facilitatorName = "", // Would need from API
                        studentCount = classroom.currentStudentCount,
                        isActive = classroom.isActive,
                        enrolledAt = null, // This is a facilitator classroom
                        cachedAt = System.currentTimeMillis(),
                        lastUpdated = System.currentTimeMillis()
                    )
                }
                classroomDao.insertClassrooms(entities)
            }
        } catch (e: Exception) {
            // Sync failed, continue with cached data
        }
    }
    
    /**
     * Caches classroom data locally
     */
    private suspend fun cacheClassroom(classroom: Classroom) {
        val entity = ClassroomEntity(
            id = classroom.id,
            name = classroom.name,
            code = classroom.classroomCode,
            gradeLevel = classroom.grade.name,
            schoolName = "", // Would need from API
            facilitatorId = classroom.facilitatorId,
            facilitatorName = "", // Would need from API
            studentCount = classroom.currentStudentCount,
            isActive = classroom.isActive,
            enrolledAt = null,
            cachedAt = System.currentTimeMillis(),
            lastUpdated = System.currentTimeMillis()
        )
        classroomDao.insertClassroom(entity)
    }
    
    /**
     * Marks classroom as enrolled for student
     */
    suspend fun markClassroomAsEnrolled(classroomId: String): Result<Unit> {
        return try {
            classroomDao.markAsEnrolled(classroomId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Extension function to convert ClassroomEntity to domain model
 */
private fun ClassroomEntity.toDomainModel(): Classroom {
    return Classroom(
        id = this.id,
        name = this.name,
        description = "", // Not stored in entity
        facilitatorId = this.facilitatorId,
        grade = Grade.valueOf(this.gradeLevel),
        classroomCode = this.code,
        isActive = this.isActive,
        maxStudents = 30, // Default, not stored in entity
        currentStudentCount = this.studentCount,
        createdAt = this.cachedAt.toString(),
        updatedAt = this.lastUpdated.toString(),
        hasActiveSession = false, // Would need session data
        currentSessionId = null,
        currentLessonId = null,
        sessionStartTime = null
    )
}

/**
 * Dashboard statistics for facilitator
 */
data class FacilitatorDashboardStats(
    val totalClassrooms: Int,
    val activeClassrooms: Int,
    val totalStudents: Int,
    val activeSessions: Int,
    val completedLessons: Int,
    val lastActivity: String
)