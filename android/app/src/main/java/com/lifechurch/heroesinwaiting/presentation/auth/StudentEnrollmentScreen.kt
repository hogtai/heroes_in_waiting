package com.lifechurch.heroesinwaiting.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifechurch.heroesinwaiting.presentation.components.*
import com.lifechurch.heroesinwaiting.presentation.theme.HeroesInWaitingTheme

@Composable
fun StudentEnrollmentScreen(
    onEnrollmentSuccess: () -> Unit,
    onBackToWelcome: () -> Unit,
    modifier: Modifier = Modifier
) {
    var classroomCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var classroomPreview by remember { mutableStateOf<ClassroomPreview?>(null) }
    var showPreview by remember { mutableStateOf(false) }
    
    data class ClassroomPreview(
        val teacherName: String,
        val schoolName: String,
        val gradeLevel: String,
        val studentCount: Int
    )
    
    HeroesInWaitingTheme {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Fun hero characters for students
            Text(
                text = "🦸‍♂️🦸‍♀️",
                style = MaterialTheme.typography.displayLarge
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Join Your Hero Class!",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Enter the special code your teacher gave you to join your hero training!",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Classroom code input
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔑",
                        style = MaterialTheme.typography.displaySmall
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Class Code",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Ask your teacher for the special code!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = classroomCode,
                        onValueChange = { newCode ->
                            classroomCode = newCode.uppercase().take(8)
                            errorMessage = null
                            showPreview = false
                        },
                        placeholder = {
                            Text(
                                text = "ABC123",
                                style = MaterialTheme.typography.headlineMedium
                            )
                        },
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            textAlign = TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        isError = errorMessage != null
                    )
                    
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Check code button
            HeroButton(
                text = "Check Code",
                onClick = {
                    when {
                        classroomCode.isBlank() -> {
                            errorMessage = "Please enter your class code!"
                        }
                        classroomCode.length < 4 -> {
                            errorMessage = "Class codes are usually longer. Check with your teacher!"
                        }
                        else -> {
                            isLoading = true
                            errorMessage = null
                            // TODO: Implement actual classroom code verification
                            // For now, simulate API call and show preview
                            // verifyClassroomCode(classroomCode) { preview -> 
                            //     classroomPreview = preview
                            //     showPreview = true
                            //     isLoading = false
                            // }
                            
                            // Simulate successful verification for demo
                            classroomPreview = ClassroomPreview(
                                teacherName = "Ms. Johnson",
                                schoolName = "Lincoln Elementary",
                                gradeLevel = "4th Grade",
                                studentCount = 23
                            )
                            showPreview = true
                            isLoading = false
                        }
                    }
                },
                icon = Icons.Default.Search,
                enabled = !isLoading && classroomCode.isNotBlank()
            )
            
            if (isLoading) {
                Spacer(modifier = Modifier.height(24.dp))
                LoadingHero(message = "Checking your class code...")
            }
            
            // Classroom preview
            if (showPreview && classroomPreview != null) {
                Spacer(modifier = Modifier.height(24.dp))
                
                HeroCard(
                    title = "Is this your class?",
                    subtitle = null,
                    onClick = { },
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "🏫",
                            style = MaterialTheme.typography.displayMedium
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = classroomPreview!!.teacherName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Text(
                            text = classroomPreview!!.schoolName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Text(
                            text = "${classroomPreview!!.gradeLevel} • ${classroomPreview!!.studentCount} heroes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HeroButton(
                        text = "Try Different Code",
                        onClick = {
                            showPreview = false
                            classroomPreview = null
                            classroomCode = ""
                        },
                        modifier = Modifier.weight(1f),
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        icon = Icons.Default.ArrowBack
                    )
                    
                    HeroButton(
                        text = "Join Class!",
                        onClick = {
                            isLoading = true
                            // TODO: Implement actual enrollment
                            // enrollInClassroom(classroomCode, onEnrollmentSuccess)
                            onEnrollmentSuccess()
                        },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Check,
                        backgroundColor = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Help section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🤔",
                        style = MaterialTheme.typography.displaySmall
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Need Help?",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Ask your teacher for the class code. It might look like \"ABC123\" or \"HERO2024\".",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Back to welcome button
            TextButton(
                onClick = onBackToWelcome
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Back to Welcome",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}