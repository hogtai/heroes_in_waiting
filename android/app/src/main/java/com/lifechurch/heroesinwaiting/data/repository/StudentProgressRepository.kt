package com.lifechurch.heroesinwaiting.data.repository

import com.lifechurch.heroesinwaiting.data.local.dao.ProgressDao
import com.lifechurch.heroesinwaiting.data.local.dao.LessonDao
import com.lifechurch.heroesinwaiting.data.local.entities.ProgressEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for student progress tracking and dashboard analytics
 * Manages lesson completion, scores, and engagement metrics
 */
@Singleton
class StudentProgressRepository @Inject constructor(
    private val progressDao: ProgressDao,
    private val lessonDao: LessonDao,
    private val authRepository: AuthRepository
) {
    
    /**
     * Gets comprehensive dashboard stats for the current student
     */
    fun getStudentDashboardStats(): Flow<StudentDashboardStats> {
        return combine(
            getStudentProgress(),
            getTotalAvailableLessons()
        ) { progressList, totalLessons ->
            calculateDashboardStats(progressList, totalLessons)
        }.catch { 
            emit(StudentDashboardStats.empty())
        }
    }
    
    /**
     * Gets all progress for the current student
     */
    private fun getStudentProgress(): Flow<List<ProgressEntity>> {
        return authRepository.getCurrentUserId()?.let { userId ->
            progressDao.getProgressByStudent(userId)
        } ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }
    
    /**
     * Gets lesson progress specifically
     */
    fun getLessonProgress(): Flow<List<ProgressEntity>> {
        return authRepository.getCurrentUserId()?.let { userId ->
            progressDao.getProgressByStudentAndType(userId, "lesson")
        } ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }
    
    /**
     * Gets activity progress
     */
    fun getActivityProgress(): Flow<List<ProgressEntity>> {
        return authRepository.getCurrentUserId()?.let { userId ->
            progressDao.getProgressByStudentAndType(userId, "activity")
        } ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }
    
    /**
     * Records lesson completion
     */
    suspend fun recordLessonCompletion(
        lessonId: String,
        timeSpent: Int,
        score: Double? = null
    ): Result<Unit> {
        return try {
            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                val progressEntity = ProgressEntity(
                    id = "${userId}_${lessonId}_lesson",
                    studentId = userId,
                    itemId = lessonId,
                    itemType = "lesson",
                    completedAt = System.currentTimeMillis(),
                    timeSpent = timeSpent,
                    score = score,
                    responses = null,
                    syncedToServer = false
                )
                progressDao.insertProgress(progressEntity)
                Result.success(Unit)
            } else {
                Result.failure(Exception("User not authenticated"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Records activity completion
     */
    suspend fun recordActivityCompletion(
        activityId: String,
        timeSpent: Int,
        score: Double? = null,
        responses: String? = null
    ): Result<Unit> {
        return try {
            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                val progressEntity = ProgressEntity(
                    id = "${userId}_${activityId}_activity",
                    studentId = userId,
                    itemId = activityId,
                    itemType = "activity",
                    completedAt = System.currentTimeMillis(),
                    timeSpent = timeSpent,
                    score = score,
                    responses = responses,
                    syncedToServer = false
                )
                progressDao.insertProgress(progressEntity)
                Result.success(Unit)
            } else {
                Result.failure(Exception("User not authenticated"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Gets current lesson for student dashboard
     */
    suspend fun getCurrentLesson(): Result<CurrentLessonInfo?> {
        return try {
            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                // Get the most recent lesson progress to determine current lesson
                val recentProgress = progressDao.getProgressByStudentAndType(userId, "lesson")
                recentProgress.collect { progressList ->
                    if (progressList.isNotEmpty()) {
                        val lastCompletedLessonId = progressList.first().itemId
                        // Logic to determine next lesson would go here
                        // For now, return null to indicate no current lesson
                        return Result.success(null)
                    } else {
                        // Student hasn't completed any lessons yet
                        return Result.success(null)
                    }
                }
                Result.success(null)
            } else {
                Result.failure(Exception("User not authenticated"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Gets recent activity for dashboard
     */
    suspend fun getRecentActivity(limit: Int = 5): Result<List<RecentActivityItem>> {
        return try {
            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                val allProgress = progressDao.getProgressByStudent(userId)
                allProgress.collect { progressList ->
                    val recentItems = progressList.take(limit).map { progress ->
                        RecentActivityItem(
                            id = progress.itemId,
                            type = progress.itemType,
                            completedAt = progress.completedAt,
                            score = progress.score,
                            timeSpent = progress.timeSpent
                        )
                    }
                    return Result.success(recentItems)
                }
                Result.success(emptyList())
            } else {
                Result.failure(Exception("User not authenticated"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Gets hero points (calculated from progress scores and completions)
     */
    suspend fun getHeroPoints(): Result<Int> {
        return try {
            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                val completedLessons = progressDao.getCompletedCount(userId, "lesson")
                val completedActivities = progressDao.getCompletedCount(userId, "activity")
                val averageScore = progressDao.getAverageScore(userId) ?: 0.0
                
                // Calculate hero points: base points + score bonus
                val basePoints = (completedLessons * 10) + (completedActivities * 5)
                val scoreBonus = (averageScore * 10).toInt()
                val totalPoints = basePoints + scoreBonus
                
                Result.success(totalPoints)
            } else {
                Result.failure(Exception("User not authenticated"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Checks if student has completed a specific item
     */
    suspend fun hasCompletedItem(itemId: String, itemType: String): Result<Boolean> {
        return try {
            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                val progress = progressDao.getProgressByStudentAndItem(userId, itemId, itemType)
                Result.success(progress != null)
            } else {
                Result.failure(Exception("User not authenticated"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Syncs progress data to server (placeholder for future implementation)
     */
    suspend fun syncProgressToServer(): Result<Unit> {
        return try {
            val unsyncedProgress = progressDao.getUnsyncedProgress()
            // TODO: Implement API call to sync progress to server
            // For now, just mark as synced locally
            if (unsyncedProgress.isNotEmpty()) {
                val progressIds = unsyncedProgress.map { it.id }
                progressDao.markAsSynced(progressIds)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Gets total available lessons count
     */
    private fun getTotalAvailableLessons(): Flow<Int> {
        return lessonDao.getAllActiveLessonsFlow().map { lessons ->
            lessons.size
        }
    }
    
    /**
     * Calculates comprehensive dashboard statistics
     */
    private suspend fun calculateDashboardStats(
        progressList: List<ProgressEntity>,
        totalLessons: Int
    ): StudentDashboardStats {
        val completedLessons = progressList.count { it.itemType == "lesson" }
        val completedActivities = progressList.count { it.itemType == "activity" }
        val totalTimeSpent = progressList.mapNotNull { it.timeSpent }.sum()
        val averageScore = progressList.mapNotNull { it.score }.average().takeIf { !it.isNaN() } ?: 0.0
        
        // Calculate hero points
        val basePoints = (completedLessons * 10) + (completedActivities * 5)
        val scoreBonus = (averageScore * 10).toInt()
        val heroPoints = basePoints + scoreBonus
        
        // Calculate progress percentage
        val progressPercentage = if (totalLessons > 0) {
            (completedLessons.toFloat() / totalLessons.toFloat()) * 100f
        } else {
            0f
        }
        
        return StudentDashboardStats(
            completedLessons = completedLessons,
            completedActivities = completedActivities,
            heroPoints = heroPoints,
            totalTimeSpent = totalTimeSpent,
            averageScore = averageScore,
            progressPercentage = progressPercentage,
            totalAvailableLessons = totalLessons,
            hasCurrentLesson = false, // Would need current lesson logic
            recentActivity = emptyList() // Would be populated separately
        )
    }
}

/**
 * Dashboard statistics for student
 */
data class StudentDashboardStats(
    val completedLessons: Int,
    val completedActivities: Int,
    val heroPoints: Int,
    val totalTimeSpent: Int, // in seconds
    val averageScore: Double,
    val progressPercentage: Float,
    val totalAvailableLessons: Int,
    val hasCurrentLesson: Boolean,
    val recentActivity: List<RecentActivityItem>
) {
    companion object {
        fun empty() = StudentDashboardStats(
            completedLessons = 0,
            completedActivities = 0,
            heroPoints = 0,
            totalTimeSpent = 0,
            averageScore = 0.0,
            progressPercentage = 0f,
            totalAvailableLessons = 0,
            hasCurrentLesson = false,
            recentActivity = emptyList()
        )
    }
    
    val timeSpentInMinutes: Int
        get() = totalTimeSpent / 60
}

/**
 * Current lesson information
 */
data class CurrentLessonInfo(
    val lessonId: String,
    val lessonTitle: String,
    val lessonNumber: Int,
    val estimatedDuration: Int,
    val isStarted: Boolean = false,
    val progressPercentage: Float = 0f
)

/**
 * Recent activity item for dashboard
 */
data class RecentActivityItem(
    val id: String,
    val type: String, // "lesson", "activity", "scenario"
    val completedAt: Long,
    val score: Double?,
    val timeSpent: Int?
) {
    val displayType: String
        get() = when (type) {
            "lesson" -> "Lesson"
            "activity" -> "Activity"
            "scenario" -> "Scenario"
            else -> type.capitalize()
        }
}