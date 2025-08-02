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
 * Espresso instrumentation tests for emotional check-in analytics integration
 * Tests UI interactions for emotional check-ins while ensuring COPPA compliance
 * and proper behavioral analytics tracking
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class EmotionalCheckinUITest {

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

    // ================== Emotional Check-in Analytics Tests ==================

    @Test
    fun testEmotionalCheckinStartAnalytics() {
        // Given: User is logged in as student
        navigateToStudentDashboard()
        
        // When: User starts emotional check-in
        composeTestRule.onNodeWithContentDescription("Emotional Check-in").performClick()
        composeTestRule.onNodeWithText("How are you feeling today?").assertIsDisplayed()
        
        // Then: Check-in start analytics should be tracked
        runTest {
            val analytics = testUtils.getStoredAnalytics()
            val checkinStartEvent = analytics.find { 
                it.interactionType == "emotional_checkin_start" 
            }
            
            assertNotNull(checkinStartEvent, "Emotional check-in start should be tracked")
            assertEquals("emotional_wellbeing", checkinStartEvent!!.behavioralCategory)
            assertTrue(checkinStartEvent.behavioralIndicators.containsKey("checkin_timestamp"))
            
            // Verify COPPA compliance
            val complianceResult = testUtils.validateSingleEventCOPPACompliance(checkinStartEvent)
            assertTrue(complianceResult.isCompliant, "Check-in analytics must be COPPA compliant")
        }
    }

    @Test
    fun testEmotionSelectionAnalytics() {
        // Given: User is in emotional check-in flow
        navigateToEmotionalCheckin()
        
        // When: User selects different emotions
        composeTestRule.onNodeWithContentDescription("Happy emotion").performClick()
        composeTestRule.onNodeWithContentDescription("Intensity level 4").performClick()
        composeTestRule.onNodeWithText("Next").performClick()
        
        // Then: Emotion selection analytics should be captured
        runTest {
            val analytics = testUtils.getStoredAnalytics()
            val emotionEvent = analytics.find { 
                it.interactionType == "emotion_selection" 
            }
            
            assertNotNull(emotionEvent, "Emotion selection should be tracked")
            assertEquals("emotional_wellbeing", emotionEvent!!.behavioralCategory)
            assertEquals("happy", emotionEvent.behavioralIndicators["selected_emotion"])
            assertEquals(4, emotionEvent.behavioralIndicators["emotion_intensity"])
            
            // Ensure emotional data is anonymous
            val sessionId = emotionEvent.sessionId
            assertTrue(sessionId.startsWith("anon_"), "Session ID should be anonymous")
            assertFalse(emotionEvent.behavioralIndicators.containsValue("student_name"))
        }
    }

    @Test
    fun testMultipleEmotionSelectionTracking() {
        // Given: User is in emotional check-in
        navigateToEmotionalCheckin()
        
        // When: User selects multiple emotions in sequence
        val emotions = listOf("happy", "excited", "calm", "worried", "sad")
        emotions.forEachIndexed { index, emotion ->
            composeTestRule.onNodeWithContentDescription("$emotion emotion").performClick()
            composeTestRule.onNodeWithContentDescription("Intensity level ${index + 1}").performClick()
            if (index < emotions.size - 1) {
                composeTestRule.onNodeWithText("Change emotion").performClick()
            }
        }
        
        // Then: All emotion changes should be tracked
        runTest {
            val analytics = testUtils.getStoredAnalytics()
            val emotionEvents = analytics.filter { 
                it.interactionType == "emotion_selection" 
            }
            
            assertTrue(emotionEvents.size >= emotions.size, 
                "All emotion selections should be tracked")
            
            // Verify emotion progression analytics
            val trackedEmotions = emotionEvents.map { 
                it.behavioralIndicators["selected_emotion"] as String 
            }
            emotions.forEach { emotion ->
                assertTrue(trackedEmotions.contains(emotion), 
                    "Emotion $emotion should be tracked")
            }
        }
    }

    @Test
    fun testEmotionalContextAnalytics() {
        // Given: User has selected an emotion
        navigateToEmotionalCheckin()
        composeTestRule.onNodeWithContentDescription("Worried emotion").performClick()
        composeTestRule.onNodeWithContentDescription("Intensity level 3").performClick()
        composeTestRule.onNodeWithText("Next").performClick()
        
        // When: User provides emotional context
        composeTestRule.onNodeWithText("What's making you feel this way?").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("School context").performClick()
        composeTestRule.onNodeWithContentDescription("Friendship context").performClick()
        composeTestRule.onNodeWithText("Continue").performClick()
        
        // Then: Emotional context should be tracked anonymously
        runTest {
            val analytics = testUtils.getStoredAnalytics()
            val contextEvent = analytics.find { 
                it.interactionType == "emotional_context" 
            }
            
            assertNotNull(contextEvent, "Emotional context should be tracked")
            assertEquals("emotional_wellbeing", contextEvent!!.behavioralCategory)
            
            val contexts = contextEvent.behavioralIndicators["selected_contexts"] as List<*>
            assertTrue(contexts.contains("school"), "School context should be tracked")
            assertTrue(contexts.contains("friendship"), "Friendship context should be tracked")
            
            // Verify no specific details are stored
            val behavioralIndicators = contextEvent.behavioralIndicators
            assertFalse(behavioralIndicators.containsKey("specific_incident"))
            assertFalse(behavioralIndicators.containsKey("person_names"))
        }
    }

    @Test
    fun testSupportRecommendationAnalytics() {
        // Given: User has completed emotion and context selection
        completeEmotionalCheckinFlow()
        
        // When: User receives and interacts with support recommendations
        composeTestRule.onNodeWithText("Recommended Activities").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Deep breathing exercise").performClick()
        composeTestRule.onNodeWithText("Start Activity").performClick()
        
        // Then: Support recommendation analytics should be tracked
        runTest {
            val analytics = testUtils.getStoredAnalytics()
            val recommendationEvent = analytics.find { 
                it.interactionType == "support_recommendation_selected" 
            }
            
            assertNotNull(recommendationEvent, "Support recommendation should be tracked")
            assertEquals("emotional_wellbeing", recommendationEvent!!.behavioralCategory)
            assertEquals("deep_breathing", recommendationEvent.behavioralIndicators["recommended_activity"])
            assertTrue(recommendationEvent.behavioralIndicators.containsKey("recommendation_timestamp"))
        }
    }

    @Test
    fun testEncouragementMessageAnalytics() {
        // Given: Completed emotional check-in flow
        completeEmotionalCheckinFlow()
        
        // When: User receives encouragement message
        composeTestRule.onNodeWithText("You're doing great!").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Encouragement message").assertIsDisplayed()
        composeTestRule.onNodeWithText("Thank you").performClick()
        
        // Then: Encouragement interaction should be tracked
        runTest {
            val analytics = testUtils.getStoredAnalytics()
            val encouragementEvent = analytics.find { 
                it.interactionType == "encouragement_received" 
            }
            
            assertNotNull(encouragementEvent, "Encouragement interaction should be tracked")
            assertEquals("emotional_wellbeing", encouragementEvent!!.behavioralCategory)
            assertTrue(encouragementEvent.behavioralIndicators.containsKey("encouragement_type"))
            assertEquals("positive_affirmation", encouragementEvent.behavioralIndicators["encouragement_type"])
        }
    }

    // ================== COPPA Compliance Tests ==================

    @Test
    fun testEmotionalCheckinCOPPACompliance() {
        // Given: Complete emotional check-in workflow
        navigateToEmotionalCheckin()
        
        // When: User completes full emotional check-in
        composeTestRule.onNodeWithContentDescription("Sad emotion").performClick()
        composeTestRule.onNodeWithContentDescription("Intensity level 2").performClick()
        composeTestRule.onNodeWithText("Next").performClick()
        composeTestRule.onNodeWithContentDescription("Family context").performClick()
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithText("Complete Check-in").performClick()
        
        // Then: All emotional data should be COPPA compliant
        runTest {
            val analytics = testUtils.getStoredAnalytics()
            
            analytics.forEach { event ->
                val complianceResult = testUtils.validateSingleEventCOPPACompliance(event)
                assertTrue(complianceResult.isCompliant, 
                    "Emotional check-in event ${event.interactionType} must be COPPA compliant")
                
                // Specifically verify no sensitive emotional details
                val eventString = event.toString()
                assertFalse(eventString.contains("personal_story", ignoreCase = true))
                assertFalse(eventString.contains("specific_incident", ignoreCase = true))
                assertFalse(eventString.contains("family_member", ignoreCase = true))
            }
        }
    }

    @Test
    fun testAnonymousEmotionalDataStorage() {
        // Given: Multiple students complete emotional check-ins
        navigateToEmotionalCheckin()
        
        // When: Emotional data is collected
        composeTestRule.onNodeWithContentDescription("Happy emotion").performClick()
        composeTestRule.onNodeWithContentDescription("Intensity level 5").performClick()
        composeTestRule.onNodeWithText("Next").performClick()
        composeTestRule.onNodeWithText("Complete Check-in").performClick()
        
        // Then: All emotional data should be anonymous
        runTest {
            val analytics = testUtils.getStoredAnalytics()
            val emotionalEvents = analytics.filter { 
                it.behavioralCategory == "emotional_wellbeing" 
            }
            
            emotionalEvents.forEach { event ->
                // Verify anonymous session tracking
                assertTrue(event.sessionId.startsWith("anon_"), 
                    "Session ID should be anonymous")
                
                // Verify no student identifiers
                assertFalse(event.behavioralIndicators.containsKey("student_name"))
                assertFalse(event.behavioralIndicators.containsKey("student_email"))
                assertFalse(event.behavioralIndicators.containsKey("student_id"))
                
                // Verify only categorical emotional data
                val emotionValue = event.behavioralIndicators["selected_emotion"]
                assertTrue(emotionValue in listOf("happy", "sad", "angry", "worried", "excited", "calm"))
            }
        }
    }

    // ================== Educational Analytics Tests ==================

    @Test
    fun testEmotionalWellbeingTrendTracking() {
        // Given: Multiple emotional check-ins over time
        navigateToEmotionalCheckin()
        
        // When: User completes check-ins with different emotions
        val emotionProgression = listOf(
            Pair("worried", 2),
            Pair("calm", 3),
            Pair("happy", 4)
        )
        
        emotionProgression.forEachIndexed { index, (emotion, intensity) ->
            composeTestRule.onNodeWithContentDescription("$emotion emotion").performClick()
            composeTestRule.onNodeWithContentDescription("Intensity level $intensity").performClick()
            composeTestRule.onNodeWithText("Complete Check-in").performClick()
            
            if (index < emotionProgression.size - 1) {
                // Navigate back to start new check-in
                composeTestRule.onNodeWithContentDescription("New Check-in").performClick()
            }
        }
        
        // Then: Emotional wellbeing trends should be trackable
        runTest {
            val analytics = testUtils.getStoredAnalytics()
            val emotionEvents = analytics.filter { 
                it.interactionType == "emotion_selection" 
            }.sortedBy { it.interactionTimestamp }
            
            assertEquals(3, emotionEvents.size, "Three emotion selections should be tracked")
            
            // Verify emotional progression tracking
            val intensityProgression = emotionEvents.map { 
                it.behavioralIndicators["emotion_intensity"] as Int 
            }
            assertEquals(listOf(2, 3, 4), intensityProgression, 
                "Emotional intensity progression should be tracked")
        }
    }

    @Test
    fun testEmotionalSupportEffectivenessTracking() {
        // Given: User selects worried emotion
        navigateToEmotionalCheckin()
        composeTestRule.onNodeWithContentDescription("Worried emotion").performClick()
        composeTestRule.onNodeWithContentDescription("Intensity level 3").performClick()
        composeTestRule.onNodeWithText("Next").performClick()
        composeTestRule.onNodeWithText("Continue").performClick()
        
        // When: User engages with recommended support activity
        composeTestRule.onNodeWithContentDescription("Calming activity").performClick()
        composeTestRule.onNodeWithText("Start Activity").performClick()
        
        // Simulate completing the activity
        Thread.sleep(2000) // Simulate activity time
        composeTestRule.onNodeWithText("Activity Complete").performClick()
        composeTestRule.onNodeWithContentDescription("Feel better rating 4").performClick()
        
        // Then: Support effectiveness should be tracked
        runTest {
            val analytics = testUtils.getStoredAnalytics()
            val effectivenessEvent = analytics.find { 
                it.interactionType == "support_effectiveness" 
            }
            
            assertNotNull(effectivenessEvent, "Support effectiveness should be tracked")
            assertEquals(3, effectivenessEvent!!.behavioralIndicators["initial_intensity"])
            assertEquals(4, effectivenessEvent.behavioralIndicators["post_activity_rating"])
            assertEquals("calming_activity", effectivenessEvent.behavioralIndicators["activity_type"])
        }
    }

    // ================== Performance Tests ==================

    @Test
    fun testEmotionalCheckinPerformanceImpact() {
        // Given: Emotional check-in flow
        val startTime = System.currentTimeMillis()
        
        // When: User completes emotional check-in quickly
        navigateToEmotionalCheckin()
        composeTestRule.onNodeWithContentDescription("Happy emotion").performClick()
        composeTestRule.onNodeWithContentDescription("Intensity level 4").performClick()
        composeTestRule.onNodeWithText("Next").performClick()
        composeTestRule.onNodeWithText("Complete Check-in").performClick()
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        
        // Then: Check-in should complete quickly with analytics
        assertTrue(totalTime < 5000, 
            "Emotional check-in should complete under 5 seconds with analytics")
        
        runTest {
            val analytics = testUtils.getStoredAnalytics()
            assertTrue(analytics.isNotEmpty(), "Analytics should be captured during check-in")
        }
    }

    // ================== Helper Methods ==================

    private fun navigateToStudentDashboard() {
        composeTestRule.onNodeWithText("Student").performClick()
        composeTestRule.onNodeWithText("Enter Classroom Code").performClick()
        composeTestRule.onNodeWithText("ABC123").performTextInput()
        composeTestRule.onNodeWithText("Join").performClick()
    }

    private fun navigateToEmotionalCheckin() {
        navigateToStudentDashboard()
        composeTestRule.onNodeWithContentDescription("Emotional Check-in").performClick()
    }

    private fun completeEmotionalCheckinFlow() {
        navigateToEmotionalCheckin()
        composeTestRule.onNodeWithContentDescription("Worried emotion").performClick()
        composeTestRule.onNodeWithContentDescription("Intensity level 3").performClick()
        composeTestRule.onNodeWithText("Next").performClick()
        composeTestRule.onNodeWithContentDescription("School context").performClick()
        composeTestRule.onNodeWithText("Continue").performClick()
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