package com.lifechurch.heroesinwaiting.data.analytics

/**
 * Extensions for AnalyticsService to provide student-specific tracking methods
 * Focuses on behavioral analytics and educational engagement patterns
 */

/**
 * Track student dashboard interaction with engagement insights
 */
fun AnalyticsService.trackStudentDashboardInteraction(
    classroomId: String,
    interactionType: String, // "view_dashboard", "check_progress", "access_lesson"
    engagementLevel: String = "medium", // "high", "medium", "low"
    sessionContext: Map<String, Any> = emptyMap()
) {
    trackUserInteraction(
        eventType = "student_dashboard",
        eventAction = interactionType,
        classroomId = classroomId,
        properties = mapOf(
            "engagement_level" to engagementLevel,
            "session_context" to sessionContext,
            "interaction_timestamp" to System.currentTimeMillis()
        )
    )
}

/**
 * Track emotional check-in completion with behavioral indicators
 */
fun AnalyticsService.trackEmotionalCheckinCompletion(
    classroomId: String,
    lessonId: String?,
    emotionSelected: String,
    intensityLevel: Int,
    needsSupport: Boolean,
    timeToComplete: Long,
    contextualFactors: Map<String, Any> = emptyMap()
) {
    trackEmotionalCheckin(
        classroomId = classroomId,
        lessonId = lessonId,
        emotionType = emotionSelected,
        intensityLevel = intensityLevel,
        needsSupport = needsSupport,
        contextualFactors = contextualFactors + mapOf(
            "time_to_complete" to timeToComplete,
            "self_reflection_quality" to when {
                timeToComplete > 60000 -> "thoughtful" // More than 1 minute
                timeToComplete > 30000 -> "moderate" // 30-60 seconds
                else -> "quick" // Less than 30 seconds
            }
        )
    )
}

/**
 * Track lesson activity completion with behavioral growth indicators
 */
fun AnalyticsService.trackLessonActivityCompletion(
    classroomId: String,
    lessonId: String,
    activityId: String,
    activityType: String,
    completionRate: Float,
    timeSpent: Long,
    interactionCount: Int,
    helpRequested: Boolean = false,
    peerInteraction: Boolean = false,
    behavioralGrowthIndicators: Map<String, Any> = emptyMap()
) {
    trackBehavioralAnalytics(
        classroomId = classroomId,
        lessonId = lessonId,
        sessionId = getCurrentSessionId(),
        interactionType = "activity_completion",
        timeSpentSeconds = timeSpent / 1000,
        behavioralCategory = inferBehavioralCategory(activityType),
        behavioralIndicators = mapOf(
            "activity_type" to activityType,
            "completion_rate" to completionRate,
            "interaction_count" to interactionCount,
            "help_requested" to helpRequested,
            "peer_interaction" to peerInteraction,
            "behavioral_growth" to behavioralGrowthIndicators,
            "engagement_quality" to calculateEngagementQuality(
                timeSpent, 
                interactionCount, 
                completionRate
            )
        ),
        sessionContext = if (peerInteraction) "group_activity" else "individual_activity",
        additionalMetadata = mapOf(
            "activity_id" to activityId,
            "support_pattern" to if (helpRequested) "seeks_help" else "independent"
        )
    )
}

/**
 * Track empathy-building activity responses
 */
fun AnalyticsService.trackEmpathyActivityResponse(
    classroomId: String,
    lessonId: String,
    activityId: String,
    empathyResponse: String,
    emotionalAwareness: String, // "high", "developing", "needs_support"
    perspectiveTaking: Boolean,
    helpingBehaviorIntent: Boolean
) {
    trackEmpathyInteraction(
        classroomId = classroomId,
        lessonId = lessonId,
        activityId = activityId,
        interactionType = "empathy_activity_response",
        empathyScore = calculateEmpathyScore(emotionalAwareness, perspectiveTaking, helpingBehaviorIntent),
        contextualFactors = mapOf(
            "empathy_response" to empathyResponse,
            "emotional_awareness" to emotionalAwareness,
            "perspective_taking" to perspectiveTaking,
            "helping_behavior_intent" to helpingBehaviorIntent,
            "response_thoughtfulness" to if (empathyResponse.length > 50) "detailed" else "brief"
        )
    )
}

/**
 * Track communication and social interaction during activities
 */
fun AnalyticsService.trackSocialInteractionActivity(
    classroomId: String,
    lessonId: String,
    activityId: String,
    interactionType: String, // "peer_discussion", "group_work", "sharing_circle"
    participationLevel: String, // "active", "moderate", "observer"
    communicationQuality: String, // "clear", "developing", "needs_support"
    conflictResolution: Boolean = false,
    leadershipBehavior: Boolean = false,
    peerCount: Int = 0
) {
    trackCommunicationInteraction(
        classroomId = classroomId,
        lessonId = lessonId,
        interactionType = interactionType,
        communicationQuality = communicationQuality,
        peerInteractionCount = peerCount,
        conflictResolution = conflictResolution
    )
    
    if (leadershipBehavior) {
        trackLeadershipBehavior(
            classroomId = classroomId,
            lessonId = lessonId,
            leadershipType = "peer_collaboration",
            helpingBehavior = true,
            leadershipEffectiveness = calculateLeadershipEffectiveness(
                participationLevel, 
                communicationQuality
            ),
            peerResponse = "positive"
        )
    }
}

/**
 * Track confidence-building moments during activities
 */
fun AnalyticsService.trackConfidenceBuildingMoment(
    classroomId: String,
    lessonId: String,
    activityId: String,
    confidenceLevel: Float,
    participationChange: String, // "increased", "maintained", "decreased"
    supportNeeded: Boolean,
    encouragementReceived: Boolean,
    achievement: String? = null
) {
    trackConfidenceBuilding(
        classroomId = classroomId,
        lessonId = lessonId,
        activityId = activityId,
        confidenceLevel = confidenceLevel,
        participationLevel = participationChange,
        supportNeeded = supportNeeded
    )
    
    // Track additional confidence context
    trackUserInteraction(
        eventType = "confidence_building",
        eventAction = "confidence_moment",
        classroomId = classroomId,
        lessonId = lessonId,
        activityId = activityId,
        properties = mapOf(
            "participation_change" to participationChange,
            "encouragement_received" to encouragementReceived,
            "achievement" to (achievement ?: "general_participation"),
            "growth_indicator" to if (confidenceLevel > 3.5f) "positive_growth" else "needs_support"
        )
    )
}

// Helper functions for calculating behavioral metrics

private fun inferBehavioralCategory(activityType: String): String {
    return when {
        activityType.contains("empathy", ignoreCase = true) -> "empathy"
        activityType.contains("communication", ignoreCase = true) -> "communication"
        activityType.contains("leadership", ignoreCase = true) -> "leadership"
        activityType.contains("confidence", ignoreCase = true) -> "confidence"
        else -> "engagement"
    }
}

private fun calculateEngagementQuality(timeSpent: Long, interactions: Int, completion: Float): String {
    val timeMinutes = timeSpent / 60000.0
    val interactionRate = if (timeMinutes > 0) interactions / timeMinutes else 0.0
    
    return when {
        completion > 0.8f && interactionRate > 2.0 -> "high_quality"
        completion > 0.6f && interactionRate > 1.0 -> "good_quality"
        completion > 0.4f -> "moderate_quality"
        else -> "needs_support"
    }
}

private fun calculateEmpathyScore(
    emotionalAwareness: String,
    perspectiveTaking: Boolean,
    helpingBehaviorIntent: Boolean
): Float {
    var score = 0f
    
    score += when (emotionalAwareness) {
        "high" -> 2.0f
        "developing" -> 1.5f
        "needs_support" -> 1.0f
        else -> 1.0f
    }
    
    if (perspectiveTaking) score += 1.5f
    if (helpingBehaviorIntent) score += 1.5f
    
    return minOf(score, 5.0f) // Cap at 5.0
}

private fun calculateLeadershipEffectiveness(
    participationLevel: String,
    communicationQuality: String
): Float {
    var effectiveness = 0f
    
    effectiveness += when (participationLevel) {
        "active" -> 2.0f
        "moderate" -> 1.5f
        "observer" -> 1.0f
        else -> 1.0f
    }
    
    effectiveness += when (communicationQuality) {
        "clear" -> 3.0f
        "developing" -> 2.0f
        "needs_support" -> 1.0f
        else -> 1.0f
    }
    
    return minOf(effectiveness, 5.0f) // Cap at 5.0
}