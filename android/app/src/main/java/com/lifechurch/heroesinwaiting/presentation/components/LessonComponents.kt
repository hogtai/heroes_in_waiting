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
import com.lifechurch.heroesinwaiting.data.repository.DownloadProgress

/**
 * Lesson Card component for displaying lesson information in grid layouts
 * Enhanced with accessibility, visual feedback, and dark mode support
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
            .semantics {
                contentDescription = "Lesson ${lesson.lessonNumber}: ${lesson.title}"
                role = Role.Button
                stateDescription = when {
                    !isAvailable -> "Not available"
                    progressPercent > 0 -> "In progress"
                    lesson.isDownloaded -> "Downloaded"
                    else -> "Available"
                }
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Lesson number and category badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Badge(
                    colors = BadgeDefaults.badgeColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text(
                        text = "Lesson ${lesson.lessonNumber}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = getCategoryIcon(lesson.category),
                    contentDescription = "Category: ${lesson.category.displayName}",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            HeroesVerticalSpacer(12.dp)
            
            // Title and description
            Text(
                text = lesson.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
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
            
            // Progress indicator (if applicable)
            if (progressPercent > 0) {
                LinearProgressIndicator(
                    progress = progressPercent,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                HeroesVerticalSpacer(8.dp)
            }
            
            // Lesson metadata
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Duration
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${lesson.estimatedDuration}m",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Grade levels
                Text(
                    text = lesson.targetGrades.joinToString(", ") { it.displayName },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Offline indicator
            if (lesson.isDownloaded) {
                HeroesVerticalSpacer(8.dp)
                OfflineLessonIndicator(
                    isAvailableOffline = true,
                    modifier = Modifier.fillMaxWidth()
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

/**
 * Download progress indicator for lesson downloads
 */
@Composable
fun LessonDownloadProgressIndicator(
    downloadProgress: DownloadProgress?,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (downloadProgress) {
                    is DownloadProgress.Starting -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Preparing download...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    is DownloadProgress.Downloading -> {
                        CircularProgressIndicator(
                            progress = downloadProgress.progress / 100f,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Downloading... ${downloadProgress.progress}%",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    is DownloadProgress.Completed -> {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Download completed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    is DownloadProgress.Failed -> {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Download failed: ${downloadProgress.error}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    null -> {
                        // No download in progress
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Cancel button (only show during download)
                if (downloadProgress is DownloadProgress.Downloading || downloadProgress is DownloadProgress.Starting) {
                    TextButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Cancel")
                    }
                }
            }
            
            // Progress bar for downloading state
            if (downloadProgress is DownloadProgress.Downloading) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = downloadProgress.progress / 100f,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Offline lesson indicator
 */
@Composable
fun OfflineLessonIndicator(
    isAvailableOffline: Boolean,
    modifier: Modifier = Modifier
) {
    if (isAvailableOffline) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.OfflineStorage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Available offline",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
} 