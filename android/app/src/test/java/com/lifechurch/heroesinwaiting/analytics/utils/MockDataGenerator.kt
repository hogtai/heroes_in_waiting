package com.lifechurch.heroesinwaiting.analytics.utils

import com.lifechurch.heroesinwaiting.data.local.entities.BehavioralAnalyticsEntity
import com.lifechurch.heroesinwaiting.data.local.entities.AnalyticsEventEntity
import com.lifechurch.heroesinwaiting.data.local.entities.AnalyticsSyncBatchEntity
import java.util.UUID
import kotlin.random.Random

/**
 * Mock data generator for comprehensive analytics testing
 * Generates realistic, COPPA-compliant test data for all analytics scenarios
 */
object MockDataGenerator {
    
    // Predefined test data sets for consistent testing
    private val behavioralCategories = listOf("empathy", "confidence", "communication", "leadership")
    private val interactionTypes = listOf(
        "lesson_start", "lesson_complete", "activity_start", "activity_complete",
        "peer_interaction", "voice_sharing", "emotional_response", "problem_solving",
        "group_discussion", "individual_reflection", "peer_support", "conflict_resolution"
    )
    
    private val empathyInteractions = listOf(
        "peer_support_offer", "emotional_recognition", "comfort_provision", 
        "active_listening", "empathy_demonstration", "inclusive_behavior"
    )
    
    private val confidenceInteractions = listOf(
        "voice_sharing", "idea_presentation", "leadership_initiative", 
        "public_speaking", "risk_taking", "self_advocacy"
    )
    
    private val communicationInteractions = listOf(
        "active_listening", "clear_expression", "respectful_disagreement",
        "collaborative_discussion", "question_asking", "idea_building"
    )
    
    private val leadershipInteractions = listOf(
        "initiative_taking", "team_organizing", "conflict_mediation",
        "decision_making", "responsibility_acceptance", "peer_guidance"
    )
    
    /**
     * Generate realistic empathy tracking scenario
     */
    fun generateEmpathyScenario(): List<BehavioralAnalyticsEntity> {
        val sessionId = AnalyticsTestUtils.generateAnonymousSessionId()
        val baseTimestamp = System.currentTimeMillis() - Random.nextLong(0, 7200000) // Last 2 hours
        
        return listOf(
            // Initial empathy lesson start
            createBehavioralEntity(
                sessionId = sessionId,
                behavioralCategory = "empathy",
                interactionType = "lesson_start",
                timestamp = baseTimestamp,
                behavioralIndicators = mapOf(
                    "lesson_topic" to "understanding_feelings",
                    "engagement_level" to "high",
                    "emotional_readiness" to "prepared"
                )
            ),
            
            // Peer support scenario
            createBehavioralEntity(
                sessionId = sessionId,
                behavioralCategory = "empathy",
                interactionType = "peer_support_offer",
                timestamp = baseTimestamp + 300000, // 5 minutes later
                timeSpentSeconds = 120,
                behavioralIndicators = mapOf(
                    "empathy_score" to 4,
                    "response_quality" to "high",
                    "emotional_context" to "peer_sadness",
                    "intervention_type" to "comfort_words",
                    "peer_response" to "positive"
                )
            ),
            
            // Emotional recognition activity
            createBehavioralEntity(
                sessionId = sessionId,
                behavioralCategory = "empathy",
                interactionType = "emotional_recognition",
                timestamp = baseTimestamp + 600000, // 10 minutes later
                timeSpentSeconds = 180,
                behavioralIndicators = mapOf(
                    "empathy_score" to 5,
                    "recognition_accuracy" to "excellent",
                    "emotional_vocabulary" to "advanced",
                    "facial_expression_reading" to "accurate"
                )
            )
        )
    }
    
    /**
     * Generate realistic confidence building scenario
     */
    fun generateConfidenceScenario(): List<BehavioralAnalyticsEntity> {
        val sessionId = AnalyticsTestUtils.generateAnonymousSessionId()
        val baseTimestamp = System.currentTimeMillis() - Random.nextLong(0, 3600000) // Last hour
        
        return listOf(
            // Voice sharing activity
            createBehavioralEntity(
                sessionId = sessionId,
                behavioralCategory = "confidence",
                interactionType = "voice_sharing",
                timestamp = baseTimestamp,
                timeSpentSeconds = 90,
                behavioralIndicators = mapOf(
                    "confidence_score" to 3,
                    "voice_volume" to "appropriate",
                    "eye_contact" to "good",
                    "speaking_clarity" to "clear",
                    "audience_engagement" to "attentive"
                )
            ),
            
            // Leadership initiative
            createBehavioralEntity(
                sessionId = sessionId,
                behavioralCategory = "confidence",
                interactionType = "leadership_initiative",
                timestamp = baseTimestamp + 450000, // 7.5 minutes later
                timeSpentSeconds = 240,
                behavioralIndicators = mapOf(
                    "confidence_score" to 4,
                    "initiative_level" to "high",
                    "peer_following" to "willing",
                    "decision_confidence" to "strong",
                    "task_completion" to "successful"
                )
            )
        )
    }
    
    /**
     * Generate realistic communication skills scenario
     */
    fun generateCommunicationScenario(): List<BehavioralAnalyticsEntity> {
        val sessionId = AnalyticsTestUtils.generateAnonymousSessionId()
        val baseTimestamp = System.currentTimeMillis() - Random.nextLong(0, 1800000) // Last 30 minutes
        
        return listOf(
            // Active listening exercise
            createBehavioralEntity(
                sessionId = sessionId,
                behavioralCategory = "communication",
                interactionType = "active_listening",
                timestamp = baseTimestamp,
                timeSpentSeconds = 150,
                behavioralIndicators = mapOf(
                    "communication_score" to 4,
                    "listening_quality" to "excellent",
                    "response_relevance" to "high",
                    "turn_taking" to "respectful",
                    "clarifying_questions" to "thoughtful"
                )
            ),
            
            // Group discussion participation
            createBehavioralEntity(
                sessionId = sessionId,
                behavioralCategory = "communication",
                interactionType = "collaborative_discussion",
                timestamp = baseTimestamp + 300000, // 5 minutes later
                timeSpentSeconds = 360,
                behavioralIndicators = mapOf(
                    "communication_score" to 3,
                    "participation_frequency" to "balanced",
                    "idea_quality" to "creative",
                    "respectful_disagreement" to "demonstrated",
                    "consensus_building" to "effective"
                )
            )
        )
    }
    
    /**
     * Generate realistic leadership behavior scenario
     */
    fun generateLeadershipScenario(): List<BehavioralAnalyticsEntity> {
        val sessionId = AnalyticsTestUtils.generateAnonymousSessionId()
        val baseTimestamp = System.currentTimeMillis() - Random.nextLong(0, 2700000) // Last 45 minutes
        
        return listOf(
            // Team organizing activity
            createBehavioralEntity(
                sessionId = sessionId,
                behavioralCategory = "leadership",
                interactionType = "team_organizing",
                timestamp = baseTimestamp,
                timeSpentSeconds = 180,
                behavioralIndicators = mapOf(
                    "leadership_score" to 4,
                    "organization_skill" to "high",
                    "delegation_effectiveness" to "good",
                    "team_morale" to "positive",
                    "goal_clarity" to "clear"
                )
            ),
            
            // Conflict resolution
            createBehavioralEntity(
                sessionId = sessionId,
                behavioralCategory = "leadership",
                interactionType = "conflict_mediation",
                timestamp = baseTimestamp + 600000, // 10 minutes later
                timeSpentSeconds = 300,
                behavioralIndicators = mapOf(
                    "leadership_score" to 5,
                    "mediation_success" to "excellent",
                    "fairness_perception" to "high",
                    "solution_creativity" to "innovative",
                    "relationship_preservation" to "successful"
                )
            )
        )
    }
    
    /**
     * Generate complete lesson analytics workflow
     */
    fun generateCompleteLessonWorkflow(): List<BehavioralAnalyticsEntity> {
        val sessionId = AnalyticsTestUtils.generateAnonymousSessionId()
        val lessonStartTime = System.currentTimeMillis() - Random.nextLong(0, 1800000) // Last 30 minutes
        val workflow = mutableListOf<BehavioralAnalyticsEntity>()
        
        // Lesson start
        workflow.add(createBehavioralEntity(
            sessionId = sessionId,
            behavioralCategory = "engagement",
            interactionType = "lesson_start",
            timestamp = lessonStartTime,
            behavioralIndicators = mapOf(
                "lesson_id" to AnalyticsTestUtils.TEST_LESSON_ID,
                "lesson_topic" to "building_empathy",
                "preparation_level" to "ready",
                "class_size" to Random.nextInt(15, 25)
            )
        ))
        
        // Multiple activities throughout lesson
        val activities = listOf("warm_up", "main_activity", "reflection", "sharing")
        activities.forEachIndexed { index, activity ->
            val category = behavioralCategories[index % behavioralCategories.size]
            workflow.add(createBehavioralEntity(
                sessionId = sessionId,
                behavioralCategory = category,
                interactionType = "${activity}_activity",
                timestamp = lessonStartTime + ((index + 1) * 300000L), // 5 minutes apart
                timeSpentSeconds = Random.nextLong(120, 360),
                behavioralIndicators = generateActivityIndicators(category, activity)
            ))
        }
        
        // Lesson completion
        workflow.add(createBehavioralEntity(
            sessionId = sessionId,
            behavioralCategory = "engagement",
            interactionType = "lesson_complete",
            timestamp = lessonStartTime + 1800000, // 30 minutes total
            behavioralIndicators = mapOf(
                "completion_status" to "successful",
                "overall_engagement" to "high",
                "learning_objectives_met" to "yes",
                "student_satisfaction" to Random.nextInt(3, 6)
            )
        ))
        
        return workflow
    }
    
    /**
     * Generate offline analytics scenario
     */
    fun generateOfflineScenario(): List<BehavioralAnalyticsEntity> {
        val sessionId = AnalyticsTestUtils.generateAnonymousSessionId()
        val baseTimestamp = System.currentTimeMillis() - Random.nextLong(0, 7200000)
        
        return listOf(
            createBehavioralEntity(
                sessionId = sessionId,
                behavioralCategory = "empathy",
                interactionType = "offline_peer_support",
                timestamp = baseTimestamp,
                timeSpentSeconds = 150,
                behavioralIndicators = mapOf(
                    "empathy_score" to 4,
                    "offline_mode" to true,
                    "cached_response" to true,
                    "sync_required" to true
                ),
                isOfflineRecorded = true,
                needsSync = true
            ),
            
            createBehavioralEntity(
                sessionId = sessionId,
                behavioralCategory = "confidence",
                interactionType = "offline_voice_sharing",
                timestamp = baseTimestamp + 300000,
                timeSpentSeconds = 120,
                behavioralIndicators = mapOf(
                    "confidence_score" to 3,
                    "offline_mode" to true,
                    "local_storage" to true,
                    "pending_upload" to true
                ),
                isOfflineRecorded = true,
                needsSync = true
            )
        )
    }
    
    /**
     * Generate performance testing dataset
     */
    fun generatePerformanceDataset(size: Int = 1000): List<BehavioralAnalyticsEntity> {
        val data = mutableListOf<BehavioralAnalyticsEntity>()
        val sessionIds = (1..10).map { AnalyticsTestUtils.generateAnonymousSessionId() }
        
        repeat(size) { index ->
            val sessionId = sessionIds[index % sessionIds.size]
            val category = behavioralCategories[index % behavioralCategories.size]
            val interactionType = when (category) {
                "empathy" -> empathyInteractions[index % empathyInteractions.size]
                "confidence" -> confidenceInteractions[index % confidenceInteractions.size]
                "communication" -> communicationInteractions[index % communicationInteractions.size]
                "leadership" -> leadershipInteractions[index % leadershipInteractions.size]
                else -> interactionTypes[index % interactionTypes.size]
            }
            
            data.add(createBehavioralEntity(
                sessionId = sessionId,
                behavioralCategory = category,
                interactionType = interactionType,
                timestamp = System.currentTimeMillis() - Random.nextLong(0, 86400000), // Last 24 hours
                timeSpentSeconds = Random.nextLong(30, 600),
                behavioralIndicators = generateRandomIndicators(category)
            ))
        }
        
        return data
    }
    
    /**
     * Generate analytics events for API testing
     */
    fun generateAnalyticsEvents(count: Int = 50): List<AnalyticsEventEntity> {
        val events = mutableListOf<AnalyticsEventEntity>()
        val eventCategories = listOf("lesson_interaction", "ui_navigation", "content_engagement", "social_interaction")
        val eventActions = listOf("click", "view", "complete", "share", "bookmark", "download")
        
        repeat(count) { index ->
            events.add(AnalyticsEventEntity(
                id = UUID.randomUUID().toString(),
                sessionId = AnalyticsTestUtils.generateAnonymousSessionId(),
                classroomId = AnalyticsTestUtils.TEST_CLASSROOM_ID,
                userId = null, // COPPA compliance
                eventCategory = eventCategories[index % eventCategories.size],
                eventAction = eventActions[index % eventActions.size],
                eventLabel = "test_event_$index",
                eventValue = Random.nextInt(1, 6),
                eventProperties = mapOf(
                    "lesson_id" to AnalyticsTestUtils.TEST_LESSON_ID,
                    "activity_id" to AnalyticsTestUtils.TEST_ACTIVITY_ID,
                    "engagement_level" to listOf("low", "medium", "high")[Random.nextInt(3)]
                ),
                deviceInfo = AnalyticsTestUtils.createMockDeviceInfo(),
                timestamp = System.currentTimeMillis() - Random.nextLong(0, 3600000),
                isOfflineRecorded = Random.nextBoolean(),
                needsSync = Random.nextBoolean(),
                syncAttempts = Random.nextInt(0, 3),
                lastSyncAttempt = if (Random.nextBoolean()) System.currentTimeMillis() - Random.nextLong(0, 1800000) else null,
                createdAt = System.currentTimeMillis() - Random.nextLong(0, 86400000)
            ))
        }
        
        return events
    }
    
    /**
     * Generate sync batch entities for testing
     */
    fun generateSyncBatches(count: Int = 10): List<AnalyticsSyncBatchEntity> {
        val batches = mutableListOf<AnalyticsSyncBatchEntity>()
        val statuses = listOf("pending", "in_progress", "completed", "failed")
        
        repeat(count) { index ->
            val batchSize = Random.nextInt(5, 25)
            batches.add(AnalyticsSyncBatchEntity(
                id = UUID.randomUUID().toString(),
                batchTimestamp = System.currentTimeMillis() - Random.nextLong(0, 3600000),
                eventIds = (1..batchSize).map { UUID.randomUUID().toString() },
                syncStatus = statuses[index % statuses.size],
                syncAttempts = Random.nextInt(0, 5),
                lastSyncAttempt = if (Random.nextBoolean()) System.currentTimeMillis() - Random.nextLong(0, 1800000) else null,
                errorMessage = if (Random.nextBoolean()) "Network timeout" else null,
                createdAt = System.currentTimeMillis() - Random.nextLong(0, 86400000),
                completedAt = if (Random.nextBoolean()) System.currentTimeMillis() - Random.nextLong(0, 1800000) else null,
                retryAfter = if (Random.nextBoolean()) System.currentTimeMillis() + Random.nextLong(60000, 300000) else null
            ))
        }
        
        return batches
    }
    
    // Helper methods
    
    private fun createBehavioralEntity(
        sessionId: String,
        behavioralCategory: String,
        interactionType: String,
        timestamp: Long,
        timeSpentSeconds: Long = Random.nextLong(30, 300),
        behavioralIndicators: Map<String, Any> = emptyMap(),
        isOfflineRecorded: Boolean = false,
        needsSync: Boolean = true
    ): BehavioralAnalyticsEntity {
        return BehavioralAnalyticsEntity(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            classroomId = AnalyticsTestUtils.TEST_CLASSROOM_ID,
            lessonId = AnalyticsTestUtils.TEST_LESSON_ID,
            activityId = AnalyticsTestUtils.TEST_ACTIVITY_ID,
            interactionType = interactionType,
            interactionTimestamp = timestamp,
            timeSpentSeconds = timeSpentSeconds,
            interactionCount = Random.nextInt(1, 5),
            behavioralCategory = behavioralCategory,
            behavioralIndicators = behavioralIndicators,
            deviceType = "mobile",
            sessionContext = "classroom",
            screenSize = "normal",
            appVersion = "1.0.0",
            isOfflineRecorded = isOfflineRecorded,
            needsSync = needsSync,
            syncAttempts = if (needsSync) Random.nextInt(0, 3) else 0,
            lastSyncAttempt = if (needsSync && Random.nextBoolean()) timestamp + Random.nextLong(60000, 300000) else null,
            loadTimeMs = Random.nextLong(100, 500),
            errorOccurred = Random.nextBoolean() && Random.nextFloat() < 0.1f, // 10% chance
            errorDetails = if (Random.nextBoolean() && Random.nextFloat() < 0.1f) "Test error message" else null,
            createdAt = timestamp,
            updatedAt = timestamp + Random.nextLong(0, 60000)
        )
    }
    
    private fun generateActivityIndicators(category: String, activity: String): Map<String, Any> {
        return when (category) {
            "empathy" -> mapOf(
                "empathy_score" to Random.nextInt(1, 6),
                "emotional_recognition" to listOf("low", "medium", "high")[Random.nextInt(3)],
                "peer_response" to listOf("positive", "neutral", "supportive")[Random.nextInt(3)]
            )
            "confidence" -> mapOf(
                "confidence_score" to Random.nextInt(1, 6),
                "participation_level" to listOf("shy", "moderate", "active")[Random.nextInt(3)],
                "leadership_displayed" to Random.nextBoolean()
            )
            "communication" -> mapOf(
                "communication_score" to Random.nextInt(1, 6),
                "clarity_level" to listOf("unclear", "adequate", "clear")[Random.nextInt(3)],
                "listening_quality" to listOf("distracted", "attentive", "engaged")[Random.nextInt(3)]
            )
            "leadership" -> mapOf(
                "leadership_score" to Random.nextInt(1, 6),
                "initiative_level" to listOf("follower", "participant", "leader")[Random.nextInt(3)],
                "team_impact" to listOf("minimal", "positive", "significant")[Random.nextInt(3)]
            )
            else -> mapOf(
                "engagement_level" to listOf("low", "medium", "high")[Random.nextInt(3)],
                "activity_type" to activity
            )
        }
    }
    
    private fun generateRandomIndicators(category: String): Map<String, Any> {
        val baseIndicators = mapOf(
            "${category}_score" to Random.nextInt(1, 6),
            "engagement_level" to listOf("low", "medium", "high")[Random.nextInt(3)],
            "session_quality" to listOf("poor", "average", "good", "excellent")[Random.nextInt(4)]
        )
        
        return when (category) {
            "empathy" -> baseIndicators + mapOf(
                "emotional_context" to listOf("joy", "sadness", "frustration", "excitement")[Random.nextInt(4)],
                "peer_interaction" to Random.nextBoolean()
            )
            "confidence" -> baseIndicators + mapOf(
                "voice_volume" to listOf("quiet", "appropriate", "loud")[Random.nextInt(3)],
                "eye_contact" to Random.nextBoolean()
            )
            "communication" -> baseIndicators + mapOf(
                "turn_taking" to Random.nextBoolean(),
                "question_asking" to Random.nextInt(0, 5)
            )
            "leadership" -> baseIndicators + mapOf(
                "initiative_count" to Random.nextInt(0, 3),
                "peer_following" to Random.nextBoolean()
            )
            else -> baseIndicators
        }
    }
}