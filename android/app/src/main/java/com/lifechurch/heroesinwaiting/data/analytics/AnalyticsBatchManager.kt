package com.lifechurch.heroesinwaiting.data.analytics

import com.lifechurch.heroesinwaiting.data.local.dao.AnalyticsEventDao
import com.lifechurch.heroesinwaiting.data.local.dao.BehavioralAnalyticsDao
import com.lifechurch.heroesinwaiting.data.local.dao.AnalyticsSyncBatchDao
import com.lifechurch.heroesinwaiting.data.local.entities.AnalyticsSyncBatchEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Advanced batch manager for analytics data
 * Implements intelligent batching, retry logic, and adaptive sync strategies
 */
@Singleton
class AnalyticsBatchManager @Inject constructor(
    private val analyticsEventDao: AnalyticsEventDao,
    private val behavioralAnalyticsDao: BehavioralAnalyticsDao,
    private val syncBatchDao: AnalyticsSyncBatchDao
) {
    
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    /**
     * Creates optimized batches based on data type and network conditions
     */
    suspend fun createOptimizedBatches(
        networkQuality: NetworkQuality = NetworkQuality.MEDIUM,
        batteryLevel: BatteryLevel = BatteryLevel.NORMAL,
        wifiOnly: Boolean = false
    ): List<String> {
        val batchIds = mutableListOf<String>()
        
        // Get pending events and behavioral analytics
        val pendingEvents = analyticsEventDao.getPendingSyncEvents(getBatchSizeForConditions(networkQuality))
        val pendingBehavioral = behavioralAnalyticsDao.getPendingSyncAnalytics(getBatchSizeForConditions(networkQuality))
        
        // Create behavioral analytics batch (higher priority)
        if (pendingBehavioral.isNotEmpty()) {
            val behavioralBatchId = createBehavioralAnalyticsBatch(
                pendingBehavioral.map { it.id },
                calculatePriority(networkQuality, batteryLevel, "behavioral"),
                wifiOnly
            )
            batchIds.add(behavioralBatchId)
        }
        
        // Create general events batch
        if (pendingEvents.isNotEmpty()) {
            val eventsBatchId = createGeneralEventsBatch(
                pendingEvents.map { it.id },
                calculatePriority(networkQuality, batteryLevel, "general"),
                wifiOnly
            )
            batchIds.add(eventsBatchId)
        }
        
        return batchIds
    }
    
    /**
     * Creates a behavioral analytics batch with intelligent sizing
     */
    private suspend fun createBehavioralAnalyticsBatch(
        eventIds: List<String>,
        priority: Int,
        requiresWifi: Boolean
    ): String {
        val batchId = UUID.randomUUID().toString()
        val batch = AnalyticsSyncBatchEntity(
            batchId = batchId,
            eventIds = eventIds,
            eventCount = eventIds.size,
            batchType = "behavioral",
            status = "pending",
            priority = priority,
            createdAt = System.currentTimeMillis(),
            scheduledAt = System.currentTimeMillis(),
            requiresWifi = requiresWifi,
            estimatedDataSize = estimateBehavioralDataSize(eventIds),
            retryDelayMs = calculateRetryDelay(priority),
            maxRetries = calculateMaxRetries(priority)
        )
        
        syncBatchDao.insertSyncBatch(batch)
        analyticsEventDao.assignBatchId(eventIds, batchId)
        
        return batchId
    }
    
    /**
     * Creates a general events batch
     */
    private suspend fun createGeneralEventsBatch(
        eventIds: List<String>,
        priority: Int,
        requiresWifi: Boolean
    ): String {
        val batchId = UUID.randomUUID().toString()
        val batch = AnalyticsSyncBatchEntity(
            batchId = batchId,
            eventIds = eventIds,
            eventCount = eventIds.size,
            batchType = "general",
            status = "pending",
            priority = priority,
            createdAt = System.currentTimeMillis(),
            scheduledAt = System.currentTimeMillis(),
            requiresWifi = requiresWifi,
            estimatedDataSize = estimateGeneralDataSize(eventIds),
            retryDelayMs = calculateRetryDelay(priority),
            maxRetries = calculateMaxRetries(priority)
        )
        
        syncBatchDao.insertSyncBatch(batch)
        analyticsEventDao.assignBatchId(eventIds, batchId)
        
        return batchId
    }
    
    /**
     * Processes retry logic for failed batches with exponential backoff
     */
    suspend fun processRetryableBatches(): List<String> {
        val retryableBatches = syncBatchDao.getRetryableBatches(System.currentTimeMillis())
        val retriedBatchIds = mutableListOf<String>()
        
        retryableBatches.forEach { batch ->
            if (shouldRetryBatch(batch)) {
                val nextRetryTime = calculateNextRetryTime(batch)
                
                // Update batch with retry information
                syncBatchDao.updateBatchStatus(batch.batchId, "pending", System.currentTimeMillis())
                syncBatchDao.markBatchFailed(
                    batch.batchId,
                    "Scheduled for retry",
                    nextRetryTime
                )
                
                retriedBatchIds.add(batch.batchId)
            } else {
                // Mark as permanently failed if max retries exceeded
                syncBatchDao.markBatchFailed(
                    batch.batchId,
                    "Max retries exceeded",
                    Long.MAX_VALUE
                )
            }
        }
        
        return retriedBatchIds
    }
    
    /**
     * Handles adaptive batch sizing based on network conditions
     */
    private fun getBatchSizeForConditions(networkQuality: NetworkQuality): Int {
        return when (networkQuality) {
            NetworkQuality.HIGH -> 100    // Large batches for good connections
            NetworkQuality.MEDIUM -> 50   // Medium batches for moderate connections
            NetworkQuality.LOW -> 20      // Small batches for poor connections
            NetworkQuality.NONE -> 0      // No batching when offline
        }
    }
    
    /**
     * Calculates priority based on conditions and data type
     */
    private fun calculatePriority(
        networkQuality: NetworkQuality,
        batteryLevel: BatteryLevel,
        dataType: String
    ): Int {
        var priority = 0
        
        // Base priority by data type
        priority += when (dataType) {
            "behavioral" -> 10  // Behavioral analytics are high priority
            "general" -> 5      // General events are medium priority
            else -> 3
        }
        
        // Adjust for network quality
        priority += when (networkQuality) {
            NetworkQuality.HIGH -> 3
            NetworkQuality.MEDIUM -> 2
            NetworkQuality.LOW -> 1
            NetworkQuality.NONE -> 0
        }
        
        // Adjust for battery level
        priority += when (batteryLevel) {
            BatteryLevel.HIGH -> 2
            BatteryLevel.NORMAL -> 1
            BatteryLevel.LOW -> 0
            BatteryLevel.CRITICAL -> -5
        }
        
        return maxOf(priority, 0)
    }
    
    /**
     * Calculates retry delay with exponential backoff
     */
    private fun calculateRetryDelay(priority: Int): Long {
        val baseDelay = 30000L // 30 seconds base
        val priorityMultiplier = when {
            priority >= 15 -> 0.5  // High priority gets shorter delays
            priority >= 10 -> 1.0  // Normal delay
            priority >= 5 -> 2.0   // Lower priority gets longer delays
            else -> 4.0            // Lowest priority gets much longer delays
        }
        
        return (baseDelay * priorityMultiplier).toLong()
    }
    
    /**
     * Calculates maximum retries based on priority
     */
    private fun calculateMaxRetries(priority: Int): Int {
        return when {
            priority >= 15 -> 5  // High priority gets more retries
            priority >= 10 -> 3  // Normal retries
            priority >= 5 -> 2   // Fewer retries
            else -> 1            // Minimal retries for low priority
        }
    }
    
    /**
     * Determines if a batch should be retried
     */
    private fun shouldRetryBatch(batch: AnalyticsSyncBatchEntity): Boolean {
        return batch.syncAttempts < batch.maxRetries &&
                System.currentTimeMillis() >= (batch.nextRetryAt ?: 0)
    }
    
    /**
     * Calculates next retry time with exponential backoff
     */
    private fun calculateNextRetryTime(batch: AnalyticsSyncBatchEntity): Long {
        val baseDelay = batch.retryDelayMs
        val exponentialMultiplier = 1 shl batch.syncAttempts // 2^attempts
        val jitter = (Math.random() * 0.1 * baseDelay).toLong() // 10% jitter
        
        return System.currentTimeMillis() + (baseDelay * exponentialMultiplier) + jitter
    }
    
    /**
     * Estimates data size for behavioral analytics
     */
    private fun estimateBehavioralDataSize(eventIds: List<String>): Long {
        // Behavioral analytics tend to be larger due to complex behavioral indicators
        return eventIds.size * 2048L // 2KB per behavioral event
    }
    
    /**
     * Estimates data size for general events
     */
    private fun estimateGeneralDataSize(eventIds: List<String>): Long {
        // General events are typically smaller
        return eventIds.size * 1024L // 1KB per general event
    }
    
    /**
     * Monitors batch health and suggests optimizations
     */
    suspend fun generateBatchHealthReport(): BatchHealthReport {
        val pendingCount = syncBatchDao.getPendingBatchCount()
        val failedCount = syncBatchDao.getFailedBatchCount()
        val pendingEventCount = syncBatchDao.getPendingEventCount()
        
        val healthStatus = when {
            failedCount > 10 -> "unhealthy"
            pendingCount > 20 -> "concerning"
            pendingEventCount > 1000 -> "backlog"
            else -> "healthy"
        }
        
        val recommendations = mutableListOf<String>()
        
        if (failedCount > 5) {
            recommendations.add("High failure rate detected. Check network connectivity.")
        }
        
        if (pendingCount > 15) {
            recommendations.add("Large number of pending batches. Consider increasing sync frequency.")
        }
        
        if (pendingEventCount > 500) {
            recommendations.add("Event backlog detected. Enable more aggressive batching.")
        }
        
        return BatchHealthReport(
            healthStatus = healthStatus,
            pendingBatches = pendingCount,
            failedBatches = failedCount,
            pendingEvents = pendingEventCount,
            recommendations = recommendations
        )
    }
    
    /**
     * Cleanup old completed and failed batches
     */
    suspend fun cleanupOldBatches() {
        val cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L) // 7 days
        
        syncBatchDao.deleteCompletedOldBatches(cutoffTime)
        syncBatchDao.deleteFailedOldBatches(cutoffTime)
    }
    
    /**
     * Emergency cleanup for storage optimization
     */
    suspend fun emergencyCleanup() {
        // More aggressive cleanup for storage issues
        val cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000L) // 1 day
        
        syncBatchDao.deleteCompletedOldBatches(cutoffTime)
        
        // Delete failed batches that have exceeded max retries
        val failedBatches = syncBatchDao.getSyncBatchesByStatus("failed")
        failedBatches.filter { it.syncAttempts >= it.maxRetries }.forEach { batch ->
            syncBatchDao.deleteSyncBatch(batch.batchId)
        }
    }
}

// Supporting enums and data classes

enum class BatteryLevel {
    CRITICAL, LOW, NORMAL, HIGH
}

data class BatchHealthReport(
    val healthStatus: String,
    val pendingBatches: Int,
    val failedBatches: Int,
    val pendingEvents: Int,
    val recommendations: List<String>
)