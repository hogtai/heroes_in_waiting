package com.lifechurch.heroesinwaiting.data.local.dao

import androidx.room.*
import com.lifechurch.heroesinwaiting.data.local.entities.AnalyticsSyncBatchEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for analytics sync batch management
 * Handles efficient batching and retry logic for offline-to-online sync
 */
@Dao
interface AnalyticsSyncBatchDao {
    
    // ================== Insert Operations ==================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncBatch(batch: AnalyticsSyncBatchEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncBatches(batches: List<AnalyticsSyncBatchEntity>)
    
    // ================== Query Operations ==================
    
    @Query("SELECT * FROM analytics_sync_batches WHERE batchId = :batchId")
    suspend fun getSyncBatchById(batchId: String): AnalyticsSyncBatchEntity?
    
    @Query("SELECT * FROM analytics_sync_batches WHERE status = :status ORDER BY priority DESC, createdAt ASC")
    suspend fun getSyncBatchesByStatus(status: String): List<AnalyticsSyncBatchEntity>
    
    @Query("SELECT * FROM analytics_sync_batches WHERE status = 'pending' ORDER BY priority DESC, createdAt ASC")
    suspend fun getPendingSyncBatches(): List<AnalyticsSyncBatchEntity>
    
    @Query("SELECT * FROM analytics_sync_batches WHERE status = 'failed' AND nextRetryAt <= :currentTime ORDER BY priority DESC, createdAt ASC")
    suspend fun getRetryableBatches(currentTime: Long): List<AnalyticsSyncBatchEntity>
    
    @Query("SELECT * FROM analytics_sync_batches WHERE status = 'syncing' AND syncStartedAt < :timeoutThreshold")
    suspend fun getStuckSyncBatches(timeoutThreshold: Long): List<AnalyticsSyncBatchEntity>
    
    @Query("SELECT * FROM analytics_sync_batches ORDER BY createdAt DESC")
    fun getAllSyncBatches(): Flow<List<AnalyticsSyncBatchEntity>>
    
    // ================== Status Update Operations ==================
    
    @Query("UPDATE analytics_sync_batches SET status = :status, syncStartedAt = :startTime WHERE batchId = :batchId")
    suspend fun updateBatchStatus(batchId: String, status: String, startTime: Long = System.currentTimeMillis())
    
    @Query("UPDATE analytics_sync_batches SET status = 'completed', syncCompletedAt = :completedTime, successCount = :successCount, failureCount = :failureCount WHERE batchId = :batchId")
    suspend fun markBatchCompleted(batchId: String, completedTime: Long, successCount: Int, failureCount: Int)
    
    @Query("UPDATE analytics_sync_batches SET status = 'failed', syncAttempts = syncAttempts + 1, lastError = :error, nextRetryAt = :nextRetryAt WHERE batchId = :batchId")
    suspend fun markBatchFailed(batchId: String, error: String, nextRetryAt: Long)
    
    @Query("UPDATE analytics_sync_batches SET syncAttempts = syncAttempts + 1 WHERE batchId = :batchId")
    suspend fun incrementSyncAttempts(batchId: String)
    
    // ================== Cleanup Operations ==================
    
    @Query("DELETE FROM analytics_sync_batches WHERE status = 'completed' AND syncCompletedAt < :olderThan")
    suspend fun deleteCompletedOldBatches(olderThan: Long)
    
    @Query("DELETE FROM analytics_sync_batches WHERE status = 'failed' AND syncAttempts >= maxRetries AND createdAt < :olderThan")
    suspend fun deleteFailedOldBatches(olderThan: Long)
    
    @Query("DELETE FROM analytics_sync_batches WHERE batchId = :batchId")
    suspend fun deleteSyncBatch(batchId: String)
    
    // ================== Analytics Queries ==================
    
    @Query("""
        SELECT status, COUNT(*) as count, SUM(eventCount) as totalEvents
        FROM analytics_sync_batches 
        GROUP BY status
    """)
    suspend fun getSyncBatchStatusSummary(): List<BatchStatusSummary>
    
    @Query("""
        SELECT batchType, COUNT(*) as count, AVG(eventCount) as avgEvents
        FROM analytics_sync_batches 
        WHERE createdAt >= :startTime 
        GROUP BY batchType
    """)
    suspend fun getBatchTypeSummary(startTime: Long): List<BatchTypeSummary>
    
    @Query("""
        SELECT COUNT(*) as totalBatches,
               SUM(CASE WHEN status = 'completed' THEN 1 ELSE 0 END) as completedBatches,
               SUM(CASE WHEN status = 'failed' THEN 1 ELSE 0 END) as failedBatches,
               SUM(CASE WHEN status = 'pending' THEN 1 ELSE 0 END) as pendingBatches,
               SUM(eventCount) as totalEvents
        FROM analytics_sync_batches
        WHERE createdAt >= :startTime
    """)
    suspend fun getSyncHealthSummary(startTime: Long): SyncHealthSummary
    
    @Query("SELECT COUNT(*) FROM analytics_sync_batches WHERE status = 'pending'")
    suspend fun getPendingBatchCount(): Int
    
    @Query("SELECT COUNT(*) FROM analytics_sync_batches WHERE status = 'failed'")
    suspend fun getFailedBatchCount(): Int
    
    @Query("SELECT SUM(eventCount) FROM analytics_sync_batches WHERE status = 'pending'")
    suspend fun getPendingEventCount(): Int
    
    // ================== Update Operations ==================
    
    @Update
    suspend fun updateSyncBatch(batch: AnalyticsSyncBatchEntity)
    
    @Query("UPDATE analytics_sync_batches SET priority = :priority WHERE batchId = :batchId")
    suspend fun updateBatchPriority(batchId: String, priority: Int)
    
    @Query("UPDATE analytics_sync_batches SET requiresWifi = :requiresWifi WHERE batchId = :batchId")
    suspend fun updateBatchWifiRequirement(batchId: String, requiresWifi: Boolean)
    
    // ================== Delete Operations ==================
    
    @Delete
    suspend fun deleteSyncBatch(batch: AnalyticsSyncBatchEntity)
    
    @Query("DELETE FROM analytics_sync_batches WHERE status = 'completed' AND eventCount = 0")
    suspend fun deleteEmptyCompletedBatches()
}

// ================== Data Classes for Query Results ==================

data class BatchStatusSummary(
    val status: String,
    val count: Int,
    val totalEvents: Int
)

data class BatchTypeSummary(
    val batchType: String,
    val count: Int,
    val avgEvents: Double
)

data class SyncHealthSummary(
    val totalBatches: Int,
    val completedBatches: Int,
    val failedBatches: Int,
    val pendingBatches: Int,
    val totalEvents: Int
)