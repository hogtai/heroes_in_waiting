package com.lifechurch.heroesinwaiting.data.repository

import com.lifechurch.heroesinwaiting.data.api.ApiService
import com.lifechurch.heroesinwaiting.data.api.response.*
import com.lifechurch.heroesinwaiting.data.local.dao.BehavioralAnalyticsDao
import com.lifechurch.heroesinwaiting.data.local.dao.AnalyticsEventDao
import com.lifechurch.heroesinwaiting.data.local.dao.AnalyticsSyncBatchDao
import com.lifechurch.heroesinwaiting.data.local.entities.BehavioralAnalyticsEntity
import com.lifechurch.heroesinwaiting.data.local.entities.AnalyticsEventEntity
import com.lifechurch.heroesinwaiting.data.local.entities.AnalyticsSyncBatchEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Response
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing analytics data with offline-first architecture
 * Implements COPPA-compliant analytics tracking and efficient sync operations
 */
@Singleton
class AnalyticsRepository @Inject constructor(
    private val apiService: ApiService,
    private val behavioralAnalyticsDao: BehavioralAnalyticsDao,
    private val analyticsEventDao: AnalyticsEventDao,
    private val syncBatchDao: AnalyticsSyncBatchDao
) {
    
    // ================== Behavioral Analytics Operations ==================
    
    /**
     * Track a behavioral analytics event (offline-first)
     * Automatically queues for sync when online
     */
    suspend fun trackBehavioralAnalytics(
        classroomId: String,
        lessonId: String?,
        sessionId: String,
        interactionType: String,
        timeSpentSeconds: Long,
        behavioralCategory: String,
        behavioralIndicators: Map<String, Any> = emptyMap(),
        deviceType: String = "mobile",
        sessionContext: String = "classroom",
        additionalMetadata: Map<String, Any> = emptyMap()
    ): Result<String> {
        return try {
            val analyticsEntity = BehavioralAnalyticsEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                classroomId = classroomId,
                lessonId = lessonId,
                interactionType = interactionType,
                interactionTimestamp = System.currentTimeMillis(),
                timeSpentSeconds = timeSpentSeconds,
                behavioralCategory = behavioralCategory,
                behavioralIndicators = behavioralIndicators,
                deviceType = deviceType,
                sessionContext = sessionContext,
                screenSize = getScreenSize(),
                appVersion = getAppVersion(),
                additionalMetadata = additionalMetadata
            )
            
            // Store locally first (offline-first approach)
            behavioralAnalyticsDao.insertBehavioralAnalytics(analyticsEntity)
            
            // Attempt immediate sync if online
            tryImmediateSync(analyticsEntity)
            
            Result.success(analyticsEntity.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Track a general analytics event (offline-first)
     */
    suspend fun trackAnalyticsEvent(
        eventType: String,
        eventCategory: String,
        eventAction: String,
        eventLabel: String? = null,
        sessionId: String,
        classroomId: String? = null,
        lessonId: String? = null,
        activityId: String? = null,
        properties: Map<String, Any> = emptyMap(),
        value: Double? = null,
        duration: Long? = null
    ): Result<String> {
        return try {
            val eventEntity = AnalyticsEventEntity(
                id = UUID.randomUUID().toString(),
                eventType = eventType,
                eventCategory = eventCategory,
                eventAction = eventAction,
                eventLabel = eventLabel,
                sessionId = sessionId,
                classroomId = classroomId,
                lessonId = lessonId,
                activityId = activityId,
                timestamp = System.currentTimeMillis(),
                duration = duration,
                properties = properties,
                value = value,
                deviceType = getDeviceType(),
                appVersion = getAppVersion(),
                connectionType = getConnectionType()
            )
            
            // Store locally first
            analyticsEventDao.insertAnalyticsEvent(eventEntity)
            
            // Attempt immediate sync if online
            tryImmediateEventSync(eventEntity)
            
            Result.success(eventEntity.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ================== Sync Operations ==================
    
    /**
     * Sync pending analytics data to server
     * Uses efficient batching to minimize network requests
     */
    suspend fun syncPendingAnalytics(): Result<SyncResult> {
        return try {
            val pendingBehavioral = behavioralAnalyticsDao.getPendingSyncAnalytics(50)
            val pendingEvents = analyticsEventDao.getPendingSyncEvents(100)
            
            var successCount = 0
            var failureCount = 0
            val errors = mutableListOf<String>()
            
            // Sync behavioral analytics in batches
            if (pendingBehavioral.isNotEmpty()) {
                val batchResult = syncBehavioralAnalyticsBatch(pendingBehavioral)
                if (batchResult.isSuccess) {
                    successCount += pendingBehavioral.size
                } else {
                    failureCount += pendingBehavioral.size
                    errors.add("Behavioral analytics sync failed: ${batchResult.exceptionOrNull()?.message}")
                }
            }
            
            // Sync general events
            if (pendingEvents.isNotEmpty()) {
                val eventResult = syncAnalyticsEventsBatch(pendingEvents)
                if (eventResult.isSuccess) {
                    successCount += pendingEvents.size
                } else {
                    failureCount += pendingEvents.size
                    errors.add("Analytics events sync failed: ${eventResult.exceptionOrNull()?.message}")
                }
            }
            
            Result.success(SyncResult(successCount, failureCount, errors))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create and manage sync batches for efficient data transmission
     */
    suspend fun createSyncBatch(
        eventIds: List<String>,
        batchType: String = "mixed",
        priority: Int = 0,
        requiresWifi: Boolean = false
    ): Result<String> {
        return try {
            val batchId = UUID.randomUUID().toString()
            val batch = AnalyticsSyncBatchEntity(
                batchId = batchId,
                eventIds = eventIds,
                eventCount = eventIds.size,
                batchType = batchType,
                status = "pending",
                priority = priority,
                createdAt = System.currentTimeMillis(),
                scheduledAt = System.currentTimeMillis(),
                requiresWifi = requiresWifi,
                estimatedDataSize = estimateDataSize(eventIds)
            )
            
            syncBatchDao.insertSyncBatch(batch)
            
            // Assign batch ID to events
            analyticsEventDao.assignBatchId(eventIds, batchId)
            
            Result.success(batchId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ================== Analytics Retrieval Operations ==================
    
    /**
     * Get enhanced classroom analytics (online with offline fallback)
     */
    suspend fun getEnhancedClassroomAnalytics(
        classroomId: String,
        token: String,
        timeframe: String = "30d",
        includeComparison: Boolean = false
    ): Result<EnhancedClassroomAnalyticsResponse> {
        return try {
            val response = apiService.getEnhancedClassroomAnalytics(
                classroomId = classroomId,
                token = token,
                timeframe = timeframe,
                includeComparison = includeComparison
            )
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                // Fallback to local analytics summary
                val localSummary = generateLocalAnalyticsSummary(classroomId)
                Result.failure(Exception("Server unavailable, local summary: $localSummary"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get lesson effectiveness analytics
     */
    suspend fun getLessonEffectivenessAnalytics(
        lessonId: String,
        token: String,
        timeframe: String = "30d"
    ): Result<LessonEffectivenessResponse> {
        return try {
            val response = apiService.getLessonEffectivenessAnalytics(
                lessonId = lessonId,
                token = token,
                timeframe = timeframe
            )
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch lesson effectiveness data"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get facilitator insights
     */
    suspend fun getFacilitatorInsights(
        token: String,
        timeframe: String = "30d"
    ): Result<FacilitatorInsightsResponse> {
        return try {
            val response = apiService.getFacilitatorInsights(
                token = token,
                timeframe = timeframe
            )
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch facilitator insights"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ================== Local Analytics Queries ==================
    
    /**
     * Get behavioral analytics by session (offline-capable)
     */
    fun getBehavioralAnalyticsBySession(sessionId: String): Flow<List<BehavioralAnalyticsEntity>> {
        return behavioralAnalyticsDao.getBehavioralAnalyticsBySession(sessionId)
    }
    
    /**
     * Get analytics events by classroom (offline-capable)
     */
    fun getAnalyticsEventsByClassroom(classroomId: String): Flow<List<AnalyticsEventEntity>> {
        return analyticsEventDao.getAnalyticsEventsByClassroom(classroomId)
    }
    
    /**
     * Get sync batch status
     */
    fun getSyncBatchStatus(): Flow<List<AnalyticsSyncBatchEntity>> {
        return syncBatchDao.getAllSyncBatches()
    }
    
    // ================== Cleanup Operations ==================
    
    /**
     * Clean up old analytics data
     */
    suspend fun cleanupOldAnalytics(retentionDays: Int = 90): Result<CleanupResult> {
        return try {
            val cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)
            
            val behavioralDeleted = behavioralAnalyticsDao.deleteSyncedOldAnalytics(cutoffTime)
            val eventsDeleted = analyticsEventDao.deleteSyncedOldEvents(cutoffTime)
            val batchesDeleted = syncBatchDao.deleteCompletedOldBatches(cutoffTime)
            
            Result.success(CleanupResult(behavioralDeleted, eventsDeleted, batchesDeleted))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ================== Private Helper Methods ==================
    
    private suspend fun tryImmediateSync(analytics: BehavioralAnalyticsEntity) {
        try {
            if (isOnline()) {
                val request = BehavioralAnalyticsRequest(
                    classroomId = analytics.classroomId,
                    lessonId = analytics.lessonId,
                    sessionId = analytics.sessionId,
                    interactionType = analytics.interactionType,
                    timeSpentSeconds = analytics.timeSpentSeconds,
                    behavioralCategory = analytics.behavioralCategory,
                    behavioralIndicators = analytics.behavioralIndicators,
                    deviceType = analytics.deviceType,
                    sessionContext = analytics.sessionContext,
                    additionalMetadata = analytics.additionalMetadata
                )
                
                val response = apiService.trackBehavioralAnalytics(getAuthToken(), request)
                if (response.isSuccessful) {
                    behavioralAnalyticsDao.markAsSynced(analytics.id, System.currentTimeMillis())
                }
            }
        } catch (e: Exception) {
            // Fail silently - will be synced later
        }
    }
    
    private suspend fun tryImmediateEventSync(event: AnalyticsEventEntity) {
        try {
            if (isOnline()) {
                val request = AnalyticsEventRequest(
                    eventType = event.eventType,
                    userId = null, // COPPA compliant - no user ID
                    sessionId = event.sessionId,
                    lessonId = event.lessonId,
                    classroomId = event.classroomId,
                    properties = event.properties,
                    timestamp = event.timestamp.toString()
                )
                
                val response = apiService.logAnalyticsEvent(getAuthToken(), request)
                if (response.isSuccessful) {
                    analyticsEventDao.markEventAsSynced(event.id, System.currentTimeMillis())
                }
            }
        } catch (e: Exception) {
            // Fail silently - will be synced later
        }
    }
    
    private suspend fun syncBehavioralAnalyticsBatch(analytics: List<BehavioralAnalyticsEntity>): Result<Unit> {
        return try {
            val requests = analytics.map { entity ->
                BehavioralAnalyticsRequest(
                    classroomId = entity.classroomId,
                    lessonId = entity.lessonId,
                    sessionId = entity.sessionId,
                    interactionType = entity.interactionType,
                    timeSpentSeconds = entity.timeSpentSeconds,
                    behavioralCategory = entity.behavioralCategory,
                    behavioralIndicators = entity.behavioralIndicators,
                    deviceType = entity.deviceType,
                    sessionContext = entity.sessionContext,
                    additionalMetadata = entity.additionalMetadata
                )
            }
            
            val batchRequest = BehavioralAnalyticsBatchRequest(
                events = requests,
                batchId = UUID.randomUUID().toString(),
                deviceType = getDeviceType(),
                appVersion = getAppVersion()
            )
            
            val response = apiService.trackBehavioralAnalyticsBatch(getAuthToken(), batchRequest)
            if (response.isSuccessful) {
                val ids = analytics.map { it.id }
                behavioralAnalyticsDao.markBatchAsSynced(ids)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Batch sync failed: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun syncAnalyticsEventsBatch(events: List<AnalyticsEventEntity>): Result<Unit> {
        return try {
            // Sync events individually for now - could be batched in future
            events.forEach { event ->
                val request = AnalyticsEventRequest(
                    eventType = event.eventType,
                    userId = null,
                    sessionId = event.sessionId,
                    lessonId = event.lessonId,
                    classroomId = event.classroomId,
                    properties = event.properties,
                    timestamp = event.timestamp.toString()
                )
                
                val response = apiService.logAnalyticsEvent(getAuthToken(), request)
                if (response.isSuccessful) {
                    analyticsEventDao.markEventAsSynced(event.id, System.currentTimeMillis())
                } else {
                    analyticsEventDao.markEventSyncFailed(
                        event.id,
                        System.currentTimeMillis(),
                        response.errorBody()?.string() ?: "Unknown error"
                    )
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun generateLocalAnalyticsSummary(classroomId: String): String {
        val summary = behavioralAnalyticsDao.getClassroomAnalyticsSummary(
            classroomId = classroomId,
            startTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L) // Last 7 days
        )
        return "Local: ${summary.totalEvents} events, ${summary.uniqueSessions} sessions"
    }
    
    private fun estimateDataSize(eventIds: List<String>): Long {
        // Rough estimate: 1KB per event on average
        return eventIds.size * 1024L
    }
    
    // ================== Utility Methods ==================
    
    private fun isOnline(): Boolean {
        // Implementation would check network connectivity
        return true // Placeholder
    }
    
    private fun getAuthToken(): String {
        // Implementation would get current auth token
        return "Bearer token" // Placeholder
    }
    
    private fun getDeviceType(): String = "mobile"
    
    private fun getAppVersion(): String = "1.0.0" // Would get from BuildConfig
    
    private fun getConnectionType(): String = "wifi" // Would detect actual connection
    
    private fun getScreenSize(): String = "normal" // Would detect actual screen size
}

// ================== Data Classes ==================

data class SyncResult(
    val successCount: Int,
    val failureCount: Int,
    val errors: List<String>
)

data class CleanupResult(
    val behavioralDeleted: Int,
    val eventsDeleted: Int,
    val batchesDeleted: Int
)