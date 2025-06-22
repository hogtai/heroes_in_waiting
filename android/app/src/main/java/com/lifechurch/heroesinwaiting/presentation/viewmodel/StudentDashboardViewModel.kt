package com.lifechurch.heroesinwaiting.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifechurch.heroesinwaiting.data.model.Classroom
import com.lifechurch.heroesinwaiting.data.repository.ClassroomRepository
import com.lifechurch.heroesinwaiting.data.repository.StudentProgressRepository
import com.lifechurch.heroesinwaiting.data.repository.StudentDashboardStats
import com.lifechurch.heroesinwaiting.data.repository.CurrentLessonInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Student Dashboard Screen
 * Manages student progress, classroom info, and engagement features
 */
@HiltViewModel
class StudentDashboardViewModel @Inject constructor(
    private val studentProgressRepository: StudentProgressRepository,
    private val classroomRepository: ClassroomRepository
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow(StudentDashboardUiState())
    val uiState: StateFlow<StudentDashboardUiState> = _uiState.asStateFlow()
    
    // Student's classroom
    private val _classroom = MutableStateFlow<Classroom?>(null)
    val classroom: StateFlow<Classroom?> = _classroom.asStateFlow()
    
    // Dashboard statistics
    private val _dashboardStats = MutableStateFlow<StudentDashboardStats?>(null)
    val dashboardStats: StateFlow<StudentDashboardStats?> = _dashboardStats.asStateFlow()
    
    // Current lesson information
    private val _currentLesson = MutableStateFlow<CurrentLessonInfo?>(null)
    val currentLesson: StateFlow<CurrentLessonInfo?> = _currentLesson.asStateFlow()
    
    // Events
    private val _events = MutableSharedFlow<StudentDashboardEvent>()
    val events: SharedFlow<StudentDashboardEvent> = _events.asSharedFlow()
    
    init {
        loadDashboardData()
    }
    
    /**
     * Loads all dashboard data
     */
    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Load student's classroom
                classroomRepository.getStudentClassroom()
                    .catch { exception ->
                        _uiState.value = _uiState.value.copy(
                            error = exception.message ?: "Failed to load classroom"
                        )
                    }
                    .collect { classroom ->
                        _classroom.value = classroom
                    }
                
                // Load dashboard statistics
                studentProgressRepository.getStudentDashboardStats()
                    .catch { exception ->
                        _uiState.value = _uiState.value.copy(
                            error = exception.message ?: "Failed to load progress"
                        )
                    }
                    .collect { stats ->
                        _dashboardStats.value = stats
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                
                // Load current lesson information
                loadCurrentLesson()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load dashboard data"
                )
            }
        }
    }
    
    /**
     * Loads current lesson information
     */
    private fun loadCurrentLesson() {
        viewModelScope.launch {
            val result = studentProgressRepository.getCurrentLesson()
            result.fold(
                onSuccess = { lessonInfo ->
                    _currentLesson.value = lessonInfo
                },
                onFailure = { exception ->
                    // Don't show error for current lesson failure
                    _currentLesson.value = null
                }
            )
        }
    }
    
    /**
     * Refreshes dashboard data
     */
    fun refreshDashboard() {
        loadDashboardData()
    }
    
    /**
     * Navigates to emotional check-in
     */
    fun navigateToEmotionalCheckin() {
        viewModelScope.launch {
            _events.emit(StudentDashboardEvent.NavigateToEmotionalCheckin)
        }
    }
    
    /**
     * Navigates to current lesson
     */
    fun navigateToCurrentLesson() {
        val lesson = _currentLesson.value
        if (lesson != null) {
            viewModelScope.launch {
                _events.emit(StudentDashboardEvent.NavigateToLesson(lesson.lessonId))
            }
        }
    }
    
    /**
     * Navigates to help screen
     */
    fun navigateToHelp() {
        viewModelScope.launch {
            _events.emit(StudentDashboardEvent.NavigateToHelp)
        }
    }
    
    /**
     * Handles leaving classroom
     */
    fun leaveClassroom() {
        viewModelScope.launch {
            _events.emit(StudentDashboardEvent.LeaveClassroom)
        }
    }
    
    /**
     * Records emotional check-in completion
     */
    fun recordEmotionalCheckin(emotion: String, notes: String? = null) {
        viewModelScope.launch {
            // This would typically call a repository method to record the check-in
            // For now, just emit a success event
            _events.emit(StudentDashboardEvent.EmotionalCheckinCompleted)
        }
    }
    
    /**
     * Clears error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Retries failed operation
     */
    fun retry() {
        clearError()
        loadDashboardData()
    }
    
    /**
     * Gets formatted progress text
     */
    fun getProgressText(): String {
        val stats = _dashboardStats.value
        return if (stats != null) {
            "${stats.completedLessons} of ${stats.totalAvailableLessons} lessons completed"
        } else {
            "Loading progress..."
        }
    }
    
    /**
     * Gets formatted hero points text
     */
    fun getHeroPointsText(): String {
        val points = _dashboardStats.value?.heroPoints ?: 0
        return when (points) {
            0 -> "Start your journey to earn Hero Points!"
            1 -> "1 Hero Point earned"
            else -> "$points Hero Points earned"
        }
    }
    
    /**
     * Gets encouraging message based on progress
     */
    fun getEncouragementMessage(): String {
        val stats = _dashboardStats.value ?: return "Welcome to Heroes in Waiting!"
        
        return when {
            stats.completedLessons == 0 -> "Ready to start your heroes journey?"
            stats.completedLessons < 3 -> "Great start! Keep going, hero!"
            stats.completedLessons < 5 -> "You're doing amazing! Heroes never give up!"
            stats.progressPercentage >= 50f -> "Wow! You're more than halfway there!"
            else -> "Every hero faces challenges - you've got this!"
        }
    }
    
    /**
     * Gets classroom welcome message
     */
    fun getClassroomWelcomeMessage(): String {
        val classroom = _classroom.value
        return if (classroom != null) {
            "Welcome to ${classroom.name}!"
        } else {
            "Welcome back, Hero!"
        }
    }
    
    /**
     * Checks if student has completed any lessons
     */
    fun hasCompletedLessons(): Boolean {
        return _dashboardStats.value?.completedLessons ?: 0 > 0
    }
    
    /**
     * Checks if student has current lesson
     */
    fun hasCurrentLesson(): Boolean {
        return _currentLesson.value != null
    }
    
    /**
     * Checks if student is in a classroom
     */
    fun isInClassroom(): Boolean {
        return _classroom.value != null
    }
    
    /**
     * Gets time-based greeting
     */
    fun getTimeBasedGreeting(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    
    /**
     * Gets progress percentage for display
     */
    fun getProgressPercentage(): Float {
        return _dashboardStats.value?.progressPercentage ?: 0f
    }
    
    /**
     * Gets formatted time spent learning
     */
    fun getTimeSpentText(): String {
        val stats = _dashboardStats.value
        return if (stats != null && stats.timeSpentInMinutes > 0) {
            "${stats.timeSpentInMinutes} minutes learning"
        } else {
            "Start learning to track your time!"
        }
    }
}

/**
 * UI state for Student Dashboard
 */
data class StudentDashboardUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false,
    val hasCompletedEmotionalCheckin: Boolean = false
) {
    val showEmptyState: Boolean
        get() = !isLoading && error == null
        
    val showErrorState: Boolean
        get() = !isLoading && error != null
}

/**
 * Events for Student Dashboard
 */
sealed class StudentDashboardEvent {
    object NavigateToEmotionalCheckin : StudentDashboardEvent()
    object NavigateToHelp : StudentDashboardEvent()
    object LeaveClassroom : StudentDashboardEvent()
    object EmotionalCheckinCompleted : StudentDashboardEvent()
    data class NavigateToLesson(val lessonId: String) : StudentDashboardEvent()
}