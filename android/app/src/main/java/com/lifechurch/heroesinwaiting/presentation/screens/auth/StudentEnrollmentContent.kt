package com.lifechurch.heroesinwaiting.presentation.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifechurch.heroesinwaiting.data.model.*
import com.lifechurch.heroesinwaiting.presentation.components.*
import com.lifechurch.heroesinwaiting.presentation.theme.HeroesSpacing
import com.lifechurch.heroesinwaiting.presentation.theme.StudentEngagementTextStyle
import com.lifechurch.heroesinwaiting.presentation.viewmodel.AuthViewModel

@Composable
fun StudentEnrollmentContent(
    onBack: () -> Unit,
    viewModel: AuthViewModel
) {
    var classroomCode by remember { mutableStateOf("") }
    var selectedGrade by remember { mutableStateOf<Grade?>(null) }
    var selectedSchoolType by remember { mutableStateOf<SchoolType?>(null) }
    var showPreview by remember { mutableStateOf(false) }
    
    val uiState by viewModel.uiState.collectAsState()
    val classroomPreview by viewModel.classroomPreview.collectAsState()
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            HeroesSecondaryButton(
                text = "← Back",
                onClick = {
                    if (showPreview) {
                        showPreview = false
                        viewModel.clearClassroomPreview()
                    } else {
                        onBack()
                    }
                },
                modifier = Modifier.width(120.dp)
            )
        }
        
        if (!showPreview) {
            // Step 1: Enter classroom code and basic info
            EnrollmentFormContent(
                classroomCode = classroomCode,
                onClassroomCodeChange = { classroomCode = it },
                selectedGrade = selectedGrade,
                onGradeSelect = { selectedGrade = it },
                selectedSchoolType = selectedSchoolType,
                onSchoolTypeSelect = { selectedSchoolType = it },
                onPreviewClassroom = {
                    if (viewModel.isValidClassroomCode(classroomCode)) {
                        viewModel.previewClassroom(classroomCode)
                        showPreview = true
                    }
                },
                canPreview = viewModel.isValidClassroomCode(classroomCode) && selectedGrade != null,
                isLoading = uiState.isLoading,
                error = uiState.error,
                onClearError = { viewModel.clearError() }
            )
        } else {
            // Step 2: Show classroom preview and confirm enrollment
            ClassroomPreviewContent(
                preview = classroomPreview,
                onConfirmEnrollment = {
                    selectedGrade?.let { grade ->
                        val demographicInfo = DemographicInfo(
                            grade = grade,
                            schoolType = selectedSchoolType
                        )
                        viewModel.enrollStudent(classroomCode, grade, demographicInfo)
                    }
                },
                isLoading = uiState.isLoading,
                error = uiState.error,
                onClearError = { viewModel.clearError() }
            )
        }
    }
}

@Composable
private fun EnrollmentFormContent(
    classroomCode: String,
    onClassroomCodeChange: (String) -> Unit,
    selectedGrade: Grade?,
    onGradeSelect: (Grade) -> Unit,
    selectedSchoolType: SchoolType?,
    onSchoolTypeSelect: (SchoolType) -> Unit,
    onPreviewClassroom: () -> Unit,
    canPreview: Boolean,
    isLoading: Boolean,
    error: String?,
    onClearError: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Join Your Classroom",
            style = StudentEngagementTextStyle,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        
        // Show error if any
        error?.let { errorMessage ->
            HeroesErrorDisplay(
                message = errorMessage,
                onRetry = onClearError
            )
        }
        
        // Classroom code input
        HeroesTextField(
            value = classroomCode,
            onValueChange = { newCode ->
                // Only allow digits and limit to 6 characters
                if (newCode.all { it.isDigit() } && newCode.length <= 6) {
                    onClassroomCodeChange(newCode)
                }
            },
            label = "Classroom Code",
            placeholder = "123456",
            isError = classroomCode.isNotEmpty() && classroomCode.length != 6,
            errorMessage = if (classroomCode.isNotEmpty() && classroomCode.length != 6) {
                "Classroom code must be 6 digits"
            } else null
        )
        
        // Grade selection
        HeroesSectionHeader("What grade are you in?")
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Grade.GRADE_4, Grade.GRADE_5, Grade.GRADE_6).forEach { grade ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedGrade == grade,
                            onClick = { onGradeSelect(grade) }
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedGrade == grade,
                        onClick = { onGradeSelect(grade) }
                    )
                    HeroesHorizontalSpacer(HeroesSpacing.Small)
                    Text(
                        text = grade.displayName,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        
        // School type (optional)
        HeroesSectionHeader("What type of school do you go to? (Optional)")
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                SchoolType.PUBLIC,
                SchoolType.PRIVATE,
                SchoolType.CHARTER,
                SchoolType.HOMESCHOOL
            ).forEach { schoolType ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedSchoolType == schoolType,
                            onClick = { onSchoolTypeSelect(schoolType) }
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedSchoolType == schoolType,
                        onClick = { onSchoolTypeSelect(schoolType) }
                    )
                    HeroesHorizontalSpacer(HeroesSpacing.Small)
                    Text(
                        text = schoolType.displayName,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        
        // Preview button
        HeroesLargeButton(
            text = "Find My Classroom",
            onClick = onPreviewClassroom,
            enabled = canPreview,
            isLoading = isLoading
        )
    }
}

@Composable
private fun ClassroomPreviewContent(
    preview: com.lifechurch.heroesinwaiting.data.api.response.ClassroomPreviewResponse?,
    onConfirmEnrollment: () -> Unit,
    isLoading: Boolean,
    error: String?,
    onClearError: () -> Unit
) {
    if (preview == null && !isLoading) {
        HeroesErrorDisplay(
            message = "Could not find classroom. Please check your code.",
            onRetry = onClearError
        )
        return
    }
    
    if (isLoading) {
        HeroesLoadingIndicator(message = "Finding your classroom...")
        return
    }
    
    preview?.let { classroomPreview ->
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Is this your classroom?",
                style = StudentEngagementTextStyle,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            
            // Show error if any
            error?.let { errorMessage ->
                HeroesErrorDisplay(
                    message = errorMessage,
                    onRetry = onClearError
                )
            }
            
            // Classroom info card
            HeroesCard {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = classroomPreview.classroom.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = "Teacher: ${classroomPreview.facilitator.displayName}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Text(
                        text = "Grade: ${classroomPreview.classroom.grade.displayName}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Text(
                        text = "Students: ${classroomPreview.classroom.currentStudentCount}/${classroomPreview.classroom.maxStudents}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    classroomPreview.classroom.description?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Enrollment status
            if (!classroomPreview.canJoin) {
                HeroesErrorDisplay(
                    message = "This classroom is full or not accepting new students."
                )
            } else {
                if (classroomPreview.requiresWaitingRoom) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Text(
                            text = "You'll need to wait for your teacher to approve your request to join.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                HeroesLargeButton(
                    text = if (classroomPreview.requiresWaitingRoom) {
                        "Request to Join Classroom"
                    } else {
                        "Join Classroom"
                    },
                    onClick = onConfirmEnrollment,
                    enabled = classroomPreview.canJoin,
                    isLoading = isLoading
                )
            }
        }
    }
}