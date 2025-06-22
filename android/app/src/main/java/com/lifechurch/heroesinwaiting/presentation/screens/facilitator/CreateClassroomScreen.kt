package com.lifechurch.heroesinwaiting.presentation.screens.facilitator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifechurch.heroesinwaiting.data.model.Grade
import com.lifechurch.heroesinwaiting.presentation.components.*
import com.lifechurch.heroesinwaiting.presentation.theme.*
import com.lifechurch.heroesinwaiting.presentation.viewmodel.CreateClassroomViewModel
import com.lifechurch.heroesinwaiting.presentation.viewmodel.CreateClassroomEvent

/**
 * Create Classroom Screen for facilitators
 * Professional form interface for creating new classrooms
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateClassroomScreen(
    onNavigateBack: () -> Unit,
    onClassroomCreated: () -> Unit,
    viewModel: CreateClassroomViewModel = hiltViewModel()
) {
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    
    // Collect state from ViewModel
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val name by viewModel.name.collectAsStateWithLifecycle()
    val description by viewModel.description.collectAsStateWithLifecycle()
    val grade by viewModel.grade.collectAsStateWithLifecycle()
    val capacity by viewModel.capacity.collectAsStateWithLifecycle()
    
    // Collect validation errors
    val nameError by viewModel.nameError.collectAsStateWithLifecycle()
    val descriptionError by viewModel.descriptionError.collectAsStateWithLifecycle()
    val gradeError by viewModel.gradeError.collectAsStateWithLifecycle()
    val capacityError by viewModel.capacityError.collectAsStateWithLifecycle()
    
    val isFormValid by viewModel.isFormValid.collectAsStateWithLifecycle()
    
    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CreateClassroomEvent.ClassroomCreated -> {
                    onClassroomCreated()
                }
                is CreateClassroomEvent.NavigateBack -> {
                    onNavigateBack()
                }
            }
        }
    }
    
    // Handle back navigation with unsaved changes check
    val handleBackNavigation = {
        if (viewModel.hasUnsavedChanges()) {
            showUnsavedChangesDialog = true
        } else {
            onNavigateBack()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Create Classroom",
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = handleBackNavigation) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        
        Box(modifier = Modifier.fillMaxSize()) {
            // Main content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    CreateClassroomForm(
                        name = name,
                        description = description,
                        grade = grade,
                        capacity = capacity,
                        nameError = nameError,
                        descriptionError = descriptionError,
                        gradeError = gradeError,
                        capacityError = capacityError,
                        isFormValid = isFormValid,
                        isLoading = uiState.isLoading,
                        onNameChange = viewModel::updateName,
                        onDescriptionChange = viewModel::updateDescription,
                        onGradeChange = viewModel::updateGrade,
                        onCapacityChange = viewModel::updateCapacity,
                        onCreateClassroom = viewModel::createClassroom,
                        onCancel = handleBackNavigation,
                        gradeOptions = viewModel.getGradeOptions()
                    )
                }
            }
            
            // Loading overlay
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    HeroesLoadingIndicator(message = "Creating your classroom...")
                }
            }
            
            // Error display
            if (uiState.showErrorState) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    HeroesErrorDisplay(
                        message = uiState.error ?: "Failed to create classroom",
                        onRetry = { viewModel.createClassroom() }
                    )
                }
            }
        }
    }
    
    // Unsaved changes confirmation dialog
    if (showUnsavedChangesDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedChangesDialog = false },
            title = { Text("Unsaved Changes") },
            text = { Text("You have unsaved changes. Are you sure you want to leave?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnsavedChangesDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedChangesDialog = false }) {
                    Text("Stay")
                }
            }
        )
    }
}

@Composable
private fun CreateClassroomForm(
    name: String,
    description: String,
    grade: Grade?,
    capacity: Int,
    nameError: String?,
    descriptionError: String?,
    gradeError: String?,
    capacityError: String?,
    isFormValid: Boolean,
    isLoading: Boolean,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onGradeChange: (Grade?) -> Unit,
    onCapacityChange: (Int) -> Unit,
    onCreateClassroom: () -> Unit,
    onCancel: () -> Unit,
    gradeOptions: List<Grade>
) {
    HeroesCard {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Groups,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "New Classroom Details",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Text(
                text = "Create a new classroom for your Heroes in Waiting lessons. All fields marked with * are required.",
                style = FacilitatorProfessionalTextStyle,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            HeroesVerticalSpacer(HeroesSpacing.Medium)
            
            // Classroom Name Field (Required)
            HeroesTextField(
                value = name,
                onValueChange = onNameChange,
                label = "Classroom Name *",
                placeholder = "Enter a name for your classroom",
                isError = nameError != null,
                errorMessage = nameError,
                leadingIcon = Icons.Default.Groups
            )
            
            // Description Field (Optional)
            HeroesTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = "Description (Optional)",
                placeholder = "Brief description of your classroom or program",
                isError = descriptionError != null,
                errorMessage = descriptionError
            )
            
            // Grade Level Dropdown (Required)
            GradeDropdownField(
                selectedGrade = grade,
                onGradeSelected = onGradeChange,
                gradeOptions = gradeOptions,
                isError = gradeError != null,
                errorMessage = gradeError
            )
            
            // Student Capacity Field (Required)
            StudentCapacityField(
                capacity = capacity,
                onCapacityChange = onCapacityChange,
                isError = capacityError != null,
                errorMessage = capacityError
            )
            
            HeroesVerticalSpacer(HeroesSpacing.Large)
            
            // Action Buttons
            CreateClassroomActions(
                isFormValid = isFormValid,
                isLoading = isLoading,
                onCreateClassroom = onCreateClassroom,
                onCancel = onCancel
            )
        }
    }
}

@Composable
private fun GradeDropdownField(
    selectedGrade: Grade?,
    onGradeSelected: (Grade?) -> Unit,
    gradeOptions: List<Grade>,
    isError: Boolean,
    errorMessage: String?
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedGrade?.displayName ?: "",
                onValueChange = { },
                readOnly = true,
                label = { 
                    Text(
                        text = "Grade Level *",
                        fontSize = 16.sp
                    ) 
                },
                placeholder = { 
                    Text(
                        text = "Select grade level",
                        fontSize = 16.sp
                    ) 
                },
                trailingIcon = {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                isError = isError,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .menuAnchor(),
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    errorBorderColor = MaterialTheme.colorScheme.error
                )
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                gradeOptions.forEach { grade ->
                    DropdownMenuItem(
                        text = { 
                            Text(
                                text = grade.displayName,
                                style = MaterialTheme.typography.bodyLarge
                            ) 
                        },
                        onClick = {
                            onGradeSelected(grade)
                            expanded = false
                        }
                    )
                }
            }
        }
        
        if (isError && errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@Composable
private fun StudentCapacityField(
    capacity: Int,
    onCapacityChange: (Int) -> Unit,
    isError: Boolean,
    errorMessage: String?
) {
    Column {
        OutlinedTextField(
            value = capacity.toString(),
            onValueChange = { newValue ->
                newValue.toIntOrNull()?.let { onCapacityChange(it) }
            },
            label = {
                Text(
                    text = "Student Capacity *",
                    fontSize = 16.sp
                )
            },
            placeholder = {
                Text(
                    text = "Enter maximum students (1-50)",
                    fontSize = 16.sp
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.People,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            },
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
            shape = RoundedCornerShape(12.dp),
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.error
            )
        )
        
        if (isError && errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@Composable
private fun CreateClassroomActions(
    isFormValid: Boolean,
    isLoading: Boolean,
    onCreateClassroom: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Primary action - Create Classroom
        HeroesLargeButton(
            text = "Create Classroom",
            onClick = onCreateClassroom,
            enabled = isFormValid && !isLoading,
            isLoading = isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        )
        
        // Secondary action - Cancel
        HeroesSecondaryButton(
            text = "Cancel",
            onClick = onCancel,
            enabled = !isLoading
        )
    }
}