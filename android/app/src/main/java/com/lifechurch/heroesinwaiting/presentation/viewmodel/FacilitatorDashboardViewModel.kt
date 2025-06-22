package com.lifechurch.heroesinwaiting.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifechurch.heroesinwaiting.data.model.Classroom
import com.lifechurch.heroesinwaiting.data.repository.ClassroomRepository
import com.lifechurch.heroesinwaiting.data.repository.FacilitatorDashboardStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Facilitator Dashboard Screen
 * Manages classroom data, statistics, and user interactions
 */
@HiltViewModel
class FacilitatorDashboardViewModel @Inject constructor(
    private val classroomRepository: ClassroomRepository
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow(FacilitatorDashboardUiState())
    val uiState: StateFlow<FacilitatorDashboardUiState> = _uiState.asStateFlow()
    
    // Classrooms data
    private val _classrooms = MutableStateFlow<List<Classroom>>(emptyList())
    val classrooms: StateFlow<List<Classroom>> = _classrooms.asStateFlow()
    
    // Dashboard statistics
    private val _dashboardStats = MutableStateFlow<FacilitatorDashboardStats?>(null)
    val dashboardStats: StateFlow<FacilitatorDashboardStats?> = _dashboardStats.asStateFlow()
    
    // Events
    private val _events = MutableSharedFlow<FacilitatorDashboardEvent>()
    val events: SharedFlow<FacilitatorDashboardEvent> = _events.asSharedFlow()
    
    init {
        loadDashboardData()
    }
    
    /**
     * Loads all dashboard data (classrooms and statistics)
     */
    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Load classrooms
                classroomRepository.getFacilitatorClassrooms()
                    .catch { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load classrooms"
                        )
                    }
                    .collect { classroomList ->
                        _classrooms.value = classroomList
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                
                // Load dashboard statistics
                loadDashboardStats()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load dashboard data"
                )
            }
        }
    }
    
    /**
     * Loads dashboard statistics
     */
    fun loadDashboardStats() {
        viewModelScope.launch {
            val result = classroomRepository.getFacilitatorDashboardStats()
            result.fold(
                onSuccess = { stats ->
                    _dashboardStats.value = stats
                },
                onFailure = { exception ->
                    // Don't show error for stats failure, just use empty stats
                    _dashboardStats.value = FacilitatorDashboardStats(
                        totalClassrooms = _classrooms.value.size,
                        activeClassrooms = _classrooms.value.count { it.isActive },
                        totalStudents = _classrooms.value.sumOf { it.currentStudentCount },
                        activeSessions = _classrooms.value.count { it.hasActiveSession },
                        completedLessons = 0,
                        lastActivity = System.currentTimeMillis().toString()
                    )
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
     * Navigates to create classroom screen
     */
    fun navigateToCreateClassroom() {
        viewModelScope.launch {
            _events.emit(FacilitatorDashboardEvent.NavigateToCreateClassroom)
        }
    }
    
    /**
     * Navigates to lessons screen
     */
    fun navigateToLessons() {
        viewModelScope.launch {
            _events.emit(FacilitatorDashboardEvent.NavigateToLessons)
        }
    }
    
    /**
     * Navigates to analytics screen
     */
    fun navigateToAnalytics() {
        viewModelScope.launch {
            _events.emit(FacilitatorDashboardEvent.NavigateToAnalytics)
        }
    }
    
    /**
     * Handles classroom selection
     */
    fun onClassroomSelected(classroom: Classroom) {
        viewModelScope.launch {
            _events.emit(FacilitatorDashboardEvent.NavigateToClassroomDetails(classroom.id))
        }
    }
    
    /**
     * Creates a new classroom
     */
    fun createClassroom(
        name: String,
        description: String?,
        grade: com.lifechurch.heroesinwaiting.data.model.Grade,
        maxStudents: Int = 30
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingClassroom = true, error = null)
            
            val result = classroomRepository.createClassroom(name, description, grade, maxStudents)
            
            result.fold(
                onSuccess = { classroom ->
                    _uiState.value = _uiState.value.copy(isCreatingClassroom = false)
                    _events.emit(FacilitatorDashboardEvent.ClassroomCreated(classroom))
                    // Refresh data to show new classroom
                    loadDashboardData()
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isCreatingClassroom = false,
                        error = exception.message ?: "Failed to create classroom"
                    )
                }
            )
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
     * Gets formatted classroom count text
     */
    fun getClassroomCountText(): String {
        val count = _classrooms.value.size
        return when (count) {
            0 -> "No classrooms yet"
            1 -> "1 classroom"
            else -> "$count classrooms"
        }
    }
    
    /**
     * Gets formatted student count text
     */
    fun getStudentCountText(): String {
        val count = _dashboardStats.value?.totalStudents ?: 0
        return when (count) {
            0 -> "No students enrolled"
            1 -> "1 student enrolled"
            else -> "$count students enrolled"
        }
    }
    
    /**
     * Checks if facilitator has any classrooms
     */
    fun hasClassrooms(): Boolean {
        return _classrooms.value.isNotEmpty()
    }
    
    /**
     * Checks if facilitator has any active sessions
     */
    fun hasActiveSessions(): Boolean {
        return _dashboardStats.value?.activeSessions ?: 0 > 0
    }
    
    /**
     * Gets welcome message based on time of day
     */
    fun getWelcomeMessage(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Good morning!"
            hour < 17 -> "Good afternoon!"
            else -> "Good evening!"
        }
    }
}

/**
 * UI state for Facilitator Dashboard
 */
data class FacilitatorDashboardUiState(
    val isLoading: Boolean = false,
    val isCreatingClassroom: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false
) {
    val showEmptyState: Boolean
        get() = !isLoading && error == null
        
    val showErrorState: Boolean
        get() = !isLoading && error != null
}

/**
 * Events for Facilitator Dashboard
 */
sealed class FacilitatorDashboardEvent {
    object NavigateToCreateClassroom : FacilitatorDashboardEvent()
    object NavigateToLessons : FacilitatorDashboardEvent()
    object NavigateToAnalytics : FacilitatorDashboardEvent()
    data class NavigateToClassroomDetails(val classroomId: String) : FacilitatorDashboardEvent()
    data class ClassroomCreated(val classroom: Classroom) : FacilitatorDashboardEvent()
}