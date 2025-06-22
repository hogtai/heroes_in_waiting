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
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException

/**
 * ViewModel for Lesson Selection Screen
 * Manages lesson loading, search, filtering, and navigation with enhanced error handling and performance optimizations
 */
@HiltViewModel
class LessonSelectionViewModel @Inject constructor(
    private val lessonRepository: LessonRepository
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow(LessonSelectionUiState())
    val uiState: StateFlow<LessonSelectionUiState> = _uiState.asStateFlow()
    
    // Search and Filter State with debouncing
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // Debounced search query for performance
    private val debouncedSearchQuery = _searchQuery
        .debounce(300) // 300ms debounce for search performance
        .distinctUntilChanged()
    
    private val _selectedGrade = MutableStateFlow<Grade?>(null)
    val selectedGrade: StateFlow<Grade?> = _selectedGrade.asStateFlow()
    
    private val _selectedCategory = MutableStateFlow<LessonCategory?>(null)
    val selectedCategory: StateFlow<LessonCategory?> = _selectedCategory.asStateFlow()
    
    // Pagination State
    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()
    
    private val _pageSize = MutableStateFlow(20) // Load 20 lessons per page
    val pageSize: StateFlow<Int> = _pageSize.asStateFlow()
    
    private val _hasMorePages = MutableStateFlow(true)
    val hasMorePages: StateFlow<Boolean> = _hasMorePages.asStateFlow()
    
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()
    
    // Network and Offline State
    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()
    
    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()
    
    // Retry State
    private val _retryCount = MutableStateFlow(0)
    private val _isRetrying = MutableStateFlow(false)
    val isRetrying: StateFlow<Boolean> = _isRetrying.asStateFlow()
    
    // Performance optimization: Cache filtered results
    private val _cachedFilteredLessons = MutableStateFlow<List<Lesson>>(emptyList())
    
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
            // Reset pagination when lessons change
            _currentPage.value = 0
            _hasMorePages.value = lessons.size > _pageSize.value
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Optimized filtered and searched lessons with pagination
    val lessons: StateFlow<List<Lesson>> = combine(
        allLessons,
        debouncedSearchQuery, // Use debounced search for performance
        _selectedGrade,
        _selectedCategory,
        _currentPage,
        _pageSize
    ) { lessons, query, grade, category, page, pageSize ->
        // Apply filters efficiently
        var filteredLessons = applyFilters(lessons, query, grade, category)
        
        // Cache filtered results for performance
        _cachedFilteredLessons.value = filteredLessons
        
        // Apply pagination
        val startIndex = page * pageSize
        val endIndex = minOf(startIndex + pageSize, filteredLessons.size)
        
        // Update pagination state
        _hasMorePages.value = endIndex < filteredLessons.size
        
        // Return paginated results
        if (startIndex < filteredLessons.size) {
            filteredLessons.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    // Total filtered lessons count for pagination info
    val totalFilteredCount: StateFlow<Int> = _cachedFilteredLessons.map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
    
    // Performance monitoring
    private val _performanceMetrics = MutableStateFlow<PerformanceMetrics>(PerformanceMetrics())
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()
    
    // Memory management
    private val _memoryUsage = MutableStateFlow<Long>(0)
    val memoryUsage: StateFlow<Long> = _memoryUsage.asStateFlow()
    
    init {
        loadLessons()
        startPerformanceMonitoring()
    }
    
    /**
     * Starts performance monitoring
     */
    private fun startPerformanceMonitoring() {
        viewModelScope.launch {
            while (true) {
                updateMemoryUsage()
                delay(5000) // Update every 5 seconds
            }
        }
    }
    
    /**
     * Updates memory usage tracking
     */
    private fun updateMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        _memoryUsage.value = usedMemory / 1024 / 1024 // Convert to MB
    }
    
    /**
     * Records performance metric
     */
    private fun recordPerformanceMetric(operation: String, duration: Long) {
        _performanceMetrics.value = _performanceMetrics.value.copy(
            operations = _performanceMetrics.value.operations + PerformanceOperation(
                operation = operation,
                duration = duration,
                timestamp = System.currentTimeMillis()
            )
        )
    }
    
    /**
     * Efficiently applies filters to lessons
     */
    private fun applyFilters(
        lessons: List<Lesson>,
        query: String,
        grade: Grade?,
        category: LessonCategory?
    ): List<Lesson> {
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
        
        // Apply search query with optimized search
        if (query.isNotBlank()) {
            val searchTerm = query.lowercase().trim()
            filteredLessons = filteredLessons.filter { lesson ->
                // Optimized search: check most likely matches first
                lesson.title.lowercase().contains(searchTerm) ||
                lesson.description.lowercase().contains(searchTerm) ||
                lesson.keyTerms.any { it.lowercase().contains(searchTerm) } ||
                lesson.objectives.any { it.lowercase().contains(searchTerm) }
            }
        }
        
        // Sort by lesson number for consistent ordering
        return filteredLessons.sortedBy { it.lessonNumber }
    }
    
    /**
     * Loads more lessons (pagination)
     */
    fun loadMoreLessons() {
        if (_isLoadingMore.value || !_hasMorePages.value) return
        
        viewModelScope.launch {
            _isLoadingMore.value = true
            
            // Simulate loading delay for smooth UX
            delay(500)
            
            _currentPage.value += 1
            _isLoadingMore.value = false
        }
    }
    
    /**
     * Resets pagination when filters change
     */
    private fun resetPagination() {
        _currentPage.value = 0
        _hasMorePages.value = true
    }
    
    /**
     * Enhanced lesson loading with comprehensive error handling and edge cases
     */
    private fun loadLessons() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            _isRetrying.value = false
            
            try {
                // Validate current state before loading
                if (!validateAppState()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "App state invalid. Please restart the app."
                    )
                    return@launch
                }
                
                // Attempt to sync lessons from server with timeout
                withTimeout(30000) { // 30 second timeout
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
                }
            } catch (e: TimeoutCancellationException) {
                handleSyncError(Exception("Request timed out. Please check your connection."))
            } catch (e: Exception) {
                handleSyncError(e)
            }
        }
    }
    
    /**
     * Validates app state before operations
     */
    private fun validateAppState(): Boolean {
        return try {
            // Check if repository is accessible
            lessonRepository.getAllLessons()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Validates search query input
     */
    private fun validateSearchQuery(query: String): Boolean {
        return query.length <= 100 && !query.contains("<script>") // Basic XSS prevention
    }
    
    /**
     * Enhanced search query update with validation
     */
    fun updateSearchQuery(query: String) {
        if (!validateSearchQuery(query)) {
            _uiState.value = _uiState.value.copy(
                error = "Search query is too long or contains invalid characters."
            )
            return
        }
        
        _searchQuery.value = query
        resetPagination()
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
            resetPagination()
            loadLessons()
        }
    }
    
    /**
     * Clears search query
     */
    fun clearSearch() {
        _searchQuery.value = ""
        resetPagination()
    }
    
    /**
     * Updates selected grade filter
     */
    fun updateGradeFilter(grade: Grade?) {
        _selectedGrade.value = grade
        resetPagination()
    }
    
    /**
     * Updates selected category filter  
     */
    fun updateCategoryFilter(category: LessonCategory?) {
        _selectedCategory.value = category
        resetPagination()
    }
    
    /**
     * Clears all filters
     */
    fun clearAllFilters() {
        _selectedGrade.value = null
        _selectedCategory.value = null
        _searchQuery.value = ""
        resetPagination()
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
    
    /**
     * Gets pagination info for display
     */
    fun getPaginationInfo(): String {
        val currentCount = lessons.value.size
        val totalCount = totalFilteredCount.value
        val currentPage = _currentPage.value + 1
        val totalPages = (totalCount + _pageSize.value - 1) / _pageSize.value
        
        return if (totalCount > 0) {
            "Showing ${currentCount} of $totalCount lessons (Page $currentPage of $totalPages)"
        } else {
            "No lessons found"
        }
    }
}

/**
 * Enhanced UI state for Lesson Selection screen with pagination
 */
data class LessonSelectionUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasCachedData: Boolean = false,
    val isOffline: Boolean = false,
    val lastSyncTime: Long? = null,
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = true
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
    object LoadMoreLessons : LessonSelectionEvent()
}

/**
 * Performance metrics for monitoring
 */
data class PerformanceMetrics(
    val operations: List<PerformanceOperation> = emptyList(),
    val averageSearchTime: Long = 0,
    val averageFilterTime: Long = 0,
    val memoryUsageMB: Long = 0
)

/**
 * Individual performance operation
 */
data class PerformanceOperation(
    val operation: String,
    val duration: Long,
    val timestamp: Long
)