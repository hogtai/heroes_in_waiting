package com.lifechurch.heroesinwaiting.presentation.screens.facilitator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifechurch.heroesinwaiting.presentation.components.*
import com.lifechurch.heroesinwaiting.presentation.theme.*
import com.lifechurch.heroesinwaiting.presentation.viewmodel.AuthViewModel
import com.lifechurch.heroesinwaiting.presentation.viewmodel.FacilitatorDashboardViewModel
import com.lifechurch.heroesinwaiting.presentation.viewmodel.FacilitatorDashboardEvent

/**
 * Facilitator dashboard with professional interface
 * Shows classroom management, lesson delivery, and analytics
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacilitatorDashboardScreen(
    onLogout: () -> Unit,
    onNavigateToCreateClassroom: () -> Unit,
    onNavigateToLessons: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    dashboardViewModel: FacilitatorDashboardViewModel = hiltViewModel()
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    // Collect state from ViewModel
    val uiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val classrooms by dashboardViewModel.classrooms.collectAsStateWithLifecycle()
    val dashboardStats by dashboardViewModel.dashboardStats.collectAsStateWithLifecycle()
    
    // Handle events
    LaunchedEffect(Unit) {
        dashboardViewModel.events.collect { event ->
            when (event) {
                is FacilitatorDashboardEvent.NavigateToCreateClassroom -> onNavigateToCreateClassroom()
                is FacilitatorDashboardEvent.NavigateToLessons -> onNavigateToLessons()
                is FacilitatorDashboardEvent.NavigateToAnalytics -> onNavigateToAnalytics()
                is FacilitatorDashboardEvent.NavigateToClassroomDetails -> {
                    // Handle classroom details navigation
                }
                is FacilitatorDashboardEvent.ClassroomCreated -> {
                    // Handle classroom creation success
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Heroes in Waiting",
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateClassroom,
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Classroom")
            }
        }
    ) { paddingValues ->
        
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                HeroesLoadingIndicator(message = "Loading your dashboard...")
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    WelcomeSection(dashboardViewModel.getWelcomeMessage())
                }
                
                item {
                    QuickActionsSection(
                        onNavigateToCreateClassroom = { dashboardViewModel.navigateToCreateClassroom() },
                        onNavigateToLessons = { dashboardViewModel.navigateToLessons() }
                    )
                }
                
                item {
                    RecentClassroomsSection(
                        classrooms = classrooms,
                        hasClassrooms = dashboardViewModel.hasClassrooms(),
                        onNavigateToCreateClassroom = { dashboardViewModel.navigateToCreateClassroom() },
                        onClassroomSelected = { classroom -> dashboardViewModel.onClassroomSelected(classroom) }
                    )
                }
                
                item {
                    AnalyticsOverviewSection(
                        dashboardStats = dashboardStats,
                        onNavigateToAnalytics = { dashboardViewModel.navigateToAnalytics() }
                    )
                }
            }
        }
    }
    
    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        authViewModel.logout()
                        onLogout()
                    }
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun WelcomeSection(welcomeMessage: String) {
    HeroesCard {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = welcomeMessage,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Ready to make a difference in students' lives today?",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun QuickActionsSection(
    onNavigateToCreateClassroom: () -> Unit,
    onNavigateToLessons: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HeroesSectionHeader("Quick Actions")
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Create Classroom
            Card(
                onClick = onNavigateToCreateClassroom,
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Create Classroom",
                        style = FacilitatorProfessionalTextStyle,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Browse Lessons
            Card(
                onClick = onNavigateToLessons,
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Browse Lessons",
                        style = FacilitatorProfessionalTextStyle,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentClassroomsSection(
    classrooms: List<com.lifechurch.heroesinwaiting.data.model.Classroom>,
    hasClassrooms: Boolean,
    onNavigateToCreateClassroom: () -> Unit,
    onClassroomSelected: (com.lifechurch.heroesinwaiting.data.model.Classroom) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HeroesSectionHeader("Your Classrooms")
        
        if (!hasClassrooms) {
            // Empty state
            HeroesCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "No classrooms yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Create your first classroom to get started with Heroes in Waiting lessons.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    HeroesVerticalSpacer(HeroesSpacing.Small)
                    
                    HeroesLargeButton(
                        text = "Create My First Classroom",
                        onClick = onNavigateToCreateClassroom
                    )
                }
            }
        } else {
            // Show classrooms
            classrooms.take(3).forEach { classroom ->
                ClassroomCard(
                    classroom = classroom,
                    onClick = { onClassroomSelected(classroom) }
                )
            }
            
            if (classrooms.size > 3) {
                HeroesSecondaryButton(
                    text = "View All Classrooms (${classrooms.size})",
                    onClick = { /* Navigate to all classrooms */ }
                )
            }
        }
    }
}

@Composable
private fun ClassroomCard(
    classroom: com.lifechurch.heroesinwaiting.data.model.Classroom,
    onClick: () -> Unit
) {
    HeroesCard(onClick = onClick) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = classroom.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Code: ${classroom.classroomCode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Text(
                text = "${classroom.currentStudentCount} students • Grade ${classroom.grade.displayName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (classroom.hasActiveSession) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Badge {
                        Text("Active Session", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsOverviewSection(
    dashboardStats: com.lifechurch.heroesinwaiting.data.repository.FacilitatorDashboardStats?,
    onNavigateToAnalytics: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HeroesSectionHeader("Overview")
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total Students
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${dashboardStats?.totalStudents ?: 0}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "Total Students",
                        style = FacilitatorProfessionalTextStyle,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Active Sessions
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = if ((dashboardStats?.activeSessions ?: 0) > 0) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${dashboardStats?.activeSessions ?: 0}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = if ((dashboardStats?.activeSessions ?: 0) > 0) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Active Sessions",
                        style = FacilitatorProfessionalTextStyle,
                        color = if ((dashboardStats?.activeSessions ?: 0) > 0) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // Additional stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total Classrooms
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${dashboardStats?.totalClassrooms ?: 0}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Classrooms",
                        style = FacilitatorProfessionalTextStyle,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Completed Lessons
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = HeroesLightGreen
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${dashboardStats?.completedLessons ?: 0}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = HeroesGreenDark
                    )
                    Text(
                        text = "Lessons Delivered",
                        style = FacilitatorProfessionalTextStyle,
                        color = HeroesGreenDark,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // View Analytics Button
        HeroesSecondaryButton(
            text = "View Detailed Analytics",
            onClick = onNavigateToAnalytics
        )
    }
}