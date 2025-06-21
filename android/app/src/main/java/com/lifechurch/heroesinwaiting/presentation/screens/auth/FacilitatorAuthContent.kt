package com.lifechurch.heroesinwaiting.presentation.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifechurch.heroesinwaiting.presentation.components.*
import com.lifechurch.heroesinwaiting.presentation.viewmodel.AuthViewModel

@Composable
fun FacilitatorAuthContent(
    onBack: () -> Unit,
    viewModel: AuthViewModel
) {
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var organization by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    
    val uiState by viewModel.uiState.collectAsState()
    
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
                onClick = onBack,
                modifier = Modifier.width(120.dp)
            )
        }
        
        HeroesSectionHeader(
            text = if (isLoginMode) "Facilitator Login" else "Create Account"
        )
        
        // Show error if any
        uiState.error?.let { error ->
            HeroesErrorDisplay(
                message = error,
                onRetry = { viewModel.clearError() }
            )
        }
        
        // Email field
        HeroesTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email Address",
            placeholder = "teacher@school.edu",
            isError = email.isNotEmpty() && !viewModel.isValidEmail(email),
            errorMessage = if (email.isNotEmpty() && !viewModel.isValidEmail(email)) {
                "Please enter a valid email address"
            } else null
        )
        
        // Password field
        HeroesTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            placeholder = if (isLoginMode) "Enter password" else "Create a strong password",
            isError = !isLoginMode && password.isNotEmpty() && !viewModel.isValidPassword(password),
            errorMessage = if (!isLoginMode && password.isNotEmpty() && !viewModel.isValidPassword(password)) {
                "Password must be at least 8 characters with uppercase, lowercase, number, and special character"
            } else null
        )
        
        // Registration-only fields
        if (!isLoginMode) {
            HeroesTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = "First Name",
                placeholder = "John"
            )
            
            HeroesTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = "Last Name",
                placeholder = "Smith"
            )
            
            HeroesTextField(
                value = organization,
                onValueChange = { organization = it },
                label = "Organization (Optional)",
                placeholder = "Lincoln Elementary School"
            )
            
            HeroesTextField(
                value = role,
                onValueChange = { role = it },
                label = "Role (Optional)",
                placeholder = "Teacher, Counselor, Principal, etc."
            )
        }
        
        // Submit button
        val canSubmit = if (isLoginMode) {
            email.isNotEmpty() && password.isNotEmpty() && viewModel.isValidEmail(email)
        } else {
            email.isNotEmpty() && password.isNotEmpty() && firstName.isNotEmpty() && 
            lastName.isNotEmpty() && viewModel.isValidEmail(email) && viewModel.isValidPassword(password)
        }
        
        HeroesLargeButton(
            text = if (isLoginMode) "Sign In" else "Create Account",
            onClick = {
                if (isLoginMode) {
                    viewModel.loginFacilitator(email, password)
                } else {
                    viewModel.registerFacilitator(
                        email = email,
                        password = password,
                        firstName = firstName,
                        lastName = lastName,
                        organization = organization.takeIf { it.isNotEmpty() },
                        role = role.takeIf { it.isNotEmpty() }
                    )
                }
            },
            enabled = canSubmit,
            isLoading = uiState.isLoading
        )
        
        // Mode toggle
        HeroesDivider()
        
        TextButton(
            onClick = { 
                isLoginMode = !isLoginMode
                viewModel.clearError()
            }
        ) {
            Text(
                text = if (isLoginMode) {
                    "Don't have an account? Create one"
                } else {
                    "Already have an account? Sign in"
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}