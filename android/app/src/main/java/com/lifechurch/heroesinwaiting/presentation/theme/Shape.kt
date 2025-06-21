package com.lifechurch.heroesinwaiting.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Age-appropriate shapes for Heroes in Waiting
 * Rounded, friendly shapes that appeal to elementary students
 * More rounded corners for a softer, welcoming appearance
 */

val HeroesShapes = Shapes(
    // Extra small - for small buttons and chips
    extraSmall = RoundedCornerShape(8.dp),
    
    // Small - for standard buttons and small cards
    small = RoundedCornerShape(12.dp),
    
    // Medium - for cards and containers
    medium = RoundedCornerShape(16.dp),
    
    // Large - for major cards and modal dialogs
    large = RoundedCornerShape(24.dp),
    
    // Extra large - for full-screen modals and special containers
    extraLarge = RoundedCornerShape(32.dp)
)

/**
 * Custom shapes for specific UI elements
 */

// Touch targets for students - more rounded for friendliness
val StudentTouchTargetShape = RoundedCornerShape(20.dp)

// Lesson cards - welcoming and approachable
val LessonCardShape = RoundedCornerShape(20.dp)

// Activity containers - fun and engaging
val ActivityContainerShape = RoundedCornerShape(24.dp)

// Button shapes for different contexts
val PrimaryButtonShape = RoundedCornerShape(16.dp) // Primary actions
val SecondaryButtonShape = RoundedCornerShape(12.dp) // Secondary actions
val DangerButtonShape = RoundedCornerShape(8.dp) // Warning/danger actions

// Student interface elements - extra rounded for appeal
val StudentCardShape = RoundedCornerShape(24.dp)
val StudentButtonShape = RoundedCornerShape(20.dp)
val StudentInputShape = RoundedCornerShape(16.dp)

// Facilitator interface elements - professional but friendly
val FacilitatorCardShape = RoundedCornerShape(12.dp)
val FacilitatorButtonShape = RoundedCornerShape(8.dp)
val FacilitatorInputShape = RoundedCornerShape(8.dp)

// Special shapes for engagement
val EmotionSelectorShape = RoundedCornerShape(28.dp) // Circular-like for emotions
val ProgressIndicatorShape = RoundedCornerShape(8.dp)
val AlertDialogShape = RoundedCornerShape(20.dp)

// Navigation elements
val BottomNavigationShape = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 16.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp
)

val TopAppBarShape = RoundedCornerShape(
    topStart = 0.dp,
    topEnd = 0.dp,
    bottomStart = 16.dp,
    bottomEnd = 16.dp
)

// Floating elements
val FabShape = RoundedCornerShape(16.dp)
val SnackbarShape = RoundedCornerShape(12.dp)

// Content containers
val SectionContainerShape = RoundedCornerShape(16.dp)
val ListItemShape = RoundedCornerShape(8.dp)

// Image and media containers
val ImageContainerShape = RoundedCornerShape(12.dp)
val VideoPlayerShape = RoundedCornerShape(16.dp)

// Form elements
val TextFieldShape = RoundedCornerShape(12.dp)
val CheckboxShape = RoundedCornerShape(4.dp)
val RadioButtonShape = RoundedCornerShape(50) // Circular

// Accessibility shapes - larger touch targets
val AccessibleButtonShape = RoundedCornerShape(16.dp)
val AccessibleCardShape = RoundedCornerShape(20.dp)