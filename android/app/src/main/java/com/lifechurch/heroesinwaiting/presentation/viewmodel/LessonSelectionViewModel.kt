package com.lifechurch.heroesinwaiting.presentation.viewmodel

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
 * ViewModel for Lesson Selection Screen
 * Manages lesson loading, search, filtering, and navigation with enhanced error handling
 */
@HiltViewModel
class LessonSelectionViewModel @Inject constructor(
    private val lessonRepository: LessonRepository
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow(LessonSelectionUiState())
    val uiState: StateFlow<LessonSelectionUiState> = _uiState.asStateFlow()
    
    // Search and Filter State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _selectedGrade = MutableStateFlow<Grade?>(null)
    val selectedGrade: StateFlow<Grade?> = _selectedGrade.asStateFlow()
    
    private val _selectedCategory = MutableStateFlow<LessonCategory?>(null)
    val selectedCategory: StateFlow<LessonCategory?> = _selectedCategory.asStateFlow()
    
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
    private val _events = MutableSharedFlow<LessonSelectionEvent>()
    val events: SharedFlow<LessonSelectionEvent> = _events.asSharedFlow()
    
    // Base lessons from repository with enhanced error handling
    private val allLessons = lessonRepository.getAllLessons()
        .catch { exception ->
            handleLessonLoadError(exception)
        }
        .onStart {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        }
        .onEach { lessons ->
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = null,
                hasCachedData = lessons.isNotEmpty()
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Filtered and searched lessons
    val lessons: StateFlow<List<Lesson>> = combine(
        allLessons,
        _searchQuery,
        _selectedGrade,
        _selectedCategory
    ) { lessons, query, grade, category ->
        var filteredLessons = lessons
        
        // Apply grade filter
        if (grade != null) {
            filteredLessons = filteredLessons.filter { lesson ->
                lesson.targetGrades.contains(grade)
            }
        }
        
        // Apply category filter
        if (category != null) {
            filteredLessons = filteredLessons.filter { lesson ->
                lesson.category == category
            }
        }
        
        // Apply search query
        if (query.isNotBlank()) {
            val searchTerm = query.lowercase().trim()
            filteredLessons = filteredLessons.filter { lesson ->
                lesson.title.lowercase().contains(searchTerm) ||
                lesson.description.lowercase().contains(searchTerm) ||
                lesson.keyTerms.any { it.lowercase().contains(searchTerm) } ||
                lesson.objectives.any { it.lowercase().contains(searchTerm) }
            }
        }
        
        // Sort by lesson number for consistent ordering
        filteredLessons.sortedBy { it.lessonNumber }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    init {
        loadLessons()
    }
    
    /**
     * Enhanced lesson loading with comprehensive error handling
     */
    private fun loadLessons() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            _isRetrying.value = false
            
            try {
                // Attempt to sync lessons from server
                lessonRepository.syncLessons().fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(isLoading = false)
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
            is UnknownHostException -> "No internet connection. Using cached lessons."
            is SocketTimeoutException -> "Connection timed out. Using cached lessons."
            is SecurityException -> "Authentication required. Please log in again."
            else -> "Failed to load lessons: ${exception.message}"
        }
        
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = errorMessage,
            hasCachedData = allLessons.value.isNotEmpty()
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
        val hasCachedLessons = allLessons.value.isNotEmpty()
        
        when (exception) {
            is UnknownHostException -> {
                _isOffline.value = true
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = if (hasCachedLessons) {
                        "Offline mode. Using cached lessons."
                    } else {
                        "No internet connection. Please check your connection and try again."
                    },
                    hasCachedData = hasCachedLessons
                )
            }
            is SocketTimeoutException -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = if (hasCachedLessons) {
                        "Connection slow. Using cached lessons."
                    } else {
                        "Connection timed out. Please try again."
                    },
                    hasCachedData = hasCachedLessons
                )
            }
            is SecurityException -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Authentication required. Please log in again.",
                    hasCachedData = hasCachedLessons
                )
                _events.emit(LessonSelectionEvent.AuthenticationRequired)
            }
            else -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = if (hasCachedLessons) {
                        "Unable to sync. Using cached lessons."
                    } else {
                        "Failed to load lessons: ${exception.message}"
                    },
                    hasCachedData = hasCachedLessons
                )
            }
        }
    }
    
    /**
     * Retries loading lessons with exponential backoff
     */
    fun retryLoadLessons() {
        viewModelScope.launch {
            _isRetrying.value = true
            _retryCount.value += 1
            
            // Exponential backoff: 1s, 2s, 4s, 8s, max 10s
            val backoffDelay = minOf(1000L * (1 shl (_retryCount.value - 1)), 10000L)
            delay(backoffDelay)
            
            loadLessons()
        }
    }
    
    /**
     * Forces a fresh sync from server
     */
    fun forceRefresh() {
        viewModelScope.launch {
            _retryCount.value = 0
            _isOffline.value = false
            loadLessons()
        }
    }
    
    /**
     * Updates search query
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * Clears search query
     */
    fun clearSearch() {
        _searchQuery.value = ""
    }
    
    /**
     * Updates selected grade filter
     */
    fun updateGradeFilter(grade: Grade?) {
        _selectedGrade.value = grade
    }
    
    /**
     * Updates selected category filter  
     */
    fun updateCategoryFilter(category: LessonCategory?) {
        _selectedCategory.value = category
    }
    
    /**
     * Clears all filters
     */
    fun clearAllFilters() {
        _selectedGrade.value = null
        _selectedCategory.value = null
        _searchQuery.value = ""
    }
    
    /**
     * Navigates to lesson details
     */
    fun navigateToLessonDetail(lessonId: String) {
        viewModelScope.launch {
            _events.emit(LessonSelectionEvent.NavigateToLessonDetail(lessonId))
        }
    }
    
    /**
     * Gets available grade options for filtering
     */
    fun getGradeOptions(): List<Grade> {
        return listOf(Grade.GRADE_4, Grade.GRADE_5, Grade.GRADE_6, Grade.OTHER)
    }
    
    /**
     * Gets available category options for filtering
     */
    fun getCategoryOptions(): List<LessonCategory> {
        return LessonCategory.values().toList()
    }
    
    /**
     * Checks if any filters are active
     */
    fun hasActiveFilters(): Boolean {
        return _selectedGrade.value != null || 
               _selectedCategory.value != null || 
               _searchQuery.value.isNotBlank()
    }
    
    /**
     * Gets filter summary text
     */
    fun getFilterSummary(): String {
        val parts = mutableListOf<String>()
        
        _selectedGrade.value?.let { grade ->
            parts.add(grade.displayName)
        }
        
        _selectedCategory.value?.let { category ->
            parts.add(category.displayName)
        }
        
        if (_searchQuery.value.isNotBlank()) {
            parts.add("\"${_searchQuery.value}\"")
        }
        
        return when {
            parts.isEmpty() -> "All Lessons"
            parts.size == 1 -> parts.first()
            else -> parts.joinToString(" • ")
        }
    }
    
    /**
     * Gets offline status message
     */
    fun getOfflineStatusMessage(): String? {
        return if (_isOffline.value) {
            "Offline mode - using cached lessons"
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
 * Enhanced UI state for Lesson Selection screen
 */
data class LessonSelectionUiState(
    val isLoading: Boolean = false,
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
 * Enhanced events for Lesson Selection screen
 */
sealed class LessonSelectionEvent {
    data class NavigateToLessonDetail(val lessonId: String) : LessonSelectionEvent()
    object AuthenticationRequired : LessonSelectionEvent()
    object NetworkError : LessonSelectionEvent()
    object SyncCompleted : LessonSelectionEvent()
}