package com.lifechurch.heroesinwaiting.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifechurch.heroesinwaiting.data.model.*
import com.lifechurch.heroesinwaiting.data.repository.AuthRepository
import com.lifechurch.heroesinwaiting.data.api.response.ClassroomPreviewResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for authentication handling dual auth flows:
 * 1. Facilitator authentication with JWT
 * 2. Student authentication with classroom codes
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    // Authentication state
    val isLoggedIn = authRepository.isLoggedIn().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
    
    val userType = authRepository.getUserType().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    
    // UI state for forms
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    // Classroom preview state
    private val _classroomPreview = MutableStateFlow<ClassroomPreviewResponse?>(null)
    val classroomPreview: StateFlow<ClassroomPreviewResponse?> = _classroomPreview.asStateFlow()
    
    // Events
    private val _events = MutableSharedFlow<AuthEvent>()
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()
    
    /**
     * Facilitator login
     */
    fun loginFacilitator(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val result = authRepository.loginFacilitator(email, password)
            
            result.fold(
                onSuccess = { authResponse ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _events.emit(AuthEvent.NavigateToFacilitatorDashboard)
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Login failed"
                    )
                }
            )
        }
    }
    
    /**
     * Facilitator registration
     */
    fun registerFacilitator(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        organization: String? = null,
        role: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val result = authRepository.registerFacilitator(
                email, password, firstName, lastName, organization, role
            )
            
            result.fold(
                onSuccess = { authResponse ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _events.emit(AuthEvent.NavigateToFacilitatorDashboard)
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Registration failed"
                    )
                }
            )
        }
    }
    
    /**
     * Preview classroom before student enrollment
     */
    fun previewClassroom(classroomCode: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val result = authRepository.previewClassroom(classroomCode)
            
            result.fold(
                onSuccess = { preview ->
                    _classroomPreview.value = preview
                    _uiState.value = _uiState.value.copy(isLoading = false)
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Classroom not found"
                    )
                }
            )
        }
    }
    
    /**
     * Enroll student in classroom
     */
    fun enrollStudent(
        classroomCode: String,
        grade: Grade,
        demographicInfo: DemographicInfo
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val result = authRepository.enrollStudent(classroomCode, grade, demographicInfo)
            
            result.fold(
                onSuccess = { enrollmentResponse ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _events.emit(AuthEvent.NavigateToStudentDashboard)
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Enrollment failed"
                    )
                }
            )
        }
    }
    
    /**
     * Logout current user
     */
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = AuthUiState() // Reset UI state
            _classroomPreview.value = null
            _events.emit(AuthEvent.NavigateToAuth)
        }
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Clear classroom preview
     */
    fun clearClassroomPreview() {
        _classroomPreview.value = null
    }
    
    /**
     * Validate email format
     */
    fun isValidEmail(email: String): Boolean {
        return UserUtils.isValidEmail(email)
    }
    
    /**
     * Validate password strength
     */
    fun isValidPassword(password: String): Boolean {
        return UserUtils.isValidPassword(password)
    }
    
    /**
     * Validate classroom code format
     */
    fun isValidClassroomCode(code: String): Boolean {
        return code.length == 6 && code.all { it.isDigit() }
    }
}

/**
 * UI state for authentication screens
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Authentication events
 */
sealed class AuthEvent {
    object NavigateToFacilitatorDashboard : AuthEvent()
    object NavigateToStudentDashboard : AuthEvent()
    object NavigateToAuth : AuthEvent()
}