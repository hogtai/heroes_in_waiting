package com.lifechurch.heroesinwaiting.data.remote.dto

import kotlinx.serialization.Serializable

// Content-related DTOs
@Serializable
data class ClassroomData(
    val id: String,
    val name: String,
    val code: String,
    val gradeLevel: String,
    val schoolName: String,
    val facilitatorId: String,
    val facilitatorName: String,
    val studentCount: Int,
    val isActive: Boolean,
    val createdAt: String
)

@Serializable
data class ClassroomsResponse(
    val success: Boolean,
    val message: String,
    val data: List<ClassroomData>
)

@Serializable
data class ClassroomResponse(
    val success: Boolean,
    val message: String,
    val data: ClassroomData? = null
)

@Serializable
data class CreateClassroomRequest(
    val name: String,
    val gradeLevel: String,
    val schoolName: String
)

@Serializable
data class StudentsResponse(
    val success: Boolean,
    val message: String,
    val data: List<StudentData>
)

@Serializable
data class StudentData(
    val id: String,
    val classroomId: String,
    val enrolledAt: String,
    val lastActive: String?,
    val progressPercentage: Double,
    val completedLessons: Int,
    val totalLessons: Int
)

@Serializable
data class CurriculumResponse(
    val success: Boolean,
    val message: String,
    val data: CurriculumData? = null
)

@Serializable
data class CurriculumData(
    val totalLessons: Int,
    val totalActivities: Int,
    val totalScenarios: Int,
    val estimatedDuration: String,
    val gradeLevel: String,
    val description: String
)

@Serializable
data class LessonsResponse(
    val success: Boolean,
    val message: String,
    val data: List<LessonData>
)

@Serializable
data class LessonData(
    val id: String,
    val title: String,
    val description: String,
    val orderIndex: Int,
    val estimatedDuration: Int,
    val objectives: List<String>,
    val category: String,
    val isActive: Boolean
)

@Serializable
data class LessonDetailResponse(
    val success: Boolean,
    val message: String,
    val data: LessonDetailData? = null
)

@Serializable
data class LessonDetailData(
    val lesson: LessonData,
    val content: LessonContentData,
    val activities: List<ActivityData>,
    val scenarios: List<ScenarioData>
)

@Serializable
data class LessonContentData(
    val introduction: String,
    val mainContent: String,
    val keyPoints: List<String>,
    val vocabulary: List<VocabularyData>,
    val resources: List<ResourceData>
)

@Serializable
data class VocabularyData(
    val term: String,
    val definition: String,
    val example: String? = null
)

@Serializable
data class ResourceData(
    val title: String,
    val type: String, // "video", "image", "audio", "document"
    val url: String,
    val description: String? = null
)

@Serializable
data class MobileLessonResponse(
    val success: Boolean,
    val message: String,
    val data: MobileLessonData? = null
)

@Serializable
data class MobileLessonData(
    val lesson: LessonData,
    val mobileContent: MobileLessonContentData,
    val interactiveElements: List<InteractiveElementData>
)

@Serializable
data class MobileLessonContentData(
    val shortIntroduction: String,
    val keyConceptsSimplified: List<String>,
    val ageAppropriateExamples: List<String>,
    val visualAids: List<ResourceData>,
    val callToAction: String
)

@Serializable
data class InteractiveElementData(
    val type: String, // "poll", "quiz", "emotion_check", "scenario"
    val title: String,
    val content: String,
    val options: List<String>? = null,
    val correctAnswer: String? = null
)

@Serializable
data class ActivitiesResponse(
    val success: Boolean,
    val message: String,
    val data: List<ActivityData>
)

@Serializable
data class ActivityData(
    val id: String,
    val title: String,
    val description: String,
    val type: String, // "roleplay", "discussion", "creative", "reflection"
    val lessonId: String?,
    val estimatedDuration: Int,
    val materials: List<String>,
    val instructions: List<String>,
    val isActive: Boolean
)

@Serializable
data class ActivityDetailResponse(
    val success: Boolean,
    val message: String,
    val data: ActivityDetailData? = null
)

@Serializable
data class ActivityDetailData(
    val activity: ActivityData,
    val detailedInstructions: String,
    val facilitatorGuide: String,
    val assessmentCriteria: List<String>,
    val variations: List<String>
)

@Serializable
data class ScenariosResponse(
    val success: Boolean,
    val message: String,
    val data: List<ScenarioData>
)

@Serializable
data class ScenarioData(
    val id: String,
    val title: String,
    val description: String,
    val situation: String,
    val characters: List<String>,
    val lessonId: String?,
    val difficulty: String, // "easy", "medium", "hard"
    val isActive: Boolean
)

@Serializable
data class ScenarioDetailResponse(
    val success: Boolean,
    val message: String,
    val data: ScenarioDetailData? = null
)

@Serializable
data class ScenarioDetailData(
    val scenario: ScenarioData,
    val fullSituation: String,
    val possibleResponses: List<ResponseOptionData>,
    val discussionQuestions: List<String>,
    val learningObjectives: List<String>
)

@Serializable
data class ResponseOptionData(
    val id: String,
    val text: String,
    val type: String, // "hero", "bystander", "unhelpful"
    val explanation: String,
    val consequences: String
)