package com.lifechurch.heroesinwaiting.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifechurch.heroesinwaiting.data.model.Classroom
import com.lifechurch.heroesinwaiting.data.model.Grade
import com.lifechurch.heroesinwaiting.data.repository.ClassroomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Create Classroom Screen
 * Manages form state, validation, and classroom creation
 */
@HiltViewModel
class CreateClassroomViewModel @Inject constructor(
    private val classroomRepository: ClassroomRepository
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow(CreateClassroomUiState())
    val uiState: StateFlow<CreateClassroomUiState> = _uiState.asStateFlow()
    
    // Form fields
    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()
    
    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()
    
    private val _grade = MutableStateFlow<Grade?>(null)
    val grade: StateFlow<Grade?> = _grade.asStateFlow()
    
    private val _capacity = MutableStateFlow(30)
    val capacity: StateFlow<Int> = _capacity.asStateFlow()
    
    // Form validation
    private val _nameError = MutableStateFlow<String?>(null)
    val nameError: StateFlow<String?> = _nameError.asStateFlow()
    
    private val _descriptionError = MutableStateFlow<String?>(null)
    val descriptionError: StateFlow<String?> = _descriptionError.asStateFlow()
    
    private val _gradeError = MutableStateFlow<String?>(null)
    val gradeError: StateFlow<String?> = _gradeError.asStateFlow()
    
    private val _capacityError = MutableStateFlow<String?>(null)
    val capacityError: StateFlow<String?> = _capacityError.asStateFlow()
    
    // Events
    private val _events = MutableSharedFlow<CreateClassroomEvent>()
    val events: SharedFlow<CreateClassroomEvent> = _events.asSharedFlow()
    
    // Computed properties
    val isFormValid: StateFlow<Boolean> = combine(
        _name,
        _grade,
        _capacity,
        _nameError,
        _gradeError,
        _capacityError
    ) { name, grade, capacity, nameError, gradeError, capacityError ->
        name.isNotBlank() && 
        grade != null && 
        capacity in 1..50 && 
        nameError == null && 
        gradeError == null && 
        capacityError == null
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
    
    /**
     * Updates classroom name and validates it
     */
    fun updateName(newName: String) {
        _name.value = newName
        validateName(newName)
    }
    
    /**
     * Updates classroom description
     */
    fun updateDescription(newDescription: String) {
        _description.value = newDescription
        validateDescription(newDescription)
    }
    
    /**
     * Updates selected grade and validates it
     */
    fun updateGrade(newGrade: Grade?) {
        _grade.value = newGrade
        validateGrade(newGrade)
    }
    
    /**
     * Updates classroom capacity and validates it
     */
    fun updateCapacity(newCapacity: Int) {
        _capacity.value = newCapacity
        validateCapacity(newCapacity)
    }
    
    /**
     * Creates a new classroom with current form data
     */
    fun createClassroom() {
        if (!isFormValid.value) {
            validateAllFields()
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val result = classroomRepository.createClassroom(
                name = _name.value.trim(),
                description = _description.value.trim().takeIf { it.isNotBlank() },
                grade = _grade.value!!,
                maxStudents = _capacity.value
            )
            
            result.fold(
                onSuccess = { classroom ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _events.emit(CreateClassroomEvent.ClassroomCreated(classroom))
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to create classroom"
                    )
                }
            )
        }
    }
    
    /**
     * Validates classroom name
     */
    private fun validateName(name: String) {
        _nameError.value = when {
            name.isBlank() -> "Classroom name is required"
            name.length > 100 -> "Classroom name must be 100 characters or less"
            else -> null
        }
    }
    
    /**
     * Validates classroom description
     */
    private fun validateDescription(description: String) {
        _descriptionError.value = when {
            description.length > 500 -> "Description must be 500 characters or less"
            else -> null
        }
    }
    
    /**
     * Validates selected grade
     */
    private fun validateGrade(grade: Grade?) {
        _gradeError.value = when (grade) {
            null -> "Please select a grade level"
            else -> null
        }
    }
    
    /**
     * Validates classroom capacity
     */
    private fun validateCapacity(capacity: Int) {
        _capacityError.value = when {
            capacity < 1 -> "Capacity must be at least 1 student"
            capacity > 50 -> "Capacity cannot exceed 50 students"
            else -> null
        }
    }
    
    /**
     * Validates all form fields
     */
    private fun validateAllFields() {
        validateName(_name.value)
        validateDescription(_description.value)
        validateGrade(_grade.value)
        validateCapacity(_capacity.value)
    }
    
    /**
     * Clears all validation errors
     */
    fun clearErrors() {
        _nameError.value = null
        _descriptionError.value = null
        _gradeError.value = null
        _capacityError.value = null
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Resets form to initial state
     */
    fun resetForm() {
        _name.value = ""
        _description.value = ""
        _grade.value = null
        _capacity.value = 30
        clearErrors()
        _uiState.value = CreateClassroomUiState()
    }
    
    /**
     * Handles navigation back
     */
    fun navigateBack() {
        viewModelScope.launch {
            _events.emit(CreateClassroomEvent.NavigateBack)
        }
    }
    
    /**
     * Gets available grade options
     */
    fun getGradeOptions(): List<Grade> {
        return listOf(Grade.GRADE_4, Grade.GRADE_5, Grade.GRADE_6, Grade.OTHER)
    }
    
    /**
     * Gets capacity range for validation
     */
    fun getCapacityRange(): IntRange {
        return 1..50
    }
    
    /**
     * Checks if form has unsaved changes
     */
    fun hasUnsavedChanges(): Boolean {
        return _name.value.isNotBlank() || 
               _description.value.isNotBlank() || 
               _grade.value != null || 
               _capacity.value != 30
    }
}

/**
 * UI state for Create Classroom screen
 */
data class CreateClassroomUiState(
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val showErrorState: Boolean
        get() = !isLoading && error != null
}

/**
 * Events for Create Classroom screen
 */
sealed class CreateClassroomEvent {
    data class ClassroomCreated(val classroom: Classroom) : CreateClassroomEvent()
    object NavigateBack : CreateClassroomEvent()
}