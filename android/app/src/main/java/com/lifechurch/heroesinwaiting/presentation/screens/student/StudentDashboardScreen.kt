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
import com.lifechurch.heroesinwaiting.presentation.components.*
import com.lifechurch.heroesinwaiting.presentation.theme.*
import com.lifechurch.heroesinwaiting.presentation.viewmodel.AuthViewModel

/**
 * Student dashboard with age-appropriate design
 * Large, colorful, and engaging interface for elementary students
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(
    onLeaveClassroom: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var showLeaveDialog by remember { mutableStateOf(false) }
    
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
                onLeaveClassroom = { showLeaveDialog = true }
            )
        }
    ) { paddingValues ->
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                WelcomeBackSection()
            }
            
            item {
                EmotionalCheckInSection()
            }
            
            item {
                CurrentLessonSection()
            }
            
            item {
                MyProgressSection()
            }
            
            item {
                FunActivitiesSection()
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
    onLeaveClassroom: () -> Unit
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
            IconButton(onClick = { /* Navigate to help */ }) {
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
private fun WelcomeBackSection() {
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
                text = "Welcome back, Hero!",
                style = StudentEngagementTextStyle,
                color = HeroesPurpleDark,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Ready to continue your heroes journey?",
                style = MaterialTheme.typography.bodyLarge,
                color = HeroesPurpleDark,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmotionalCheckInSection() {
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
                    onClick = { /* Navigate to emotional check-in */ },
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
private fun CurrentLessonSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HeroesSectionHeader("Your Current Lesson")
        
        // Placeholder for when no active lesson
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

@Composable
private fun MyProgressSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HeroesSectionHeader("My Progress")
        
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
                        text = "0",
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
                        text = "0",
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
    }
}

@Composable
private fun FunActivitiesSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HeroesSectionHeader("Fun Activities")
        
        // Placeholder activities
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
                    text = "Your teacher will unlock fun activities as you complete lessons together.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}