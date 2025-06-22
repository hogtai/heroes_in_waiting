package com.lifechurch.heroesinwaiting.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifechurch.heroesinwaiting.data.model.*
import com.lifechurch.heroesinwaiting.data.repository.LessonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.net.UnknownHostException
import java.net.SocketTimeoutException
import javax.inject.Inject

/**
 * ViewModel for Lesson Detail Screen
 * Manages lesson loading, tabbed content, download/bookmark functionality with enhanced error handling
 */
@HiltViewModel
class LessonDetailViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    // Extract lesson ID from navigation arguments
    private val lessonId: String = checkNotNull(savedStateHandle["lessonId"])
    
    // UI State
    private val _uiState = MutableStateFlow(LessonDetailUiState())
    val uiState: StateFlow<LessonDetailUiState> = _uiState.asStateFlow()
    
    // Tab State
    private val _selectedTab = MutableStateFlow(LessonDetailTab.OVERVIEW)
    val selectedTab: StateFlow<LessonDetailTab> = _selectedTab.asStateFlow()
    
    // Network and Offline State
    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()
    
    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()
    
    // Retry State
    private val _retryCount = MutableStateFlow(0)
    private val _isRetrying = MutableStateFlow(false)
    val isRetrying: StateFlow<Boolean> = _isRetrying.asStateFlow()
    
    // Events
    private val _events = MutableSharedFlow<LessonDetailEvent>()
    val events: SharedFlow<LessonDetailEvent> = _events.asSharedFlow()
    
    // Lesson data from repository with enhanced error handling
    val lesson: StateFlow<Lesson?> = lessonRepository.getLessonById(lessonId)
        .catch { exception ->
            handleLessonLoadError(exception)
        }
        .onStart {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        }
        .onEach { lesson ->
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = if (lesson == null) "Lesson not found" else null,
                hasCachedData = lesson != null
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    init {
        loadLesson(lessonId)
    }
    
    /**
     * Enhanced lesson loading with comprehensive error handling
     */
    fun loadLesson(lessonId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            _isRetrying.value = false
            
            try {
                // Attempt to sync lesson from server if needed
                lessonRepository.syncLessons().fold(
                    onSuccess = {
                        _isOffline.value = false
                        _lastSyncTime.value = System.currentTimeMillis()
                        _retryCount.value = 0
                    },
                    onFailure = { exception ->
                        handleSyncError(exception)
                    }
                )
            } catch (e: Exception) {
                handleSyncError(e)
            }
        }
    }
    
    /**
     * Handles lesson loading errors with specific error types
     */
    private suspend fun handleLessonLoadError(exception: Throwable) {
        val errorMessage = when (exception) {
            is UnknownHostException -> "No internet connection. Using cached lesson."
            is SocketTimeoutException -> "Connection timed out. Using cached lesson."
            is SecurityException -> "Authentication required. Please log in again."
            else -> "Failed to load lesson: ${exception.message}"
        }
        
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = errorMessage,
            hasCachedData = lesson.value != null
        )
        
        // Set offline state if appropriate
        if (exception is UnknownHostException || exception is SocketTimeoutException) {
            _isOffline.value = true
        }
    }
    
    /**
     * Handles sync errors with retry logic
     */
    private suspend fun handleSyncError(exception: Throwable) {
        val hasCachedLesson = lesson.value != null
        
        when (exception) {
            is UnknownHostException -> {
                _isOffline.value = true
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = if (hasCachedLesson) {
                        "Offline mode. Using cached lesson."
                    } else {
                        "No internet connection. Please check your connection and try again."
                    },
                    hasCachedData = hasCachedLesson
                )
            }
            is SocketTimeoutException -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = if (hasCachedLesson) {
                        "Connection slow. Using cached lesson."
                    } else {
                        "Connection timed out. Please try again."
                    },
                    hasCachedData = hasCachedLesson
                )
            }
            is SecurityException -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Authentication required. Please log in again.",
                    hasCachedData = hasCachedLesson
                )
                _events.emit(LessonDetailEvent.AuthenticationRequired)
            }
            else -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = if (hasCachedLesson) {
                        "Unable to sync. Using cached lesson."
                    } else {
                        "Failed to load lesson: ${exception.message}"
                    },
                    hasCachedData = hasCachedLesson
                )
            }
        }
    }
    
    /**
     * Retries loading lesson with exponential backoff
     */
    fun retryLoadLesson() {
        viewModelScope.launch {
            _isRetrying.value = true
            _retryCount.value += 1
            
            // Exponential backoff: 1s, 2s, 4s, 8s, max 10s
            val backoffDelay = minOf(1000L * (1 shl (_retryCount.value - 1)), 10000L)
            delay(backoffDelay)
            
            loadLesson(lessonId)
        }
    }
    
    /**
     * Forces a fresh sync from server
     */
    fun forceRefresh() {
        viewModelScope.launch {
            _retryCount.value = 0
            _isOffline.value = false
            loadLesson(lessonId)
        }
    }
    
    /**
     * Updates selected tab
     */
    fun setSelectedTab(tab: LessonDetailTab) {
        _selectedTab.value = tab
    }
    
    /**
     * Starts the lesson
     */
    fun startLesson() {
        viewModelScope.launch {
            lesson.value?.let { currentLesson ->
                _events.emit(LessonDetailEvent.StartLesson(currentLesson.id))
            }
        }
    }
    
    /**
     * Enhanced download functionality with error handling and progress tracking
     */
    fun downloadLesson() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloading = true)
            
            try {
                // Use enhanced download functionality
                lessonRepository.downloadLessonForOffline(lessonId).fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isDownloading = false,
                            lesson = _uiState.value.lesson?.copy(isDownloaded = true),
                            error = null
                        )
                        _events.emit(LessonDetailEvent.LessonDownloaded)
                    },
                    onFailure = { exception ->
                        val errorMessage = when (exception) {
                            is UnknownHostException -> "Cannot download while offline. Please check your connection."
                            is SocketTimeoutException -> "Download timed out. Please try again."
                            else -> "Failed to download lesson: ${exception.message}"
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            isDownloading = false,
                            error = errorMessage
                        )
                    }
                )
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is UnknownHostException -> "Cannot download while offline. Please check your connection."
                    is SocketTimeoutException -> "Download timed out. Please try again."
                    else -> "Failed to download lesson: ${e.message}"
                }
                
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    error = errorMessage
                )
            }
        }
    }
    
    /**
     * Gets download progress for this lesson
     */
    fun getDownloadProgress(): DownloadProgress? {
        return lessonRepository.getDownloadProgress(lessonId)
    }
    
    /**
     * Checks if lesson is available offline
     */
    suspend fun isLessonAvailableOffline(): Boolean {
        return lessonRepository.isLessonAvailableOffline(lessonId)
    }
    
    /**
     * Enhanced remove download functionality with error handling
     */
    fun removeDownload() {
        viewModelScope.launch {
            try {
                lessonRepository.removeLessonFromOfflineStorage(lessonId)
                _uiState.value = _uiState.value.copy(
                    lesson = _uiState.value.lesson?.copy(isDownloaded = false),
                    error = null
                )
                _events.emit(LessonDetailEvent.LessonDownloadRemoved)
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to remove download: ${exception.message}"
                )
            }
        }
    }
    
    /**
     * Enhanced bookmark functionality with error handling
     */
    fun toggleBookmark() {
        val currentLesson = _uiState.value.lesson ?: return
        
        viewModelScope.launch {
            try {
                val updatedLesson = currentLesson.copy(isBookmarked = !currentLesson.isBookmarked)
                lessonRepository.updateLessonBookmark(currentLesson.id, updatedLesson.isBookmarked)
                _uiState.value = _uiState.value.copy(
                    lesson = updatedLesson,
                    error = null
                )
                
                if (updatedLesson.isBookmarked) {
                    _events.emit(LessonDetailEvent.LessonBookmarked)
                } else {
                    _events.emit(LessonDetailEvent.LessonUnbookmarked)
                }
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is UnknownHostException -> "Cannot update bookmark while offline."
                    is SocketTimeoutException -> "Bookmark update timed out. Please try again."
                    else -> "Failed to update bookmark: ${e.message}"
                }
                
                _uiState.value = _uiState.value.copy(error = errorMessage)
            }
        }
    }
    
    /**
     * Navigates back to lesson selection
     */
    fun navigateBack() {
        viewModelScope.launch {
            _events.emit(LessonDetailEvent.NavigateBack)
        }
    }
    
    /**
     * Clears error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Gets tab content based on selected tab and lesson data
     */
    fun getTabContent(): LessonTabContent? {
        val currentLesson = lesson.value ?: return null
        
        return when (_selectedTab.value) {
            LessonDetailTab.OVERVIEW -> LessonTabContent.Overview(
                objectives = currentLesson.objectives,
                keyTerms = currentLesson.keyTerms,
                estimatedDuration = currentLesson.estimatedDuration,
                difficultyLevel = currentLesson.difficultyLevel,
                targetGrades = currentLesson.targetGrades,
                category = currentLesson.category,
                description = currentLesson.description
            )
            
            LessonDetailTab.MATERIALS -> LessonTabContent.Materials(
                resources = currentLesson.content.resources,
                handouts = currentLesson.content.handouts,
                activities = currentLesson.activities
            )
            
            LessonDetailTab.FACILITATOR_GUIDE -> LessonTabContent.FacilitatorGuide(
                facilitatorNotes = currentLesson.content.facilitatorNotes,
                introduction = currentLesson.content.introduction,
                mainContent = currentLesson.content.mainContent,
                conclusion = currentLesson.content.conclusion
            )
        }
    }
    
    /**
     * Gets available tabs
     */
    fun getAvailableTabs(): List<LessonDetailTab> {
        return LessonDetailTab.values().toList()
    }
    
    /**
     * Gets offline status message
     */
    fun getOfflineStatusMessage(): String? {
        return if (_isOffline.value) {
            "Offline mode - using cached lesson"
        } else {
            null
        }
    }
    
    /**
     * Gets last sync time as formatted string
     */
    fun getLastSyncTimeFormatted(): String? {
        return _lastSyncTime.value?.let { timestamp ->
            val minutesAgo = (System.currentTimeMillis() - timestamp) / (1000 * 60)
            when {
                minutesAgo < 1 -> "Just now"
                minutesAgo < 60 -> "$minutesAgo minutes ago"
                minutesAgo < 1440 -> "${minutesAgo / 60} hours ago"
                else -> "${minutesAgo / 1440} days ago"
            }
        }
    }
}

/**
 * Enhanced UI state for Lesson Detail screen
 */
data class LessonDetailUiState(
    val lesson: Lesson? = null,
    val selectedTab: LessonDetailTab = LessonDetailTab.OVERVIEW,
    val isLoading: Boolean = false,
    val isDownloading: Boolean = false,
    val isDownloaded: Boolean = false,
    val isBookmarked: Boolean = false,
    val error: String? = null,
    val hasCachedData: Boolean = false,
    val isOffline: Boolean = false,
    val lastSyncTime: Long? = null
) {
    val showErrorState: Boolean
        get() = !isLoading && error != null
        
    val showLoadingState: Boolean
        get() = isLoading
        
    val showOfflineIndicator: Boolean
        get() = isOffline && hasCachedData
        
    val showNoDataState: Boolean
        get() = !isLoading && error == null && !hasCachedData
}

/**
 * Available tabs for lesson detail screen
 */
enum class LessonDetailTab(val displayName: String) {
    OVERVIEW("Overview"),
    MATERIALS("Materials"),
    FACILITATOR_GUIDE("Facilitator Guide")
}

/**
 * Content for different tabs
 */
sealed class LessonTabContent {
    data class Overview(
        val objectives: List<String>,
        val keyTerms: List<String>,
        val estimatedDuration: Int,
        val difficultyLevel: DifficultyLevel,
        val targetGrades: List<Grade>,
        val category: LessonCategory,
        val description: String
    ) : LessonTabContent()
    
    data class Materials(
        val resources: List<Resource>,
        val handouts: List<Resource>,
        val activities: List<Activity>
    ) : LessonTabContent()
    
    data class FacilitatorGuide(
        val facilitatorNotes: String?,
        val introduction: ContentSection,
        val mainContent: List<ContentSection>,
        val conclusion: ContentSection
    ) : LessonTabContent()
}

/**
 * Enhanced events for Lesson Detail screen
 */
sealed class LessonDetailEvent {
    data class StartLesson(val lessonId: String) : LessonDetailEvent()
    object NavigateBack : LessonDetailEvent()
    object LessonDownloaded : LessonDetailEvent()
    object LessonDownloadRemoved : LessonDetailEvent()
    object LessonBookmarked : LessonDetailEvent()
    object LessonUnbookmarked : LessonDetailEvent()
    object AuthenticationRequired : LessonDetailEvent()
    object NetworkError : LessonDetailEvent()
    object SyncCompleted : LessonDetailEvent()
}