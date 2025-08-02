package com.lifechurch.heroesinwaiting.analytics.unit

import android.content.Context
import android.content.SharedPreferences
import com.lifechurch.heroesinwaiting.analytics.utils.AnalyticsTestUtils
import com.lifechurch.heroesinwaiting.analytics.utils.COPPATestValidator
import com.lifechurch.heroesinwaiting.analytics.utils.MockDataGenerator
import com.lifechurch.heroesinwaiting.data.local.dao.AnalyticsEventDao
import com.lifechurch.heroesinwaiting.data.local.dao.BehavioralAnalyticsDao
import com.lifechurch.heroesinwaiting.data.privacy.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import java.security.MessageDigest
import kotlin.test.*

/**
 * Comprehensive unit tests for COPPAComplianceManager
 * Validates PII detection, anonymization, data retention, and consent management
 */
class COPPAComplianceManagerTest {
    
    @Mock
    private lateinit var context: Context
    
    @Mock
    private lateinit var sharedPreferences: SharedPreferences
    
    @Mock
    private lateinit var sharedPreferencesEditor: SharedPreferences.Editor
    
    @Mock
    private lateinit var analyticsEventDao: AnalyticsEventDao
    
    @Mock
    private lateinit var behavioralAnalyticsDao: BehavioralAnalyticsDao
    
    private lateinit var coppaComplianceManager: COPPAComplianceManager
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Mock SharedPreferences behavior
        whenever(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor)
        whenever(sharedPreferencesEditor.putString(any(), any())).thenReturn(sharedPreferencesEditor)
        whenever(sharedPreferencesEditor.putBoolean(any(), any())).thenReturn(sharedPreferencesEditor)
        whenever(sharedPreferencesEditor.putInt(any(), any())).thenReturn(sharedPreferencesEditor)
        whenever(sharedPreferencesEditor.putLong(any(), any())).thenReturn(sharedPreferencesEditor)
        whenever(sharedPreferencesEditor.apply()).then { }
        
        // Initialize the manager
        coppaComplianceManager = COPPAComplianceManager(
            context = context,
            sharedPreferences = sharedPreferences,
            analyticsEventDao = analyticsEventDao,
            behavioralAnalyticsDao = behavioralAnalyticsDao
        )
    }
    
    @After
    fun teardown() {
        AnalyticsTestUtils.cleanupTestData()
    }
    
    // ================== COPPA Initialization Tests ==================
    
    @Test
    fun `COPPA initialization with facilitator consent succeeds`() = testScope.runTest {
        // Given: Valid facilitator consent
        val facilitatorConsent = true
        val educationalPurposeOnly = true
        val dataRetentionDays = 90
        
        // When: Initializing COPPA compliance
        val result = coppaComplianceManager.initializeCOPPACompliantAnalytics(
            facilitatorConsent = facilitatorConsent,
            educationalPurposeOnly = educationalPurposeOnly,
            dataRetentionDays = dataRetentionDays
        )
        
        // Then: Initialization should succeed
        assertTrue(result.isSuccessful)
        assertEquals("COPPA compliance initialized successfully", result.message)
        assertTrue(result.complianceStatus.isCompliant)
        assertEquals("FACILITATOR_CONSENT", result.complianceStatus.consentLevel)
        assertEquals("FULL", result.complianceStatus.anonymizationLevel)
        assertEquals(90, result.complianceStatus.dataRetentionDays)
        assertEquals(100.0f, result.complianceStatus.complianceScore)
    }
    
    @Test
    fun `COPPA initialization without facilitator consent fails`() = testScope.runTest {
        // Given: No facilitator consent
        val facilitatorConsent = false
        
        // When: Initializing COPPA compliance
        val result = coppaComplianceManager.initializeCOPPACompliantAnalytics(
            facilitatorConsent = facilitatorConsent
        )
        
        // Then: Initialization should fail
        assertFalse(result.isSuccessful)
        assertTrue(result.message.contains("Facilitator consent is required"))
        assertFalse(result.complianceStatus.isCompliant)
        assertEquals("NO_CONSENT", result.complianceStatus.consentLevel)
    }
    
    @Test
    fun `COPPA initialization with excessive retention period warns`() = testScope.runTest {
        // Given: Excessive data retention period
        val dataRetentionDays = 180 // More than recommended 90 days
        
        // When: Initializing COPPA compliance
        val result = coppaComplianceManager.initializeCOPPACompliantAnalytics(
            facilitatorConsent = true,
            dataRetentionDays = dataRetentionDays
        )
        
        // Then: Should succeed but with warning
        assertTrue(result.isSuccessful)
        assertTrue(result.warnings.any { it.contains("exceeds recommended 90 days") })
        assertEquals(180, result.complianceStatus.dataRetentionDays)
    }
    
    // ================== PII Detection Tests ==================
    
    @Test
    fun `PII detection identifies email addresses`() {
        // Given: Data containing email addresses
        val dataWithEmail = mapOf(
            "user_feedback" to "Contact me at john.doe@example.com",
            "lesson_notes" to "Great lesson!"
        )
        
        // When: Checking for PII
        val hasPII = coppaComplianceManager.containsPII(dataWithEmail)
        
        // Then: PII should be detected
        assertTrue(hasPII)
    }
    
    @Test
    fun `PII detection identifies phone numbers`() {
        // Given: Data containing phone numbers
        val dataWithPhone = mapOf(
            "contact_info" to "Call me at 555-123-4567",
            "lesson_notes" to "Great lesson!"
        )
        
        // When: Checking for PII
        val hasPII = coppaComplianceManager.containsPII(dataWithPhone)
        
        // Then: PII should be detected
        assertTrue(hasPII)
    }
    
    @Test
    fun `PII detection identifies addresses`() {
        // Given: Data containing addresses
        val dataWithAddress = mapOf(
            "location" to "123 Main Street, Anytown",
            "lesson_notes" to "Great lesson!"
        )
        
        // When: Checking for PII
        val hasPII = coppaComplianceManager.containsPII(dataWithAddress)
        
        // Then: PII should be detected
        assertTrue(hasPII)
    }
    
    @Test
    fun `PII detection allows educational data without PII`() {
        // Given: Educational data without PII
        val educationalData = mapOf(
            "engagement_level" to "high",
            "empathy_score" to 4,
            "behavioral_category" to "empathy",
            "lesson_topic" to "understanding_feelings"
        )
        
        // When: Checking for PII
        val hasPII = coppaComplianceManager.containsPII(educationalData)
        
        // Then: No PII should be detected
        assertFalse(hasPII)
    }
    
    // ================== Data Anonymization Tests ==================
    
    @Test
    fun `data anonymization removes PII and preserves educational content`() {
        // Given: Data with mixed PII and educational content
        val originalData = mapOf(
            "student_email" to "student@school.edu", // PII - should be removed
            "student_phone" to "555-123-4567", // PII - should be removed
            "empathy_score" to 4, // Educational - should be preserved
            "engagement_level" to "high", // Educational - should be preserved
            "lesson_topic" to "building_empathy" // Educational - should be preserved
        )
        
        // When: Anonymizing data
        val anonymizedData = coppaComplianceManager.anonymizeData(originalData)
        
        // Then: PII should be removed, educational content preserved
        assertFalse(anonymizedData.containsKey("student_email"))
        assertFalse(anonymizedData.containsKey("student_phone"))
        assertEquals(4, anonymizedData["empathy_score"])
        assertEquals("high", anonymizedData["engagement_level"])
        assertEquals("building_empathy", anonymizedData["lesson_topic"])
        
        // Validate final data is COPPA compliant
        assertFalse(coppaComplianceManager.containsPII(anonymizedData))
    }
    
    @Test
    fun `data anonymization adds anonymization markers`() {
        // Given: Data requiring anonymization
        val originalData = mapOf(
            "student_name" to "John Doe", // PII
            "empathy_score" to 4
        )
        
        // When: Anonymizing data
        val anonymizedData = coppaComplianceManager.anonymizeData(originalData)
        
        // Then: Anonymization markers should be added
        assertEquals(true, anonymizedData["anonymized"])
        assertNotNull(anonymizedData["anonymization_timestamp"])
        assertEquals("PII_REMOVED", anonymizedData["anonymization_level"])
    }
    
    // ================== Anonymous Identifier Generation Tests ==================
    
    @Test
    fun `anonymous student ID generation creates consistent hashes`() = testScope.runTest {
        // Given: Same input data
        val classroomCode = "TEST123"
        val sessionInfo = "session_data"
        
        // When: Generating anonymous IDs multiple times
        val anonymousId1 = coppaComplianceManager.generateAnonymousStudentId(classroomCode, sessionInfo)
        val anonymousId2 = coppaComplianceManager.generateAnonymousStudentId(classroomCode, sessionInfo)
        
        // Then: IDs should be consistent (same input = same output)
        assertEquals(anonymousId1, anonymousId2)
        
        // Validate hash format (SHA-256 produces 64 character hex string)
        assertTrue(COPPATestValidator.validateHashFormat(anonymousId1))
        assertTrue(COPPATestValidator.validateHashFormat(anonymousId2))
    }
    
    @Test
    fun `anonymous student ID generation creates different hashes for different inputs`() = testScope.runTest {
        // Given: Different input data
        val classroomCode1 = "TEST123"
        val classroomCode2 = "TEST456"
        val sessionInfo = "session_data"
        
        // When: Generating anonymous IDs for different inputs
        val anonymousId1 = coppaComplianceManager.generateAnonymousStudentId(classroomCode1, sessionInfo)
        val anonymousId2 = coppaComplianceManager.generateAnonymousStudentId(classroomCode2, sessionInfo)
        
        // Then: IDs should be different
        assertNotEquals(anonymousId1, anonymousId2)
        
        // Both should be valid hash formats
        assertTrue(COPPATestValidator.validateHashFormat(anonymousId1))
        assertTrue(COPPATestValidator.validateHashFormat(anonymousId2))
    }
    
    @Test
    fun `anonymous student ID is stored and retrieved correctly`() = testScope.runTest {
        // Given: Anonymous ID generation
        val classroomCode = "TEST123"
        val sessionInfo = "session_data"
        
        // When: Generating and storing anonymous ID
        val generatedId = coppaComplianceManager.generateAnonymousStudentId(classroomCode, sessionInfo)
        
        // Wait for state update
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: ID should be available through state flow
        val storedId = coppaComplianceManager.anonymousStudentId.first()
        assertNotNull(storedId)
        
        // Validate it follows anonymous format
        assertTrue(storedId.startsWith("anon_") || COPPATestValidator.validateHashFormat(storedId))
    }
    
    // ================== Data Retention Tests ==================
    
    @Test
    fun `data retention policy is correctly configured`() = testScope.runTest {
        // Given: COPPA initialization with specific retention period
        val dataRetentionDays = 60
        
        coppaComplianceManager.initializeCOPPACompliantAnalytics(
            facilitatorConsent = true,
            dataRetentionDays = dataRetentionDays
        )
        
        // When: Getting privacy settings
        val privacySettings = coppaComplianceManager.privacySettings.first()
        
        // Then: Retention period should be configured correctly
        assertEquals(dataRetentionDays, privacySettings.dataRetentionDays)
        assertTrue(privacySettings.automaticCleanupEnabled)
    }
    
    @Test
    fun `expired data cleanup is triggered correctly`() = testScope.runTest {
        // Given: Initialized COPPA compliance
        coppaComplianceManager.initializeCOPPACompliantAnalytics(
            facilitatorConsent = true,
            dataRetentionDays = 90
        )
        
        // When: Triggering cleanup
        coppaComplianceManager.cleanupExpiredData()
        
        // Then: Cleanup operations should be called on DAOs
        verify(behavioralAnalyticsDao).deleteOlderThan(any())
        verify(analyticsEventDao).deleteOlderThan(any())
    }
    
    // ================== Consent Management Tests ==================
    
    @Test
    fun `facilitator consent can be withdrawn`() = testScope.runTest {
        // Given: Previously granted consent
        coppaComplianceManager.initializeCOPPACompliantAnalytics(
            facilitatorConsent = true
        )
        
        // When: Withdrawing consent
        coppaComplianceManager.withdrawFacilitatorConsent()
        
        // Then: Analytics should be disabled and data cleaned
        val complianceStatus = coppaComplianceManager.getCOPPAComplianceStatus()
        assertFalse(complianceStatus.isCompliant)
        assertEquals("CONSENT_WITHDRAWN", complianceStatus.consentLevel)
        
        // Cleanup should be triggered
        verify(behavioralAnalyticsDao).deleteAll()
        verify(analyticsEventDao).deleteAll()
    }
    
    @Test
    fun `consent withdrawal is persisted across app restarts`() = testScope.runTest {
        // Given: Consent withdrawal
        coppaComplianceManager.withdrawFacilitatorConsent()
        
        // When: Checking consent status after "restart" (new instance)
        val newManager = COPPAComplianceManager(
            context = context,
            sharedPreferences = sharedPreferences,
            analyticsEventDao = analyticsEventDao,
            behavioralAnalyticsDao = behavioralAnalyticsDao
        )
        
        // Then: Consent withdrawal should be persisted
        verify(sharedPreferencesEditor).putBoolean(eq("facilitator_consent_granted"), eq(false))
    }
    
    // ================== Educational Purpose Validation Tests ==================
    
    @Test
    fun `educational purpose validation allows appropriate data`() {
        // Given: Educational analytics data
        val educationalData = mapOf(
            "lesson_topic" to "empathy_building",
            "behavioral_category" to "empathy",
            "engagement_level" to "high",
            "skill_development" to "peer_support"
        )
        
        // When: Validating educational purpose
        val isEducational = coppaComplianceManager.isEducationalPurpose(educationalData)
        
        // Then: Should be recognized as educational
        assertTrue(isEducational)
    }
    
    @Test
    fun `educational purpose validation rejects non-educational data`() {
        // Given: Non-educational data
        val nonEducationalData = mapOf(
            "marketing_data" to "product_preferences",
            "shopping_behavior" to "purchase_history",
            "advertising_id" to "abc123"
        )
        
        // When: Validating educational purpose
        val isEducational = coppaComplianceManager.isEducationalPurpose(nonEducationalData)
        
        // Then: Should not be recognized as educational
        assertFalse(isEducational)
    }
    
    // ================== Compliance Status Tests ==================
    
    @Test
    fun `compliance status reflects current settings accurately`() = testScope.runTest {
        // Given: COPPA compliant initialization
        coppaComplianceManager.initializeCOPPACompliantAnalytics(
            facilitatorConsent = true,
            educationalPurposeOnly = true,
            dataRetentionDays = 90
        )
        
        // When: Getting compliance status
        val status = coppaComplianceManager.getCOPPAComplianceStatus()
        
        // Then: Status should reflect compliant state
        assertTrue(status.isCompliant)
        assertEquals("FACILITATOR_CONSENT", status.consentLevel)
        assertEquals("FULL", status.anonymizationLevel)
        assertEquals(90, status.dataRetentionDays)
        assertEquals(100.0f, status.complianceScore)
    }
    
    @Test
    fun `compliance score decreases with violations`() = testScope.runTest {
        // Given: COPPA initialization with some violations
        coppaComplianceManager.initializeCOPPACompliantAnalytics(
            facilitatorConsent = true,
            educationalPurposeOnly = false, // Violation: not educational purpose only
            dataRetentionDays = 180 // Violation: excessive retention
        )
        
        // When: Getting compliance status
        val status = coppaComplianceManager.getCOPPAComplianceStatus()
        
        // Then: Compliance score should be reduced
        assertTrue(status.complianceScore < 100.0f)
        assertTrue(status.isCompliant) // Still compliant but with warnings
    }
    
    // ================== Data Validation with Real Analytics Entities ==================
    
    @Test
    fun `behavioral analytics entities pass COPPA validation`() {
        // Given: Generated behavioral analytics data
        val testEntities = MockDataGenerator.generateEmpathyScenario()
        
        // When: Validating each entity for COPPA compliance
        val validationResults = testEntities.map { entity ->
            COPPATestValidator.validateBehavioralAnalytics(entity)
        }
        
        // Then: All entities should be COPPA compliant
        validationResults.forEach { result ->
            assertTrue(result.isCompliant, "Entity failed COPPA validation: ${result.violations}")
        }
    }
    
    @Test
    fun `large analytics dataset maintains COPPA compliance`() {
        // Given: Large performance dataset
        val largeDataset = MockDataGenerator.generatePerformanceDataset(100)
        
        // When: Validating entire dataset
        val report = COPPATestValidator.generateComplianceReport(
            behavioralEntities = largeDataset,
            eventEntities = emptyList()
        )
        
        // Then: Dataset should be fully compliant
        assertTrue(report.isFullyCompliant)
        assertEquals(100.0, report.compliancePercentage)
        assertEquals(0, report.totalViolations)
    }
}