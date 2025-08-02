package com.lifechurch.heroesinwaiting.analytics.integration

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lifechurch.heroesinwaiting.MainActivity
import com.lifechurch.heroesinwaiting.analytics.utils.EspressoAnalyticsTestUtils
import com.lifechurch.heroesinwaiting.data.analytics.AnalyticsService
import com.lifechurch.heroesinwaiting.data.local.HeroesDatabase
import com.lifechurch.heroesinwaiting.data.privacy.COPPAComplianceManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprehensive instrumentation tests for COPPA compliance validation
 * Tests zero PII collection, data anonymization, and privacy protection across all analytics flows
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class COPPAComplianceInstrumentationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var analyticsService: AnalyticsService

    @Inject
    lateinit var coppaComplianceManager: COPPAComplianceManager

    @Inject
    lateinit var database: HeroesDatabase

    private lateinit var context: Context
    private lateinit var testUtils: EspressoAnalyticsTestUtils

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().targetContext
        testUtils = EspressoAnalyticsTestUtils(database, analyticsService, coppaComplianceManager)
        
        runTest {
            testUtils.setupCOPPACompliantEnvironment()
            testUtils.clearAnalyticsData()
        }
    }

    @After
    fun teardown() {
        runTest {
            testUtils.cleanupTestData()
        }
    }

    // ================== Zero PII Collection Tests ==================

    @Test
    fun testZeroPIICollectionInLessonAnalytics() {
        // Given: User engages with lesson activities
        navigateToLessonDetail("coppa-test-lesson")
        
        // When: User performs various lesson interactions
        composeTestRule.onNodeWithText("Overview").performClick()
        composeTestRule.onNodeWithText("Activities").performClick()
        composeTestRule.onNodeWithContentDescription("Start Lesson").performClick()
        composeTestRule.onNodeWithContentDescription("Empathy Scenario").performClick()
        composeTestRule.onNodeWithText("Offer comfort").performClick()
        
        // Then: No PII should be collected in any analytics
        runTest {
            val analytics = testUtils.getStoredAnalytics()
            assertTrue(analytics.isNotEmpty(), "Analytics should be collected")
            
            analytics.forEach { event ->
                val complianceResult = testUtils.validateSingleEventCOPPACompliance(event)
                assertTrue(complianceResult.isCompliant, 
                    "Event ${event.interactionType} violates COPPA: ${complianceResult.violations}")
                
                // Specific PII pattern checks
                val eventData = event.toString()
                
                // Email patterns
                assertFalse(eventData.contains(Regex("""[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}""")),
                    "No email addresses should be in analytics")
                
                // Phone number patterns
                assertFalse(eventData.contains(Regex("""\b\d{3}[-.]?\d{3}[-.]?\d{4}\b""")),
                    "No phone numbers should be in analytics")
                
                // Social Security Number patterns
                assertFalse(eventData.contains(Regex("""\b\d{3}-\d{2}-\d{4}\b""")),
                    "No SSN patterns should be in analytics")
                
                // Address patterns
                assertFalse(eventData.contains(Regex("""\b\d+\s+\w+\s+(street|st|avenue|ave|road|rd|drive|dr|lane|ln|boulevard|blvd)\b""", RegexOption.IGNORE_CASE)),
                    "No address patterns should be in analytics")
                
                // Credit card patterns
                assertFalse(eventData.contains(Regex("""\b\d{4}[-\s]?\d{4}[-\s]?\d{4}[-\s]?\d{4}\b""")),
                    "No credit card patterns should be in analytics")
            }
        }
    }

    @Test
    fun testZeroPIICollectionInEmotionalCheckins() {
        // Given: User completes emotional check-in with various emotions
        navigateToEmotionalCheckin()
        
        // When: User provides emotional data
        composeTestRule.onNodeWithContentDescription("Worried emotion").performClick()
        composeTestRule.onNodeWithContentDescription("Intensity level 3").performClick()
        composeTestRule.onNodeWithText("Next").performClick()
        composeTestRule.onNodeWithContentDescription("Family context").performClick()
        composeTestRule.onNodeWithContentDescription("School context").performClick()
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithText("Complete Check-in").performClick()
        
        // Then: Emotional data should be anonymous and PII-free
        runTest {
            val analytics = testUtils.getStoredAnalytics()
            val emotionalEvents = analytics.filter { it.behavioralCategory == "emotional_wellbeing" }
            
            assertTrue(emotionalEvents.isNotEmpty(), "Emotional events should be collected")
            
            emotionalEvents.forEach { event ->
                // Verify no personal identifiers
                assertFalse(event.behavioralIndicators.containsKey("student_name"))
                assertFalse(event.behavioralIndicators.containsKey("parent_name"))
                assertFalse(event.behavioralIndicators.containsKey("teacher_name"))
                assertFalse(event.behavioralIndicators.containsKey("school_name"))
                
                // Verify only categorical emotional data
                val emotion = event.behavioralIndicators["selected_emotion"] as? String
                assertTrue(emotion in listOf("happy", "sad", "angry", "worried", "excited", "calm", null),
                    "Only predefined emotion categories should be stored")
                
                // Verify intensity is numeric range (1-5)
                val intensity = event.behavioralIndicators["emotion_intensity"] as? Int
                assertTrue(intensity == null || intensity in 1..5,
                    "Emotion intensity should be in range 1-5")
                
                // Verify contexts are categorical
                val contexts = event.behavioralIndicators["selected_contexts"] as? List<*>
                contexts?.forEach { context ->
                    assertTrue(context in listOf("family", "school", "friends", "activities"),
                        "Only predefined contexts should be stored")
                }
            }
        }
    }

    @Test
    fun testPIIDetectionAndRemoval() {
        // Given: Attempt to inject PII-like data through UI (should be blocked)
        navigateToLessonDetail("pii-test-lesson")
        
        // When: System processes data that might contain PII patterns
        composeTestRule.onNodeWithText("Activities").performClick()
        
        // Simulate various user inputs that might contain PII
        // (In real implementation, this would test input sanitization)
        composeTestRule.onNodeWithContentDescription("Start Lesson").performClick()
        
        // Then: Any PII should be detected and removed
        runTest {
            val analytics = testUtils.getStoredAnalytics()
            
            analytics.forEach { event ->
                // Test comprehensive PII detection
                val piiViolations = mutableListOf<String>()
                
                val allDataString = "${event.behavioralIndicators} ${event.additionalMetadata}"
                
                // Check for various PII patterns
                if (allDataString.contains(Regex("""[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}"""))) {
                    piiViolations.add("Email address detected")
                }
                
                if (allDataString.contains(Regex("""\b\d{3}[-.]?\d{3}[-.]?\d{4}\b"""))) {
                    piiViolations.add("Phone number detected")
                }
                
                // Check for common names (simplified list)
                val commonNames = listOf("john", "jane", "mike", "sarah", "david", "mary", "michael", "jennifer")
                commonNames.forEach { name ->
                    if (allDataString.contains(name, ignoreCase = true)) {
                        piiViolations.add("Potential name detected: $name")
                    }
                }
                
                assertTrue(piiViolations.isEmpty(), 
                    "PII violations found: ${piiViolations.joinToString(", ")}")
            }
        }
    }

    // ================== Anonymous Identifier Tests ==================

    @Test
    fun testAnonymousIdentifierGeneration() {
        // Given: Multiple user sessions
        repeat(3) { sessionIndex ->
            // Simulate new user session
            navigateToStudentDashboard()
            composeTestRule.onNodeWithContentDescription("Emotional Check-in").performClick()
            composeTestRule.onNodeWithContentDescription("Happy emotion").performClick()
            composeTestRule.onNodeWithContentDescription("Intensity level ${sessionIndex + 1}").performClick()
            composeTestRule.onNodeWithText("Complete Check-in").performClick()
            
            // Navigate back to start new session
            composeTestRule.onNodeWithContentDescription("Back").performClick()
            composeTestRule.onNodeWithContentDescription("Leave Classroom").performClick()
        }
        
        // Then: Each session should have proper anonymous identifiers
        runTest {
            val analytics = testUtils.getStoredAnalytics()
            val sessionIds = analytics.map { it.sessionId }.distinct()
            
            // Verify anonymous identifier properties
            sessionIds.forEach { sessionId ->
                assertTrue(sessionId.length >= 16, "Session ID should be sufficiently long")
                assertTrue(sessionId.startsWith("anon_") || sessionId.matches(Regex("[a-f0-9-]+")),
                    "Session ID should follow anonymous pattern")
                assertFalse(sessionId.contains("@"), "Session ID should not contain email patterns")
                assertFalse(sessionId.contains("user"), "Session ID should not contain 'user'")
                assertFalse(sessionId.contains("student"), "Session ID should not contain 'student'")
            }
            
            // Verify session consistency within same session
            val eventsBySession = analytics.groupBy { it.sessionId }
            eventsBySession.forEach { (sessionId, events) ->
                assertTrue(events.all { it.sessionId == sessionId },
                    "All events in session should have same session ID")
            }
        }
    }

    @Test
    fun testHashedIdentifierConsistency() {
        // Given: Same user performs multiple interactions
        navigateToLessonDetail("consistency-test")
        
        // When: User performs multiple interactions in same session
        composeTestRule.onNodeWithText("Overview").performClick()
        composeTestRule.onNodeWithText("Activities").performClick()
        composeTestRule.onNodeWithContentDescription("Start Lesson").performClick()
        
        // Then: All events should have consistent hashed identifiers
        runTest {
            val analytics = testUtils.getStoredAnalytics()
            val sessionIds = analytics.map { it.sessionId }.distinct()
            
            assertEquals(1, sessionIds.size, "All events in session should have same session ID")
            
            val sessionId = sessionIds.first()
            assertTrue(sessionId.isNotBlank(), "Session ID should not be blank")
            
            // Verify hash properties (if using SHA-256, should be 64 characters)
            if (sessionId.matches(Regex("[a-f0-9]+"))) {
                assertTrue(sessionId.length == 64 || sessionId.length == 32,
                    "Hash should be standard length (32 or 64 characters)")
            }
        }
    }

    // ================== Data Retention and Cleanup Tests ==================

    @Test
    fun testDataRetentionPolicyEnforcement() {
        // Given: Analytics data older than retention period
        runTest {
            // Create old analytics data (simulate 91 days old)
            val oldAnalytics = testUtils.createTestBehavioralAnalytics(
                interactionType = "old_data_test",
                behavioralCategory = "engagement"
            ).copy(
                createdAt = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(91)
            )
            database.behavioralAnalyticsDao().insertBehavioralAnalytics(oldAnalytics)
            
            // Create recent analytics data
            val recentAnalytics = testUtils.createTestBehavioralAnalytics(
                interactionType = "recent_data_test",
                behavioralCategory = "engagement"
            )
            database.behavioralAnalyticsDao().insertBehavioralAnalytics(recentAnalytics)
        }
        
        // When: User performs new interactions (which might trigger cleanup)
        navigateToLessonDetail("retention-test")
        composeTestRule.onNodeWithText("Overview").performClick()
        
        // Simulate data retention cleanup (this would normally be handled by background workers)
        runTest {
            // In real implementation, this would trigger automatic cleanup
            val allAnalytics = testUtils.getStoredAnalytics()
            val currentTime = System.currentTimeMillis()
            val retentionPeriod = TimeUnit.DAYS.toMillis(90)
            
            allAnalytics.forEach { event ->
                val age = currentTime - event.createdAt
                if (age > retentionPeriod) {
                    // In production, old data should be automatically cleaned up
                    assertTrue(false, "Data older than retention period should be cleaned up")
                }
            }
        }
    }

    @Test
    fun testConsentWithdrawalDataPurging() {
        // Given: User has provided analytics data
        navigateToLessonDetail("consent-test")
        composeTestRule.onNodeWithText("Overview").performClick()
        composeTestRule.onNodeWithText("Activities").performClick()
        
        runTest {
            val initialAnalytics = testUtils.getStoredAnalytics()
            assertTrue(initialAnalytics.isNotEmpty(), "Initial analytics should exist")
        }
        
        // When: Consent is withdrawn (simulate facilitator action)
        // In real implementation, this would be triggered by facilitator dashboard
        navigateToFacilitatorDashboard()
        composeTestRule.onNodeWithText("Privacy Settings").performClick()
        composeTestRule.onNodeWithText("Withdraw Consent").performClick()
        composeTestRule.onNodeWithText("Confirm Withdrawal").performClick()
        
        // Then: All related analytics data should be purged
        runTest {
            // In production implementation, consent withdrawal should purge data
            val remainingAnalytics = testUtils.getStoredAnalytics()
            
            // Verify data purging occurred
            assertTrue(remainingAnalytics.isEmpty() || 
                remainingAnalytics.none { it.classroomId == testUtils.TEST_CLASSROOM_ID },
                "Data should be purged after consent withdrawal")
        }
    }

    // ================== Educational Purpose Restriction Tests ==================

    @Test
    fun testEducationalPurposeDataRestriction() {
        // Given: User engages in educational activities
        navigateToLessonDetail("educational-purpose-test")
        composeTestRule.onNodeWithText("Activities").performClick()
        composeTestRule.onNodeWithContentDescription("Empathy Scenario").performClick()
        composeTestRule.onNodeWithText("Offer comfort").performClick()
        
        // Then: Only educational purpose data should be collected
        runTest {
            val analytics = testUtils.getStoredAnalytics()
            
            analytics.forEach { event ->
                // Verify educational context
                assertTrue(event.behavioralCategory in listOf(
                    "empathy", "confidence", "communication", "leadership", 
                    "engagement", "emotional_wellbeing"
                ), "Only educational behavioral categories should be tracked")
                
                // Verify no commercial or marketing data
                val allData = "${event.behavioralIndicators} ${event.additionalMetadata}"
                assertFalse(allData.contains("purchase", ignoreCase = true))
                assertFalse(allData.contains("advertisement", ignoreCase = true))
                assertFalse(allData.contains("marketing", ignoreCase = true))
                assertFalse(allData.contains("commercial", ignoreCase = true))
                
                // Verify educational indicators are present
                val hasEducationalIndicators = event.behavioralIndicators.keys.any { key ->
                    key.contains("score") || key.contains("level") || key.contains("quality") || 
                    key.contains("response") || key.contains("skill")
                }
                assertTrue(hasEducationalIndicators, 
                    "Events should have educational behavioral indicators")
            }
        }
    }

    @Test
    fun testThirdPartyDataSharingPrevention() {
        // Given: Analytics data has been collected
        navigateToLessonDetail("sharing-test")
        composeTestRule.onNodeWithText("Overview").performClick()
        composeTestRule.onNodeWithText("Activities").performClick()
        
        // Then: Data should not contain any third-party identifiers
        runTest {
            val analytics = testUtils.getStoredAnalytics()
            
            analytics.forEach { event ->
                // Verify no third-party tracking identifiers
                val allData = "${event.behavioralIndicators} ${event.additionalMetadata}"
                
                assertFalse(allData.contains("google", ignoreCase = true))
                assertFalse(allData.contains("facebook", ignoreCase = true))
                assertFalse(allData.contains("tracking_id", ignoreCase = true))
                assertFalse(allData.contains("advertising_id", ignoreCase = true))
                assertFalse(allData.contains("uuid", ignoreCase = true))
                assertFalse(allData.contains("ga_", ignoreCase = true)) // Google Analytics
                
                // Verify no external service references
                assertFalse(allData.contains("api_key", ignoreCase = true))
                assertFalse(allData.contains("external_id", ignoreCase = true))
                assertFalse(allData.contains("partner_id", ignoreCase = true))
            }
        }
    }

    // ================== Compliance Monitoring Tests ==================

    @Test
    fun testContinuousCOPPAComplianceMonitoring() {
        // Given: Extended user session with multiple activities
        repeat(5) { activityIndex ->
            navigateToLessonDetail("monitoring-test-$activityIndex")
            composeTestRule.onNodeWithText("Overview").performClick()
            composeTestRule.onNodeWithText("Activities").performClick()
            
            // Navigate back for next activity
            if (activityIndex < 4) {
                composeTestRule.onNodeWithContentDescription("Back").performClick()
                composeTestRule.onNodeWithContentDescription("Back").performClick()
            }
        }
        
        // Then: COPPA compliance should be maintained throughout session
        runTest {
            val analytics = testUtils.getStoredAnalytics()
            assertTrue(analytics.size >= 10, "Multiple activities should generate analytics")
            
            // Continuous compliance check
            var cumulativeComplianceScore = 0.0f
            var eventCount = 0
            
            analytics.forEach { event ->
                val complianceResult = testUtils.validateSingleEventCOPPACompliance(event)
                assertTrue(complianceResult.isCompliant, 
                    "Event ${event.interactionType} at ${event.interactionTimestamp} violates COPPA")
                
                cumulativeComplianceScore += complianceResult.complianceScore
                eventCount++
            }
            
            val averageComplianceScore = cumulativeComplianceScore / eventCount
            assertTrue(averageComplianceScore >= 95.0f, 
                "Average COPPA compliance score should be >= 95%")
        }
    }

    // ================== Helper Methods ==================

    private fun navigateToStudentDashboard() {
        composeTestRule.onNodeWithText("Student").performClick()
        composeTestRule.onNodeWithText("Enter Classroom Code").performTextInput("ABC123")
        composeTestRule.onNodeWithText("Join").performClick()
    }

    private fun navigateToLessonDetail(lessonId: String) {
        composeTestRule.onNodeWithText("Facilitator Login").performClick()
        composeTestRule.onNodeWithContentDescription("Lesson Selection").performClick()
        composeTestRule.onNodeWithContentDescription("Lesson $lessonId").performClick()
    }

    private fun navigateToEmotionalCheckin() {
        navigateToStudentDashboard()
        composeTestRule.onNodeWithContentDescription("Emotional Check-in").performClick()
    }

    private fun navigateToFacilitatorDashboard() {
        composeTestRule.onNodeWithText("Facilitator Login").performClick()
        composeTestRule.onNodeWithContentDescription("Dashboard").performClick()
    }

    private fun assertFalse(condition: Boolean, message: String = "Condition should be false") {
        assertTrue(!condition, message)
    }
}