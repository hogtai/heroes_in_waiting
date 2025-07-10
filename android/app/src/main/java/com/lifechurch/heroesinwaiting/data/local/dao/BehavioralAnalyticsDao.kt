package com.lifechurch.heroesinwaiting.data.local.dao

import androidx.room.*
import com.lifechurch.heroesinwaiting.data.local.entities.BehavioralAnalyticsEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for behavioral analytics data with offline-first architecture
 * Supports COPPA-compliant analytics collection and sync
 */
@Dao
interface BehavioralAnalyticsDao {
    
    // ================== Insert Operations ==================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBehavioralAnalytics(analytics: BehavioralAnalyticsEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBehavioralAnalyticsBatch(analytics: List<BehavioralAnalyticsEntity>)
    
    // ================== Query Operations ==================
    
    @Query("SELECT * FROM behavioral_analytics WHERE sessionId = :sessionId ORDER BY interactionTimestamp DESC")
    fun getBehavioralAnalyticsBySession(sessionId: String): Flow<List<BehavioralAnalyticsEntity>>
    
    @Query("SELECT * FROM behavioral_analytics WHERE classroomId = :classroomId ORDER BY interactionTimestamp DESC")
    fun getBehavioralAnalyticsByClassroom(classroomId: String): Flow<List<BehavioralAnalyticsEntity>>
    
    @Query("SELECT * FROM behavioral_analytics WHERE lessonId = :lessonId ORDER BY interactionTimestamp DESC")
    fun getBehavioralAnalyticsByLesson(lessonId: String): Flow<List<BehavioralAnalyticsEntity>>
    
    @Query("SELECT * FROM behavioral_analytics WHERE behavioralCategory = :category ORDER BY interactionTimestamp DESC")
    fun getBehavioralAnalyticsByCategory(category: String): Flow<List<BehavioralAnalyticsEntity>>
    
    @Query("SELECT * FROM behavioral_analytics WHERE interactionTimestamp >= :startTime AND interactionTimestamp <= :endTime ORDER BY interactionTimestamp DESC")
    fun getBehavioralAnalyticsByTimeRange(startTime: Long, endTime: Long): Flow<List<BehavioralAnalyticsEntity>>
    
    // ================== Sync Operations ==================
    
    @Query("SELECT * FROM behavioral_analytics WHERE needsSync = 1 ORDER BY interactionTimestamp ASC LIMIT :limit")
    suspend fun getPendingSyncAnalytics(limit: Int = 50): List<BehavioralAnalyticsEntity>
    
    @Query("SELECT * FROM behavioral_analytics WHERE needsSync = 1 AND syncAttempts < 3 ORDER BY interactionTimestamp ASC LIMIT :limit")
    suspend fun getRetryableAnalytics(limit: Int = 50): List<BehavioralAnalyticsEntity>
    
    @Query("UPDATE behavioral_analytics SET needsSync = 0, syncAttempts = syncAttempts + 1, lastSyncAttempt = :syncTime WHERE id = :id")
    suspend fun markAsSynced(id: String, syncTime: Long)
    
    @Query("UPDATE behavioral_analytics SET syncAttempts = syncAttempts + 1, lastSyncAttempt = :syncTime WHERE id = :id")
    suspend fun incrementSyncAttempt(id: String, syncTime: Long)
    
    @Query("UPDATE behavioral_analytics SET needsSync = 0 WHERE id IN (:ids)")
    suspend fun markBatchAsSynced(ids: List<String>)
    
    // ================== Cleanup Operations ==================
    
    @Query("DELETE FROM behavioral_analytics WHERE interactionTimestamp < :olderThan")
    suspend fun deleteOldAnalytics(olderThan: Long)
    
    @Query("DELETE FROM behavioral_analytics WHERE needsSync = 0 AND interactionTimestamp < :olderThan")
    suspend fun deleteSyncedOldAnalytics(olderThan: Long)
    
    @Query("DELETE FROM behavioral_analytics WHERE syncAttempts >= 3 AND lastSyncAttempt < :olderThan")
    suspend fun deleteFailedSyncAnalytics(olderThan: Long)
    
    // ================== Analytics Queries ==================
    
    @Query("""
        SELECT behavioralCategory, COUNT(*) as count, AVG(timeSpentSeconds) as avgTime
        FROM behavioral_analytics 
        WHERE sessionId = :sessionId 
        GROUP BY behavioralCategory
    """)
    suspend fun getBehavioralSummaryBySession(sessionId: String): List<BehavioralSummary>
    
    @Query("""
        SELECT interactionType, COUNT(*) as count, SUM(timeSpentSeconds) as totalTime
        FROM behavioral_analytics 
        WHERE classroomId = :classroomId 
        AND interactionTimestamp >= :startTime 
        GROUP BY interactionType
    """)
    suspend fun getInteractionSummaryByClassroom(classroomId: String, startTime: Long): List<InteractionSummary>
    
    @Query("""
        SELECT COUNT(*) as totalEvents, 
               SUM(timeSpentSeconds) as totalTime,
               COUNT(DISTINCT sessionId) as uniqueSessions
        FROM behavioral_analytics 
        WHERE classroomId = :classroomId 
        AND interactionTimestamp >= :startTime
    """)
    suspend fun getClassroomAnalyticsSummary(classroomId: String, startTime: Long): ClassroomAnalyticsSummary
    
    @Query("SELECT COUNT(*) FROM behavioral_analytics WHERE needsSync = 1")
    suspend fun getPendingSyncCount(): Int
    
    @Query("SELECT COUNT(*) FROM behavioral_analytics WHERE isOfflineRecorded = 1")
    suspend fun getOfflineRecordedCount(): Int
    
    // ================== Update Operations ==================
    
    @Update
    suspend fun updateBehavioralAnalytics(analytics: BehavioralAnalyticsEntity)
    
    @Query("UPDATE behavioral_analytics SET errorOccurred = 1, errorDetails = :errorDetails WHERE id = :id")
    suspend fun markAsError(id: String, errorDetails: String)
    
    // ================== Delete Operations ==================
    
    @Delete
    suspend fun deleteBehavioralAnalytics(analytics: BehavioralAnalyticsEntity)
    
    @Query("DELETE FROM behavioral_analytics WHERE id = :id")
    suspend fun deleteBehavioralAnalyticsById(id: String)
}

// ================== Data Classes for Query Results ==================

data class BehavioralSummary(
    val behavioralCategory: String,
    val count: Int,
    val avgTime: Double
)

data class InteractionSummary(
    val interactionType: String,
    val count: Int,
    val totalTime: Long
)

data class ClassroomAnalyticsSummary(
    val totalEvents: Int,
    val totalTime: Long,
    val uniqueSessions: Int
)