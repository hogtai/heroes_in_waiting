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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.net.URL
import java.util.logging.Logger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for lesson data with offline-first approach
 * Provides lessons from local database with network synchronization and comprehensive offline support
 */
@Singleton
class LessonRepository @Inject constructor(
    private val apiService: ApiService,
    private val studentApiService: StudentApiService,
    private val lessonDao: LessonDao
) {
    
    // Logger for network operations
    private val logger = Logger.getLogger(LessonRepository::class.java.name)
    
    // Download progress tracking
    private val _downloadProgress = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, DownloadProgress>> = _downloadProgress.asStateFlow()
    
    // Offline storage directory
    private val offlineStorageDir = File("/data/data/com.lifechurch.heroesinwaiting/files/offline_lessons")
    
    init {
        // Create offline storage directory if it doesn't exist
        if (!offlineStorageDir.exists()) {
            offlineStorageDir.mkdirs()
        }
    }
    
    /**
     * Gets all lessons with offline-first approach
     */
    fun getAllLessons(): Flow<List<Lesson>> {
        return lessonDao.getAllActiveLessonsFlow().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    /**
     * Gets lesson by ID with lazy loading for content
     */
    fun getLessonById(id: String): Flow<Lesson?> {
        return lessonDao.getLessonByIdFlow(id).map { entity ->
            entity?.toDomainModelLazy() // Use lazy loading version
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
     * Gets lessons that are available offline (downloaded or cached)
     */
    fun getOfflineAvailableLessons(): Flow<List<Lesson>> {
        return lessonDao.getAllActiveLessonsFlow().map { entities ->
            entities.filter { entity ->
                entity.isDownloaded || isLessonCached(entity.id)
            }.map { it.toDomainModel() }
        }
    }
    
    /**
     * Checks if a lesson is available offline
     */
    suspend fun isLessonAvailableOffline(lessonId: String): Boolean {
        val lesson = lessonDao.getLessonById(lessonId)
        return lesson?.isDownloaded == true || isLessonCached(lessonId)
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
     * Enhanced download functionality with progress tracking
     */
    suspend fun downloadLessonForOffline(lessonId: String): Result<Unit> {
        return try {
            // Update progress to starting
            updateDownloadProgress(lessonId, DownloadProgress.Starting)
            
            // Get lesson details
            val lesson = lessonDao.getLessonById(lessonId)
            if (lesson == null) {
                updateDownloadProgress(lessonId, DownloadProgress.Failed("Lesson not found"))
                return Result.failure(Exception("Lesson not found"))
            }
            
            // Create lesson directory
            val lessonDir = File(offlineStorageDir, lessonId)
            if (!lessonDir.exists()) {
                lessonDir.mkdirs()
            }
            
            updateDownloadProgress(lessonId, DownloadProgress.Downloading(0))
            
            // Download lesson content
            downloadLessonContent(lesson, lessonDir)
            
            // Download resources
            downloadLessonResources(lesson, lessonDir)
            
            // Mark as downloaded in database
            lessonDao.updateDownloadStatus(lessonId, true)
            lessonDao.updateLastUpdated(lessonId)
            
            updateDownloadProgress(lessonId, DownloadProgress.Completed)
            
            Result.success(Unit)
        } catch (e: Exception) {
            updateDownloadProgress(lessonId, DownloadProgress.Failed(e.message ?: "Download failed"))
            Result.failure(e)
        }
    }
    
    /**
     * Downloads lesson content (text, structure, etc.)
     */
    private suspend fun downloadLessonContent(lesson: LessonEntity, lessonDir: File) {
        // Save lesson metadata
        val metadataFile = File(lessonDir, "metadata.json")
        metadataFile.writeText(lesson.toJson())
        
        // Save lesson content structure
        val contentFile = File(lessonDir, "content.json")
        contentFile.writeText(lesson.mainContent ?: "")
        
        // Update progress
        updateDownloadProgress(lesson.id, DownloadProgress.Downloading(30))
    }
    
    /**
     * Downloads lesson resources (handouts, videos, etc.)
     */
    private suspend fun downloadLessonResources(lesson: LessonEntity, lessonDir: File) {
        val resourcesDir = File(lessonDir, "resources")
        if (!resourcesDir.exists()) {
            resourcesDir.mkdirs()
        }
        
        // Parse lesson content to find resources
        val resources = parseResourcesFromContent(lesson.mainContent)
        
        var downloadedCount = 0
        val totalResources = resources.size
        
        resources.forEach { resource ->
            try {
                downloadResource(resource, resourcesDir)
                downloadedCount++
                
                val progress = 30 + ((downloadedCount.toFloat() / totalResources) * 70).toInt()
                updateDownloadProgress(lesson.id, DownloadProgress.Downloading(progress))
            } catch (e: Exception) {
                // Log resource download failure but continue
                println("Failed to download resource ${resource.url}: ${e.message}")
            }
        }
    }
    
    /**
     * Downloads a single resource
     */
    private suspend fun downloadResource(resource: Resource, resourcesDir: File) {
        val fileName = "${resource.id}_${resource.title.replace(" ", "_")}"
        val fileExtension = getFileExtension(resource.mimeType)
        val resourceFile = File(resourcesDir, "$fileName.$fileExtension")
        
        if (!resourceFile.exists()) {
            URL(resource.url).openStream().use { input ->
                resourceFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
    
    /**
     * Parses resources from lesson content
     */
    private fun parseResourcesFromContent(content: String?): List<Resource> {
        // This would parse the content to extract resource URLs
        // For now, return empty list - would need actual content parsing logic
        return emptyList()
    }
    
    /**
     * Gets file extension from MIME type
     */
    private fun getFileExtension(mimeType: String): String {
        return when (mimeType.lowercase()) {
            "application/pdf" -> "pdf"
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            "video/mp4" -> "mp4"
            "audio/mpeg" -> "mp3"
            "text/plain" -> "txt"
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx"
            else -> "bin"
        }
    }
    
    /**
     * Updates download progress
     */
    private fun updateDownloadProgress(lessonId: String, progress: DownloadProgress) {
        _downloadProgress.value = _downloadProgress.value.toMutableMap().apply {
            put(lessonId, progress)
        }
    }
    
    /**
     * Gets download progress for a specific lesson
     */
    fun getDownloadProgress(lessonId: String): DownloadProgress? {
        return _downloadProgress.value[lessonId]
    }
    
    /**
     * Removes lesson from offline storage
     */
    suspend fun removeLessonFromOfflineStorage(lessonId: String): Result<Unit> {
        return try {
            // Remove from database
            lessonDao.updateDownloadStatus(lessonId, false)
            
            // Remove offline files
            val lessonDir = File(offlineStorageDir, lessonId)
            if (lessonDir.exists()) {
                lessonDir.deleteRecursively()
            }
            
            // Clear progress
            _downloadProgress.value = _downloadProgress.value.toMutableMap().apply {
                remove(lessonId)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Gets offline lesson content
     */
    suspend fun getOfflineLessonContent(lessonId: String): OfflineLessonContent? {
        val lessonDir = File(offlineStorageDir, lessonId)
        if (!lessonDir.exists()) {
            return null
        }
        
        return try {
            val metadataFile = File(lessonDir, "metadata.json")
            val contentFile = File(lessonDir, "content.json")
            val resourcesDir = File(lessonDir, "resources")
            
            if (!metadataFile.exists() || !contentFile.exists()) {
                return null
            }
            
            val metadata = metadataFile.readText()
            val content = contentFile.readText()
            val resources = if (resourcesDir.exists()) {
                resourcesDir.listFiles()?.map { it.absolutePath } ?: emptyList()
            } else {
                emptyList()
            }
            
            OfflineLessonContent(
                lessonId = lessonId,
                metadata = metadata,
                content = content,
                resourcePaths = resources,
                downloadedAt = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Checks if lesson is cached (has offline content)
     */
    private fun isLessonCached(lessonId: String): Boolean {
        val lessonDir = File(offlineStorageDir, lessonId)
        return lessonDir.exists() && File(lessonDir, "metadata.json").exists()
    }
    
    /**
     * Gets total offline storage size
     */
    suspend fun getOfflineStorageSize(): Long {
        return calculateDirectorySize(offlineStorageDir)
    }
    
    /**
     * Calculates directory size recursively
     */
    private fun calculateDirectorySize(directory: File): Long {
        return if (directory.isDirectory) {
            directory.listFiles()?.sumOf { calculateDirectorySize(it) } ?: 0L
        } else {
            directory.length()
        }
    }
    
    /**
     * Clears all offline content
     */
    suspend fun clearAllOfflineContent(): Result<Unit> {
        return try {
            // Clear database flags
            lessonDao.clearAllDownloadFlags()
            
            // Clear offline files
            if (offlineStorageDir.exists()) {
                offlineStorageDir.deleteRecursively()
                offlineStorageDir.mkdirs()
            }
            
            // Clear progress
            _downloadProgress.value = emptyMap()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
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
        val startTime = System.currentTimeMillis()
        logger.info("Starting lesson sync from API")
        
        return try {
            val response = apiService.getLessons()
            val responseTime = System.currentTimeMillis() - startTime
            
            logger.info("Lesson sync API response received in ${responseTime}ms")
            
            if (response.isSuccessful && response.body() != null) {
                val lessons = response.body()!!.lessons
                logger.info("Syncing ${lessons.size} lessons to local database")
                
                cacheLessonsFromApi(lessons)
                
                val totalTime = System.currentTimeMillis() - startTime
                logger.info("Lesson sync completed successfully in ${totalTime}ms")
                
                Result.success(Unit)
            } else {
                logger.warning("Lesson sync failed: ${response.message()}")
                Result.failure(Exception("Failed to sync lessons: ${response.message()}"))
            }
        } catch (e: Exception) {
            val totalTime = System.currentTimeMillis() - startTime
            logger.severe("Lesson sync failed after ${totalTime}ms: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Marks lesson as downloaded for offline use (legacy method)
     */
    suspend fun markLessonAsDownloaded(lessonId: String) {
        lessonDao.updateDownloadStatus(lessonId, true)
    }
    
    /**
     * Gets lesson download status
     */
    suspend fun getLessonDownloadStatus(): Flow<Pair<Int, Int>> = flow {
        val total = lessonDao.getTotalLessonsCount()
        val downloaded = lessonDao.getDownloadedLessonsCount()
        emit(Pair(downloaded, total))
    }
    
    /**
     * Updates lesson bookmark status
     */
    suspend fun updateLessonBookmark(lessonId: String, isBookmarked: Boolean) {
        // This would update bookmark status in database
        // Implementation depends on bookmark table structure
    }
    
    /**
     * Gets lesson content on demand (lazy loading)
     */
    suspend fun getLessonContent(lessonId: String): LessonContent? {
        return try {
            // Try to get from offline storage first
            val offlineContent = getOfflineLessonContent(lessonId)
            if (offlineContent != null) {
                // Parse content from offline storage
                parseLessonContentFromOffline(offlineContent)
            } else {
                // Get from database
                val lesson = lessonDao.getLessonById(lessonId)
                lesson?.let { parseLessonContentFromEntity(it) }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Gets lesson metadata without full content (for list views)
     */
    fun getLessonMetadata(id: String): Flow<Lesson?> {
        return lessonDao.getLessonByIdFlow(id).map { entity ->
            entity?.toDomainModelMetadata() // Use metadata-only version
        }
    }
    
    /**
     * Preloads lesson content for better performance
     */
    suspend fun preloadLessonContent(lessonIds: List<String>) {
        lessonIds.forEach { lessonId ->
            try {
                // Preload content in background
                getLessonContent(lessonId)
            } catch (e: Exception) {
                // Ignore preload failures
            }
        }
    }
    
    /**
     * Gets lessons with pagination for large datasets
     */
    fun getLessonsPaginated(
        page: Int,
        pageSize: Int,
        grade: Grade? = null,
        category: LessonCategory? = null
    ): Flow<List<Lesson>> {
        return flow {
            val offset = page * pageSize
            
            val lessons = if (grade != null) {
                lessonDao.getLessonsByGradePaginated(grade.name, pageSize, offset)
            } else if (category != null) {
                lessonDao.getLessonsByCategoryPaginated(category.name, pageSize, offset)
            } else {
                lessonDao.getAllLessonsPaginated(pageSize, offset)
            }
            
            emit(lessons.map { it.toDomainModelMetadata() })
        }
    }
    
    // Private helper methods
    
    /**
     * Validates lesson data integrity
     */
    private fun validateLessonData(lesson: Lesson): Boolean {
        return lesson.id.isNotBlank() &&
               lesson.title.isNotBlank() &&
               lesson.description.isNotBlank() &&
               lesson.lessonNumber > 0 &&
               lesson.estimatedDuration > 0 &&
               lesson.targetGrades.isNotEmpty()
    }
    
    /**
     * Sanitizes lesson content for security
     */
    private fun sanitizeLessonContent(content: String): String {
        return content
            .replace("<script>", "") // Remove script tags
            .replace("javascript:", "") // Remove javascript protocol
            .trim()
    }
    
    /**
     * Enhanced lesson caching with validation
     */
    private suspend fun cacheLessonsFromApi(lessons: List<Lesson>) {
        val validLessons = lessons.filter { validateLessonData(it) }
        
        if (validLessons.size != lessons.size) {
            logger.warning("Filtered out ${lessons.size - validLessons.size} invalid lessons")
        }
        
        val entities = validLessons.map { lesson ->
            LessonEntity.fromDomainModel(lesson.copy(
                title = sanitizeLessonContent(lesson.title),
                description = sanitizeLessonContent(lesson.description)
            ))
        }
        
        lessonDao.insertLessons(entities)
        logger.info("Cached ${entities.size} valid lessons")
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
        val content = LessonContent(
            introduction = ContentSection(
                id = "intro",
                title = "Introduction",
                content = mobileLesson.mobileContent.introduction.content,
                contentType = ContentType.TEXT,
                estimatedDuration = mobileLesson.mobileContent.introduction.estimatedDuration
            ),
            mainContent = mobileLesson.mobileContent.mainSections.map { section ->
                ContentSection(
                    id = section.id,
                    title = section.title,
                    content = section.content,
                    contentType = ContentType.valueOf(section.contentType.name),
                    estimatedDuration = section.estimatedDuration
                )
            },
            conclusion = ContentSection(
                id = "conclusion",
                title = "Conclusion",
                content = mobileLesson.mobileContent.conclusion.content,
                contentType = ContentType.TEXT,
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
        // Convert Lesson to MobileLesson for API response
        // This is a simplified conversion - would need full implementation
        return com.lifechurch.heroesinwaiting.data.api.response.MobileLesson(
            id = lesson.id,
            title = lesson.title,
            description = lesson.description,
            lessonNumber = lesson.lessonNumber,
            estimatedDuration = lesson.estimatedDuration,
            category = lesson.category,
            difficultyLevel = lesson.difficultyLevel,
            targetGrades = lesson.targetGrades,
            mobileContent = com.lifechurch.heroesinwaiting.data.api.response.MobileLessonContent(
                introduction = com.lifechurch.heroesinwaiting.data.api.response.MobileContentSection(
                    id = "intro",
                    title = "Introduction",
                    content = lesson.content.introduction.content,
                    contentType = com.lifechurch.heroesinwaiting.data.api.response.ContentType.TEXT,
                    estimatedDuration = lesson.content.introduction.estimatedDuration
                ),
                mainSections = lesson.content.mainContent.map { section ->
                    com.lifechurch.heroesinwaiting.data.api.response.MobileContentSection(
                        id = section.id,
                        title = section.title,
                        content = section.content,
                        contentType = com.lifechurch.heroesinwaiting.data.api.response.ContentType.valueOf(section.contentType.name),
                        estimatedDuration = section.estimatedDuration
                    )
                },
                conclusion = com.lifechurch.heroesinwaiting.data.api.response.MobileContentSection(
                    id = "conclusion",
                    title = "Conclusion",
                    content = lesson.content.conclusion.content,
                    contentType = com.lifechurch.heroesinwaiting.data.api.response.ContentType.TEXT,
                    estimatedDuration = lesson.content.conclusion.estimatedDuration
                ),
                quickSummary = lesson.description
            ),
            interactiveElements = emptyList(),
            checkpoints = emptyList()
        )
    }
}

/**
 * Download progress states
 */
sealed class DownloadProgress {
    object Starting : DownloadProgress()
    data class Downloading(val progress: Int) : DownloadProgress()
    object Completed : DownloadProgress()
    data class Failed(val error: String) : DownloadProgress()
}

/**
 * Offline lesson content structure
 */
data class OfflineLessonContent(
    val lessonId: String,
    val metadata: String,
    val content: String,
    val resourcePaths: List<String>,
    val downloadedAt: Long
)