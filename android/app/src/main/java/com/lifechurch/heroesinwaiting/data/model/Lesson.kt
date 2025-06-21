package com.lifechurch.heroesinwaiting.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents a lesson in the Heroes in Waiting curriculum
 * Each lesson contains educational content focused on anti-bullying and character development
 */
@Parcelize
data class Lesson(
    val id: String,
    val title: String,
    val description: String,
    val lessonNumber: Int, // 1-12 for the complete curriculum
    val objectives: List<String>,
    val keyTerms: List<String> = emptyList(),
    val estimatedDuration: Int, // Duration in minutes
    val targetGrades: List<Grade>,
    val difficultyLevel: DifficultyLevel,
    val category: LessonCategory,
    val tags: List<String> = emptyList(),
    val isActive: Boolean = true,
    val createdAt: String,
    val updatedAt: String? = null,
    // Content structure
    val content: LessonContent,
    val activities: List<Activity> = emptyList(),
    val assessments: List<Assessment> = emptyList(),
    // Prerequisites and sequencing
    val prerequisites: List<String> = emptyList(), // Lesson IDs that should be completed first
    val nextLessons: List<String> = emptyList(), // Suggested next lessons
    // Accessibility and accommodations
    val accessibilityFeatures: AccessibilityFeatures = AccessibilityFeatures(),
    val languageSupport: List<String> = listOf("en") // Language codes
) : Parcelable {
    
    val isSequential: Boolean
        get() = prerequisites.isNotEmpty()
    
    val hasMultipleActivities: Boolean
        get() = activities.size > 1
    
    val totalEstimatedTime: Int
        get() = estimatedDuration + activities.sumOf { it.estimatedDuration }
    
    val isBeginnerFriendly: Boolean
        get() = difficultyLevel == DifficultyLevel.BEGINNER && prerequisites.isEmpty()
}

/**
 * Lesson difficulty levels
 */
@Parcelize
enum class DifficultyLevel : Parcelable {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED;
    
    val displayName: String
        get() = when (this) {
            BEGINNER -> "Beginner"
            INTERMEDIATE -> "Intermediate"
            ADVANCED -> "Advanced"
        }
    
    val colorCode: String
        get() = when (this) {
            BEGINNER -> "#4CAF50" // Green
            INTERMEDIATE -> "#FF9800" // Orange
            ADVANCED -> "#F44336" // Red
        }
}

/**
 * Lesson categories for organization and filtering
 */
@Parcelize
enum class LessonCategory : Parcelable {
    BULLYING_PREVENTION,
    EMPATHY_BUILDING,
    CONFLICT_RESOLUTION,
    SELF_CONFIDENCE,
    COMMUNICATION_SKILLS,
    COMMUNITY_BUILDING,
    LEADERSHIP,
    RESPECT_AND_KINDNESS;
    
    val displayName: String
        get() = when (this) {
            BULLYING_PREVENTION -> "Bullying Prevention"
            EMPATHY_BUILDING -> "Empathy Building"
            CONFLICT_RESOLUTION -> "Conflict Resolution"
            SELF_CONFIDENCE -> "Self Confidence"
            COMMUNICATION_SKILLS -> "Communication Skills"
            COMMUNITY_BUILDING -> "Community Building"
            LEADERSHIP -> "Leadership"
            RESPECT_AND_KINDNESS -> "Respect & Kindness"
        }
    
    val iconName: String
        get() = when (this) {
            BULLYING_PREVENTION -> "shield"
            EMPATHY_BUILDING -> "favorite"
            CONFLICT_RESOLUTION -> "handshake"
            SELF_CONFIDENCE -> "emoji_people"
            COMMUNICATION_SKILLS -> "chat"
            COMMUNITY_BUILDING -> "groups"
            LEADERSHIP -> "star"
            RESPECT_AND_KINDNESS -> "volunteer_activism"
        }
}

/**
 * Lesson content structure containing all educational materials
 */
@Parcelize
data class LessonContent(
    val introduction: ContentSection,
    val mainContent: List<ContentSection>,
    val conclusion: ContentSection,
    val facilitatorNotes: String? = null,
    val resources: List<Resource> = emptyList(),
    val handouts: List<Resource> = emptyList()
) : Parcelable

/**
 * Individual content section within a lesson
 */
@Parcelize
data class ContentSection(
    val id: String,
    val title: String,
    val content: String, // Rich text content (could be HTML or Markdown)
    val contentType: ContentType,
    val estimatedDuration: Int, // Duration in minutes
    val isInteractive: Boolean = false,
    val multimedia: List<MultimediaContent> = emptyList(),
    val discussionPrompts: List<String> = emptyList(),
    val notes: String? = null
) : Parcelable

/**
 * Types of content sections
 */
@Parcelize
enum class ContentType : Parcelable {
    TEXT,
    VIDEO,
    AUDIO,
    INTERACTIVE,
    DISCUSSION,
    REFLECTION,
    ACTIVITY,
    ASSESSMENT;
    
    val displayName: String
        get() = when (this) {
            TEXT -> "Reading"
            VIDEO -> "Video"
            AUDIO -> "Audio"
            INTERACTIVE -> "Interactive"
            DISCUSSION -> "Discussion"
            REFLECTION -> "Reflection"
            ACTIVITY -> "Activity"
            ASSESSMENT -> "Assessment"
        }
    
    val iconName: String
        get() = when (this) {
            TEXT -> "text_snippet"
            VIDEO -> "play_circle"
            AUDIO -> "volume_up"
            INTERACTIVE -> "touch_app"
            DISCUSSION -> "forum"
            REFLECTION -> "lightbulb"
            ACTIVITY -> "extension"
            ASSESSMENT -> "quiz"
        }
}

/**
 * Multimedia content within lessons
 */
@Parcelize
data class MultimediaContent(
    val id: String,
    val title: String,
    val description: String? = null,
    val mediaType: MediaType,
    val url: String,
    val thumbnailUrl: String? = null,
    val duration: Int? = null, // Duration in seconds for video/audio
    val fileSize: Long? = null, // File size in bytes
    val mimeType: String,
    val isEmbedded: Boolean = false,
    val requiresInternet: Boolean = true,
    val accessibility: MediaAccessibility = MediaAccessibility()
) : Parcelable

/**
 * Types of multimedia content
 */
@Parcelize
enum class MediaType : Parcelable {
    VIDEO,
    AUDIO,
    IMAGE,
    ANIMATION,
    INTERACTIVE_MEDIA,
    DOCUMENT;
    
    val displayName: String
        get() = name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
}

/**
 * Accessibility features for multimedia content
 */
@Parcelize
data class MediaAccessibility(
    val hasClosedCaptions: Boolean = false,
    val hasAudioDescription: Boolean = false,
    val hasTranscript: Boolean = false,
    val hasSignLanguage: Boolean = false,
    val altText: String? = null,
    val isHighContrast: Boolean = false
) : Parcelable

/**
 * Interactive activities within lessons
 */
@Parcelize
data class Activity(
    val id: String,
    val title: String,
    val description: String,
    val instructions: String,
    val activityType: ActivityType,
    val estimatedDuration: Int, // Duration in minutes
    val isRequired: Boolean = true,
    val groupSize: GroupSize = GroupSize.INDIVIDUAL,
    val materials: List<String> = emptyList(),
    val learningObjectives: List<String> = emptyList(),
    val reflectionQuestions: List<String> = emptyList(),
    val isDigital: Boolean = true,
    val difficultyLevel: DifficultyLevel = DifficultyLevel.BEGINNER
) : Parcelable

/**
 * Types of activities
 */
@Parcelize
enum class ActivityType : Parcelable {
    ROLE_PLAY,
    DISCUSSION,
    CREATIVE_WRITING,
    DRAWING_ART,
    GAME,
    SCENARIO_ANALYSIS,
    REFLECTION,
    PROBLEM_SOLVING,
    STORYTELLING,
    PEER_INTERACTION;
    
    val displayName: String
        get() = name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
}

/**
 * Group sizes for activities
 */
@Parcelize
enum class GroupSize : Parcelable {
    INDIVIDUAL,
    PAIRS,
    SMALL_GROUP,
    LARGE_GROUP,
    WHOLE_CLASS;
    
    val displayName: String
        get() = name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
    
    val recommendedSize: String
        get() = when (this) {
            INDIVIDUAL -> "1 student"
            PAIRS -> "2 students"
            SMALL_GROUP -> "3-5 students"
            LARGE_GROUP -> "6-10 students"
            WHOLE_CLASS -> "All students"
        }
}

/**
 * Assessment and feedback collection
 */
@Parcelize
data class Assessment(
    val id: String,
    val title: String,
    val description: String,
    val assessmentType: AssessmentType,
    val questions: List<AssessmentQuestion>,
    val isRequired: Boolean = true,
    val isAnonymous: Boolean = true,
    val showResults: Boolean = false,
    val estimatedDuration: Int, // Duration in minutes
    val passingScore: Float? = null, // Percentage (0-100)
    val allowMultipleAttempts: Boolean = false,
    val isAdaptive: Boolean = false
) : Parcelable

/**
 * Types of assessments
 */
@Parcelize
enum class AssessmentType : Parcelable {
    REFLECTION,
    QUIZ,
    SURVEY,
    PEER_EVALUATION,
    SELF_ASSESSMENT,
    MOOD_CHECK,
    FEEDBACK_FORM;
    
    val displayName: String
        get() = name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
}

/**
 * Assessment questions
 */
@Parcelize
data class AssessmentQuestion(
    val id: String,
    val question: String,
    val questionType: QuestionType,
    val options: List<String> = emptyList(), // For multiple choice questions
    val isRequired: Boolean = true,
    val helpText: String? = null,
    val orderIndex: Int = 0
) : Parcelable

/**
 * Types of assessment questions
 */
@Parcelize
enum class QuestionType : Parcelable {
    MULTIPLE_CHOICE,
    TRUE_FALSE,
    SHORT_ANSWER,
    LONG_ANSWER,
    RATING_SCALE,
    EMOJI_RESPONSE,
    DRAWING_RESPONSE;
    
    val displayName: String
        get() = name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
}

/**
 * Educational resources and materials
 */
@Parcelize
data class Resource(
    val id: String,
    val title: String,
    val description: String? = null,
    val resourceType: ResourceType,
    val url: String,
    val fileSize: Long? = null,
    val mimeType: String,
    val isDownloadable: Boolean = true,
    val requiresInternet: Boolean = false,
    val targetAudience: TargetAudience = TargetAudience.BOTH
) : Parcelable

/**
 * Types of educational resources
 */
@Parcelize
enum class ResourceType : Parcelable {
    HANDOUT,
    WORKSHEET,
    PRESENTATION,
    FACILITATOR_GUIDE,
    PARENT_LETTER,
    REFERENCE_MATERIAL,
    EXTENSION_ACTIVITY;
    
    val displayName: String
        get() = name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
}

/**
 * Target audience for resources
 */
@Parcelize
enum class TargetAudience : Parcelable {
    STUDENTS,
    FACILITATORS,
    PARENTS,
    BOTH;
    
    val displayName: String
        get() = name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
}

/**
 * Accessibility features for lessons
 */
@Parcelize
data class AccessibilityFeatures(
    val hasAudioSupport: Boolean = false,
    val hasVisualSupport: Boolean = false,
    val hasLargeText: Boolean = false,
    val hasHighContrast: Boolean = false,
    val hasScreenReaderSupport: Boolean = true,
    val hasKeyboardNavigation: Boolean = true,
    val hasReducedMotion: Boolean = false,
    val hasSimplifiedInterface: Boolean = false,
    val supportedAssistiveTechnologies: List<String> = emptyList()
) : Parcelable

/**
 * Utility functions for lesson management
 */
object LessonUtils {
    
    /**
     * Gets the next lesson in sequence
     */
    fun getNextLesson(currentLessonNumber: Int, totalLessons: Int = 12): Int? {
        return if (currentLessonNumber < totalLessons) {
            currentLessonNumber + 1
        } else {
            null
        }
    }
    
    /**
     * Checks if a lesson is available based on prerequisites
     */
    fun isLessonAvailable(lesson: Lesson, completedLessonIds: List<String>): Boolean {
        return lesson.prerequisites.all { prerequisiteId ->
            completedLessonIds.contains(prerequisiteId)
        }
    }
    
    /**
     * Formats duration in a human-readable format
     */
    fun formatDuration(minutes: Int): String {
        return when {
            minutes < 60 -> "${minutes}m"
            minutes % 60 == 0 -> "${minutes / 60}h"
            else -> "${minutes / 60}h ${minutes % 60}m"
        }
    }
    
    /**
     * Gets recommended lessons based on grade level
     */
    fun getRecommendedLessons(lessons: List<Lesson>, grade: Grade): List<Lesson> {
        return lessons.filter { lesson ->
            lesson.targetGrades.contains(grade) && lesson.isActive
        }.sortedBy { it.lessonNumber }
    }
    
    /**
     * Calculates lesson completion percentage
     */
    fun calculateCompletionPercentage(
        completedSections: Int, 
        totalSections: Int
    ): Float {
        return if (totalSections > 0) {
            (completedSections.toFloat() / totalSections.toFloat()) * 100f
        } else {
            0f
        }
    }
}