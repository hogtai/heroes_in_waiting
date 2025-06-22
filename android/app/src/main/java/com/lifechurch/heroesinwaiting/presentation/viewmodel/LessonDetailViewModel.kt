package com.lifechurch.heroesinwaiting.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifechurch.heroesinwaiting.data.model.*
import com.lifechurch.heroesinwaiting.data.repository.LessonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Lesson Detail Screen
 * Manages lesson loading, tabbed content, download/bookmark functionality
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
    
    // Events
    private val _events = MutableSharedFlow<LessonDetailEvent>()
    val events: SharedFlow<LessonDetailEvent> = _events.asSharedFlow()
    
    // Lesson data from repository
    val lesson: StateFlow<Lesson?> = lessonRepository.getLessonById(lessonId)
        .catch { exception ->
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = exception.message ?: "Failed to load lesson"
            )
        }
        .onStart {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        }
        .onEach { lesson ->
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = if (lesson == null) "Lesson not found" else null
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
     * Load lesson details
     */
    fun loadLesson(lessonId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val lesson = lessonRepository.getLessonById(lessonId)
                _uiState.value = _uiState.value.copy(
                    lesson = lesson,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load lesson"
                )
            }
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
     * Downloads lesson for offline use
     */
    fun downloadLesson() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloading = true)
            
            try {
                lessonRepository.downloadLesson(lessonId)
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    lesson = _uiState.value.lesson?.copy(isDownloaded = true)
                )
                _events.emit(LessonDetailEvent.LessonDownloaded)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    error = "Failed to download lesson: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Removes lesson from offline storage
     */
    fun removeDownload() {
        viewModelScope.launch {
            try {
                lessonRepository.removeLessonFromOfflineStorage(lessonId)
                _uiState.value = _uiState.value.copy(lesson = _uiState.value.lesson?.copy(isDownloaded = false))
                _events.emit(LessonDetailEvent.LessonDownloadRemoved)
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to remove download: ${exception.message}"
                )
            }
        }
    }
    
    /**
     * Bookmarks the lesson
     */
    fun toggleBookmark() {
        val currentLesson = _uiState.value.lesson ?: return
        
        viewModelScope.launch {
            try {
                val updatedLesson = currentLesson.copy(isBookmarked = !currentLesson.isBookmarked)
                lessonRepository.updateLessonBookmark(currentLesson.id, updatedLesson.isBookmarked)
                _uiState.value = _uiState.value.copy(lesson = updatedLesson)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to update bookmark: ${e.message}")
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
     * Retries loading lesson
     */
    fun retryLoadLesson() {
        loadLesson(lessonId)
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
}

/**
 * UI state for Lesson Detail screen
 */
data class LessonDetailUiState(
    val lesson: Lesson? = null,
    val selectedTab: LessonDetailTab = LessonDetailTab.OVERVIEW,
    val isLoading: Boolean = false,
    val isDownloading: Boolean = false,
    val isDownloaded: Boolean = false,
    val isBookmarked: Boolean = false,
    val error: String? = null
) {
    val showErrorState: Boolean
        get() = !isLoading && error != null
        
    val showLoadingState: Boolean
        get() = isLoading
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
 * Events for Lesson Detail screen
 */
sealed class LessonDetailEvent {
    data class StartLesson(val lessonId: String) : LessonDetailEvent()
    object NavigateBack : LessonDetailEvent()
    object LessonDownloaded : LessonDetailEvent()
    object LessonDownloadRemoved : LessonDetailEvent()
    object LessonBookmarked : LessonDetailEvent()
    object LessonUnbookmarked : LessonDetailEvent()
}