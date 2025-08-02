package com.lifechurch.heroesinwaiting.analytics.utils

import com.lifechurch.heroesinwaiting.data.local.entities.BehavioralAnalyticsEntity
import com.lifechurch.heroesinwaiting.data.local.entities.AnalyticsEventEntity
import com.lifechurch.heroesinwaiting.data.privacy.COPPAComplianceStatus
import java.util.regex.Pattern

/**
 * COPPA compliance validation utility for testing framework
 * Ensures all analytics data meets Children's Online Privacy Protection Act requirements
 */
object COPPATestValidator {
    
    /**
     * Comprehensive PII detection patterns
     */
    private val piiPatterns = mapOf(
        "email" to Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"),
        "phone" to Pattern.compile("\\b(?:\\d{3}[-.]?)?\\d{3}[-.]?\\d{4}\\b"),
        "ssn" to Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b"),
        "address" to Pattern.compile("\\b\\d+\\s+([A-Za-z]+\\s+)+(Street|St|Avenue|Ave|Road|Rd|Drive|Dr|Lane|Ln|Boulevard|Blvd)\\b", Pattern.CASE_INSENSITIVE),
        "name" to Pattern.compile("\\b(first_name|last_name|full_name|student_name)\\b", Pattern.CASE_INSENSITIVE),
        "birth_date" to Pattern.compile("\\b(birth_date|birthday|date_of_birth|dob)\\b", Pattern.CASE_INSENSITIVE),
        "ip_address" to Pattern.compile("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b"),
        "device_id" to Pattern.compile("\\b(device_id|imei|android_id|advertising_id)\\b", Pattern.CASE_INSENSITIVE)
    )
    
    /**
     * Educational purpose validation keywords
     */
    private val educationalKeywords = setOf(
        "lesson", "activity", "engagement", "learning", "education",
        "empathy", "confidence", "communication", "leadership",
        "behavioral", "classroom", "curriculum", "skill", "development"
    )
    
    /**
     * Validate BehavioralAnalyticsEntity for COPPA compliance
     */
    fun validateBehavioralAnalytics(entity: BehavioralAnalyticsEntity): COPPAValidationResult {
        val violations = mutableListOf<String>()
        
        // Check for PII in all string fields
        checkForPII(entity.sessionId, "sessionId", violations)
        checkForPII(entity.interactionType, "interactionType", violations)
        checkForPII(entity.behavioralCategory, "behavioralCategory", violations)
        checkForPII(entity.sessionContext, "sessionContext", violations)
        checkForPII(entity.deviceType, "deviceType", violations)
        checkForPII(entity.screenSize ?: "", "screenSize", violations)
        checkForPII(entity.appVersion, "appVersion", violations)
        checkForPII(entity.errorDetails ?: "", "errorDetails", violations)
        
        // Check behavioral indicators map
        entity.behavioralIndicators.forEach { (key, value) ->
            checkForPII(key, "behavioralIndicators.key", violations)
            checkForPII(value.toString(), "behavioralIndicators.value", violations)
        }
        
        // Validate anonymous session ID format
        if (!isAnonymousSessionId(entity.sessionId)) {
            violations.add("Session ID does not follow anonymous format: ${entity.sessionId}")
        }
        
        // Validate educational purpose
        if (!hasEducationalPurpose(entity)) {
            violations.add("Analytics data does not demonstrate clear educational purpose")
        }
        
        // Check data retention compliance (timestamps should be reasonable)
        val ninetyDaysAgo = System.currentTimeMillis() - (90 * 24 * 60 * 60 * 1000L)
        if (entity.createdAt < ninetyDaysAgo) {
            violations.add("Data appears to be older than 90-day retention policy")
        }
        
        return COPPAValidationResult(
            isCompliant = violations.isEmpty(),
            violations = violations,
            entity = entity.toString()
        )
    }
    
    /**
     * Validate AnalyticsEventEntity for COPPA compliance
     */
    fun validateAnalyticsEvent(entity: AnalyticsEventEntity): COPPAValidationResult {
        val violations = mutableListOf<String>()
        
        // Check for PII in all string fields
        checkForPII(entity.sessionId, "sessionId", violations)
        checkForPII(entity.eventCategory, "eventCategory", violations)
        checkForPII(entity.eventAction, "eventAction", violations)
        checkForPII(entity.eventLabel ?: "", "eventLabel", violations)
        
        // Check event properties map
        entity.eventProperties.forEach { (key, value) ->
            checkForPII(key, "eventProperties.key", violations)
            checkForPII(value.toString(), "eventProperties.value", violations)
        }
        
        // Check device info map
        entity.deviceInfo.forEach { (key, value) ->
            checkForPII(key, "deviceInfo.key", violations)
            checkForPII(value.toString(), "deviceInfo.value", violations)
        }
        
        // Validate no user ID is present (COPPA requirement)
        if (entity.userId != null) {
            violations.add("User ID must be null for COPPA compliance, found: ${entity.userId}")
        }
        
        // Validate anonymous session ID format
        if (!isAnonymousSessionId(entity.sessionId)) {
            violations.add("Session ID does not follow anonymous format: ${entity.sessionId}")
        }
        
        return COPPAValidationResult(
            isCompliant = violations.isEmpty(),
            violations = violations,
            entity = entity.toString()
        )
    }
    
    /**
     * Check for PII patterns in text
     */
    private fun checkForPII(text: String, fieldName: String, violations: MutableList<String>) {
        piiPatterns.forEach { (type, pattern) ->
            if (pattern.matcher(text).find()) {
                violations.add("Potential $type PII found in $fieldName: $text")
            }
        }
    }
    
    /**
     * Validate anonymous session ID format
     */
    private fun isAnonymousSessionId(sessionId: String): Boolean {
        // Should start with 'anon_' or be a UUID-like format
        return sessionId.startsWith("anon_") || 
               sessionId.matches(Regex("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}"))
    }
    
    /**
     * Validate educational purpose of analytics data
     */
    private fun hasEducationalPurpose(entity: BehavioralAnalyticsEntity): Boolean {
        val allText = "${entity.interactionType} ${entity.behavioralCategory} ${entity.behavioralIndicators.keys.joinToString(" ")}"
        return educationalKeywords.any { keyword ->
            allText.contains(keyword, ignoreCase = true)
        }
    }
    
    /**
     * Validate data minimization principle
     */
    fun validateDataMinimization(entities: List<BehavioralAnalyticsEntity>): DataMinimizationResult {
        val fieldUsage = mutableMapOf<String, Int>()
        val unnecessaryFields = mutableListOf<String>()
        
        entities.forEach { entity ->
            // Count field usage
            if (entity.screenSize != null) fieldUsage["screenSize"] = fieldUsage.getOrDefault("screenSize", 0) + 1
            if (entity.errorDetails != null) fieldUsage["errorDetails"] = fieldUsage.getOrDefault("errorDetails", 0) + 1
            if (entity.loadTimeMs != null) fieldUsage["loadTimeMs"] = fieldUsage.getOrDefault("loadTimeMs", 0) + 1
            
            // Check for unnecessary granular data
            entity.behavioralIndicators.forEach { (key, value) ->
                if (key.contains("timestamp") || key.contains("precise_location")) {
                    unnecessaryFields.add("Potentially unnecessary detailed field: $key")
                }
            }
        }
        
        return DataMinimizationResult(
            fieldUsage = fieldUsage,
            unnecessaryFields = unnecessaryFields.distinct(),
            isMinimized = unnecessaryFields.isEmpty()
        )
    }
    
    /**
     * Validate consent and retention policies
     */
    fun validateConsentCompliance(
        facilitatorConsent: Boolean,
        dataRetentionDays: Int = 90,
        educationalPurposeOnly: Boolean = true
    ): ConsentComplianceResult {
        val violations = mutableListOf<String>()
        
        if (!facilitatorConsent) {
            violations.add("Facilitator consent is required for COPPA compliance")
        }
        
        if (dataRetentionDays > 90) {
            violations.add("Data retention period exceeds recommended 90 days: $dataRetentionDays")
        }
        
        if (!educationalPurposeOnly) {
            violations.add("Data collection must be limited to educational purposes only")
        }
        
        return ConsentComplianceResult(
            isCompliant = violations.isEmpty(),
            violations = violations
        )
    }
    
    /**
     * Validate SHA-256 hash format for anonymous identifiers
     */
    fun validateHashFormat(hash: String): Boolean {
        // SHA-256 produces 64 character hexadecimal string
        return hash.matches(Regex("[a-f0-9]{64}"))
    }
    
    /**
     * Generate compliance report for analytics dataset
     */
    fun generateComplianceReport(
        behavioralEntities: List<BehavioralAnalyticsEntity>,
        eventEntities: List<AnalyticsEventEntity>
    ): COPPAComplianceReport {
        val behavioralResults = behavioralEntities.map { validateBehavioralAnalytics(it) }
        val eventResults = eventEntities.map { validateAnalyticsEvent(it) }
        
        val totalViolations = behavioralResults.sumOf { it.violations.size } + 
                             eventResults.sumOf { it.violations.size }
        
        val compliancePercentage = if (behavioralEntities.isEmpty() && eventEntities.isEmpty()) {
            100.0
        } else {
            val totalEntities = behavioralEntities.size + eventEntities.size
            val compliantEntities = behavioralResults.count { it.isCompliant } + 
                                   eventResults.count { it.isCompliant }
            (compliantEntities.toDouble() / totalEntities) * 100
        }
        
        return COPPAComplianceReport(
            totalEntitiesChecked = behavioralEntities.size + eventEntities.size,
            compliantEntities = behavioralResults.count { it.isCompliant } + eventResults.count { it.isCompliant },
            totalViolations = totalViolations,
            compliancePercentage = compliancePercentage,
            behavioralResults = behavioralResults,
            eventResults = eventResults,
            isFullyCompliant = totalViolations == 0
        )
    }
}

/**
 * COPPA validation result for individual entities
 */
data class COPPAValidationResult(
    val isCompliant: Boolean,
    val violations: List<String>,
    val entity: String
)

/**
 * Data minimization validation result
 */
data class DataMinimizationResult(
    val fieldUsage: Map<String, Int>,
    val unnecessaryFields: List<String>,
    val isMinimized: Boolean
)

/**
 * Consent compliance validation result
 */
data class ConsentComplianceResult(
    val isCompliant: Boolean,
    val violations: List<String>
)

/**
 * Comprehensive COPPA compliance report
 */
data class COPPAComplianceReport(
    val totalEntitiesChecked: Int,
    val compliantEntities: Int,
    val totalViolations: Int,
    val compliancePercentage: Double,
    val behavioralResults: List<COPPAValidationResult>,
    val eventResults: List<COPPAValidationResult>,
    val isFullyCompliant: Boolean
)