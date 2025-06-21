package com.lifechurch.heroesinwaiting.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Age-appropriate color schemes for Heroes in Waiting
 * Bright, colorful, and engaging for elementary students (grades 4-6)
 * Also accessible and professional for facilitators
 */

private val DarkColorScheme = darkColorScheme(
    primary = HeroesPurple,
    onPrimary = HeroesWhite,
    primaryContainer = HeroesPurpleDark,
    onPrimaryContainer = HeroesLightPurple,
    secondary = HeroesGreen,
    onSecondary = HeroesWhite,
    secondaryContainer = HeroesGreenDark,
    onSecondaryContainer = HeroesLightGreen,
    tertiary = HeroesOrange,
    onTertiary = HeroesWhite,
    tertiaryContainer = HeroesOrangeDark,
    onTertiaryContainer = HeroesLightOrange,
    error = HeroesRed,
    onError = HeroesWhite,
    errorContainer = HeroesRedDark,
    onErrorContainer = HeroesLightRed,
    background = HeroesDarkGray,
    onBackground = HeroesLightGray,
    surface = HeroesDarkGray,
    onSurface = HeroesLightGray,
    surfaceVariant = HeroesGray,
    onSurfaceVariant = HeroesLightGray,
    outline = HeroesGray,
    outlineVariant = HeroesDarkGray,
    scrim = HeroesBlack,
    inverseSurface = HeroesLightGray,
    inverseOnSurface = HeroesDarkGray,
    inversePrimary = HeroesLightPurple,
    surfaceDim = HeroesDarkGray,
    surfaceBright = HeroesGray,
    surfaceContainerLowest = HeroesBlack,
    surfaceContainerLow = HeroesDarkGray,
    surfaceContainer = HeroesGray,
    surfaceContainerHigh = HeroesLightGray,
    surfaceContainerHighest = HeroesWhite
)

private val LightColorScheme = lightColorScheme(
    primary = HeroesPurple,
    onPrimary = HeroesWhite,
    primaryContainer = HeroesLightPurple,
    onPrimaryContainer = HeroesPurpleDark,
    secondary = HeroesGreen,
    onSecondary = HeroesWhite,
    secondaryContainer = HeroesLightGreen,
    onSecondaryContainer = HeroesGreenDark,
    tertiary = HeroesOrange,
    onTertiary = HeroesWhite,
    tertiaryContainer = HeroesLightOrange,
    onTertiaryContainer = HeroesOrangeDark,
    error = HeroesRed,
    onError = HeroesWhite,
    errorContainer = HeroesLightRed,
    onErrorContainer = HeroesRedDark,
    background = HeroesWhite,
    onBackground = HeroesBlack,
    surface = HeroesWhite,
    onSurface = HeroesBlack,
    surfaceVariant = HeroesLightGray,
    onSurfaceVariant = HeroesDarkGray,
    outline = HeroesGray,
    outlineVariant = HeroesLightGray,
    scrim = HeroesBlack,
    inverseSurface = HeroesDarkGray,
    inverseOnSurface = HeroesWhite,
    inversePrimary = HeroesLightPurple,
    surfaceDim = HeroesLightGray,
    surfaceBright = HeroesWhite,
    surfaceContainerLowest = HeroesWhite,
    surfaceContainerLow = HeroesLightGray,
    surfaceContainer = HeroesLightGray,
    surfaceContainerHigh = HeroesLightGray,
    surfaceContainerHighest = HeroesGray
)

@Composable
fun HeroesInWaitingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled for consistent branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HeroesTypography,
        shapes = HeroesShapes,
        content = content
    )
}