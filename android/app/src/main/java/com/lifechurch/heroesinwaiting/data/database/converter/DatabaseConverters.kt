package com.lifechurch.heroesinwaiting.data.database.converter

import androidx.room.TypeConverter
import com.lifechurch.heroesinwaiting.data.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Room type converters for complex data types
 */
class DatabaseConverters {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    // Grade converters
    @TypeConverter
    fun fromGrade(grade: Grade): String = grade.name
    
    @TypeConverter
    fun toGrade(gradeName: String): Grade = Grade.valueOf(gradeName)
    
    // DifficultyLevel converters
    @TypeConverter
    fun fromDifficultyLevel(level: DifficultyLevel): String = level.name
    
    @TypeConverter
    fun toDifficultyLevel(levelName: String): DifficultyLevel = DifficultyLevel.valueOf(levelName)
    
    // LessonCategory converters
    @TypeConverter
    fun fromLessonCategory(category: LessonCategory): String = category.name
    
    @TypeConverter
    fun toLessonCategory(categoryName: String): LessonCategory = LessonCategory.valueOf(categoryName)
    
    // SchoolType converters
    @TypeConverter
    fun fromSchoolType(schoolType: SchoolType?): String? = schoolType?.name
    
    @TypeConverter
    fun toSchoolType(schoolTypeName: String?): SchoolType? = 
        schoolTypeName?.let { SchoolType.valueOf(it) }
    
    // List<String> converters
    @TypeConverter
    fun fromStringList(list: List<String>): String = json.encodeToString(list)
    
    @TypeConverter
    fun toStringList(listString: String): List<String> = try {
        json.decodeFromString(listString)
    } catch (e: Exception) {
        emptyList()
    }
    
    // List<Grade> converters
    @TypeConverter
    fun fromGradeList(grades: List<Grade>): String = json.encodeToString(grades.map { it.name })
    
    @TypeConverter
    fun toGradeList(gradesString: String): List<Grade> = try {
        json.decodeFromString<List<String>>(gradesString).map { Grade.valueOf(it) }
    } catch (e: Exception) {
        emptyList()
    }
    
    // DemographicInfo converters
    @TypeConverter
    fun fromDemographicInfo(info: DemographicInfo): String = json.encodeToString(info)
    
    @TypeConverter
    fun toDemographicInfo(infoString: String): DemographicInfo = try {
        json.decodeFromString(infoString)
    } catch (e: Exception) {
        DemographicInfo(grade = Grade.OTHER)
    }
    
    // AccessibilityFeatures converters
    @TypeConverter
    fun fromAccessibilityFeatures(features: AccessibilityFeatures): String = json.encodeToString(features)
    
    @TypeConverter
    fun toAccessibilityFeatures(featuresString: String): AccessibilityFeatures = try {
        json.decodeFromString(featuresString)
    } catch (e: Exception) {
        AccessibilityFeatures()
    }
    
    // LessonContent converters
    @TypeConverter
    fun fromLessonContent(content: LessonContent): String = json.encodeToString(content)
    
    @TypeConverter
    fun toLessonContent(contentString: String): LessonContent = try {
        json.decodeFromString(contentString)
    } catch (e: Exception) {
        LessonContent(
            introduction = ContentSection(
                id = "intro",
                title = "Introduction",
                content = "Welcome to this lesson",
                contentType = ContentType.TEXT,
                estimatedDuration = 2
            ),
            mainContent = emptyList(),
            conclusion = ContentSection(
                id = "conclusion",
                title = "Conclusion",
                content = "Thank you for participating",
                contentType = ContentType.TEXT,
                estimatedDuration = 2
            )
        )
    }
    
    // List<Activity> converters
    @TypeConverter
    fun fromActivityList(activities: List<Activity>): String = json.encodeToString(activities)
    
    @TypeConverter
    fun toActivityList(activitiesString: String): List<Activity> = try {
        json.decodeFromString(activitiesString)
    } catch (e: Exception) {
        emptyList()
    }
    
    // List<Assessment> converters
    @TypeConverter
    fun fromAssessmentList(assessments: List<Assessment>): String = json.encodeToString(assessments)
    
    @TypeConverter
    fun toAssessmentList(assessmentsString: String): List<Assessment> = try {
        json.decodeFromString(assessmentsString)
    } catch (e: Exception) {
        emptyList()
    }
}