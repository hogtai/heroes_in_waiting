package com.lifechurch.heroesinwaiting.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lifechurch.heroesinwaiting.data.model.UserType
import com.lifechurch.heroesinwaiting.presentation.navigation.Screen
import com.lifechurch.heroesinwaiting.presentation.screens.auth.AuthScreen
import com.lifechurch.heroesinwaiting.presentation.screens.facilitator.FacilitatorDashboardScreen
import com.lifechurch.heroesinwaiting.presentation.screens.facilitator.CreateClassroomScreen
import com.lifechurch.heroesinwaiting.presentation.screens.student.StudentDashboardScreen
import com.lifechurch.heroesinwaiting.presentation.theme.HeroesInWaitingTheme
import com.lifechurch.heroesinwaiting.presentation.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity for Heroes in Waiting app
 * Handles navigation between facilitator and student flows with age-appropriate design
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before calling super.onCreate()
        val splashScreen = installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display for modern Android
        enableEdgeToEdge()
        
        setContent {
            HeroesInWaitingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HeroesInWaitingApp()
                }
            }
        }
    }
}

@Composable
fun HeroesInWaitingApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val userType by authViewModel.userType.collectAsState()
    
    // Determine starting destination based on authentication state
    val startDestination = when {
        !isLoggedIn -> Screen.Auth.route
        userType == UserType.FACILITATOR -> Screen.FacilitatorDashboard.route
        userType == UserType.STUDENT -> Screen.StudentDashboard.route
        else -> Screen.Auth.route
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Authentication screen
            composable(Screen.Auth.route) {
                AuthScreen(
                    onNavigateToFacilitatorDashboard = {
                        navController.navigate(Screen.FacilitatorDashboard.route) {
                            // Clear back stack when navigating to dashboard
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    },
                    onNavigateToStudentDashboard = {
                        navController.navigate(Screen.StudentDashboard.route) {
                            // Clear back stack when navigating to dashboard
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                )
            }
            
            // Facilitator dashboard and related screens
            composable(Screen.FacilitatorDashboard.route) {
                FacilitatorDashboardScreen(
                    onLogout = {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(Screen.FacilitatorDashboard.route) { inclusive = true }
                        }
                    },
                    onNavigateToCreateClassroom = {
                        navController.navigate(Screen.CreateClassroom.route)
                    },
                    onNavigateToLessons = {
                        navController.navigate(Screen.LessonManagement.route)
                    },
                    onNavigateToAnalytics = {
                        navController.navigate(Screen.Analytics.route)
                    }
                )
            }
            
            // Student dashboard and related screens
            composable(Screen.StudentDashboard.route) {
                StudentDashboardScreen(
                    onLeaveClassroom = {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(Screen.StudentDashboard.route) { inclusive = true }
                        }
                    },
                    onNavigateToHelp = {
                        navController.navigate(Screen.StudentHelp.route)
                    },
                    onNavigateToEmotionalCheckin = {
                        navController.navigate(Screen.EmotionalCheckin.route)
                    }
                )
            }
            
            // Create Classroom screen - fully implemented
            composable(Screen.CreateClassroom.route) {
                CreateClassroomScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onClassroomCreated = { navController.popBackStack() }
                )
            }
            
            composable(Screen.LessonManagement.route) {
                PlaceholderScreen(
                    title = "Lesson Management",
                    message = "Lesson management screen will be implemented in the next phase.",
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.Analytics.route) {
                PlaceholderScreen(
                    title = "Analytics",
                    message = "Analytics dashboard will be implemented in the next phase.",
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.StudentHelp.route) {
                PlaceholderScreen(
                    title = "Help",
                    message = "Help screen with tutorials and FAQs will be implemented in the next phase.",
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.EmotionalCheckin.route) {
                PlaceholderScreen(
                    title = "How Are You Feeling?",
                    message = "Emotional check-in screen will be implemented in the next phase.",
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/**
 * Placeholder screen for features not yet implemented
 * Shows a professional "under construction" interface
 */
@Composable
fun PlaceholderScreen(
    title: String,
    message: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Construction,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    
                    Button(
                        onClick = onBack,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Go Back")
                    }
                }
            }
        }
    }
}