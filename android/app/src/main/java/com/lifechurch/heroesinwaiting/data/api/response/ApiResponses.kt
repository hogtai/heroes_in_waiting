package com.lifechurch.heroesinwaiting.data.api.response

import android.os.Parcelable
import com.lifechurch.heroesinwaiting.data.model.*
import kotlinx.parcelize.Parcelize

/**
 * API Response models for the Heroes in Waiting backend
 * Enhanced with new mobile-optimized endpoints and better UX features
 */

// ================== Enhanced Student Response Models ==================

@Parcelize
data class ClassroomPreviewResponse(
    val classroom: ClassroomPreview,
    val facilitator: FacilitatorPreview,
    val currentLesson: LessonPreview?,
    val isActive: Boolean,
    val canJoin: Boolean,
    val requiresWaitingRoom: Boolean = false
) : Parcelable

@Parcelize
data class ClassroomPreview(
    val id: String,
    val name: String,
    val description: String?,
    val grade: Grade,
    val currentStudentCount: Int,
    val maxStudents: Int,
    val isSessionActive: Boolean,
    val hasStartedProgram: Boolean
) : Parcelable

@Parcelize
data class FacilitatorPreview(
    val firstName: String,
    val lastName: String,
    val organization: String?
) : Parcelable {
    val displayName: String
        get() = if (organization.isNullOrBlank()) "$firstName $lastName" else "$firstName $lastName ($organization)"
}

@Parcelize
data class LessonPreview(
    val id: String,
    val title: String,
    val lessonNumber: Int,
    val estimatedDuration: Int,
    val category: LessonCategory
) : Parcelable

@Parcelize
data class StudentEnrollmentResponse(
    val student: Student,
    val classroom: Classroom,
    val sessionId: String,
    val welcomeMessage: String,
    val nextSteps: List<String>
) : Parcelable

@Parcelize
data class EmotionalCheckinResponse(
    val success: Boolean,
    val message: String,
    val encouragementMessage: String?,
    val suggestedActivities: List<String> = emptyList(),
    val shouldNotifyFacilitator: Boolean = false
) : Parcelable

@Parcelize
data class MobileOptimizedLessonResponse(
    val lesson: MobileLesson,
    val deviceOptimizations: DeviceOptimizations,
    val offlineContent: OfflineContent?
) : Parcelable

@Parcelize
data class MobileLesson(
    val id: String,
    val title: String,
    val description: String,
    val lessonNumber: Int,
    val estimatedDuration: Int,
    val category: LessonCategory,
    val difficultyLevel: DifficultyLevel,
    val targetGrades: List<Grade>,
    val mobileContent: MobileLessonContent,
    val interactiveElements: List<MobileInteractiveElement>,
    val checkpoints: List<LessonCheckpoint> = emptyList()
) : Parcelable

@Parcelize
data class MobileLessonContent(
    val introduction: MobileContentSection,
    val mainSections: List<MobileContentSection>,
    val conclusion: MobileContentSection,
    val quickSummary: String
) : Parcelable

@Parcelize
data class MobileContentSection(
    val id: String,
    val title: String,
    val content: String,
    val contentType: ContentType,
    val estimatedDuration: Int,
    val multimedia: List<MobileMultimedia> = emptyList(),
    val interactionPrompts: List<String> = emptyList(),
    val isSkippable: Boolean = false
) : Parcelable

@Parcelize
data class MobileMultimedia(
    val id: String,
    val title: String,
    val mediaType: MediaType,
    val url: String,
    val thumbnailUrl: String?,
    val duration: Int?,
    val lowBandwidthUrl: String?,
    val accessibility: MediaAccessibility,
    val downloadable: Boolean = false
) : Parcelable

@Parcelize
data class MobileInteractiveElement(
    val id: String,
    val type: InteractiveElementType,
    val title: String,
    val description: String,
    val configuration: String, // JSON configuration
    val isRequired: Boolean = true,
    val touchTargetSize: Int = 48, // Minimum 48dp for age-appropriate design
    val colorScheme: String = "primary"
) : Parcelable

@Parcelize
enum class InteractiveElementType : Parcelable {
    TOUCH_BUTTON,
    DRAG_AND_DROP,
    MULTIPLE_CHOICE,
    EMOJI_SELECTOR,
    DRAWING_CANVAS,
    AUDIO_RECORDER,
    PHOTO_CAPTURE,
    GESTURE_RECOGNITION,
    TIMER_ACTIVITY,
    COLLABORATIVE_BOARD
}

@Parcelize
data class DeviceOptimizations(
    val screenSize: ScreenSize,
    val density: String,
    val hasTouch: Boolean,
    val recommendedTextSize: Float,
    val touchTargetSize: Int,
    val layoutConfiguration: String,
    val performanceLevel: PerformanceLevel
) : Parcelable

@Parcelize
enum class ScreenSize : Parcelable {
    PHONE_SMALL,
    PHONE_NORMAL,
    PHONE_LARGE,
    TABLET_SMALL,
    TABLET_NORMAL,
    TABLET_LARGE,
    CHROMEBOOK,
    DESKTOP
}

@Parcelize
enum class PerformanceLevel : Parcelable {
    HIGH,
    MEDIUM,
    LOW
}

@Parcelize
data class OfflineContent(
    val essentialContent: List<String>, // Content IDs that should be cached
    val mediaFiles: List<String>, // URLs for offline media
    val estimatedDownloadSize: Long, // Size in bytes
    val lastUpdated: String,
    val expiresAt: String
) : Parcelable

@Parcelize
data class LessonCheckpoint(
    val id: String,
    val sectionId: String,
    val title: String,
    val description: String,
    val isCompleted: Boolean = false,
    val completedAt: String? = null
) : Parcelable

// ================== Standard Response Models ==================

@Parcelize
data class LessonListResponse(
    val lessons: List<Lesson>,
    val totalCount: Int,
    val currentPage: Int,
    val totalPages: Int,
    val hasMore: Boolean
) : Parcelable

@Parcelize
data class StudentLessonResponse(
    val lesson: Lesson,
    val studentProgress: StudentLessonProgress,
    val classroomContext: ClassroomContext,
    val availableActivities: List<StudentActivity>
) : Parcelable

@Parcelize
data class StudentLessonProgress(
    val lessonId: String,
    val studentId: String,
    val startedAt: String?,
    val completedSections: List<String>,
    val currentSectionId: String?,
    val progressPercentage: Float,
    val timeSpent: Long, // in seconds
    val isCompleted: Boolean
) : Parcelable

@Parcelize
data class ClassroomContext(
    val sessionId: String,
    val facilitatorName: String,
    val classroomName: String,
    val sessionStartTime: String,
    val allowedFeatures: List<String>,
    val peerCount: Int
) : Parcelable

@Parcelize
data class StudentActivity(
    val id: String,
    val title: String,
    val description: String,
    val activityType: ActivityType,
    val isAvailable: Boolean,
    val requiresFacilitatorApproval: Boolean,
    val estimatedDuration: Int,
    val instructions: List<String>
) : Parcelable

@Parcelize
data class StudentProgressResponse(
    val studentId: String,
    val classroomId: String,
    val completedLessons: List<String>,
    val currentLessonId: String?,
    val totalProgressPercentage: Float,
    val streakDays: Int,
    val achievements: List<Achievement>,
    val nextRecommendedLessons: List<String>
) : Parcelable

@Parcelize
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconUrl: String?,
    val earnedAt: String,
    val category: AchievementCategory
) : Parcelable

@Parcelize
enum class AchievementCategory : Parcelable {
    COMPLETION,
    PARTICIPATION,
    KINDNESS,
    LEADERSHIP,
    HELP_OTHERS,
    CONSISTENCY,
    BREAKTHROUGH
}

@Parcelize
data class ClassroomAnalyticsResponse(
    val classroomId: String,
    val timeRange: String,
    val totalStudents: Int,
    val activeStudents: Int,
    val averageEngagement: Float,
    val completionRate: Float,
    val lessonsCompleted: Int,
    val totalTimeSpent: Long,
    val topCategories: List<CategoryEngagement>,
    val studentSummaries: List<StudentSummary>
) : Parcelable

@Parcelize
data class CategoryEngagement(
    val category: LessonCategory,
    val engagementScore: Float,
    val completionRate: Float,
    val averageTimeSpent: Long
) : Parcelable

@Parcelize
data class StudentSummary(
    val studentId: String,
    val sessionId: String, // For anonymity
    val lessonsCompleted: Int,
    val totalTimeSpent: Long,
    val engagementLevel: EngagementLevel,
    val lastActiveAt: String
) : Parcelable

@Parcelize
enum class EngagementLevel : Parcelable {
    HIGH,
    MEDIUM,
    LOW,
    INACTIVE
}

// ================== Request Models ==================

data class EmotionalCheckinRequest(
    val sessionId: String,
    val emotion: EmotionType,
    val intensity: Int, // 1-5 scale
    val note: String? = null,
    val needsHelp: Boolean = false
)

enum class EmotionType {
    HAPPY,
    EXCITED,
    CALM,
    CONFIDENT,
    WORRIED,
    SAD,
    ANGRY,
    CONFUSED,
    FRUSTRATED,
    SCARED
}

data class ActivityResponseRequest(
    val activityId: String,
    val responses: Map<String, Any>, // Flexible response structure
    val timeSpent: Long, // in seconds
    val isCompleted: Boolean,
    val needsHelp: Boolean = false
)

data class ProgressUpdateRequest(
    val lessonId: String,
    val sectionId: String?,
    val action: ProgressAction,
    val timeSpent: Long, // in seconds
    val metadata: Map<String, Any> = emptyMap()
)

enum class ProgressAction {
    STARTED,
    SECTION_COMPLETED,
    LESSON_COMPLETED,
    PAUSED,
    RESUMED,
    SKIPPED
}

data class StudentFeedbackRequest(
    val lessonId: String,
    val rating: Int, // 1-5 stars
    val feedback: String?,
    val categories: List<FeedbackCategory> = emptyList(),
    val isAnonymous: Boolean = true
)

enum class FeedbackCategory {
    TOO_EASY,
    TOO_HARD,
    JUST_RIGHT,
    CONFUSING,
    BORING,
    ENGAGING,
    HELPFUL,
    NOT_HELPFUL
}

data class AnalyticsEventRequest(
    val eventType: String,
    val userId: String?,
    val sessionId: String?,
    val lessonId: String?,
    val classroomId: String?,
    val properties: Map<String, Any> = emptyMap(),
    val timestamp: String
)

// ================== Enhanced Analytics Request Models ==================

data class BehavioralAnalyticsRequest(
    val classroomId: String,
    val lessonId: String?,
    val sessionId: String,
    val interactionType: String,
    val timeSpentSeconds: Long,
    val interactionCount: Int = 1,
    val behavioralCategory: String,
    val behavioralIndicators: Map<String, Any> = emptyMap(),
    val deviceType: String = "mobile",
    val sessionContext: String,
    val offlineMode: Boolean = false,
    val additionalMetadata: Map<String, Any> = emptyMap()
)

data class BehavioralAnalyticsBatchRequest(
    val events: List<BehavioralAnalyticsRequest>,
    val batchId: String,
    val deviceType: String = "mobile",
    val appVersion: String,
    val offlineMode: Boolean = false
)

data class BatchUploadResponse(
    val success: Boolean,
    val processedCount: Int,
    val failedCount: Int,
    val batchId: String,
    val failedEvents: List<String> = emptyList()
)

data class UpdateFacilitatorRequest(
    val firstName: String?,
    val lastName: String?,
    val organization: String?,
    val role: String?
)

// ================== Enhanced Analytics Response Models ==================

@Parcelize
data class EnhancedClassroomAnalyticsResponse(
    val classroomId: String,
    val timeframe: String,
    val engagementMetrics: EngagementMetrics,
    val behavioralMetrics: BehavioralMetrics,
    val lessonEffectiveness: List<LessonEffectivenessMetric>,
    val studentSummaries: List<AnonymousStudentSummary>,
    val trends: TrendData,
    val lastUpdated: String
) : Parcelable

@Parcelize
data class ClassroomAnalyticsSummaryResponse(
    val classroomId: String,
    val totalStudents: Int,
    val activeStudents: Int,
    val averageEngagement: Float,
    val completionRate: Float,
    val totalLessonsCompleted: Int,
    val currentStreak: Int,
    val lastActivity: String
) : Parcelable

@Parcelize
data class LessonEffectivenessResponse(
    val lessonId: String,
    val effectivenessScore: Float,
    val engagementMetrics: EngagementMetrics,
    val behavioralImpact: BehavioralImpactMetrics,
    val completionStats: CompletionStats,
    val feedback: LessonFeedbackSummary,
    val recommendations: List<String>
) : Parcelable

@Parcelize
data class LessonTrendsResponse(
    val lessonId: String,
    val period: String,
    val trendData: List<TrendDataPoint>,
    val summary: TrendSummary
) : Parcelable

@Parcelize
data class FacilitatorInsightsResponse(
    val facilitatorId: String,
    val timeframe: String,
    val totalClassrooms: Int,
    val insights: FacilitatorInsights,
    val recommendations: List<String>,
    val lastUpdated: String
) : Parcelable

@Parcelize
data class CurriculumInsightsResponse(
    val gradeLevel: String,
    val insights: CurriculumInsights,
    val lessons: List<LessonEffectivenessMetric>,
    val lastUpdated: String
) : Parcelable

// ================== Supporting Analytics Data Models ==================

@Parcelize
data class EngagementMetrics(
    val averageEngagementScore: Float,
    val interactionRate: Float,
    val timeSpentAverage: Long,
    val completionRate: Float,
    val dropOffPoints: List<String>
) : Parcelable

@Parcelize
data class BehavioralMetrics(
    val empathyScore: Float,
    val confidenceScore: Float,
    val communicationScore: Float,
    val leadershipScore: Float,
    val improvementAreas: List<String>
) : Parcelable

@Parcelize
data class BehavioralImpactMetrics(
    val overallImpact: Float,
    val categoryImpacts: Map<String, Float>,
    val improvement: Float,
    val trend: String
) : Parcelable

@Parcelize
data class LessonEffectivenessMetric(
    val lessonId: String,
    val lessonTitle: String,
    val effectivenessScore: Float,
    val engagementScore: Float,
    val completionRate: Float,
    val averageTimeSpent: Long,
    val totalImplementations: Int
) : Parcelable

@Parcelize
data class AnonymousStudentSummary(
    val sessionId: String,
    val engagementLevel: String,
    val progressPercentage: Float,
    val lessonsCompleted: Int,
    val lastActivity: String,
    val behavioralGrowth: Float
) : Parcelable

@Parcelize
data class TrendData(
    val engagement: List<TrendDataPoint>,
    val behavioral: List<TrendDataPoint>,
    val completion: List<TrendDataPoint>
) : Parcelable

@Parcelize
data class TrendDataPoint(
    val date: String,
    val value: Float,
    val metadata: Map<String, Any> = emptyMap()
) : Parcelable

@Parcelize
data class TrendSummary(
    val direction: String, // "increasing", "decreasing", "stable"
    val changePercentage: Float,
    val significance: String, // "high", "medium", "low"
    val keyFactors: List<String>
) : Parcelable

@Parcelize
data class CompletionStats(
    val totalAttempts: Int,
    val completionRate: Float,
    val averageTimeToComplete: Long,
    val commonExitPoints: List<String>
) : Parcelable

@Parcelize
data class LessonFeedbackSummary(
    val averageRating: Float,
    val totalFeedbacks: Int,
    val positiveKeywords: List<String>,
    val improvementSuggestions: List<String>
) : Parcelable

@Parcelize
data class FacilitatorInsights(
    val engagement: InsightCategory,
    val behavioral: InsightCategory,
    val curriculum: InsightCategory
) : Parcelable

@Parcelize
data class CurriculumInsights(
    val overview: CurriculumOverview,
    val topPerformingLessons: List<LessonEffectivenessMetric>,
    val improvementOpportunities: List<LessonImprovementOpportunity>
) : Parcelable

@Parcelize
data class InsightCategory(
    val trend: String,
    val avgScore: Float,
    val recommendations: List<String>
) : Parcelable

@Parcelize
data class CurriculumOverview(
    val totalLessons: Int,
    val avgEngagement: Float,
    val avgBehavioralImpact: Float,
    val totalImplementations: Int
) : Parcelable

@Parcelize
data class LessonImprovementOpportunity(
    val lessonId: String,
    val title: String,
    val currentScore: Float,
    val improvementPotential: Float,
    val suggestions: List<String>
) : Parcelable

/**
 * Standard API Error Response
 */
@Parcelize
data class ApiErrorResponse(
    val error: String,
    val message: String,
    val statusCode: Int,
    val timestamp: String,
    val details: Map<String, Any>? = null
) : Parcelable