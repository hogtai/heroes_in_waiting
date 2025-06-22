package com.lifechurch.heroesinwaiting.presentation.screens.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoodBad
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifechurch.heroesinwaiting.presentation.components.*
import com.lifechurch.heroesinwaiting.presentation.theme.*
import com.lifechurch.heroesinwaiting.presentation.viewmodel.AuthViewModel
import com.lifechurch.heroesinwaiting.presentation.viewmodel.StudentDashboardViewModel
import com.lifechurch.heroesinwaiting.presentation.viewmodel.StudentDashboardEvent

/**
 * Student dashboard with age-appropriate design
 * Large, colorful, and engaging interface for elementary students
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(
    onLeaveClassroom: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToEmotionalCheckin: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    dashboardViewModel: StudentDashboardViewModel = hiltViewModel()
) {
    var showLeaveDialog by remember { mutableStateOf(false) }
    
    // Collect state from ViewModel
    val uiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val classroom by dashboardViewModel.classroom.collectAsStateWithLifecycle()
    val dashboardStats by dashboardViewModel.dashboardStats.collectAsStateWithLifecycle()
    val currentLesson by dashboardViewModel.currentLesson.collectAsStateWithLifecycle()
    
    // Handle events
    LaunchedEffect(Unit) {
        dashboardViewModel.events.collect { event ->
            when (event) {
                is StudentDashboardEvent.NavigateToEmotionalCheckin -> onNavigateToEmotionalCheckin()
                is StudentDashboardEvent.NavigateToHelp -> onNavigateToHelp()
                is StudentDashboardEvent.LeaveClassroom -> onLeaveClassroom()
                is StudentDashboardEvent.NavigateToLesson -> {
                    // Handle lesson navigation
                }
                is StudentDashboardEvent.EmotionalCheckinCompleted -> {
                    // Handle emotional check-in completion
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "My Heroes Journey",
                        style = StudentEngagementTextStyle,
                        color = MaterialTheme.colorScheme.onPrimary
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            StudentBottomNavigation(
                onLeaveClassroom = { showLeaveDialog = true },
                onNavigateToHelp = onNavigateToHelp
            )
        }
    ) { paddingValues ->
        
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                HeroesLoadingIndicator(message = "Loading your heroes journey...")
            }
        } else if (uiState.showErrorState) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                HeroesErrorDisplay(
                    message = uiState.error ?: "Something went wrong",
                    onRetry = { dashboardViewModel.retry() }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    WelcomeBackSection(
                        welcomeMessage = dashboardViewModel.getClassroomWelcomeMessage(),
                        encouragementMessage = dashboardViewModel.getEncouragementMessage()
                    )
                }
                
                item {
                    EmotionalCheckInSection(
                        onNavigateToEmotionalCheckin = { dashboardViewModel.navigateToEmotionalCheckin() }
                    )
                }
                
                item {
                    CurrentLessonSection(
                        currentLesson = currentLesson,
                        hasCurrentLesson = dashboardViewModel.hasCurrentLesson(),
                        onNavigateToLesson = { dashboardViewModel.navigateToCurrentLesson() }
                    )
                }
                
                item {
                    MyProgressSection(
                        dashboardStats = dashboardStats,
                        progressText = dashboardViewModel.getProgressText(),
                        heroPointsText = dashboardViewModel.getHeroPointsText()
                    )
                }
                
                item {
                    FunActivitiesSection(hasCompletedLessons = dashboardViewModel.hasCompletedLessons())
                }
            }
        }
    }
    
    // Leave classroom confirmation dialog
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { 
                Text(
                    "Leave Classroom?",
                    style = MaterialTheme.typography.titleLarge
                ) 
            },
            text = { 
                Text(
                    "Are you sure you want to leave your classroom? You'll need your teacher's classroom code to join again.",
                    style = MaterialTheme.typography.bodyLarge
                ) 
            },
            confirmButton = {
                HeroesDangerButton(
                    text = "Leave Classroom",
                    onClick = {
                        authViewModel.logout()
                        onLeaveClassroom()
                    },
                    modifier = Modifier.width(150.dp)
                )
            },
            dismissButton = {
                HeroesSecondaryButton(
                    text = "Stay",
                    onClick = { showLeaveDialog = false },
                    modifier = Modifier.width(100.dp)
                )
            },
            shape = AlertDialogShape
        )
    }
}

@Composable
private fun StudentBottomNavigation(
    onLeaveClassroom: () -> Unit,
    onNavigateToHelp: () -> Unit
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = { /* Current screen */ }) {
                Icon(
                    Icons.Default.Home,
                    contentDescription = "Home",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onNavigateToHelp) {
                Icon(
                    Icons.Default.Help,
                    contentDescription = "Help",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onLeaveClassroom) {
                Icon(
                    Icons.Default.MoodBad,
                    contentDescription = "Leave",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun WelcomeBackSection(
    welcomeMessage: String,
    encouragementMessage: String
) {
    HeroesCard(
        colors = CardDefaults.cardColors(
            containerColor = HeroesLightPurple,
            contentColor = HeroesPurpleDark
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = HeroesPurple
            )
            Text(
                text = welcomeMessage,
                style = StudentEngagementTextStyle,
                color = HeroesPurpleDark,
                textAlign = TextAlign.Center
            )
            Text(
                text = encouragementMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = HeroesPurpleDark,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmotionalCheckInSection(onNavigateToEmotionalCheckin: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HeroesSectionHeader("How are you feeling today?")
        
        HeroesCard {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "It's important to check in with your feelings!",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                
                HeroesLargeButton(
                    text = "Tell Us How You Feel 😊",
                    onClick = onNavigateToEmotionalCheckin,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HeroesGreen,
                        contentColor = HeroesWhite
                    )
                )
            }
        }
    }
}

@Composable
private fun CurrentLessonSection(
    currentLesson: com.lifechurch.heroesinwaiting.data.repository.CurrentLessonInfo?,
    hasCurrentLesson: Boolean,
    onNavigateToLesson: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HeroesSectionHeader("Your Current Lesson")
        
        if (hasCurrentLesson && currentLesson != null) {
            // Show current lesson
            HeroesCard {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = HeroesGreen
                    )
                    Text(
                        text = "Lesson ${currentLesson.lessonNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = currentLesson.lessonTitle,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Estimated time: ${currentLesson.estimatedDuration} minutes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    
                    HeroesLargeButton(
                        text = if (currentLesson.isStarted) "Continue Lesson" else "Start Lesson",
                        onClick = onNavigateToLesson,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HeroesGreen,
                            contentColor = HeroesWhite
                        )
                    )
                }
            }
        } else {
            // No current lesson
            HeroesCard {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "No lesson in progress",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Your teacher will start a lesson when the class is ready. Check back soon!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MyProgressSection(
    dashboardStats: com.lifechurch.heroesinwaiting.data.repository.StudentDashboardStats?,
    progressText: String,
    heroPointsText: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HeroesSectionHeader("My Progress")
        
        // Progress summary
        HeroesCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = progressText,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (dashboardStats != null && dashboardStats.progressPercentage > 0) {
                    LinearProgressIndicator(
                        progress = dashboardStats.progressPercentage / 100f,
                        modifier = Modifier.fillMaxWidth(),
                        color = HeroesGreen
                    )
                }
                
                Text(
                    text = heroPointsText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Lessons completed
            HeroesCard(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = HeroesLightGreen,
                    contentColor = HeroesGreenDark
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${dashboardStats?.completedLessons ?: 0}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = HeroesGreenDark
                    )
                    Text(
                        text = "Lessons Completed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HeroesGreenDark,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Heroes points
            HeroesCard(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = HeroesLightOrange,
                    contentColor = HeroesOrangeDark
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${dashboardStats?.heroPoints ?: 0}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = HeroesOrangeDark
                    )
                    Text(
                        text = "Hero Points",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HeroesOrangeDark,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // Time spent learning
        if (dashboardStats?.timeSpentInMinutes ?: 0 > 0) {
            HeroesCard(
                colors = CardDefaults.cardColors(
                    containerColor = HeroesLightPurple,
                    contentColor = HeroesPurpleDark
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Time spent learning:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HeroesPurpleDark
                    )
                    Text(
                        text = "${dashboardStats?.timeSpentInMinutes} minutes",
                        style = MaterialTheme.typography.titleMedium,
                        color = HeroesPurpleDark
                    )
                }
            }
        }
    }
}

@Composable
private fun FunActivitiesSection(hasCompletedLessons: Boolean) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HeroesSectionHeader("Fun Activities")
        
        if (hasCompletedLessons) {
            // Show available activities
            HeroesCard {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = HeroesOrange
                    )
                    Text(
                        text = "Great job completing lessons!",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Keep learning to unlock more fun activities and earn Hero Points!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    HeroesSecondaryButton(
                        text = "Explore More",
                        onClick = { /* Navigate to activities */ }
                    )
                }
            }
        } else {
            // Encourage to start learning
            HeroesCard {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "More activities coming soon!",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Complete your first lesson to unlock fun activities and start earning Hero Points!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}