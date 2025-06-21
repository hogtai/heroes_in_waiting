package com.lifechurch.heroesinwaiting.data.remote.dto

import kotlinx.serialization.Serializable

// Enhanced UX and mobile-optimized DTOs
@Serializable
data class EmotionalCheckinRequest(
    val studentId: String,
    val emotion: String,
    val intensity: Int, // 1-5 scale
    val context: String? = null,
    val timestamp: String
)

@Serializable
data class EmotionalCheckinResponse(
    val success: Boolean,
    val message: String,
    val data: EmotionalCheckinData? = null
)

@Serializable
data class EmotionalCheckinData(
    val checkinId: String,
    val encouragement: String,
    val recommendedActivity: String? = null,
    val supportMessage: String,
    val emoji: String
)

@Serializable
data class StudentDashboardResponse(
    val success: Boolean,
    val message: String,
    val data: StudentDashboardData? = null
)

@Serializable
data class StudentDashboardData(
    val welcomeMessage: String,
    val heroLevel: String,
    val totalPoints: Int,
    val currentStreak: Int,
    val nextLesson: NextItemData?,
    val recentAchievements: List<AchievementData>,
    val dailyQuest: DailyQuestData?,
    val friendlyReminder: String,
    val progressVisualization: ProgressVisualizationData
)

@Serializable
data class DailyQuestData(
    val id: String,
    val title: String,
    val description: String,
    val type: String, // "kindness", "courage", "empathy"
    val points: Int,
    val completed: Boolean,
    val emoji: String
)

@Serializable
data class ProgressVisualizationData(
    val completionPercentage: Double,
    val currentMilestone: String,
    val nextMilestone: String,
    val heroPath: List<HeroPathStepData>
)

@Serializable
data class HeroPathStepData(
    val stepNumber: Int,
    val title: String,
    val completed: Boolean,
    val icon: String,
    val unlocked: Boolean
)

@Serializable
data class FacilitatorInsightsResponse(
    val success: Boolean,
    val message: String,
    val data: FacilitatorInsightsData? = null
)

@Serializable
data class FacilitatorInsightsData(
    val facilitatorId: String,
    val summary: FacilitatorSummaryData,
    val recentActivity: List<RecentActivityData>,
    val studentHighlights: List<StudentHighlightData>,
    val actionItems: List<ActionItemData>,
    val weeklyGoals: List<WeeklyGoalData>
)

@Serializable
data class FacilitatorSummaryData(
    val totalStudents: Int,
    val activeClassrooms: Int,
    val completedSessions: Int,
    val averageEngagement: Double,
    val thisWeekProgress: Double
)

@Serializable
data class RecentActivityData(
    val type: String, // "lesson_completed", "scenario_response", "emotional_checkin"
    val studentId: String,
    val classroomId: String,
    val description: String,
    val timestamp: String,
    val needsAttention: Boolean
)

@Serializable
data class StudentHighlightData(
    val studentId: String,
    val type: String, // "achievement", "improvement", "concern"
    val message: String,
    val timestamp: String,
    val actionRequired: Boolean
)

@Serializable
data class ActionItemData(
    val id: String,
    val priority: String, // "high", "medium", "low"
    val type: String, // "follow_up", "review_content", "check_progress"
    val title: String,
    val description: String,
    val relatedStudentId: String? = null,
    val relatedClassroomId: String? = null,
    val dueDate: String? = null
)

@Serializable
data class WeeklyGoalData(
    val id: String,
    val title: String,
    val description: String,
    val targetValue: Int,
    val currentValue: Int,
    val unit: String, // "students", "lessons", "activities"
    val completed: Boolean
)

@Serializable
data class AuditSessionsResponse(
    val success: Boolean,
    val message: String,
    val data: List<AuditSessionData>
)

@Serializable
data class AuditSessionData(
    val sessionId: String,
    val facilitatorId: String,
    val facilitatorName: String,
    val action: String,
    val resourceAccessed: String? = null,
    val ipAddress: String,
    val userAgent: String,
    val timestamp: String,
    val success: Boolean,
    val errorMessage: String? = null
)

@Serializable
data class StudentAuditResponse(
    val success: Boolean,
    val message: String,
    val data: StudentAuditData? = null
)

@Serializable
data class StudentAuditData(
    val studentId: String,
    val activities: List<StudentActivityData>,
    val summary: StudentAuditSummary
)

@Serializable
data class StudentActivityData(
    val activityId: String,
    val activityType: String, // "lesson", "activity", "scenario", "emotional_checkin"
    val contentTitle: String,
    val startTime: String,
    val endTime: String? = null,
    val completed: Boolean,
    val ipAddress: String,
    val deviceInfo: String,
    val responses: Map<String, String>? = null
)

@Serializable
data class StudentAuditSummary(
    val totalActivities: Int,
    val totalTimeSpent: Int, // in minutes
    val firstActivity: String,
    val lastActivity: String,
    val mostActiveDay: String,
    val averageSessionLength: Double
)