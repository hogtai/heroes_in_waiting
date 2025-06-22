package com.lifechurch.heroesinwaiting.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifechurch.heroesinwaiting.data.model.Lesson
import com.lifechurch.heroesinwaiting.data.model.LessonCategory
import com.lifechurch.heroesinwaiting.data.model.GradeLevel

/**
 * Lesson Card component for displaying lesson information in grid layouts
 */
@Composable
fun LessonCard(
    lesson: Lesson,
    progressPercent: Float = 0f,
    isAvailable: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    HeroesCard(
        onClick = onClick,
        enabled = isAvailable,
        colors = CardDefaults.cardColors(
            containerColor = when {
                !isAvailable -> MaterialTheme.colorScheme.surfaceVariant
                progressPercent > 0 -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Lesson number and category badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Badge {
                    Text("Lesson ${lesson.lessonNumber}")
                }
                Icon(
                    imageVector = getCategoryIcon(lesson.category),
                    contentDescription = lesson.category.displayName,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            HeroesVerticalSpacer(12.dp)
            
            // Title and description
            Text(
                text = lesson.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            HeroesVerticalSpacer(8.dp)
            
            Text(
                text = lesson.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            HeroesVerticalSpacer(12.dp)
            
            // Duration and grade info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${lesson.totalEstimatedTime} mins",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Grades ${lesson.targetGrades.joinToString { it.displayName }}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Progress indicator
            if (progressPercent > 0) {
                HeroesVerticalSpacer(12.dp)
                LinearProgressIndicator(
                    progress = progressPercent / 100f,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
                HeroesVerticalSpacer(4.dp)
                Text(
                    text = "${progressPercent.toInt()}% Complete",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Progress indicator component for lesson completion status
 */
@Composable
fun LessonProgressIndicator(
    progressPercent: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        LinearProgressIndicator(
            progress = progressPercent.coerceIn(0f, 1f),
            modifier = Modifier.fillMaxWidth(),
            color = when {
                progressPercent >= 0.9f -> MaterialTheme.colorScheme.tertiary
                progressPercent >= 0.7f -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.primary
            }
        )
        HeroesVerticalSpacer(4.dp)
        Text(
            text = "${(progressPercent * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Filter chips for lesson filtering by category and grade
 */
@Composable
fun LessonFilterChips(
    selectedCategory: LessonCategory?,
    selectedGrade: GradeLevel?,
    onCategorySelected: (LessonCategory?) -> Unit,
    onGradeSelected: (GradeLevel?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Category filters
        Text(
            text = "Category",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HeroesVerticalSpacer(8.dp)
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategorySelected(null) },
                    label = { Text("All") }
                )
            }
            items(LessonCategory.values().size) { index ->
                val category = LessonCategory.values()[index]
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                    label = { Text(category.displayName) },
                    leadingIcon = {
                        Icon(
                            imageVector = getCategoryIcon(category),
                            contentDescription = null
                        )
                    }
                )
            }
        }
        
        HeroesVerticalSpacer(16.dp)
        
        // Grade filters
        Text(
            text = "Grade Level",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HeroesVerticalSpacer(8.dp)
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedGrade == null,
                    onClick = { onGradeSelected(null) },
                    label = { Text("All Grades") }
                )
            }
            items(GradeLevel.values().size) { index ->
                val grade = GradeLevel.values()[index]
                FilterChip(
                    selected = selectedGrade == grade,
                    onClick = { onGradeSelected(grade) },
                    label = { Text(grade.displayName) }
                )
            }
        }
    }
}

/**
 * Action buttons for lesson interactions
 */
@Composable
fun LessonActionButtons(
    lesson: Lesson,
    onStartLesson: () -> Unit,
    onDownload: () -> Unit,
    onBookmark: () -> Unit,
    isDownloading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Bookmark button
        IconButton(
            onClick = onBookmark,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = if (lesson.isBookmarked) {
                    Icons.Default.Bookmark
                } else {
                    Icons.Default.BookmarkBorder
                },
                contentDescription = "Bookmark lesson",
                tint = if (lesson.isBookmarked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        
        // Download button
        HeroesSecondaryButton(
            text = when {
                lesson.isDownloaded -> "Downloaded"
                isDownloading -> "Downloading..."
                else -> "Download"
            },
            onClick = onDownload,
            enabled = !isDownloading && !lesson.isDownloaded,
            modifier = Modifier.weight(1f)
        )
        
        // Start lesson button
        HeroesLargeButton(
            text = "Start",
            onClick = onStartLesson,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Search bar component for lesson filtering
 */
@Composable
fun LessonSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    HeroTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = "Search lessons...",
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search"
            )
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search"
                    )
                }
            }
        } else null,
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Lesson statistics card for displaying completion metrics
 */
@Composable
fun LessonStatsCard(
    totalLessons: Int,
    completedLessons: Int,
    inProgressLessons: Int,
    modifier: Modifier = Modifier
) {
    HeroesCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Curriculum Progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            HeroesVerticalSpacer(16.dp)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "Completed",
                    value = completedLessons.toString(),
                    color = MaterialTheme.colorScheme.tertiary
                )
                StatItem(
                    label = "In Progress",
                    value = inProgressLessons.toString(),
                    color = MaterialTheme.colorScheme.secondary
                )
                StatItem(
                    label = "Remaining",
                    value = (totalLessons - completedLessons - inProgressLessons).toString(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            HeroesVerticalSpacer(16.dp)
            
            val progressPercent = if (totalLessons > 0) {
                (completedLessons.toFloat() / totalLessons.toFloat()) * 100f
            } else 0f
            
            LinearProgressIndicator(
                progress = progressPercent / 100f,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
            
            HeroesVerticalSpacer(8.dp)
            
            Text(
                text = "${progressPercent.toInt()}% of curriculum completed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Get appropriate icon for lesson category
 */
private fun getCategoryIcon(category: LessonCategory): ImageVector {
    return when (category) {
        LessonCategory.INTRODUCTION -> Icons.Default.Star
        LessonCategory.EMPATHY -> Icons.Default.Favorite
        LessonCategory.COMMUNICATION -> Icons.Default.Chat
        LessonCategory.CONFLICT_RESOLUTION -> Icons.Default.Handshake
        LessonCategory.LEADERSHIP -> Icons.Default.Person
        LessonCategory.COMMUNITY -> Icons.Default.Group
        LessonCategory.REFLECTION -> Icons.Default.Psychology
        LessonCategory.ASSESSMENT -> Icons.Default.Assessment
    }
} 