package com.lifechurch.heroesinwaiting.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifechurch.heroesinwaiting.data.model.*
import com.lifechurch.heroesinwaiting.data.repository.LessonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Lesson Selection Screen
 * Manages lesson loading, search, filtering, and navigation
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
    
    // Events
    private val _events = MutableSharedFlow<LessonSelectionEvent>()
    val events: SharedFlow<LessonSelectionEvent> = _events.asSharedFlow()
    
    // Base lessons from repository
    private val allLessons = lessonRepository.getAllLessons()
        .catch { exception ->
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = exception.message ?: "Failed to load lessons"
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
     * Load lessons from repository
     */
    private fun loadLessons() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // Trigger sync if needed
            lessonRepository.syncLessons().fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                },
                onFailure = { exception ->
                    // Don't show error if we have cached lessons
                    val hasLessons = allLessons.value.isNotEmpty()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = if (hasLessons) null else exception.message
                    )
                }
            )
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
     * Retries loading lessons
     */
    fun retryLoadLessons() {
        loadLessons()
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
}

/**
 * UI state for Lesson Selection screen
 */
data class LessonSelectionUiState(
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val showErrorState: Boolean
        get() = !isLoading && error != null
        
    val showLoadingState: Boolean
        get() = isLoading
}

/**
 * Events for Lesson Selection screen
 */
sealed class LessonSelectionEvent {
    data class NavigateToLessonDetail(val lessonId: String) : LessonSelectionEvent()
}