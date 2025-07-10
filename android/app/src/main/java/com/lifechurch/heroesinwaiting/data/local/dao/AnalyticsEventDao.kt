package com.lifechurch.heroesinwaiting.data.local.dao

import androidx.room.*
import com.lifechurch.heroesinwaiting.data.local.entities.AnalyticsEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for general analytics events with offline-first architecture
 * Supports batching and efficient sync operations
 */
@Dao
interface AnalyticsEventDao {
    
    // ================== Insert Operations ==================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalyticsEvent(event: AnalyticsEventEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalyticsEventBatch(events: List<AnalyticsEventEntity>)
    
    // ================== Query Operations ==================
    
    @Query("SELECT * FROM analytics_events WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getAnalyticsEventsBySession(sessionId: String): Flow<List<AnalyticsEventEntity>>
    
    @Query("SELECT * FROM analytics_events WHERE classroomId = :classroomId ORDER BY timestamp DESC")
    fun getAnalyticsEventsByClassroom(classroomId: String): Flow<List<AnalyticsEventEntity>>
    
    @Query("SELECT * FROM analytics_events WHERE lessonId = :lessonId ORDER BY timestamp DESC")
    fun getAnalyticsEventsByLesson(lessonId: String): Flow<List<AnalyticsEventEntity>>
    
    @Query("SELECT * FROM analytics_events WHERE eventType = :eventType ORDER BY timestamp DESC")
    fun getAnalyticsEventsByType(eventType: String): Flow<List<AnalyticsEventEntity>>
    
    @Query("SELECT * FROM analytics_events WHERE eventCategory = :category ORDER BY timestamp DESC")
    fun getAnalyticsEventsByCategory(category: String): Flow<List<AnalyticsEventEntity>>
    
    @Query("SELECT * FROM analytics_events WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getAnalyticsEventsByTimeRange(startTime: Long, endTime: Long): Flow<List<AnalyticsEventEntity>>
    
    // ================== Sync Operations ==================
    
    @Query("SELECT * FROM analytics_events WHERE isSynced = 0 ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getPendingSyncEvents(limit: Int = 100): List<AnalyticsEventEntity>
    
    @Query("SELECT * FROM analytics_events WHERE isSynced = 0 AND syncAttempts < 3 ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getRetryableEvents(limit: Int = 100): List<AnalyticsEventEntity>
    
    @Query("SELECT * FROM analytics_events WHERE batchId = :batchId")
    suspend fun getEventsByBatchId(batchId: String): List<AnalyticsEventEntity>
    
    @Query("UPDATE analytics_events SET isSynced = 1, syncAttempts = syncAttempts + 1, lastSyncAttempt = :syncTime WHERE id = :id")
    suspend fun markEventAsSynced(id: String, syncTime: Long)
    
    @Query("UPDATE analytics_events SET syncAttempts = syncAttempts + 1, lastSyncAttempt = :syncTime, syncError = :error WHERE id = :id")
    suspend fun markEventSyncFailed(id: String, syncTime: Long, error: String)
    
    @Query("UPDATE analytics_events SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markBatchAsSynced(ids: List<String>)
    
    @Query("UPDATE analytics_events SET batchId = :batchId WHERE id IN (:ids)")
    suspend fun assignBatchId(ids: List<String>, batchId: String)
    
    // ================== Cleanup Operations ==================
    
    @Query("DELETE FROM analytics_events WHERE timestamp < :olderThan")
    suspend fun deleteOldEvents(olderThan: Long)
    
    @Query("DELETE FROM analytics_events WHERE isSynced = 1 AND timestamp < :olderThan")
    suspend fun deleteSyncedOldEvents(olderThan: Long)
    
    @Query("DELETE FROM analytics_events WHERE syncAttempts >= 3 AND lastSyncAttempt < :olderThan")
    suspend fun deleteFailedSyncEvents(olderThan: Long)
    
    @Query("DELETE FROM analytics_events WHERE batchId = :batchId")
    suspend fun deleteEventsByBatchId(batchId: String)
    
    // ================== Analytics Queries ==================
    
    @Query("""
        SELECT eventType, COUNT(*) as count, AVG(duration) as avgDuration
        FROM analytics_events 
        WHERE sessionId = :sessionId 
        GROUP BY eventType
    """)
    suspend fun getEventSummaryBySession(sessionId: String): List<EventSummary>
    
    @Query("""
        SELECT eventCategory, COUNT(*) as count, AVG(value) as avgValue
        FROM analytics_events 
        WHERE classroomId = :classroomId 
        AND timestamp >= :startTime 
        GROUP BY eventCategory
    """)
    suspend fun getEventSummaryByClassroom(classroomId: String, startTime: Long): List<EventCategorySummary>
    
    @Query("""
        SELECT COUNT(*) as totalEvents, 
               COUNT(DISTINCT sessionId) as uniqueSessions,
               AVG(duration) as avgDuration
        FROM analytics_events 
        WHERE classroomId = :classroomId 
        AND timestamp >= :startTime
    """)
    suspend fun getClassroomEventSummary(classroomId: String, startTime: Long): ClassroomEventSummary
    
    @Query("SELECT COUNT(*) FROM analytics_events WHERE isSynced = 0")
    suspend fun getPendingSyncCount(): Int
    
    @Query("SELECT COUNT(*) FROM analytics_events WHERE connectionType = 'offline'")
    suspend fun getOfflineEventCount(): Int
    
    @Query("SELECT COUNT(*) FROM analytics_events WHERE syncError IS NOT NULL")
    suspend fun getFailedSyncCount(): Int
    
    // ================== Update Operations ==================
    
    @Update
    suspend fun updateAnalyticsEvent(event: AnalyticsEventEntity)
    
    @Query("UPDATE analytics_events SET syncError = :error WHERE id = :id")
    suspend fun updateSyncError(id: String, error: String)
    
    // ================== Delete Operations ==================
    
    @Delete
    suspend fun deleteAnalyticsEvent(event: AnalyticsEventEntity)
    
    @Query("DELETE FROM analytics_events WHERE id = :id")
    suspend fun deleteAnalyticsEventById(id: String)
}

// ================== Data Classes for Query Results ==================

data class EventSummary(
    val eventType: String,
    val count: Int,
    val avgDuration: Double?
)

data class EventCategorySummary(
    val eventCategory: String,
    val count: Int,
    val avgValue: Double?
)

data class ClassroomEventSummary(
    val totalEvents: Int,
    val uniqueSessions: Int,
    val avgDuration: Double?
)