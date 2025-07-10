package com.lifechurch.heroesinwaiting.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.lifechurch.heroesinwaiting.data.local.converters.StringListConverter

/**
 * Entity for tracking analytics sync batches
 * Helps manage offline-to-online sync operations efficiently
 */
@Entity(tableName = "analytics_sync_batches")
@TypeConverters(StringListConverter::class)
data class AnalyticsSyncBatchEntity(
    @PrimaryKey
    val batchId: String,
    
    // Batch Details
    val eventIds: List<String>, // References to analytics events
    val eventCount: Int,
    val batchType: String, // "behavioral", "general", "mixed"
    
    // Sync Status
    val status: String, // "pending", "syncing", "completed", "failed"
    val priority: Int = 0, // Higher number = higher priority
    
    // Timing
    val createdAt: Long,
    val scheduledAt: Long,
    val syncStartedAt: Long? = null,
    val syncCompletedAt: Long? = null,
    
    // Sync Details
    val syncAttempts: Int = 0,
    val maxRetries: Int = 3,
    val retryDelayMs: Long = 30000, // 30 seconds
    val nextRetryAt: Long? = null,
    
    // Results
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val lastError: String? = null,
    
    // Network Context
    val requiresWifi: Boolean = false,
    val estimatedDataSize: Long = 0,
    
    // Metadata
    val metadata: Map<String, Any> = emptyMap()
)