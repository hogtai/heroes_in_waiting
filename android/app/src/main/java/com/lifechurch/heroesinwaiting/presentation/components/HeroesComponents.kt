package com.lifechurch.heroesinwaiting.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifechurch.heroesinwaiting.presentation.theme.*

/**
 * Age-appropriate UI components for Heroes in Waiting
 * All components follow 48dp+ touch target requirements for elementary students
 */

/**
 * Large, colorful button suitable for elementary students
 * Minimum 48dp height with generous padding
 */
@Composable
fun HeroesLargeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    )
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp), // Ensures minimum touch target
        enabled = enabled && !isLoading,
        colors = colors,
        shape = StudentButtonShape,
        contentPadding = PaddingValues(16.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                style = StudentButtonTextStyle,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Secondary button with outline style
 */
@Composable
fun HeroesSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        enabled = enabled,
        shape = StudentButtonShape,
        contentPadding = PaddingValues(16.dp),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            width = 2.dp // Thicker border for visibility
        )
    ) {
        Text(
            text = text,
            style = StudentButtonTextStyle,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Danger/warning button with red coloring
 */
@Composable
fun HeroesDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = HeroesRed,
            contentColor = HeroesWhite
        ),
        shape = StudentButtonShape,
        contentPadding = PaddingValues(16.dp)
    ) {
        Text(
            text = text,
            style = StudentButtonTextStyle,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Card component with age-appropriate styling
 */
@Composable
fun HeroesCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    colors: CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            colors = colors,
            elevation = elevation,
            shape = StudentCardShape
        ) {
            content()
        }
    } else {
        Card(
            modifier = modifier,
            colors = colors,
            elevation = elevation,
            shape = StudentCardShape
        ) {
            content()
        }
    }
}

/**
 * Text input field with large, friendly styling
 */
@Composable
fun HeroesTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp), // Larger touch target
            enabled = enabled,
            singleLine = singleLine,
            isError = isError,
            shape = StudentInputShape,
            textStyle = MaterialTheme.typography.bodyLarge // Larger text for readability
        )
        
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

/**
 * Loading indicator with friendly styling
 */
@Composable
fun HeroesLoadingIndicator(
    modifier: Modifier = Modifier,
    message: String = "Loading..."
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Error display component
 */
@Composable
fun HeroesErrorDisplay(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = HeroesLightRed,
            contentColor = HeroesRedDark
        ),
        shape = StudentCardShape
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Oops! Something went wrong",
                style = MaterialTheme.typography.titleMedium,
                color = HeroesRedDark,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = HeroesRedDark,
                textAlign = TextAlign.Center
            )
            if (onRetry != null) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HeroesRed,
                        contentColor = HeroesWhite
                    ),
                    shape = StudentButtonShape
                ) {
                    Text("Try Again")
                }
            }
        }
    }
}

/**
 * Success display component
 */
@Composable
fun HeroesSuccessDisplay(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = HeroesLightGreen,
            contentColor = HeroesGreenDark
        ),
        shape = StudentCardShape
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Great job!",
                style = MaterialTheme.typography.titleMedium,
                color = HeroesGreenDark,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = HeroesGreenDark,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Section header for organizing content
 */
@Composable
fun HeroesSectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.fillMaxWidth(),
        textAlign = TextAlign.Start
    )
}

/**
 * Divider with friendly styling
 */
@Composable
fun HeroesDivider(
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 2.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    )
}

/**
 * Spacer components for consistent spacing
 */
@Composable
fun HeroesVerticalSpacer(
    size: HeroesSpacing = HeroesSpacing.Medium
) {
    Spacer(modifier = Modifier.height(size.dp))
}

@Composable
fun HeroesHorizontalSpacer(
    size: HeroesSpacing = HeroesSpacing.Medium
) {
    Spacer(modifier = Modifier.width(size.dp))
}

enum class HeroesSpacing(val dp: androidx.compose.ui.unit.Dp) {
    XSmall(4.dp),
    Small(8.dp),
    Medium(16.dp),
    Large(24.dp),
    XLarge(32.dp),
    XXLarge(48.dp)
}