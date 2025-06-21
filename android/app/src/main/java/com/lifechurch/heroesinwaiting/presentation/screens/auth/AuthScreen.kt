package com.lifechurch.heroesinwaiting.presentation.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifechurch.heroesinwaiting.presentation.components.HeroesLargeButton
import com.lifechurch.heroesinwaiting.presentation.components.HeroesCard
import com.lifechurch.heroesinwaiting.presentation.viewmodel.AuthViewModel
import com.lifechurch.heroesinwaiting.presentation.viewmodel.AuthEvent
import com.lifechurch.heroesinwaiting.presentation.theme.StudentEngagementTextStyle

/**
 * Main authentication screen with dual paths:
 * 1. Facilitator login/registration
 * 2. Student classroom enrollment
 */
@Composable
fun AuthScreen(
    onNavigateToFacilitatorDashboard: () -> Unit,
    onNavigateToStudentDashboard: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var selectedPath by remember { mutableStateOf<AuthPath?>(null) }
    
    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AuthEvent.NavigateToFacilitatorDashboard -> onNavigateToFacilitatorDashboard()
                AuthEvent.NavigateToStudentDashboard -> onNavigateToStudentDashboard()
                else -> {}
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Hero section
        Text(
            text = "Heroes in Waiting",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Building Character, Preventing Bullying",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        when (selectedPath) {
            null -> {
                // Path selection
                PathSelectionContent(
                    onSelectFacilitator = { selectedPath = AuthPath.FACILITATOR },
                    onSelectStudent = { selectedPath = AuthPath.STUDENT }
                )
            }
            AuthPath.FACILITATOR -> {
                FacilitatorAuthContent(
                    onBack = { selectedPath = null },
                    viewModel = viewModel
                )
            }
            AuthPath.STUDENT -> {
                StudentEnrollmentContent(
                    onBack = { selectedPath = null },
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun PathSelectionContent(
    onSelectFacilitator: () -> Unit,
    onSelectStudent: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Who are you?",
            style = StudentEngagementTextStyle,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        // Facilitator option
        HeroesCard(
            onClick = onSelectFacilitator,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "I'm a Teacher/Facilitator",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Manage classrooms and deliver lessons",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Student option
        HeroesCard(
            onClick = onSelectStudent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "I'm a Student",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Join a classroom with a code",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private enum class AuthPath {
    FACILITATOR,
    STUDENT
}