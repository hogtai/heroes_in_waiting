package com.lifechurch.heroesinwaiting.analytics.utils

import com.lifechurch.heroesinwaiting.data.analytics.AnalyticsService
import com.lifechurch.heroesinwaiting.data.local.HeroesDatabase
import com.lifechurch.heroesinwaiting.data.local.entities.BehavioralAnalyticsEntity
import com.lifechurch.heroesinwaiting.data.privacy.COPPAComplianceManager
import com.lifechurch.heroesinwaiting.data.privacy.COPPAComplianceStatus
import com.lifechurch.heroesinwaiting.data.privacy.COPPAValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

/**
 * Utility class for Espresso analytics testing
 * Provides helper methods for setting up test environments and validating analytics data
 */
class EspressoAnalyticsTestUtils(
    private val database: HeroesDatabase,
    private val analyticsService: AnalyticsService,
    private val coppaComplianceManager: COPPAComplianceManager
) {
    
    // Test constants
    companion object {
        const val TEST_CLASSROOM_ID = "test-classroom-123"
        const val TEST_LESSON_ID = "test-lesson-456"
        const val TEST_SESSION_ID = "test-session-789"
        const val TEST_FACILITATOR_ID = "test-facilitator-abc"
    }
    
    /**
     * Sets up a COPPA compliant test environment
     */
    suspend fun setupCOPPACompliantEnvironment() {
        // Setup compliant COPPA status
        val compliantStatus = COPPAComplianceStatus(
            isCompliant = true,
            consentLevel = "FACILITATOR_CONSENT",
            anonymizationLevel = "FULL",
            dataRetentionDays = 90,
            complianceScore = 100.0f
        )
        
        // Configure anonymous student ID
        val anonymousSessionId = generateAnonymousSessionId()
        
        // This would need to be implemented based on actual COPPAComplianceManager interface
        // coppaComplianceManager.setComplianceStatus(compliantStatus)
        // coppaComplianceManager.setAnonymousStudentId(anonymousSessionId)
    }
    
    /**
     * Generates a COPPA-compliant anonymous session ID
     */
    fun generateAnonymousSessionId(): String {
        return "anon_${UUID.randomUUID().toString().replace("-", "").substring(0, 16)}"
    }
    
    /**
     * Clears all analytics data from the test database
     */
    suspend fun clearAnalyticsData() {
        database.behavioralAnalyticsDao().deleteAll()
        database.analyticsEventDao().deleteAll()
        database.analyticsSyncBatchDao().deleteAll()
    }
    
    /**
     * Retrieves all stored analytics data from the database
     */
    suspend fun getStoredAnalytics(): List<BehavioralAnalyticsEntity> {
        return database.behavioralAnalyticsDao().getAllBehavioralAnalytics()
    }
    
    /**
     * Validates COPPA compliance for a collection of analytics events
     */
    fun validateAnalyticsCOPPACompliance(analytics: List<BehavioralAnalyticsEntity>): COPPAValidationResult {
        val violations = mutableListOf<String>()
        
        analytics.forEach { event ->
            val singleEventResult = validateSingleEventCOPPACompliance(event)
            if (!singleEventResult.isCompliant) {
                violations.addAll(singleEventResult.violations)
            }
        }
        
        return COPPAValidationResult(
            isCompliant = violations.isEmpty(),
            violations = violations,
            complianceScore = if (violations.isEmpty()) 100.0f else 0.0f
        )
    }
    
    /**
     * Validates COPPA compliance for a single analytics event
     */
    fun validateSingleEventCOPPACompliance(event: BehavioralAnalyticsEntity): COPPAValidationResult {
        val violations = mutableListOf<String>()
        
        // Check for PII patterns in all event data
        val eventString = event.toString()
        
        // Email pattern check
        if (eventString.contains(Regex("""[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}"""))) {
            violations.add("Email address detected in analytics data")
        }
        
        // Phone number pattern check
        if (eventString.contains(Regex("""\b\d{3}-\d{3}-\d{4}\b"""))) {
            violations.add("Phone number detected in analytics data")
        }
        
        // Name patterns (common first names)
        val commonNames = listOf("john", "jane", "mike", "sarah", "david", "mary")
        commonNames.forEach { name ->
            if (eventString.contains(name, ignoreCase = true)) {
                violations.add("Potential name detected: $name")
            }
        }
        
        // Address patterns
        if (eventString.contains(Regex("""\b\d+\s\w+\s(street|st|avenue|ave|road|rd)\b""", RegexOption.IGNORE_CASE))) {
            violations.add("Address pattern detected in analytics data")
        }
        
        // Check session ID is anonymous
        if (event.sessionId.contains("@") || event.sessionId.length < 16) {
            violations.add("Session ID does not appear to be properly anonymous")
        }
        
        // Check behavioral indicators for PII
        event.behavioralIndicators.values.forEach { value ->
            val valueString = value.toString()
            if (valueString.contains("@") || valueString.length > 100) {
                violations.add("Behavioral indicator may contain PII: ${valueString.take(20)}...")
            }
        }
        
        return COPPAValidationResult(
            isCompliant = violations.isEmpty(),
            violations = violations,
            complianceScore = if (violations.isEmpty()) 100.0f else (100.0f - violations.size * 25.0f).coerceAtLeast(0.0f)
        )
    }
    
    /**
     * Creates test behavioral analytics data with COPPA compliance
     */
    fun createTestBehavioralAnalytics(
        interactionType: String = "test_interaction",
        behavioralCategory: String = "engagement",
        behavioralIndicators: Map<String, Any> = emptyMap(),
        sessionId: String = generateAnonymousSessionId()
    ): BehavioralAnalyticsEntity {
        return BehavioralAnalyticsEntity(
            id = 0, // Auto-generated
            classroomId = TEST_CLASSROOM_ID,
            lessonId = TEST_LESSON_ID,
            sessionId = sessionId,
            interactionType = interactionType,
            interactionTimestamp = System.currentTimeMillis(),
            timeSpentSeconds = 0,
            behavioralCategory = behavioralCategory,
            behavioralIndicators = behavioralIndicators,
            deviceType = "test_device",
            sessionContext = "test_context",
            additionalMetadata = mapOf("test" to "data"),
            isOfflineRecorded = false,
            needsSync = true,
            syncAttempts = 0,
            lastSyncAttempt = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Validates that analytics tracking doesn't impact UI performance
     */
    suspend fun measureAnalyticsPerformanceImpact(operation: suspend () -> Unit): Long {
        val startTime = System.currentTimeMillis()
        operation()
        val endTime = System.currentTimeMillis()
        return endTime - startTime
    }
    
    /**
     * Simulates network connectivity changes for offline testing
     */
    fun simulateNetworkConnectivityChange(isOnline: Boolean) {
        // This would interface with the network state manager
        // For now, we'll simulate through test configuration
    }
    
    /**
     * Cleans up all test data and resets test environment
     */
    suspend fun cleanupTestData() {
        clearAnalyticsData()
        // Reset COPPA compliance manager to default state
        // Reset any other test state
    }
    
    /**
     * Validates educational analytics accuracy
     */
    fun validateEducationalAnalyticsAccuracy(
        analytics: List<BehavioralAnalyticsEntity>,
        expectedCategory: String,
        expectedScore: Int? = null
    ): Boolean {
        val relevantAnalytics = analytics.filter { it.behavioralCategory == expectedCategory }
        
        if (relevantAnalytics.isEmpty()) {
            return false
        }
        
        // Validate score range (1-5 for educational metrics)
        expectedScore?.let { score ->
            val actualScore = relevantAnalytics.first().behavioralIndicators["${expectedCategory}_score"] as? Int
            if (actualScore != score || actualScore !in 1..5) {
                return false
            }
        }
        
        // Validate educational purpose indicators
        val hasEducationalContext = relevantAnalytics.any { event ->
            event.behavioralIndicators.keys.any { key ->
                key.contains("educational") || key.contains("learning") || key.contains("skill")
            }
        }
        
        return hasEducationalContext
    }
    
    /**
     * Generates realistic test data for behavioral analytics
     */
    fun generateRealisticBehavioralData(category: String): Map<String, Any> {
        return when (category) {
            "empathy" -> mapOf(
                "empathy_score" to (1..5).random(),
                "response_quality" to listOf("low", "medium", "high").random(),
                "emotional_context" to listOf("peer_distress", "conflict_resolution", "support_needed").random(),
                "intervention_type" to listOf("comfort_offer", "active_listening", "problem_solving").random()
            )
            "confidence" -> mapOf(
                "confidence_score" to (1..5).random(),
                "participation_level" to listOf("low", "moderate", "high").random(),
                "voice_volume" to listOf("quiet", "appropriate", "loud").random(),
                "eye_contact" to listOf(true, false).random()
            )
            "communication" -> mapOf(
                "communication_score" to (1..5).random(),
                "listening_quality" to listOf("poor", "fair", "good", "excellent").random(),
                "response_relevance" to listOf("low", "medium", "high").random(),
                "turn_taking" to listOf(true, false).random()
            )
            "leadership" -> mapOf(
                "leadership_score" to (1..5).random(),
                "initiative_level" to listOf("low", "moderate", "high").random(),
                "peer_response" to listOf("negative", "neutral", "positive").random(),
                "task_completion" to listOf("incomplete", "partial", "successful").random()
            )
            else -> mapOf(
                "engagement_level" to listOf("low", "medium", "high").random(),
                "interaction_count" to (1..10).random()
            )
        }
    }
}

/**
 * Data class for COPPA validation results
 */
data class COPPAValidationResult(
    val isCompliant: Boolean,
    val violations: List<String>,
    val complianceScore: Float
)