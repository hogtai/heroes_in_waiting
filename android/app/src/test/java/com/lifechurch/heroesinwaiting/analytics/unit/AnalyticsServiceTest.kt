package com.lifechurch.heroesinwaiting.analytics.unit

import android.content.Context
import android.content.SharedPreferences
import com.lifechurch.heroesinwaiting.analytics.utils.AnalyticsTestUtils
import com.lifechurch.heroesinwaiting.analytics.utils.COPPATestValidator
import com.lifechurch.heroesinwaiting.analytics.utils.MockDataGenerator
import com.lifechurch.heroesinwaiting.data.analytics.AnalyticsService
import com.lifechurch.heroesinwaiting.data.local.entities.BehavioralAnalyticsEntity
import com.lifechurch.heroesinwaiting.data.privacy.COPPAComplianceManager
import com.lifechurch.heroesinwaiting.data.privacy.COPPAComplianceStatus
import com.lifechurch.heroesinwaiting.data.privacy.COPPAInitializationResult
import com.lifechurch.heroesinwaiting.data.repository.AnalyticsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive unit tests for AnalyticsService
 * Validates COPPA compliance, behavioral tracking, and offline-first functionality
 */
class AnalyticsServiceTest {
    
    @Mock
    private lateinit var context: Context
    
    @Mock
    private lateinit var analyticsRepository: AnalyticsRepository
    
    @Mock
    private lateinit var sharedPreferences: SharedPreferences
    
    @Mock
    private lateinit var sharedPreferencesEditor: SharedPreferences.Editor
    
    @Mock
    private lateinit var coppaComplianceManager: COPPAComplianceManager
    
    private lateinit var analyticsService: AnalyticsService
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Mock SharedPreferences behavior
        whenever(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor)
        whenever(sharedPreferencesEditor.putString(any(), any())).thenReturn(sharedPreferencesEditor)
        whenever(sharedPreferencesEditor.apply()).then { }
        
        // Mock COPPA compliance manager default state
        whenever(coppaComplianceManager.anonymousStudentId).thenReturn(
            MutableStateFlow(AnalyticsTestUtils.generateAnonymousSessionId())
        )
        
        // Initialize the service
        analyticsService = AnalyticsService(
            context = context,
            analyticsRepository = analyticsRepository,
            sharedPreferences = sharedPreferences,
            coppaComplianceManager = coppaComplianceManager
        )
    }
    
    @After
    fun teardown() {
        AnalyticsTestUtils.cleanupTestData()
    }
    
    // ================== COPPA Compliance Tests ==================
    
    @Test
    fun `analytics service respects COPPA compliance settings`() = testScope.runTest {
        // Given: COPPA compliance is disabled
        val nonCompliantStatus = COPPAComplianceStatus(
            isCompliant = false,
            consentLevel = "NO_CONSENT",
            anonymizationLevel = "NONE",
            dataRetentionDays = 90,
            complianceScore = 0.0f
        )
        
        whenever(coppaComplianceManager.getCOPPAComplianceStatus())
            .thenReturn(nonCompliantStatus)
        
        // When: Attempting to track analytics
        analyticsService.trackLessonStart(
            classroomId = AnalyticsTestUtils.TEST_CLASSROOM_ID,
            lessonId = AnalyticsTestUtils.TEST_LESSON_ID,
            lessonTitle = "Test Lesson",
            gradeLevel = "3rd"
        )
        
        // Then: No analytics should be stored due to non-compliance
        verify(analyticsRepository, never()).insertBehavioralAnalytics(any())
    }
    
    @Test
    fun `analytics service processes data when COPPA compliant`() = testScope.runTest {
        // Given: COPPA compliance is enabled
        val compliantStatus = COPPAComplianceStatus(
            isCompliant = true,
            consentLevel = "FACILITATOR_CONSENT",
            anonymizationLevel = "FULL",
            dataRetentionDays = 90,
            complianceScore = 100.0f
        )
        
        whenever(coppaComplianceManager.getCOPPAComplianceStatus())
            .thenReturn(compliantStatus)
        
        whenever(coppaComplianceManager.anonymizeData(any()))
            .thenAnswer { invocation -> invocation.arguments[0] as Map<String, Any> }
        
        // When: Tracking analytics with compliance
        analyticsService.trackLessonStart(
            classroomId = AnalyticsTestUtils.TEST_CLASSROOM_ID,
            lessonId = AnalyticsTestUtils.TEST_LESSON_ID,
            lessonTitle = "Test Lesson",
            gradeLevel = "3rd"
        )
        
        // Then: Analytics should be stored
        verify(analyticsRepository).insertBehavioralAnalytics(any())
    }
    
    @Test
    fun `anonymous session IDs are properly generated`() = testScope.runTest {
        // Given: Clean session state
        whenever(sharedPreferences.getString(any(), any())).thenReturn(null)
        whenever(coppaComplianceManager.anonymousStudentId).thenReturn(MutableStateFlow(null))
        
        val compliantStatus = COPPAComplianceStatus(
            isCompliant = true,
            consentLevel = "FACILITATOR_CONSENT",
            anonymizationLevel = "FULL",
            dataRetentionDays = 90,
            complianceScore = 100.0f
        )
        
        whenever(coppaComplianceManager.getCOPPAComplianceStatus()).thenReturn(compliantStatus)
        whenever(coppaComplianceManager.anonymizeData(any())).thenAnswer { invocation -> 
            invocation.arguments[0] as Map<String, Any> 
        }
        
        // Capture the analytics entity for validation
        val analyticsCaptor = argumentCaptor<BehavioralAnalyticsEntity>()
        
        // When: Tracking analytics
        analyticsService.trackLessonStart(
            classroomId = AnalyticsTestUtils.TEST_CLASSROOM_ID,
            lessonId = AnalyticsTestUtils.TEST_LESSON_ID,
            lessonTitle = "Test Lesson",
            gradeLevel = "3rd"
        )
        
        // Then: Verify anonymous session ID is generated and stored
        verify(sharedPreferencesEditor).putString(eq("anonymous_session_key"), any())
        verify(analyticsRepository).insertBehavioralAnalytics(analyticsCaptor.capture())
        
        val capturedEntity = analyticsCaptor.firstValue
        assertNotNull(capturedEntity.sessionId)
        assertTrue(capturedEntity.sessionId.isNotBlank())
        
        // Validate COPPA compliance of generated entity
        val validationResult = COPPATestValidator.validateBehavioralAnalytics(capturedEntity)
        assertTrue(validationResult.isCompliant, "Generated analytics must be COPPA compliant")
    }
    
    // ================== Behavioral Analytics Tracking Tests ==================
    
    @Test
    fun `empathy tracking captures appropriate behavioral indicators`() = testScope.runTest {
        // Given: COPPA compliant environment
        setupCompliantEnvironment()
        val analyticsCaptor = argumentCaptor<BehavioralAnalyticsEntity>()
        
        // When: Tracking empathy interaction
        analyticsService.trackEmpathyInteraction(
            classroomId = AnalyticsTestUtils.TEST_CLASSROOM_ID,
            lessonId = AnalyticsTestUtils.TEST_LESSON_ID,
            empathyScore = 4,
            responseQuality = "high",
            emotionalContext = "peer_distress",
            interventionType = "comfort_offer"
        )
        
        // Then: Verify empathy-specific data is captured
        verify(analyticsRepository).insertBehavioralAnalytics(analyticsCaptor.capture())
        
        val capturedEntity = analyticsCaptor.firstValue
        assertEquals("empathy", capturedEntity.behavioralCategory)
        assertEquals("empathy_interaction", capturedEntity.interactionType)
        
        val indicators = capturedEntity.behavioralIndicators
        assertEquals(4, indicators["empathy_score"])
        assertEquals("high", indicators["response_quality"])
        assertEquals("peer_distress", indicators["emotional_context"])
        assertEquals("comfort_offer", indicators["intervention_type"])
        
        // Validate COPPA compliance
        val validationResult = COPPATestValidator.validateBehavioralAnalytics(capturedEntity)
        assertTrue(validationResult.isCompliant)
    }
    
    @Test
    fun `confidence tracking measures confidence building effectively`() = testScope.runTest {
        // Given: COPPA compliant environment
        setupCompliantEnvironment()
        val analyticsCaptor = argumentCaptor<BehavioralAnalyticsEntity>()
        
        // When: Tracking confidence interaction
        analyticsService.trackConfidenceBuilding(
            classroomId = AnalyticsTestUtils.TEST_CLASSROOM_ID,
            lessonId = AnalyticsTestUtils.TEST_LESSON_ID,
            confidenceScore = 3,
            participationLevel = "moderate",
            voiceVolume = "appropriate",
            eyeContact = true
        )
        
        // Then: Verify confidence-specific metrics
        verify(analyticsRepository).insertBehavioralAnalytics(analyticsCaptor.capture())
        
        val capturedEntity = analyticsCaptor.firstValue
        assertEquals("confidence", capturedEntity.behavioralCategory)
        
        val indicators = capturedEntity.behavioralIndicators
        assertEquals(3, indicators["confidence_score"])
        assertEquals("moderate", indicators["participation_level"])
        assertEquals("appropriate", indicators["voice_volume"])
        assertEquals(true, indicators["eye_contact"])
    }
    
    @Test
    fun `communication tracking captures interaction quality`() = testScope.runTest {
        // Given: COPPA compliant environment
        setupCompliantEnvironment()
        val analyticsCaptor = argumentCaptor<BehavioralAnalyticsEntity>()
        
        // When: Tracking communication interaction
        analyticsService.trackCommunicationSkill(
            classroomId = AnalyticsTestUtils.TEST_CLASSROOM_ID,
            lessonId = AnalyticsTestUtils.TEST_LESSON_ID,
            communicationScore = 4,
            listeningQuality = "excellent",
            responseRelevance = "high",
            turnTaking = true
        )
        
        // Then: Verify communication-specific data
        verify(analyticsRepository).insertBehavioralAnalytics(analyticsCaptor.capture())
        
        val capturedEntity = analyticsCaptor.firstValue
        assertEquals("communication", capturedEntity.behavioralCategory)
        
        val indicators = capturedEntity.behavioralIndicators
        assertEquals(4, indicators["communication_score"])
        assertEquals("excellent", indicators["listening_quality"])
        assertEquals("high", indicators["response_relevance"])
        assertEquals(true, indicators["turn_taking"])
    }
    
    @Test
    fun `leadership tracking identifies leadership behaviors`() = testScope.runTest {
        // Given: COPPA compliant environment
        setupCompliantEnvironment()
        val analyticsCaptor = argumentCaptor<BehavioralAnalyticsEntity>()
        
        // When: Tracking leadership behavior
        analyticsService.trackLeadershipBehavior(
            classroomId = AnalyticsTestUtils.TEST_CLASSROOM_ID,
            lessonId = AnalyticsTestUtils.TEST_LESSON_ID,
            leadershipScore = 5,
            initiativeLevel = "high",
            peerResponse = "positive",
            taskCompletion = "successful"
        )
        
        // Then: Verify leadership-specific metrics
        verify(analyticsRepository).insertBehavioralAnalytics(analyticsCaptor.capture())
        
        val capturedEntity = analyticsCaptor.firstValue
        assertEquals("leadership", capturedEntity.behavioralCategory)
        
        val indicators = capturedEntity.behavioralIndicators
        assertEquals(5, indicators["leadership_score"])
        assertEquals("high", indicators["initiative_level"])
        assertEquals("positive", indicators["peer_response"])
        assertEquals("successful", indicators["task_completion"])
    }
    
    // ================== Offline-First Functionality Tests ==================
    
    @Test
    fun `analytics are stored locally when offline`() = testScope.runTest {
        // Given: COPPA compliant environment with offline mode
        setupCompliantEnvironment()
        val analyticsCaptor = argumentCaptor<BehavioralAnalyticsEntity>()
        
        // Simulate offline mode
        whenever(analyticsRepository.isOnline()).thenReturn(false)
        
        // When: Tracking analytics while offline
        analyticsService.trackLessonStart(
            classroomId = AnalyticsTestUtils.TEST_CLASSROOM_ID,
            lessonId = AnalyticsTestUtils.TEST_LESSON_ID,
            lessonTitle = "Offline Lesson",
            gradeLevel = "3rd"
        )
        
        // Then: Analytics should be stored locally with offline flag
        verify(analyticsRepository).insertBehavioralAnalytics(analyticsCaptor.capture())
        
        val capturedEntity = analyticsCaptor.firstValue
        assertTrue(capturedEntity.isOfflineRecorded)
        assertTrue(capturedEntity.needsSync)
        assertEquals(0, capturedEntity.syncAttempts)
    }
    
    @Test
    fun `analytics sync is triggered when coming online`() = testScope.runTest {
        // Given: COPPA compliant environment
        setupCompliantEnvironment()
        
        // When: Network connectivity is restored
        analyticsService.onNetworkAvailable()
        
        // Then: Sync should be triggered
        verify(analyticsRepository).syncPendingAnalytics()
    }
    
    // ================== Data Validation Tests ==================
    
    @Test
    fun `analytics data validation prevents invalid data storage`() = testScope.runTest {
        // Given: COPPA compliant environment
        setupCompliantEnvironment()
        
        // When: Attempting to track with invalid data (empty classroom ID)
        analyticsService.trackLessonStart(
            classroomId = "", // Invalid empty classroom ID
            lessonId = AnalyticsTestUtils.TEST_LESSON_ID,
            lessonTitle = "Test Lesson",
            gradeLevel = "3rd"
        )
        
        // Then: No analytics should be stored due to validation failure
        verify(analyticsRepository, never()).insertBehavioralAnalytics(any())
    }
    
    @Test
    fun `analytics timestamps are accurate and reasonable`() = testScope.runTest {
        // Given: COPPA compliant environment
        setupCompliantEnvironment()
        val analyticsCaptor = argumentCaptor<BehavioralAnalyticsEntity>()
        val startTime = System.currentTimeMillis()
        
        // When: Tracking analytics
        analyticsService.trackLessonStart(
            classroomId = AnalyticsTestUtils.TEST_CLASSROOM_ID,
            lessonId = AnalyticsTestUtils.TEST_LESSON_ID,
            lessonTitle = "Test Lesson",
            gradeLevel = "3rd"
        )
        
        val endTime = System.currentTimeMillis()
        
        // Then: Timestamps should be within reasonable range
        verify(analyticsRepository).insertBehavioralAnalytics(analyticsCaptor.capture())
        
        val capturedEntity = analyticsCaptor.firstValue
        assertTrue(capturedEntity.interactionTimestamp >= startTime)
        assertTrue(capturedEntity.interactionTimestamp <= endTime)
        assertTrue(capturedEntity.createdAt >= startTime)
        assertTrue(capturedEntity.createdAt <= endTime)
    }
    
    // ================== Performance Tests ==================
    
    @Test
    fun `analytics tracking has minimal performance impact`() = testScope.runTest {
        // Given: COPPA compliant environment
        setupCompliantEnvironment()
        val startTime = System.currentTimeMillis()
        
        // When: Tracking multiple analytics events
        repeat(10) { index ->
            analyticsService.trackLessonStart(
                classroomId = AnalyticsTestUtils.TEST_CLASSROOM_ID,
                lessonId = AnalyticsTestUtils.TEST_LESSON_ID,
                lessonTitle = "Performance Test $index",
                gradeLevel = "3rd"
            )
        }
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        
        // Then: All tracking should complete quickly (under 100ms)
        assertTrue(totalTime < 100, "Analytics tracking took too long: ${totalTime}ms")
        verify(analyticsRepository, times(10)).insertBehavioralAnalytics(any())
    }
    
    // ================== Error Handling Tests ==================
    
    @Test
    fun `analytics service handles repository errors gracefully`() = testScope.runTest {
        // Given: COPPA compliant environment with repository error
        setupCompliantEnvironment()
        whenever(analyticsRepository.insertBehavioralAnalytics(any()))
            .thenThrow(RuntimeException("Database error"))
        
        // When: Tracking analytics with repository error
        var exceptionThrown = false
        try {
            analyticsService.trackLessonStart(
                classroomId = AnalyticsTestUtils.TEST_CLASSROOM_ID,
                lessonId = AnalyticsTestUtils.TEST_LESSON_ID,
                lessonTitle = "Error Test",
                gradeLevel = "3rd"
            )
        } catch (e: Exception) {
            exceptionThrown = true
        }
        
        // Then: Service should handle error gracefully without crashing
        assertFalse(exceptionThrown, "Analytics service should handle errors gracefully")
    }
    
    @Test
    fun `analytics service validates behavioral indicator ranges`() = testScope.runTest {
        // Given: COPPA compliant environment
        setupCompliantEnvironment()
        
        // When: Tracking with out-of-range scores
        analyticsService.trackEmpathyInteraction(
            classroomId = AnalyticsTestUtils.TEST_CLASSROOM_ID,
            lessonId = AnalyticsTestUtils.TEST_LESSON_ID,
            empathyScore = 10, // Invalid: should be 1-5
            responseQuality = "high",
            emotionalContext = "peer_distress",
            interventionType = "comfort_offer"
        )
        
        // Then: Invalid data should be normalized or rejected
        verify(analyticsRepository, never()).insertBehavioralAnalytics(
            argThat { entity ->
                val score = entity.behavioralIndicators["empathy_score"] as? Int
                score != null && score > 5
            }
        )
    }
    
    // ================== Helper Methods ==================
    
    private fun setupCompliantEnvironment() {
        val compliantStatus = COPPAComplianceStatus(
            isCompliant = true,
            consentLevel = "FACILITATOR_CONSENT",
            anonymizationLevel = "FULL",
            dataRetentionDays = 90,
            complianceScore = 100.0f
        )
        
        whenever(coppaComplianceManager.getCOPPAComplianceStatus()).thenReturn(compliantStatus)
        whenever(coppaComplianceManager.anonymizeData(any())).thenAnswer { invocation ->
            invocation.arguments[0] as Map<String, Any>
        }
        whenever(analyticsRepository.isOnline()).thenReturn(true)
    }
}