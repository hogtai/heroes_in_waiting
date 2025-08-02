package com.lifechurch.heroesinwaiting.analytics.utils

import com.lifechurch.heroesinwaiting.data.local.entities.BehavioralAnalyticsEntity
import com.lifechurch.heroesinwaiting.data.local.entities.AnalyticsEventEntity
import com.lifechurch.heroesinwaiting.data.local.entities.AnalyticsSyncBatchEntity
import kotlinx.coroutines.test.TestScope
import java.util.UUID
import java.security.MessageDigest

/**
 * Comprehensive test utilities for analytics testing framework
 * Provides COPPA-compliant test data generation and validation tools
 */
object AnalyticsTestUtils {
    
    // Test constants
    const val TEST_CLASSROOM_ID = "test-classroom-123"
    const val TEST_LESSON_ID = "test-lesson-456"
    const val TEST_ACTIVITY_ID = "test-activity-789"
    const val TEST_FACILITATOR_ID = "test-facilitator-101"
    
    // Anonymous test identifiers (COPPA compliant)
    private val anonymousSessionIds = mutableSetOf<String>()
    
    /**
     * Generate COPPA-compliant anonymous session ID
     * No personally identifiable information
     */
    fun generateAnonymousSessionId(): String {
        val sessionId = "anon_${UUID.randomUUID().toString().take(8)}_${System.currentTimeMillis()}"
        anonymousSessionIds.add(sessionId)
        return sessionId
    }
    
    /**
     * Generate SHA-256 hash for anonymous identifier
     */
    fun generateAnonymousHash(identifier: String, salt: String = "test_salt"): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest("$identifier$salt".toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Create test behavioral analytics entity with COPPA compliance
     */
    fun createTestBehavioralAnalyticsEntity(
        sessionId: String = generateAnonymousSessionId(),
        behavioralCategory: String = "empathy",
        interactionType: String = "lesson_start",
        timeSpentSeconds: Long = 120L,
        behavioralIndicators: Map<String, Any> = mapOf(
            "engagement_level" to "high",
            "response_quality" to "excellent",
            "emotional_context" to "peer_support"
        )
    ): BehavioralAnalyticsEntity {
        return BehavioralAnalyticsEntity(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            classroomId = TEST_CLASSROOM_ID,
            lessonId = TEST_LESSON_ID,
            activityId = TEST_ACTIVITY_ID,
            interactionType = interactionType,
            interactionTimestamp = System.currentTimeMillis(),
            timeSpentSeconds = timeSpentSeconds,
            interactionCount = 1,
            behavioralCategory = behavioralCategory,
            behavioralIndicators = behavioralIndicators,
            deviceType = "mobile",
            sessionContext = "classroom",
            screenSize = "normal",
            appVersion = "1.0.0",
            isOfflineRecorded = false,
            needsSync = true,
            syncAttempts = 0,
            lastSyncAttempt = null,
            loadTimeMs = 250L,
            errorOccurred = false,
            errorDetails = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Create test analytics event entity
     */
    fun createTestAnalyticsEventEntity(
        eventCategory: String = "lesson_interaction",
        eventAction: String = "tab_click",
        eventValue: Int = 1
    ): AnalyticsEventEntity {
        return AnalyticsEventEntity(
            id = UUID.randomUUID().toString(),
            sessionId = generateAnonymousSessionId(),
            classroomId = TEST_CLASSROOM_ID,
            userId = null, // No user ID for COPPA compliance
            eventCategory = eventCategory,
            eventAction = eventAction,
            eventLabel = "test_event",
            eventValue = eventValue,
            eventProperties = mapOf(
                "lesson_id" to TEST_LESSON_ID,
                "activity_id" to TEST_ACTIVITY_ID,
                "interaction_count" to 1
            ),
            deviceInfo = mapOf(
                "platform" to "Android",
                "app_version" to "1.0.0",
                "device_type" to "mobile"
            ),
            timestamp = System.currentTimeMillis(),
            isOfflineRecorded = false,
            needsSync = true,
            syncAttempts = 0,
            lastSyncAttempt = null,
            createdAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Create test sync batch entity
     */
    fun createTestSyncBatchEntity(
        batchSize: Int = 10
    ): AnalyticsSyncBatchEntity {
        return AnalyticsSyncBatchEntity(
            id = UUID.randomUUID().toString(),
            batchTimestamp = System.currentTimeMillis(),
            eventIds = (1..batchSize).map { UUID.randomUUID().toString() },
            syncStatus = "pending",
            syncAttempts = 0,
            lastSyncAttempt = null,
            errorMessage = null,
            createdAt = System.currentTimeMillis(),
            completedAt = null,
            retryAfter = null
        )
    }
    
    /**
     * Generate test data for empathy development tracking
     */
    fun createEmpathyTrackingData(): List<BehavioralAnalyticsEntity> {
        return listOf(
            createTestBehavioralAnalyticsEntity(
                behavioralCategory = "empathy",
                interactionType = "peer_support_response",
                behavioralIndicators = mapOf(
                    "empathy_score" to 4,
                    "response_quality" to "high",
                    "emotional_context" to "peer_distress",
                    "intervention_type" to "comfort_offer"
                )
            ),
            createTestBehavioralAnalyticsEntity(
                behavioralCategory = "empathy",
                interactionType = "emotional_recognition",
                behavioralIndicators = mapOf(
                    "empathy_score" to 5,
                    "response_quality" to "excellent",
                    "emotional_context" to "peer_celebration",
                    "recognition_accuracy" to "high"
                )
            )
        )
    }
    
    /**
     * Generate test data for confidence building tracking
     */
    fun createConfidenceTrackingData(): List<BehavioralAnalyticsEntity> {
        return listOf(
            createTestBehavioralAnalyticsEntity(
                behavioralCategory = "confidence",
                interactionType = "voice_sharing",
                behavioralIndicators = mapOf(
                    "confidence_score" to 3,
                    "participation_level" to "moderate",
                    "voice_volume" to "appropriate",
                    "eye_contact" to "good"
                )
            ),
            createTestBehavioralAnalyticsEntity(
                behavioralCategory = "confidence",
                interactionType = "leadership_demonstration",
                behavioralIndicators = mapOf(
                    "confidence_score" to 4,
                    "initiative_taking" to "high",
                    "peer_response" to "positive",
                    "task_completion" to "successful"
                )
            )
        )
    }
    
    /**
     * Generate test data for communication skill tracking
     */
    fun createCommunicationTrackingData(): List<BehavioralAnalyticsEntity> {
        return listOf(
            createTestBehavioralAnalyticsEntity(
                behavioralCategory = "communication",
                interactionType = "active_listening",
                behavioralIndicators = mapOf(
                    "communication_score" to 4,
                    "listening_quality" to "high",
                    "response_relevance" to "excellent",
                    "turn_taking" to "appropriate"
                )
            ),
            createTestBehavioralAnalyticsEntity(
                behavioralCategory = "communication",
                interactionType = "collaborative_discussion",
                behavioralIndicators = mapOf(
                    "communication_score" to 3,
                    "collaboration_level" to "good",
                    "idea_sharing" to "frequent",
                    "respectful_disagreement" to "demonstrated"
                )
            )
        )
    }
    
    /**
     * Generate test data for leadership behavior tracking
     */
    fun createLeadershipTrackingData(): List<BehavioralAnalyticsEntity> {
        return listOf(
            createTestBehavioralAnalyticsEntity(
                behavioralCategory = "leadership",
                interactionType = "initiative_taking",
                behavioralIndicators = mapOf(
                    "leadership_score" to 4,
                    "initiative_frequency" to "high",
                    "peer_following" to "positive",
                    "decision_quality" to "good"
                )
            ),
            createTestBehavioralAnalyticsEntity(
                behavioralCategory = "leadership",
                interactionType = "conflict_resolution",
                behavioralIndicators = mapOf(
                    "leadership_score" to 5,
                    "mediation_success" to "excellent",
                    "fairness_demonstrated" to "high",
                    "solution_creativity" to "innovative"
                )
            )
        )
    }
    
    /**
     * Generate large dataset for performance testing
     */
    fun generateLargeAnalyticsDataset(size: Int = 1000): List<BehavioralAnalyticsEntity> {
        val data = mutableListOf<BehavioralAnalyticsEntity>()
        val categories = listOf("empathy", "confidence", "communication", "leadership")
        val interactionTypes = listOf(
            "lesson_start", "activity_complete", "peer_interaction", 
            "voice_sharing", "emotional_response", "problem_solving"
        )
        
        repeat(size) { index ->
            val category = categories[index % categories.size]
            val interactionType = interactionTypes[index % interactionTypes.size]
            
            data.add(createTestBehavioralAnalyticsEntity(
                behavioralCategory = category,
                interactionType = interactionType,
                timeSpentSeconds = (30..300).random().toLong(),
                behavioralIndicators = mapOf(
                    "${category}_score" to (1..5).random(),
                    "engagement_level" to listOf("low", "medium", "high").random(),
                    "session_quality" to listOf("poor", "average", "good", "excellent").random()
                )
            ))
        }
        
        return data
    }
    
    /**
     * Validate that analytics data contains no PII
     */
    fun validateCOPPACompliance(entity: BehavioralAnalyticsEntity): Boolean {
        val entityString = entity.toString()
        
        // Check for common PII patterns
        val piiPatterns = listOf(
            Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"), // Email
            Regex("\\b\\d{3}-\\d{3}-\\d{4}\\b"), // Phone number
            Regex("\\b\\d{3}\\s\\w+\\s(street|st|avenue|ave|road|rd)\\b", RegexOption.IGNORE_CASE), // Address
            Regex("\\b(john|jane|mike|sarah|david|mary)\\s+(doe|smith|johnson|brown)\\b", RegexOption.IGNORE_CASE) // Names
        )
        
        return piiPatterns.none { it.containsMatchIn(entityString) }
    }
    
    /**
     * Generate mock device info for testing
     */
    fun createMockDeviceInfo(): Map<String, Any> {
        return mapOf(
            "platform" to "Android",
            "os_version" to "API 30",
            "app_version" to "1.0.0",
            "device_model" to "Test Device",
            "screen_density" to "xhdpi",
            "available_memory" to 2048,
            "network_type" to "wifi"
        )
    }
    
    /**
     * Create test scenario for offline-first testing
     */
    fun createOfflineScenario(): List<BehavioralAnalyticsEntity> {
        return listOf(
            createTestBehavioralAnalyticsEntity(
                interactionType = "offline_lesson_start",
                behavioralIndicators = mapOf(
                    "offline_mode" to true,
                    "cache_used" to true,
                    "sync_pending" to true
                )
            ).copy(isOfflineRecorded = true, needsSync = true),
            
            createTestBehavioralAnalyticsEntity(
                interactionType = "offline_activity_complete",
                behavioralIndicators = mapOf(
                    "offline_mode" to true,
                    "completion_cached" to true,
                    "data_queued" to true
                )
            ).copy(isOfflineRecorded = true, needsSync = true)
        )
    }
    
    /**
     * Clean up test data after test execution
     */
    fun cleanupTestData() {
        anonymousSessionIds.clear()
    }
    
    /**
     * Assert analytics data meets quality standards
     */
    fun assertAnalyticsQuality(entities: List<BehavioralAnalyticsEntity>) {
        entities.forEach { entity ->
            assert(entity.sessionId.isNotBlank()) { "Session ID must not be blank" }
            assert(entity.classroomId.isNotBlank()) { "Classroom ID must not be blank" }
            assert(entity.behavioralCategory.isNotBlank()) { "Behavioral category must not be blank" }
            assert(entity.interactionTimestamp > 0) { "Interaction timestamp must be positive" }
            assert(entity.timeSpentSeconds >= 0) { "Time spent must be non-negative" }
            assert(validateCOPPACompliance(entity)) { "Analytics data must be COPPA compliant" }
        }
    }
    
    /**
     * Create test scope for coroutine testing
     */
    fun createTestScope(): TestScope = TestScope()
}