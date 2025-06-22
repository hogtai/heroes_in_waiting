package com.lifechurch.heroesinwaiting.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
            
            // Placeholder screens for navigation - these will be implemented in future phases
            composable(Screen.CreateClassroom.route) {
                PlaceholderScreen(
                    title = "Create Classroom",
                    message = "Classroom creation screen will be implemented in the next phase.",
                    onBack = { navController.popBackStack() }
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