package com.lifechurch.heroesinwaiting.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Shapes optimized for young users - friendly, rounded corners
// Larger corner radii for a softer, more approachable feel

val HeroesShapes = Shapes(
    // Extra small - for small elements like chips
    extraSmall = RoundedCornerShape(8.dp),
    
    // Small - for buttons and smaller cards
    small = RoundedCornerShape(12.dp),
    
    // Medium - for main cards and containers
    medium = RoundedCornerShape(16.dp),
    
    // Large - for bottom sheets and large containers
    large = RoundedCornerShape(24.dp),
    
    // Extra large - for special containers and dialogs
    extraLarge = RoundedCornerShape(32.dp)
)