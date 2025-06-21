package com.lifechurch.heroesinwaiting.data.remote.dto

import kotlinx.serialization.Serializable

// Progress and analytics DTOs
@Serializable
data class StudentProgressRequest(
    val studentId: String,
    val completedAt: String? = null,
    val timeSpent: Int? = null,
    val responses: Map<String, String>? = null
)

@Serializable
data class ProgressResponse(
    val success: Boolean,
    val message: String,
    val data: ProgressData? = null
)

@Serializable
data class ProgressData(
    val studentId: String,
    val itemId: String,
    val itemType: String, // "lesson", "activity", "scenario"
    val completedAt: String,
    val progressPercentage: Double,
    val nextItem: NextItemData? = null
)

@Serializable
data class NextItemData(
    val id: String,
    val title: String,
    val type: String,
    val unlocked: Boolean
)

@Serializable
data class ScenarioResponseRequest(
    val studentId: String,
    val responseId: String,
    val reasoning: String? = null,
    val emotionalState: String? = null
)

@Serializable
data class ScenarioResponseResponse(
    val success: Boolean,
    val message: String,
    val data: ScenarioResponseData? = null
)

@Serializable
data class ScenarioResponseData(
    val studentId: String,
    val scenarioId: String,
    val responseId: String,
    val feedback: String,
    val points: Int,
    val correctResponse: Boolean,
    val explanation: String
)

@Serializable
data class StudentProgressResponse(
    val success: Boolean,
    val message: String,
    val data: StudentProgressData? = null
)

@Serializable
data class StudentProgressData(
    val studentId: String,
    val overallProgress: Double,
    val completedLessons: List<CompletedItemData>,
    val completedActivities: List<CompletedItemData>,
    val scenarioResponses: List<ScenarioResponseSummary>,
    val currentLevel: String,
    val totalPoints: Int,
    val achievements: List<AchievementData>
)

@Serializable
data class CompletedItemData(
    val id: String,
    val title: String,
    val completedAt: String,
    val timeSpent: Int,
    val score: Double? = null
)

@Serializable
data class ScenarioResponseSummary(
    val scenarioId: String,
    val scenarioTitle: String,
    val responseType: String,
    val correct: Boolean,
    val completedAt: String
)

@Serializable
data class AchievementData(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val earnedAt: String,
    val category: String
)

@Serializable
data class ClassroomProgressResponse(
    val success: Boolean,
    val message: String,
    val data: ClassroomProgressData? = null
)

@Serializable
data class ClassroomProgressData(
    val classroomId: String,
    val totalStudents: Int,
    val activeStudents: Int,
    val averageProgress: Double,
    val completedLessons: Map<String, Int>, // lessonId -> completionCount
    val studentSummaries: List<StudentProgressSummary>,
    val weeklyActivity: List<WeeklyActivityData>
)

@Serializable
data class StudentProgressSummary(
    val studentId: String,
    val progress: Double,
    val lastActive: String,
    val completedItems: Int,
    val totalTimeSpent: Int
)

@Serializable
data class WeeklyActivityData(
    val week: String,
    val activeStudents: Int,
    val completedLessons: Int,
    val totalTimeSpent: Int
)

@Serializable
data class AnalyticsResponse(
    val success: Boolean,
    val message: String,
    val data: AnalyticsData? = null
)

@Serializable
data class AnalyticsData(
    val classroomId: String,
    val timeframe: String,
    val engagement: EngagementData,
    val progress: ProgressAnalyticsData,
    val behavior: BehaviorAnalyticsData,
    val recommendations: List<RecommendationData>
)

@Serializable
data class EngagementData(
    val averageSessionTime: Double,
    val totalSessions: Int,
    val activeStudentsPercentage: Double,
    val mostEngagingContent: List<ContentEngagementData>
)

@Serializable
data class ContentEngagementData(
    val contentId: String,
    val contentTitle: String,
    val contentType: String,
    val averageTimeSpent: Double,
    val completionRate: Double
)

@Serializable
data class ProgressAnalyticsData(
    val averageProgressPercentage: Double,
    val fastestCompletions: List<String>,
    val strugglingStudents: List<String>,
    val progressTrends: List<ProgressTrendData>
)

@Serializable
data class ProgressTrendData(
    val date: String,
    val averageProgress: Double,
    val newCompletions: Int
)

@Serializable
data class BehaviorAnalyticsData(
    val scenarioPerformance: Map<String, Double>, // scenarioType -> averageScore
    val commonMisconceptions: List<String>,
    val improvementAreas: List<String>,
    val positiveIndicators: List<String>
)

@Serializable
data class RecommendationData(
    val type: String, // "content", "pacing", "intervention"
    val priority: String, // "high", "medium", "low"
    val title: String,
    val description: String,
    val actionItems: List<String>
)