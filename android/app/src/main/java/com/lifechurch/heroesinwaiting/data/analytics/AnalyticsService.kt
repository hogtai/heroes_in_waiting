package com.lifechurch.heroesinwaiting.data.analytics

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import com.lifechurch.heroesinwaiting.data.repository.AnalyticsRepository
import com.lifechurch.heroesinwaiting.data.privacy.COPPAComplianceManager
import com.lifechurch.heroesinwaiting.data.privacy.AnalyticsDataType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for tracking behavioral interactions and educational analytics
 * Implements COPPA-compliant anonymous tracking with offline-first architecture
 */
@Singleton
class AnalyticsService @Inject constructor(
    private val context: Context,
    private val analyticsRepository: AnalyticsRepository,
    private val sharedPreferences: SharedPreferences,
    private val coppaComplianceManager: COPPAComplianceManager
) {
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Anonymous session tracking (COPPA compliant)
    private val anonymousSessionId: String by lazy {
        coppaComplianceManager.anonymousStudentId.value
            ?: sharedPreferences.getString(ANONYMOUS_SESSION_KEY, null)
            ?: UUID.randomUUID().toString().also { sessionId ->
                sharedPreferences.edit().putString(ANONYMOUS_SESSION_KEY, sessionId).apply()
            }
    }
    
    // ================== Behavioral Analytics Tracking ==================
    
    /**
     * Track lesson start interaction
     */
    fun trackLessonStart(
        classroomId: String,
        lessonId: String,
        lessonTitle: String,
        gradeLevel: String,
        deviceInfo: DeviceInfo = getCurrentDeviceInfo()
    ) {
        serviceScope.launch {
            // Check COPPA compliance before tracking
            if (!isCOPPACompliant()) return@launch
            
            val behavioralIndicators = mapOf(
                "lesson_title" to lessonTitle,
                "grade_level" to gradeLevel,
                "start_timestamp" to System.currentTimeMillis()
            )
            
            val additionalMetadata = mapOf(
                "screen_size" to deviceInfo.screenSize,
                "connection_type" to deviceInfo.connectionType
            )
            
            // Validate COPPA compliance for this data
            val validationResult = coppaComplianceManager.validateCOPPACompliance(
                classroomId = classroomId,
                dataToCollect = behavioralIndicators + additionalMetadata
            )
            
            if (!validationResult.isCompliant) {
                // Log compliance violation but don't track
                return@launch
            }
            
            // Anonymize data before tracking
            val anonymizedIndicators = coppaComplianceManager.createAnonymizedAnalyticsData(
                behavioralIndicators, 
                AnalyticsDataType.BEHAVIORAL
            )
            
            val anonymizedMetadata = coppaComplianceManager.createAnonymizedAnalyticsData(
                additionalMetadata,
                AnalyticsDataType.METADATA
            )
            
            analyticsRepository.trackBehavioralAnalytics(
                classroomId = classroomId,
                lessonId = lessonId,
                sessionId = anonymousSessionId,
                interactionType = "lesson_start",
                timeSpentSeconds = 0,
                behavioralCategory = "engagement",
                behavioralIndicators = anonymizedIndicators,
                deviceType = deviceInfo.deviceType,
                sessionContext = "classroom",
                additionalMetadata = anonymizedMetadata
            )
        }
    }
    
    /**
     * Track lesson completion with engagement metrics
     */
    fun trackLessonCompletion(
        classroomId: String,
        lessonId: String,
        timeSpentSeconds: Long,
        completionRate: Float,
        interactionCount: Int,
        behavioralGrowthIndicators: Map<String, Any> = emptyMap()
    ) {
        serviceScope.launch {
            analyticsRepository.trackBehavioralAnalytics(
                classroomId = classroomId,
                lessonId = lessonId,
                sessionId = anonymousSessionId,
                interactionType = "lesson_completion",
                timeSpentSeconds = timeSpentSeconds,
                behavioralCategory = "achievement",
                behavioralIndicators = mapOf(
                    "completion_rate" to completionRate,
                    "interaction_count" to interactionCount,
                    "engagement_level" to calculateEngagementLevel(timeSpentSeconds, interactionCount),
                    "behavioral_growth" to behavioralGrowthIndicators
                ),
                additionalMetadata = mapOf(
                    "completion_timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    /**
     * Track empathy-related interactions
     */
    fun trackEmpathyInteraction(
        classroomId: String,
        lessonId: String,
        activityId: String,
        interactionType: String, // "empathy_response", "helping_behavior", "peer_support"
        empathyScore: Float,
        contextualFactors: Map<String, Any> = emptyMap()
    ) {
        serviceScope.launch {
            analyticsRepository.trackBehavioralAnalytics(
                classroomId = classroomId,
                lessonId = lessonId,
                sessionId = anonymousSessionId,
                interactionType = interactionType,
                timeSpentSeconds = 0,
                behavioralCategory = "empathy",
                behavioralIndicators = mapOf(
                    "empathy_score" to empathyScore,
                    "activity_id" to activityId,
                    "contextual_factors" to contextualFactors,
                    "peer_interaction" to (contextualFactors["peer_involved"] ?: false)
                ),
                sessionContext = "group_activity"
            )
        }
    }
    
    /**
     * Track confidence-building interactions
     */
    fun trackConfidenceBuilding(
        classroomId: String,
        lessonId: String,
        activityId: String,
        confidenceLevel: Float, // 1-5 scale
        participationLevel: String, // "high", "medium", "low"
        supportNeeded: Boolean = false
    ) {
        serviceScope.launch {
            analyticsRepository.trackBehavioralAnalytics(
                classroomId = classroomId,
                lessonId = lessonId,
                sessionId = anonymousSessionId,
                interactionType = "confidence_building",
                timeSpentSeconds = 0,
                behavioralCategory = "confidence",
                behavioralIndicators = mapOf(
                    "confidence_level" to confidenceLevel,
                    "participation_level" to participationLevel,
                    "support_needed" to supportNeeded,
                    "activity_id" to activityId
                ),
                sessionContext = "individual_activity"
            )
        }
    }
    
    /**
     * Track communication and social interactions
     */
    fun trackCommunicationInteraction(
        classroomId: String,
        lessonId: String,
        interactionType: String, // "peer_discussion", "presentation", "conflict_resolution"
        communicationQuality: String, // "effective", "developing", "needs_support"
        peerInteractionCount: Int,
        conflictResolution: Boolean = false
    ) {
        serviceScope.launch {
            analyticsRepository.trackBehavioralAnalytics(
                classroomId = classroomId,
                lessonId = lessonId,
                sessionId = anonymousSessionId,
                interactionType = interactionType,
                timeSpentSeconds = 0,
                behavioralCategory = "communication",
                behavioralIndicators = mapOf(
                    "communication_quality" to communicationQuality,
                    "peer_interaction_count" to peerInteractionCount,
                    "conflict_resolution" to conflictResolution,
                    "social_engagement" to (peerInteractionCount > 0)
                ),
                sessionContext = "group_activity"
            )
        }
    }
    
    /**
     * Track leadership and helping behaviors
     */
    fun trackLeadershipBehavior(
        classroomId: String,
        lessonId: String,
        leadershipType: String, // "peer_mentoring", "group_leadership", "initiative_taking"
        helpingBehavior: Boolean,
        leadershipEffectiveness: Float,
        peerResponse: String = "positive" // "positive", "neutral", "negative"
    ) {
        serviceScope.launch {
            analyticsRepository.trackBehavioralAnalytics(
                classroomId = classroomId,
                lessonId = lessonId,
                sessionId = anonymousSessionId,
                interactionType = "leadership_behavior",
                timeSpentSeconds = 0,
                behavioralCategory = "leadership",
                behavioralIndicators = mapOf(
                    "leadership_type" to leadershipType,
                    "helping_behavior" to helpingBehavior,
                    "effectiveness" to leadershipEffectiveness,
                    "peer_response" to peerResponse
                ),
                sessionContext = "group_activity"
            )
        }
    }
    
    // ================== Emotional Check-in Tracking ==================
    
    /**
     * Track emotional check-in with behavioral context
     */
    fun trackEmotionalCheckin(
        classroomId: String,
        lessonId: String?,
        emotionType: String,
        intensityLevel: Int, // 1-5 scale
        needsSupport: Boolean,
        contextualFactors: Map<String, Any> = emptyMap()
    ) {
        serviceScope.launch {
            analyticsRepository.trackBehavioralAnalytics(
                classroomId = classroomId,
                lessonId = lessonId,
                sessionId = anonymousSessionId,
                interactionType = "emotional_checkin",
                timeSpentSeconds = 0,
                behavioralCategory = "emotional_awareness",
                behavioralIndicators = mapOf(
                    "emotion_type" to emotionType,
                    "intensity_level" to intensityLevel,
                    "needs_support" to needsSupport,
                    "contextual_factors" to contextualFactors
                ),
                sessionContext = "individual_reflection"
            )
        }
    }
    
    // ================== General Event Tracking ==================
    
    /**
     * Track general user interactions
     */
    fun trackUserInteraction(
        eventType: String,
        eventAction: String,
        classroomId: String? = null,
        lessonId: String? = null,
        activityId: String? = null,
        properties: Map<String, Any> = emptyMap(),
        duration: Long? = null
    ) {
        serviceScope.launch {
            analyticsRepository.trackAnalyticsEvent(
                eventType = eventType,
                eventCategory = "user_interaction",
                eventAction = eventAction,
                sessionId = anonymousSessionId,
                classroomId = classroomId,
                lessonId = lessonId,
                activityId = activityId,
                properties = properties,
                duration = duration
            )
        }
    }
    
    /**
     * Track system events and errors
     */
    fun trackSystemEvent(
        eventType: String,
        eventAction: String,
        errorMessage: String? = null,
        properties: Map<String, Any> = emptyMap()
    ) {
        serviceScope.launch {
            analyticsRepository.trackAnalyticsEvent(
                eventType = eventType,
                eventCategory = "system",
                eventAction = eventAction,
                sessionId = anonymousSessionId,
                properties = properties + mapOfNotNull(
                    "error_message" to errorMessage
                )
            )
        }
    }
    
    // ================== Session Management ==================
    
    /**
     * Start a new anonymous session (COPPA compliant)
     */
    fun startNewSession(classroomId: String, facilitatorId: String) {
        val newSessionId = UUID.randomUUID().toString()
        sharedPreferences.edit().putString(ANONYMOUS_SESSION_KEY, newSessionId).apply()
        
        serviceScope.launch {
            analyticsRepository.trackAnalyticsEvent(
                eventType = "session_start",
                eventCategory = "session",
                eventAction = "start",
                sessionId = newSessionId,
                classroomId = classroomId,
                properties = mapOf(
                    "facilitator_id" to facilitatorId,
                    "session_start_time" to System.currentTimeMillis()
                )
            )
        }
    }
    
    /**
     * End current session
     */
    fun endCurrentSession(classroomId: String, sessionDuration: Long) {
        serviceScope.launch {
            analyticsRepository.trackAnalyticsEvent(
                eventType = "session_end",
                eventCategory = "session",
                eventAction = "end",
                sessionId = anonymousSessionId,
                classroomId = classroomId,
                duration = sessionDuration,
                properties = mapOf(
                    "session_end_time" to System.currentTimeMillis(),
                    "session_duration_minutes" to (sessionDuration / 60000)
                )
            )
        }
    }
    
    // ================== Privacy and Consent Management ==================
    
    /**
     * Check if analytics tracking is enabled (COPPA compliance)
     */
    fun isAnalyticsEnabled(): Boolean {
        return coppaComplianceManager.privacySettings.value.analyticsEnabled
    }
    
    /**
     * Check if current tracking is COPPA compliant
     */
    private fun isCOPPACompliant(): Boolean {
        val complianceStatus = coppaComplianceManager.getCOPPAComplianceStatus()
        return complianceStatus.isCompliant && isAnalyticsEnabled()
    }
    
    /**
     * Enable or disable analytics tracking with COPPA compliance
     */
    fun setAnalyticsEnabled(enabled: Boolean) {
        coppaComplianceManager.updatePrivacySettings(analyticsEnabled = enabled)
        
        if (!enabled) {
            serviceScope.launch {
                // Clear all analytics data when disabled
                coppaComplianceManager.clearAllAnalyticsData()
            }
        }
    }
    
    /**
     * Initialize COPPA-compliant analytics (called by facilitator)
     */
    suspend fun initializeCOPPACompliantAnalytics(facilitatorConsent: Boolean): Boolean {
        val result = coppaComplianceManager.initializeCOPPACompliantAnalytics(
            facilitatorConsent = facilitatorConsent,
            educationalPurposeOnly = true,
            dataRetentionDays = 90
        )
        return result.success
    }
    
    /**
     * Get current COPPA compliance status
     */
    fun getCOPPAComplianceStatus() = coppaComplianceManager.getCOPPAComplianceStatus()
    
    /**
     * Get current anonymous session ID
     */
    fun getCurrentSessionId(): String = anonymousSessionId
    
    // ================== Sync Management ==================
    
    /**
     * Trigger manual sync of pending analytics
     */
    fun syncAnalytics() {
        serviceScope.launch {
            analyticsRepository.syncPendingAnalytics()
        }
    }
    
    // ================== Helper Methods ==================
    
    private fun calculateEngagementLevel(timeSpent: Long, interactions: Int): String {
        val minutes = timeSpent / 60
        val interactionsPerMinute = if (minutes > 0) interactions.toDouble() / minutes else 0.0
        
        return when {
            interactionsPerMinute >= 3.0 -> "high"
            interactionsPerMinute >= 1.5 -> "medium"
            else -> "low"
        }
    }
    
    private fun getCurrentDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            deviceType = "mobile",
            screenSize = getScreenSize(),
            connectionType = getConnectionType()
        )
    }
    
    private fun getScreenSize(): String {
        val metrics = context.resources.displayMetrics
        val density = metrics.density
        val widthDp = metrics.widthPixels / density
        val heightDp = metrics.heightPixels / density
        
        return when {
            widthDp >= 600 -> "tablet"
            widthDp >= 480 -> "large_phone"
            else -> "phone"
        }
    }
    
    private fun getConnectionType(): String {
        // Implementation would check actual connection type
        return "wifi"
    }
    
    companion object {
        private const val ANONYMOUS_SESSION_KEY = "anonymous_session_id"
        private const val ANALYTICS_ENABLED_KEY = "analytics_enabled"
    }
}

// ================== Data Classes ==================

data class DeviceInfo(
    val deviceType: String,
    val screenSize: String,
    val connectionType: String
)

// ================== Extension Functions ==================

private fun <K, V> mapOfNotNull(vararg pairs: Pair<K, V?>): Map<K, V> {
    return pairs.mapNotNull { (key, value) -> value?.let { key to it } }.toMap()
}