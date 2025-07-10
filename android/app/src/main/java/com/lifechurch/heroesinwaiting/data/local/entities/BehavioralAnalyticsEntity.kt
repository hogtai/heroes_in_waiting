package com.lifechurch.heroesinwaiting.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.lifechurch.heroesinwaiting.data.local.converters.StringListConverter
import com.lifechurch.heroesinwaiting.data.local.converters.MapConverter

/**
 * COPPA-compliant behavioral analytics entity for tracking student interactions
 * Uses anonymous session IDs instead of personal identifiers
 */
@Entity(tableName = "behavioral_analytics")
@TypeConverters(StringListConverter::class, MapConverter::class)
data class BehavioralAnalyticsEntity(
    @PrimaryKey
    val id: String,
    
    // Session Information (Anonymous)
    val sessionId: String,
    val classroomId: String,
    val lessonId: String?,
    val activityId: String?,
    
    // Interaction Details
    val interactionType: String, // "lesson_start", "activity_complete", "question_answer", etc.
    val interactionTimestamp: Long,
    val timeSpentSeconds: Long,
    val interactionCount: Int = 1,
    
    // Behavioral Categorization
    val behavioralCategory: String, // "engagement", "empathy", "confidence", "communication"
    val behavioralIndicators: Map<String, Any> = emptyMap(),
    
    // Device and Context
    val deviceType: String = "mobile",
    val sessionContext: String, // "classroom", "individual", "group"
    val screenSize: String?,
    val appVersion: String,
    
    // Offline Support
    val isOfflineRecorded: Boolean = false,
    val needsSync: Boolean = true,
    val syncAttempts: Int = 0,
    val lastSyncAttempt: Long? = null,
    
    // Performance Metrics
    val loadTimeMs: Long? = null,
    val errorOccurred: Boolean = false,
    val errorDetails: String? = null,
    
    // Additional Metadata
    val additionalMetadata: Map<String, Any> = emptyMap(),
    val localTimestamp: Long = System.currentTimeMillis()
)