package com.lifechurch.heroesinwaiting.analytics.ui

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
import javax.inject.Inject
import kotlin.test.assertTrue

/**
 * Espresso instrumentation tests for analytics functionality during lesson activities
 * Tests UI interactions that trigger analytics tracking while ensuring COPPA compliance
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LessonAnalyticsEspressoTest {

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
        
        // Setup COPPA compliant test environment
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

    // ================== Lesson Navigation Analytics Tests ==================

    @Test
    fun testLessonDetailViewTracking() {
        // Given: User navigates to facilitator dashboard
        composeTestRule.onNodeWithText("Facilitator Login").performClick()
        
        // When: User selects a lesson
        composeTestRule.onNodeWithContentDescription("Lesson Selection").performClick()
        composeTestRule.onNodeWithText("Empathy Building").performClick()
        
        // Then: Lesson view analytics should be tracked
        runTest {
            val analytics = testUtils.getStoredAnalytics()
            assertTrue(analytics.any { 
                it.interactionType == "lesson_view" && 
                it.behavioralCategory == "engagement" 
            }, "Lesson view should be tracked")
            
            // Verify COPPA compliance
            val complianceResult = testUtils.validateAnalyticsCOPPACompliance(analytics)
            assertTrue(complianceResult.isCompliant, "Analytics must be COPPA compliant")
        }
    }

    @Test
    fun testLessonStartAnalyticsTracking() {
        // Given: User is on lesson detail screen  
        navigateToLessonDetail("test-lesson-1")
        
        // When: User starts the lesson
        composeTestRule.onNodeWithContentDescription("Start Lesson").performClick()
        
        // Then: Lesson start analytics should be captured
        runTest {
            val analytics = testUtils.getStoredAnalytics()
            val lessonStartEvent = analytics.find { 
                it.interactionType == "lesson_start" 
            }
            
            assertNotNull(lessonStartEvent, "Lesson start event should be tracked")
            assertTrue(lessonStartEvent!!.behavioralIndicators.containsKey("start_timestamp"))
            assertTrue(lessonStartEvent.timeSpentSeconds == 0) // Initial start
            
            // Verify anonymous tracking
            assertTrue(lessonStartEvent.sessionId.isNotBlank())
            assertFalse(lessonStartEvent.sessionId.contains("@")) // No email patterns
        }
    }

    @Test
    fun testLessonTabInteractionTracking() {
        // Given: User is on lesson detail screen
        navigateToLessonDetail("test-lesson-1")
        
        // When: User switches between lesson tabs
        composeTestRule.onNodeWithText("Overview").performClick()
        composeTestRule.onNodeWithText("Activities").performClick()
        composeTestRule.onNodeWithText("Resources").performClick()
        
        // Then: Tab interaction analytics should be tracked
        runTest {
            val analytics = testUtils.getStoredAnalytics()
            val tabInteractions = analytics.filter { 
                it.interactionType == "tab_interaction" 
            }
            
            assertTrue(tabInteractions.size >= 3, "Multiple tab interactions should be tracked")
            
            // Verify tab names are captured (educational purpose)
            val tabNames = tabInteractions.map { 
                it.behavioralIndicators["tab_name"] as String 
            }
            assertTrue(tabNames.contains("Overview"))
            assertTrue(tabNames.contains("Activities"))
            assertTrue(tabNames.contains("Resources"))
        }
    }

    // ================== Behavioral Analytics UI Tests ==================

    @Test
    fun testEmpathyInteractionUITracking() {
        // Given: User is in an empathy-focused lesson
        navigateToLessonDetail("empathy-lesson-1")
        
        // Simulate empathy interaction through UI
        composeTestRule.onNodeWithText("Activities").performClick()
        composeTestRule.onNodeWithContentDescription("Empathy Scenario").performClick()
        
        // When: User selects empathy response
        composeTestRule.onNodeWithText("Offer comfort").performClick()
        composeTestRule.onNodeWithContentDescription("High empathy score").performClick()
        
        // Then: Empathy analytics should be captured
        runTest {
            val analytics = testUtils.getStoredAnalytics()
            val empathyEvent = analytics.find { 
                it.behavioralCategory == "empathy" 
            }
            
            assertNotNull(empathyEvent, "Empathy interaction should be tracked")
            assertEquals("empathy_interaction", empathyEvent!!.interactionType)
            assertTrue(empathyEvent.behavioralIndicators.containsKey("empathy_score"))
            assertEquals("comfort_offer", empathyEvent.behavioralIndicators["intervention_type"])
        }
    }

    @Test
    fun testConfidenceBuildingUITracking() {
        // Given: User is in a confidence-building lesson
        navigateToLessonDetail("confidence-lesson-1")
        
        // When: User participates in confidence activity
        composeTestRule.onNodeWithText("Activities").performClick()
        composeTestRule.onNodeWithContentDescription("Confidence Activity").performClick()
        composeTestRule.onNodeWithText("Share idea").performClick()
        
        // Then: Confidence analytics should be captured
        runTest {
            val analytics = testUtils.getStoredAnalytics()
            val confidenceEvent = analytics.find { 
                it.behavioralCategory == "confidence" 
            }
            
            assertNotNull(confidenceEvent, "Confidence interaction should be tracked")
            assertTrue(confidenceEvent!!.behavioralIndicators.containsKey("confidence_score"))
            assertTrue(confidenceEvent.behavioralIndicators.containsKey("participation_level"))
        }
    }

    @Test
    fun testCommunicationSkillUITracking() {
        // Given: User is in a communication-focused lesson
        navigateToLessonDetail("communication-lesson-1")
        
        // When: User engages in communication activity
        composeTestRule.onNodeWithText("Activities").performClick()
        composeTestRule.onNodeWithContentDescription("Communication Practice").performClick()
        composeTestRule.onNodeWithText("Active listening").performClick()
        
        // Then: Communication analytics should be captured
        runTest {
            val analytics = testUtils.getStoredAnalytics()
            val communicationEvent = analytics.find { 
                it.behavioralCategory == "communication" 
            }
            
            assertNotNull(communicationEvent, "Communication interaction should be tracked")
            assertEquals("excellent", communicationEvent!!.behavioralIndicators["listening_quality"])
            assertTrue(communicationEvent.behavioralIndicators.containsKey("communication_score"))
        }
    }

    @Test
    fun testLeadershipBehaviorUITracking() {
        // Given: User is in a leadership-focused lesson
        navigateToLessonDetail("leadership-lesson-1")
        
        // When: User demonstrates leadership behavior
        composeTestRule.onNodeWithText("Activities").performClick()
        composeTestRule.onNodeWithContentDescription("Leadership Challenge").performClick()
        composeTestRule.onNodeWithText("Take initiative").performClick()
        
        // Then: Leadership analytics should be captured
        runTest {
            val analytics = testUtils.getStoredAnalytics()
            val leadershipEvent = analytics.find { 
                it.behavioralCategory == "leadership" 
            }
            
            assertNotNull(leadershipEvent, "Leadership interaction should be tracked")
            assertEquals("high", leadershipEvent!!.behavioralIndicators["initiative_level"])
            assertTrue(leadershipEvent.behavioralIndicators.containsKey("leadership_score"))
        }
    }

    // ================== COPPA Compliance UI Tests ==================

    @Test
    fun testNoPIICollectionDuringUIInteractions() {
        // Given: Complete lesson workflow
        navigateToLessonDetail("test-lesson-1")
        
        // When: User performs various UI interactions
        composeTestRule.onNodeWithText("Overview").performClick()
        composeTestRule.onNodeWithText("Activities").performClick() 
        composeTestRule.onNodeWithContentDescription("Start Lesson").performClick()
        
        // Then: No PII should be collected in any analytics
        runTest {
            val analytics = testUtils.getStoredAnalytics()
            
            analytics.forEach { event ->
                val complianceResult = testUtils.validateSingleEventCOPPACompliance(event)
                assertTrue(complianceResult.isCompliant, 
                    "Event ${event.interactionType} must be COPPA compliant")
                
                // Specifically check for PII patterns
                val eventString = event.toString()
                assertFalse(eventString.contains("@"), "No email addresses should be stored")
                assertFalse(eventString.matches(Regex(".*\\d{3}-\\d{3}-\\d{4}.*")), 
                    "No phone numbers should be stored")
                assertFalse(eventString.contains("password", ignoreCase = true), 
                    "No password data should be stored")
            }
        }
    }

    @Test
    fun testAnonymousSessionTrackingConsistency() {
        // Given: User performs multiple UI interactions
        navigateToLessonDetail("test-lesson-1")
        
        // When: Multiple analytics events are generated
        composeTestRule.onNodeWithText("Overview").performClick()
        composeTestRule.onNodeWithText("Activities").performClick()
        composeTestRule.onNodeWithContentDescription("Start Lesson").performClick()
        
        // Then: All events should have the same anonymous session ID
        runTest {
            val analytics = testUtils.getStoredAnalytics()
            assertTrue(analytics.size >= 3, "Multiple events should be tracked")
            
            val sessionIds = analytics.map { it.sessionId }.distinct()
            assertEquals(1, sessionIds.size, "All events should have same anonymous session ID")
            
            val sessionId = sessionIds.first()
            assertTrue(sessionId.isNotBlank(), "Session ID should not be blank")
            assertTrue(sessionId.length >= 32, "Session ID should be sufficiently long")
        }
    }

    // ================== Performance Tests ==================

    @Test
    fun testAnalyticsPerformanceImpactOnUI() {
        // Given: Lesson detail screen is loaded
        val startTime = System.currentTimeMillis()
        
        // When: User performs rapid UI interactions
        navigateToLessonDetail("test-lesson-1")
        
        repeat(10) {
            composeTestRule.onNodeWithText("Overview").performClick()
            composeTestRule.onNodeWithText("Activities").performClick()
        }
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        
        // Then: UI should remain responsive (under 2 seconds for 20 interactions)
        assertTrue(totalTime < 2000, "UI should remain responsive with analytics tracking")
        
        runTest {
            val analytics = testUtils.getStoredAnalytics()
            assertTrue(analytics.size >= 20, "All interactions should be tracked")
        }
    }

    // ================== Helper Methods ==================

    private fun navigateToLessonDetail(lessonId: String) {
        // Navigate to facilitator dashboard
        composeTestRule.onNodeWithText("Facilitator Login").performClick()
        
        // Navigate to lesson selection
        composeTestRule.onNodeWithContentDescription("Lesson Selection").performClick()
        
        // Select specific lesson
        composeTestRule.onNodeWithContentDescription("Lesson $lessonId").performClick()
    }

    private fun assertNotNull(value: Any?, message: String) {
        assertTrue(value != null, message)
    }

    private fun assertEquals(expected: Any?, actual: Any?, message: String? = null) {
        val msg = message ?: "Expected $expected but was $actual"
        assertTrue(expected == actual, msg)
    }

    private fun assertFalse(condition: Boolean, message: String) {
        assertTrue(!condition, message)
    }
}