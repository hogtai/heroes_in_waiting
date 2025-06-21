package com.lifechurch.heroesinwaiting.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Heroes in Waiting theme optimized for grades 4-6
// Bright, engaging, and accessible design

private val HeroesLightColorScheme = lightColorScheme(
    // Primary colors - Purple for heroes and heroic actions
    primary = HeroesPurple,
    onPrimary = HeroesTextOnColor,
    primaryContainer = HeroesPurpleLight,
    onPrimaryContainer = HeroesPurpleDark,
    
    // Secondary colors - Green for growth and learning
    secondary = HeroesGreen,
    onSecondary = HeroesTextOnColor,
    secondaryContainer = HeroesGreenLight,
    onSecondaryContainer = HeroesGreenDark,
    
    // Tertiary colors - Orange for energy and enthusiasm
    tertiary = HeroesOrange,
    onTertiary = HeroesTextOnColor,
    tertiaryContainer = HeroesOrangeLight,
    onTertiaryContainer = HeroesOrangeDark,
    
    // Error colors
    error = HeroesError,
    onError = HeroesTextOnColor,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    
    // Background colors
    background = HeroesBackground,
    onBackground = HeroesTextPrimary,
    
    // Surface colors
    surface = HeroesSurface,
    onSurface = HeroesTextPrimary,
    surfaceVariant = HeroesGrayLight,
    onSurfaceVariant = HeroesTextSecondary,
    
    // Outline colors
    outline = HeroesGray,
    outlineVariant = HeroesGrayLight,
    
    // Additional semantic colors
    scrim = Color(0x66000000),
    inverseSurface = HeroesGrayDark,
    inverseOnSurface = HeroesTextOnColor,
    inversePrimary = HeroesPurpleLight,
    
    // Surface tint for dynamic theming
    surfaceTint = HeroesPurple
)

@Composable
fun HeroesInWaitingTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HeroesLightColorScheme,
        typography = HeroesTypography,
        shapes = HeroesShapes,
        content = content
    )
}