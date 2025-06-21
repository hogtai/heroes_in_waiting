package com.lifechurch.heroesinwaiting.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifechurch.heroesinwaiting.presentation.components.*
import com.lifechurch.heroesinwaiting.presentation.theme.HeroesInWaitingTheme

@Composable
fun FacilitatorAuthScreen(
    onLoginSuccess: () -> Unit,
    onBackToWelcome: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var organization by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    HeroesInWaitingTheme {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Hero icon and title
            Text(
                text = "🦸‍♀️",
                style = MaterialTheme.typography.displayLarge
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (isLogin) "Welcome Back, Hero!" else "Join the Heroes!",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = if (isLogin) 
                    "Sign in to guide young heroes in their journey" 
                else 
                    "Create your account to start making a difference",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Form fields
            if (!isLogin) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HeroTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = "First Name",
                        modifier = Modifier.weight(1f),
                        leadingIcon = Icons.Default.Person
                    )
                    
                    HeroTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = "Last Name",
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                HeroTextField(
                    value = organization,
                    onValueChange = { organization = it },
                    label = "School/Organization",
                    placeholder = "Lincoln Elementary School",
                    leadingIcon = Icons.Default.School
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            HeroTextField(
                value = email,
                onValueChange = { 
                    email = it
                    errorMessage = null
                },
                label = "Email Address",
                placeholder = "teacher@school.edu",
                leadingIcon = Icons.Default.Email,
                isError = errorMessage != null
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            HeroTextField(
                value = password,
                onValueChange = { 
                    password = it
                    errorMessage = null
                },
                label = "Password",
                leadingIcon = Icons.Default.Lock,
                isPassword = true,
                isError = errorMessage != null
            )
            
            if (!isLogin) {
                Spacer(modifier = Modifier.height(16.dp))
                
                HeroTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "Confirm Password",
                    leadingIcon = Icons.Default.Lock,
                    isPassword = true,
                    isError = confirmPassword.isNotEmpty() && password != confirmPassword,
                    errorMessage = if (confirmPassword.isNotEmpty() && password != confirmPassword) 
                        "Passwords don't match" else null
                )
            }
            
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Action button
            if (isLoading) {
                LoadingHero(
                    message = if (isLogin) "Signing you in..." else "Creating your hero account..."
                )
            } else {
                HeroButton(
                    text = if (isLogin) "Sign In" else "Create Account",
                    onClick = {
                        // Validate inputs
                        when {
                            email.isBlank() -> errorMessage = "Please enter your email"
                            password.isBlank() -> errorMessage = "Please enter your password"
                            !isLogin && firstName.isBlank() -> errorMessage = "Please enter your first name"
                            !isLogin && lastName.isBlank() -> errorMessage = "Please enter your last name"
                            !isLogin && organization.isBlank() -> errorMessage = "Please enter your organization"
                            !isLogin && password != confirmPassword -> errorMessage = "Passwords don't match"
                            !isLogin && password.length < 8 -> errorMessage = "Password must be at least 8 characters"
                            else -> {
                                isLoading = true
                                // TODO: Implement actual authentication logic
                                // For now, simulate loading and success
                                // authenticateUser(email, password, isLogin, onSuccess = onLoginSuccess)
                            }
                        }
                    },
                    icon = if (isLogin) Icons.Default.Login else Icons.Default.PersonAdd,
                    enabled = !isLoading
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Toggle between login and register
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isLogin) "New to Heroes in Waiting?" else "Already have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                TextButton(
                    onClick = { 
                        isLogin = !isLogin
                        errorMessage = null
                        // Clear form when switching
                        if (isLogin) {
                            firstName = ""
                            lastName = ""
                            organization = ""
                            confirmPassword = ""
                        }
                    }
                ) {
                    Text(
                        text = if (isLogin) "Sign Up" else "Sign In",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
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