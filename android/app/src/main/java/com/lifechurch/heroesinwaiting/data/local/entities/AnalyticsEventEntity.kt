package com.lifechurch.heroesinwaiting.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.lifechurch.heroesinwaiting.data.local.converters.MapConverter

/**
 * General analytics event entity for tracking all types of interactions
 * Supports both online and offline event collection
 */
@Entity(tableName = "analytics_events")
@TypeConverters(MapConverter::class)
data class AnalyticsEventEntity(
    @PrimaryKey
    val id: String,
    
    // Event Classification
    val eventType: String, // "lesson_interaction", "behavioral_indicator", "system_event"
    val eventCategory: String, // "engagement", "learning", "technical", "behavioral"
    val eventAction: String, // "start", "complete", "error", "interaction"
    val eventLabel: String?, // Additional context
    
    // Session Context (Anonymous)
    val sessionId: String,
    val classroomId: String?,
    val lessonId: String?,
    val activityId: String?,
    
    // Timing Information
    val timestamp: Long,
    val duration: Long? = null,
    val sequence: Int = 0, // Order within session
    
    // Event Properties
    val properties: Map<String, Any> = emptyMap(),
    val value: Double? = null,
    
    // Device Context
    val deviceType: String,
    val appVersion: String,
    val connectionType: String, // "wifi", "cellular", "offline"
    
    // Sync Status
    val isSynced: Boolean = false,
    val syncAttempts: Int = 0,
    val lastSyncAttempt: Long? = null,
    val syncError: String? = null,
    
    // Local Metadata
    val localTimestamp: Long = System.currentTimeMillis(),
    val batchId: String? = null // For batching sync operations
)