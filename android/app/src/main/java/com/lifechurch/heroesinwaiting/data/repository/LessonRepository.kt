package com.lifechurch.heroesinwaiting.data.repository

import com.lifechurch.heroesinwaiting.data.api.ApiService
import com.lifechurch.heroesinwaiting.data.api.StudentApiService
import com.lifechurch.heroesinwaiting.data.database.dao.LessonDao
import com.lifechurch.heroesinwaiting.data.database.entity.LessonEntity
import com.lifechurch.heroesinwaiting.data.model.*
import com.lifechurch.heroesinwaiting.data.api.response.MobileOptimizedLessonResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for lesson data with offline-first approach
 * Provides lessons from local database with network synchronization
 */
@Singleton
class LessonRepository @Inject constructor(
    private val apiService: ApiService,
    private val studentApiService: StudentApiService,
    private val lessonDao: LessonDao
) {
    
    /**
     * Gets all lessons with offline-first approach
     */
    fun getAllLessons(): Flow<List<Lesson>> {
        return lessonDao.getAllActiveLessonsFlow().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    /**
     * Gets lesson by ID with offline-first approach
     */
    fun getLessonById(id: String): Flow<Lesson?> {
        return lessonDao.getLessonByIdFlow(id).map { entity ->
            entity?.toDomainModel()
        }
    }
    
    /**
     * Gets lessons filtered by grade
     */
    fun getLessonsByGrade(grade: Grade): Flow<List<Lesson>> {
        return lessonDao.getLessonsByGradeFlow(grade).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    /**
     * Gets lessons filtered by category
     */
    fun getLessonsByCategory(category: LessonCategory): Flow<List<Lesson>> {
        return lessonDao.getLessonsByCategoryFlow(category).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    /**
     * Gets downloaded lessons for offline use
     */
    fun getDownloadedLessons(): Flow<List<Lesson>> {
        return lessonDao.getDownloadedLessonsFlow().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    /**
     * Searches lessons by query
     */
    fun searchLessons(query: String): Flow<List<Lesson>> {
        return lessonDao.searchLessonsFlow(query).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    /**
     * Gets mobile-optimized lesson content
     */
    suspend fun getMobileOptimizedLesson(
        lessonId: String,
        token: String,
        deviceType: String = "mobile",
        screenDensity: String? = null
    ): Result<MobileOptimizedLessonResponse> {
        return try {
            val response = apiService.getMobileOptimizedLesson(
                lessonId = lessonId,
                token = token,
                deviceType = deviceType,
                screenDensity = screenDensity
            )
            if (response.isSuccessful && response.body() != null) {
                // Cache the lesson for offline use
                val mobileLesson = response.body()!!.lesson
                cacheMobileLesson(mobileLesson)
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to load lesson: ${response.message()}"))
            }
        } catch (e: Exception) {
            // Try to get from local cache
            val cachedLesson = lessonDao.getLessonById(lessonId)
            if (cachedLesson != null) {
                Result.success(createMobileOptimizedResponseFromCache(cachedLesson))
            } else {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Gets student lesson view (no JWT required)
     */
    suspend fun getStudentLesson(
        lessonId: String,
        sessionId: String
    ): Result<Lesson> {
        return try {
            val response = studentApiService.getStudentLesson(lessonId, sessionId)
            if (response.isSuccessful && response.body() != null) {
                val lesson = response.body()!!.lesson
                // Cache the lesson
                cacheLessonFromApi(lesson)
                Result.success(lesson)
            } else {
                // Try local cache
                val cachedLesson = lessonDao.getLessonById(lessonId)
                if (cachedLesson != null) {
                    Result.success(cachedLesson.toDomainModel())
                } else {
                    Result.failure(Exception("Lesson not available offline"))
                }
            }
        } catch (e: Exception) {
            // Try local cache
            val cachedLesson = lessonDao.getLessonById(lessonId)
            if (cachedLesson != null) {
                Result.success(cachedLesson.toDomainModel())
            } else {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Gets recommended lessons based on grade and completed lessons
     */
    suspend fun getRecommendedLessons(
        token: String,
        grade: String,
        completedLessons: String? = null
    ): Result<List<Lesson>> {
        return try {
            val response = apiService.getRecommendedLessons(token, grade, completedLessons)
            if (response.isSuccessful && response.body() != null) {
                val lessons = response.body()!!.lessons
                // Cache lessons
                cacheLessonsFromApi(lessons)
                Result.success(lessons)
            } else {
                // Return cached lessons by grade
                val gradeEnum = Grade.valueOf(grade.uppercase())
                val cachedLessons = lessonDao.getLessonsByGrade(gradeEnum)
                Result.success(cachedLessons.map { it.toDomainModel() })
            }
        } catch (e: Exception) {
            // Return cached lessons by grade
            try {
                val gradeEnum = Grade.valueOf(grade.uppercase())
                val cachedLessons = lessonDao.getLessonsByGrade(gradeEnum)
                Result.success(cachedLessons.map { it.toDomainModel() })
            } catch (gradeException: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Synchronizes lessons from API to local database
     */
    suspend fun syncLessons(): Result<Unit> {
        return try {
            val response = apiService.getLessons()
            if (response.isSuccessful && response.body() != null) {
                val lessons = response.body()!!.lessons
                cacheLessonsFromApi(lessons)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to sync lessons: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Marks lesson as downloaded for offline use
     */
    suspend fun markLessonAsDownloaded(lessonId: String) {
        lessonDao.updateDownloadStatus(lessonId, true)
    }
    
    /**
     * Removes lesson from offline storage
     */
    suspend fun removeLessonFromOfflineStorage(lessonId: String) {
        lessonDao.updateDownloadStatus(lessonId, false)
    }
    
    /**
     * Gets lesson download status
     */
    suspend fun getLessonDownloadStatus(): Flow<Pair<Int, Int>> = flow {
        val total = lessonDao.getTotalLessonsCount()
        val downloaded = lessonDao.getDownloadedLessonsCount()
        emit(Pair(downloaded, total))
    }
    
    // Private helper methods
    
    private suspend fun cacheLessonsFromApi(lessons: List<Lesson>) {
        val entities = lessons.map { LessonEntity.fromDomainModel(it) }
        lessonDao.insertLessons(entities)
    }
    
    private suspend fun cacheLessonFromApi(lesson: Lesson) {
        val entity = LessonEntity.fromDomainModel(lesson)
        lessonDao.insertLesson(entity)
    }
    
    private suspend fun cacheMobileLesson(mobileLesson: com.lifechurch.heroesinwaiting.data.api.response.MobileLesson) {
        // Convert MobileLesson to Lesson and cache
        val lesson = convertMobileLessonToLesson(mobileLesson)
        cacheLessonFromApi(lesson)
    }
    
    private fun convertMobileLessonToLesson(mobileLesson: com.lifechurch.heroesinwaiting.data.api.response.MobileLesson): Lesson {
        // Convert MobileLesson to standard Lesson format
        val content = LessonContent(
            introduction = ContentSection(
                id = "intro",
                title = mobileLesson.mobileContent.introduction.title,
                content = mobileLesson.mobileContent.introduction.content,
                contentType = ContentType.TEXT, // Simplified conversion
                estimatedDuration = mobileLesson.mobileContent.introduction.estimatedDuration
            ),
            mainContent = mobileLesson.mobileContent.mainSections.map { section ->
                ContentSection(
                    id = section.id,
                    title = section.title,
                    content = section.content,
                    contentType = ContentType.TEXT, // Simplified conversion
                    estimatedDuration = section.estimatedDuration
                )
            },
            conclusion = ContentSection(
                id = "conclusion",
                title = mobileLesson.mobileContent.conclusion.title,
                content = mobileLesson.mobileContent.conclusion.content,
                contentType = ContentType.TEXT, // Simplified conversion
                estimatedDuration = mobileLesson.mobileContent.conclusion.estimatedDuration
            )
        )
        
        return Lesson(
            id = mobileLesson.id,
            title = mobileLesson.title,
            description = mobileLesson.description,
            lessonNumber = mobileLesson.lessonNumber,
            objectives = emptyList(), // Would need to be mapped from mobile lesson
            keyTerms = emptyList(),
            estimatedDuration = mobileLesson.estimatedDuration,
            targetGrades = mobileLesson.targetGrades,
            difficultyLevel = mobileLesson.difficultyLevel,
            category = mobileLesson.category,
            tags = emptyList(),
            isActive = true,
            createdAt = System.currentTimeMillis().toString(),
            updatedAt = null,
            content = content,
            activities = emptyList(), // Would need conversion from interactive elements
            assessments = emptyList(),
            prerequisites = emptyList(),
            nextLessons = emptyList(),
            accessibilityFeatures = AccessibilityFeatures(),
            languageSupport = listOf("en")
        )
    }
    
    private suspend fun createMobileOptimizedResponseFromCache(entity: LessonEntity): MobileOptimizedLessonResponse {
        // Create a simplified mobile response from cached data
        // This would need more complete implementation in production
        return MobileOptimizedLessonResponse(
            lesson = convertLessonToMobileLesson(entity.toDomainModel()),
            deviceOptimizations = com.lifechurch.heroesinwaiting.data.api.response.DeviceOptimizations(
                screenSize = com.lifechurch.heroesinwaiting.data.api.response.ScreenSize.PHONE_NORMAL,
                density = "mdpi",
                hasTouch = true,
                recommendedTextSize = 16f,
                touchTargetSize = 48,
                layoutConfiguration = "standard",
                performanceLevel = com.lifechurch.heroesinwaiting.data.api.response.PerformanceLevel.MEDIUM
            ),
            offlineContent = null
        )
    }
    
    private fun convertLessonToMobileLesson(lesson: Lesson): com.lifechurch.heroesinwaiting.data.api.response.MobileLesson {
        // Convert standard Lesson to MobileLesson format
        val mobileContent = com.lifechurch.heroesinwaiting.data.api.response.MobileLessonContent(
            introduction = com.lifechurch.heroesinwaiting.data.api.response.MobileContentSection(
                id = lesson.content.introduction.id,
                title = lesson.content.introduction.title,
                content = lesson.content.introduction.content,
                contentType = lesson.content.introduction.contentType,
                estimatedDuration = lesson.content.introduction.estimatedDuration
            ),
            mainSections = lesson.content.mainContent.map { section ->
                com.lifechurch.heroesinwaiting.data.api.response.MobileContentSection(
                    id = section.id,
                    title = section.title,
                    content = section.content,
                    contentType = section.contentType,
                    estimatedDuration = section.estimatedDuration
                )
            },
            conclusion = com.lifechurch.heroesinwaiting.data.api.response.MobileContentSection(
                id = lesson.content.conclusion.id,
                title = lesson.content.conclusion.title,
                content = lesson.content.conclusion.content,
                contentType = lesson.content.conclusion.contentType,
                estimatedDuration = lesson.content.conclusion.estimatedDuration
            ),
            quickSummary = lesson.description
        )
        
        return com.lifechurch.heroesinwaiting.data.api.response.MobileLesson(
            id = lesson.id,
            title = lesson.title,
            description = lesson.description,
            lessonNumber = lesson.lessonNumber,
            estimatedDuration = lesson.estimatedDuration,
            category = lesson.category,
            difficultyLevel = lesson.difficultyLevel,
            targetGrades = lesson.targetGrades,
            mobileContent = mobileContent,
            interactiveElements = emptyList(), // Would need conversion from activities
            checkpoints = emptyList()
        )
    }
}