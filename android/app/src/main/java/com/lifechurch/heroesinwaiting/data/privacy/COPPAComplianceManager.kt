package com.lifechurch.heroesinwaiting.data.privacy

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import com.lifechurch.heroesinwaiting.data.local.dao.AnalyticsEventDao
import com.lifechurch.heroesinwaiting.data.local.dao.BehavioralAnalyticsDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * COPPA (Children's Online Privacy Protection Act) compliance manager
 * Ensures all analytics collection follows COPPA requirements for educational platforms
 */
@Singleton
class COPPAComplianceManager @Inject constructor(
    private val context: Context,
    private val sharedPreferences: SharedPreferences,
    private val analyticsEventDao: AnalyticsEventDao,
    private val behavioralAnalyticsDao: BehavioralAnalyticsDao
) {
    
    // Privacy state tracking
    private val _privacySettings = MutableStateFlow(PrivacySettings())
    val privacySettings: StateFlow<PrivacySettings> = _privacySettings.asStateFlow()
    
    // Anonymous identifiers
    private val _anonymousStudentId = MutableStateFlow<String?>(null)
    val anonymousStudentId: StateFlow<String?> = _anonymousStudentId.asStateFlow()
    
    init {
        loadPrivacySettings()
        ensureAnonymousIdentifiers()
    }
    
    /**
     * Initializes COPPA-compliant analytics collection
     * Must be called before any analytics tracking begins
     */
    suspend fun initializeCOPPACompliantAnalytics(
        facilitatorConsent: Boolean,
        educationalPurposeOnly: Boolean = true,
        dataRetentionDays: Int = 90
    ): COPPAInitializationResult {
        
        val settings = PrivacySettings(
            facilitatorConsentGranted = facilitatorConsent,
            educationalPurposeOnly = educationalPurposeOnly,
            analyticsEnabled = facilitatorConsent,
            dataRetentionDays = dataRetentionDays,
            anonymousTrackingOnly = true,
            shareWithThirdParties = false,
            allowBehavioralAnalytics = facilitatorConsent,
            consentTimestamp = System.currentTimeMillis()
        )
        
        _privacySettings.value = settings
        savePrivacySettings(settings)
        
        if (facilitatorConsent) {
            generateAnonymousIdentifiers()
        } else {
            clearAllAnalyticsData()
        }
        
        return COPPAInitializationResult(
            success = true,
            anonymousTrackingEnabled = facilitatorConsent,
            dataRetentionDays = dataRetentionDays,
            complianceLevel = "COPPA_COMPLIANT"
        )
    }
    
    /**
     * Generates anonymous session and student identifiers
     * No personally identifiable information is stored
     */
    private fun generateAnonymousIdentifiers() {
        // Generate anonymous student identifier (session-based)
        val sessionId = UUID.randomUUID().toString()
        val hashedId = hashIdentifier(sessionId)
        
        _anonymousStudentId.value = hashedId
        sharedPreferences.edit()
            .putString(ANONYMOUS_STUDENT_ID_KEY, hashedId)
            .putLong(SESSION_START_TIME_KEY, System.currentTimeMillis())
            .apply()
    }
    
    /**
     * Creates anonymized analytics data that complies with COPPA
     */
    fun createAnonymizedAnalyticsData(
        originalData: Map<String, Any>,
        dataType: AnalyticsDataType
    ): Map<String, Any> {
        val anonymizedData = mutableMapOf<String, Any>()
        
        // Only include COPPA-compliant data fields
        originalData.forEach { (key, value) ->
            when {
                isCOPPACompliantField(key, dataType) -> {
                    anonymizedData[key] = if (needsAnonymization(key)) {
                        anonymizeValue(value)
                    } else {
                        value
                    }
                }
                // Remove any potentially identifying information
                isPotentiallyIdentifying(key) -> {
                    // Don't include this field
                }
                else -> {
                    // Include field as-is if it's safe
                    if (isEducationallyRelevant(key, dataType)) {
                        anonymizedData[key] = value
                    }
                }
            }
        }
        
        // Add COPPA compliance metadata
        anonymizedData[COMPLIANCE_MARKER_KEY] = "COPPA_ANONYMIZED"
        anonymizedData[ANONYMIZATION_TIMESTAMP_KEY] = System.currentTimeMillis()
        anonymizedData[DATA_TYPE_KEY] = dataType.name
        
        return anonymizedData
    }
    
    /**
     * Validates that data collection is COPPA compliant
     */
    fun validateCOPPACompliance(
        classroomId: String,
        dataToCollect: Map<String, Any>
    ): COPPAValidationResult {
        val settings = _privacySettings.value
        
        // Check basic consent requirements
        if (!settings.facilitatorConsentGranted) {
            return COPPAValidationResult(
                isCompliant = false,
                violations = listOf("No facilitator consent granted"),
                recommendations = listOf("Obtain explicit facilitator consent before collecting data")
            )
        }
        
        // Check for potential PII
        val violations = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        
        dataToCollect.forEach { (key, value) ->
            if (isPotentiallyIdentifying(key)) {
                violations.add("Field '$key' may contain personally identifying information")
                recommendations.add("Remove or anonymize field '$key'")
            }
            
            if (containsPII(value.toString())) {
                violations.add("Value for '$key' contains potential PII")
                recommendations.add("Sanitize value for field '$key'")
            }
        }
        
        // Check data retention compliance
        if (settings.dataRetentionDays > MAX_COPPA_RETENTION_DAYS) {
            violations.add("Data retention period exceeds COPPA recommendations")
            recommendations.add("Reduce retention period to ${MAX_COPPA_RETENTION_DAYS} days or less")
        }
        
        return COPPAValidationResult(
            isCompliant = violations.isEmpty(),
            violations = violations,
            recommendations = recommendations
        )
    }
    
    /**
     * Performs automatic data anonymization for COPPA compliance
     */
    suspend fun performDataAnonymization(): AnonymizationResult {
        var anonymizedCount = 0
        var errorCount = 0
        
        try {
            // Anonymize behavioral analytics
            val behavioralEvents = behavioralAnalyticsDao.getPendingSyncAnalytics(1000)
            behavioralEvents.forEach { event ->
                try {
                    val anonymizedIndicators = createAnonymizedAnalyticsData(
                        event.behavioralIndicators,
                        AnalyticsDataType.BEHAVIORAL
                    )
                    val anonymizedMetadata = createAnonymizedAnalyticsData(
                        event.additionalMetadata,
                        AnalyticsDataType.METADATA
                    )
                    
                    // Update with anonymized data
                    val anonymizedEvent = event.copy(
                        behavioralIndicators = anonymizedIndicators,
                        additionalMetadata = anonymizedMetadata
                    )
                    behavioralAnalyticsDao.updateBehavioralAnalytics(anonymizedEvent)
                    anonymizedCount++
                } catch (e: Exception) {
                    errorCount++
                }
            }
            
            // Anonymize general analytics events
            val generalEvents = analyticsEventDao.getPendingSyncEvents(1000)
            generalEvents.forEach { event ->
                try {
                    val anonymizedProperties = createAnonymizedAnalyticsData(
                        event.properties,
                        AnalyticsDataType.GENERAL
                    )
                    
                    val anonymizedEvent = event.copy(
                        properties = anonymizedProperties
                    )
                    analyticsEventDao.updateAnalyticsEvent(anonymizedEvent)
                    anonymizedCount++
                } catch (e: Exception) {
                    errorCount++
                }
            }
            
        } catch (e: Exception) {
            return AnonymizationResult(
                success = false,
                anonymizedRecords = anonymizedCount,
                errors = errorCount + 1,
                message = "Anonymization failed: ${e.message}"
            )
        }
        
        return AnonymizationResult(
            success = errorCount == 0,
            anonymizedRecords = anonymizedCount,
            errors = errorCount,
            message = if (errorCount == 0) "Anonymization completed successfully" 
                     else "Anonymization completed with $errorCount errors"
        )
    }
    
    /**
     * Clears all analytics data (for consent withdrawal)
     */
    suspend fun clearAllAnalyticsData(): DataClearanceResult {
        return try {
            // Clear all behavioral analytics
            val behavioralCount = behavioralAnalyticsDao.getPendingSyncCount()
            behavioralAnalyticsDao.deleteOldAnalytics(System.currentTimeMillis())
            
            // Clear all general events
            val eventsCount = analyticsEventDao.getPendingSyncCount()
            analyticsEventDao.deleteOldEvents(System.currentTimeMillis())
            
            // Clear anonymous identifiers
            clearAnonymousIdentifiers()
            
            DataClearanceResult(
                success = true,
                behavioralRecordsCleared = behavioralCount,
                eventRecordsCleared = eventsCount,
                totalCleared = behavioralCount + eventsCount
            )
        } catch (e: Exception) {
            DataClearanceResult(
                success = false,
                error = e.message
            )
        }
    }
    
    /**
     * Updates privacy settings (facilitator-controlled)
     */
    fun updatePrivacySettings(
        analyticsEnabled: Boolean? = null,
        behavioralAnalyticsEnabled: Boolean? = null,
        dataRetentionDays: Int? = null
    ) {
        val currentSettings = _privacySettings.value
        val updatedSettings = currentSettings.copy(
            analyticsEnabled = analyticsEnabled ?: currentSettings.analyticsEnabled,
            allowBehavioralAnalytics = behavioralAnalyticsEnabled ?: currentSettings.allowBehavioralAnalytics,
            dataRetentionDays = dataRetentionDays ?: currentSettings.dataRetentionDays,
            lastUpdated = System.currentTimeMillis()
        )
        
        _privacySettings.value = updatedSettings
        savePrivacySettings(updatedSettings)
    }
    
    /**
     * Gets current COPPA compliance status
     */
    fun getCOPPAComplianceStatus(): COPPAComplianceStatus {
        val settings = _privacySettings.value
        
        return COPPAComplianceStatus(
            isCompliant = settings.facilitatorConsentGranted && 
                         settings.anonymousTrackingOnly && 
                         !settings.shareWithThirdParties,
            consentLevel = if (settings.facilitatorConsentGranted) "FACILITATOR_GRANTED" else "NO_CONSENT",
            anonymizationLevel = if (settings.anonymousTrackingOnly) "FULL_ANONYMIZATION" else "NONE",
            dataRetentionDays = settings.dataRetentionDays,
            complianceScore = calculateComplianceScore(settings)
        )
    }
    
    // Private helper methods
    
    private fun isCOPPACompliantField(key: String, dataType: AnalyticsDataType): Boolean {
        val allowedFields = when (dataType) {
            AnalyticsDataType.BEHAVIORAL -> ALLOWED_BEHAVIORAL_FIELDS
            AnalyticsDataType.GENERAL -> ALLOWED_GENERAL_FIELDS
            AnalyticsDataType.METADATA -> ALLOWED_METADATA_FIELDS
        }
        return allowedFields.contains(key)
    }
    
    private fun isPotentiallyIdentifying(key: String): Boolean {
        val identifyingKeywords = listOf(
            "name", "email", "phone", "address", "ip", "device_id", 
            "user_id", "student_id", "personal", "contact"
        )
        return identifyingKeywords.any { keyword -> 
            key.lowercase().contains(keyword) 
        }
    }
    
    private fun containsPII(value: String): Boolean {
        // Simple PII detection patterns
        val emailPattern = Regex("""\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b""")
        val phonePattern = Regex("""\b\d{3}-?\d{3}-?\d{4}\b""")
        
        return emailPattern.containsMatchIn(value) || phonePattern.containsMatchIn(value)
    }
    
    private fun isEducationallyRelevant(key: String, dataType: AnalyticsDataType): Boolean {
        val educationalKeywords = listOf(
            "lesson", "activity", "engagement", "completion", "progress",
            "behavioral", "empathy", "confidence", "communication", "learning"
        )
        return educationalKeywords.any { keyword -> 
            key.lowercase().contains(keyword) 
        }
    }
    
    private fun needsAnonymization(key: String): Boolean {
        val sensitiveKeys = listOf("session_id", "classroom_id", "facilitator_id")
        return sensitiveKeys.contains(key)
    }
    
    private fun anonymizeValue(value: Any): String {
        return hashIdentifier(value.toString())
    }
    
    private fun hashIdentifier(identifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(identifier.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }.take(16) // 16 character hash
    }
    
    private fun calculateComplianceScore(settings: PrivacySettings): Float {
        var score = 0f
        
        if (settings.facilitatorConsentGranted) score += 3f
        if (settings.anonymousTrackingOnly) score += 2f
        if (!settings.shareWithThirdParties) score += 2f
        if (settings.dataRetentionDays <= MAX_COPPA_RETENTION_DAYS) score += 2f
        if (settings.educationalPurposeOnly) score += 1f
        
        return score / 10f // Normalize to 0-1 scale
    }
    
    private fun ensureAnonymousIdentifiers() {
        val existingId = sharedPreferences.getString(ANONYMOUS_STUDENT_ID_KEY, null)
        if (existingId == null && _privacySettings.value.analyticsEnabled) {
            generateAnonymousIdentifiers()
        } else {
            _anonymousStudentId.value = existingId
        }
    }
    
    private fun clearAnonymousIdentifiers() {
        _anonymousStudentId.value = null
        sharedPreferences.edit()
            .remove(ANONYMOUS_STUDENT_ID_KEY)
            .remove(SESSION_START_TIME_KEY)
            .apply()
    }
    
    private fun loadPrivacySettings() {
        val settings = PrivacySettings(
            facilitatorConsentGranted = sharedPreferences.getBoolean(FACILITATOR_CONSENT_KEY, false),
            educationalPurposeOnly = sharedPreferences.getBoolean(EDUCATIONAL_PURPOSE_KEY, true),
            analyticsEnabled = sharedPreferences.getBoolean(ANALYTICS_ENABLED_KEY, false),
            dataRetentionDays = sharedPreferences.getInt(DATA_RETENTION_DAYS_KEY, 90),
            anonymousTrackingOnly = sharedPreferences.getBoolean(ANONYMOUS_TRACKING_KEY, true),
            shareWithThirdParties = sharedPreferences.getBoolean(THIRD_PARTY_SHARING_KEY, false),
            allowBehavioralAnalytics = sharedPreferences.getBoolean(BEHAVIORAL_ANALYTICS_KEY, false),
            consentTimestamp = sharedPreferences.getLong(CONSENT_TIMESTAMP_KEY, 0),
            lastUpdated = sharedPreferences.getLong(LAST_UPDATED_KEY, 0)
        )
        _privacySettings.value = settings
    }
    
    private fun savePrivacySettings(settings: PrivacySettings) {
        sharedPreferences.edit()
            .putBoolean(FACILITATOR_CONSENT_KEY, settings.facilitatorConsentGranted)
            .putBoolean(EDUCATIONAL_PURPOSE_KEY, settings.educationalPurposeOnly)
            .putBoolean(ANALYTICS_ENABLED_KEY, settings.analyticsEnabled)
            .putInt(DATA_RETENTION_DAYS_KEY, settings.dataRetentionDays)
            .putBoolean(ANONYMOUS_TRACKING_KEY, settings.anonymousTrackingOnly)
            .putBoolean(THIRD_PARTY_SHARING_KEY, settings.shareWithThirdParties)
            .putBoolean(BEHAVIORAL_ANALYTICS_KEY, settings.allowBehavioralAnalytics)
            .putLong(CONSENT_TIMESTAMP_KEY, settings.consentTimestamp)
            .putLong(LAST_UPDATED_KEY, settings.lastUpdated)
            .apply()
    }
    
    companion object {
        // SharedPreferences keys
        private const val FACILITATOR_CONSENT_KEY = "facilitator_consent"
        private const val EDUCATIONAL_PURPOSE_KEY = "educational_purpose"
        private const val ANALYTICS_ENABLED_KEY = "analytics_enabled"
        private const val DATA_RETENTION_DAYS_KEY = "data_retention_days"
        private const val ANONYMOUS_TRACKING_KEY = "anonymous_tracking"
        private const val THIRD_PARTY_SHARING_KEY = "third_party_sharing"
        private const val BEHAVIORAL_ANALYTICS_KEY = "behavioral_analytics"
        private const val CONSENT_TIMESTAMP_KEY = "consent_timestamp"
        private const val LAST_UPDATED_KEY = "last_updated"
        private const val ANONYMOUS_STUDENT_ID_KEY = "anonymous_student_id"
        private const val SESSION_START_TIME_KEY = "session_start_time"
        
        // Compliance constants
        private const val MAX_COPPA_RETENTION_DAYS = 90
        private const val COMPLIANCE_MARKER_KEY = "_coppa_compliance"
        private const val ANONYMIZATION_TIMESTAMP_KEY = "_anonymized_at"
        private const val DATA_TYPE_KEY = "_data_type"
        
        // Allowed fields for different data types
        private val ALLOWED_BEHAVIORAL_FIELDS = setOf(
            "interaction_type", "behavioral_category", "engagement_level",
            "time_spent", "completion_rate", "interaction_count",
            "empathy_score", "confidence_level", "communication_quality",
            "leadership_behavior", "help_requested", "peer_interaction"
        )
        
        private val ALLOWED_GENERAL_FIELDS = setOf(
            "event_type", "event_action", "event_category",
            "lesson_category", "activity_type", "grade_level",
            "duration", "device_type", "app_version"
        )
        
        private val ALLOWED_METADATA_FIELDS = setOf(
            "timestamp", "session_context", "offline_mode",
            "device_type", "screen_size", "app_version",
            "connection_type", "lesson_category"
        )
    }
}

// Supporting data classes and enums

data class PrivacySettings(
    val facilitatorConsentGranted: Boolean = false,
    val educationalPurposeOnly: Boolean = true,
    val analyticsEnabled: Boolean = false,
    val dataRetentionDays: Int = 90,
    val anonymousTrackingOnly: Boolean = true,
    val shareWithThirdParties: Boolean = false,
    val allowBehavioralAnalytics: Boolean = false,
    val consentTimestamp: Long = 0,
    val lastUpdated: Long = 0
)

enum class AnalyticsDataType {
    BEHAVIORAL, GENERAL, METADATA
}

data class COPPAInitializationResult(
    val success: Boolean,
    val anonymousTrackingEnabled: Boolean,
    val dataRetentionDays: Int,
    val complianceLevel: String
)

data class COPPAValidationResult(
    val isCompliant: Boolean,
    val violations: List<String>,
    val recommendations: List<String>
)

data class AnonymizationResult(
    val success: Boolean,
    val anonymizedRecords: Int,
    val errors: Int,
    val message: String
)

data class DataClearanceResult(
    val success: Boolean,
    val behavioralRecordsCleared: Int = 0,
    val eventRecordsCleared: Int = 0,
    val totalCleared: Int = 0,
    val error: String? = null
)

data class COPPAComplianceStatus(
    val isCompliant: Boolean,
    val consentLevel: String,
    val anonymizationLevel: String,
    val dataRetentionDays: Int,
    val complianceScore: Float
)