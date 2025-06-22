package com.lifechurch.heroesinwaiting.presentation.screens.facilitator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifechurch.heroesinwaiting.presentation.components.HeroesComponents.*
import com.lifechurch.heroesinwaiting.presentation.viewmodel.LessonDetailTab
import com.lifechurch.heroesinwaiting.presentation.viewmodel.LessonDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonDetailScreen(
    lessonId: String,
    onStartLesson: (String, String) -> Unit,
    onBack: () -> Unit,
    viewModel: LessonDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(lessonId) {
        viewModel.loadLesson(lessonId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.lesson?.title ?: "Lesson Details",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Bookmark button
                    IconButton(
                        onClick = { viewModel.toggleBookmark() },
                        enabled = uiState.lesson != null
                    ) {
                        Icon(
                            imageVector = if (uiState.lesson?.isBookmarked == true) {
                                Icons.Default.Bookmark
                            } else {
                                Icons.Default.BookmarkBorder
                            },
                            contentDescription = "Bookmark lesson"
                        )
                    }
                    
                    // Share button
                    IconButton(
                        onClick = { /* TODO: Implement share */ },
                        enabled = uiState.lesson != null
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share lesson")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    HeroesLoadingIndicator()
                }
                uiState.error != null -> {
                    HeroesErrorDisplay(
                        message = uiState.error!!,
                        onRetry = { viewModel.loadLesson(lessonId) }
                    )
                }
                uiState.lesson != null -> {
                    LessonDetailContent(
                        lesson = uiState.lesson!!,
                        selectedTab = uiState.selectedTab,
                        isDownloading = uiState.isDownloading,
                        onTabSelected = { viewModel.setSelectedTab(it) },
                        onDownload = { viewModel.downloadLesson() },
                        onStartLesson = { classroomId -> onStartLesson(lessonId, classroomId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LessonDetailContent(
    lesson: com.lifechurch.heroesinwaiting.data.model.Lesson,
    selectedTab: LessonDetailTab,
    isDownloading: Boolean,
    onTabSelected: (LessonDetailTab) -> Unit,
    onDownload: () -> Unit,
    onStartLesson: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Hero Header Section
        LessonHeroHeader(
            lesson = lesson,
            isDownloading = isDownloading,
            onDownload = onDownload,
            onStartLesson = onStartLesson
        )
        
        // Tab Layout
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            modifier = Modifier.fillMaxWidth()
        ) {
            LessonDetailTab.values().forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    text = { Text(tab.displayName) }
                )
            }
        }
        
        // Tab Content
        when (selectedTab) {
            LessonDetailTab.OVERVIEW -> LessonOverviewTab(lesson = lesson)
            LessonDetailTab.MATERIALS -> LessonMaterialsTab(lesson = lesson)
            LessonDetailTab.FACILITATOR_GUIDE -> LessonFacilitatorGuideTab(lesson = lesson)
        }
    }
}

@Composable
private fun LessonHeroHeader(
    lesson: com.lifechurch.heroesinwaiting.data.model.Lesson,
    isDownloading: Boolean,
    onDownload: () -> Unit,
    onStartLesson: (String) -> Unit
) {
    HeroesCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Lesson number and category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Badge {
                    Text("Lesson ${lesson.lessonNumber}")
                }
                Text(
                    text = lesson.category.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            HeroesVerticalSpacer(12.dp)
            
            // Title and description
            Text(
                text = lesson.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            HeroesVerticalSpacer(8.dp)
            
            Text(
                text = lesson.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            HeroesVerticalSpacer(16.dp)
            
            // Metadata row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${lesson.totalEstimatedTime} mins",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Grades ${lesson.targetGrades.joinToString { it.displayName }}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            HeroesVerticalSpacer(16.dp)
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeroesSecondaryButton(
                    text = if (lesson.isDownloaded) "Downloaded" else "Download",
                    onClick = onDownload,
                    enabled = !isDownloading && !lesson.isDownloaded,
                    modifier = Modifier.weight(1f)
                )
                
                HeroesLargeButton(
                    text = "Start Lesson",
                    onClick = { onStartLesson("classroom-id") }, // TODO: Get actual classroom ID
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LessonOverviewTab(lesson: com.lifechurch.heroesinwaiting.data.model.Lesson) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Learning Objectives
        HeroesSectionHeader("Learning Objectives")
        HeroesVerticalSpacer(8.dp)
        
        lesson.learningObjectives.forEach { objective ->
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text("•", modifier = Modifier.padding(end = 8.dp))
                Text(
                    text = objective,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        HeroesVerticalSpacer(24.dp)
        
        // Key Terms & Concepts
        HeroesSectionHeader("Key Terms & Concepts")
        HeroesVerticalSpacer(8.dp)
        
        lesson.keyTerms.forEach { term ->
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text("•", modifier = Modifier.padding(end = 8.dp))
                Text(
                    text = term,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        HeroesVerticalSpacer(24.dp)
        
        // Lesson Structure
        HeroesSectionHeader("Lesson Structure")
        HeroesVerticalSpacer(8.dp)
        
        lesson.activities.forEachIndexed { index, activity ->
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Badge {
                    Text("${index + 1}")
                }
                HeroesHorizontalSpacer(12.dp)
                Column {
                    Text(
                        text = activity.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${activity.estimatedTime} mins",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LessonMaterialsTab(lesson: com.lifechurch.heroesinwaiting.data.model.Lesson) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Handouts and Worksheets
        HeroesSectionHeader("Handouts & Worksheets")
        HeroesVerticalSpacer(8.dp)
        
        lesson.handouts.forEach { handout ->
            HeroesCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    HeroesHorizontalSpacer(12.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = handout.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = handout.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { /* TODO: Download handout */ }) {
                        Icon(Icons.Default.Download, contentDescription = "Download")
                    }
                }
            }
            HeroesVerticalSpacer(8.dp)
        }
        
        HeroesVerticalSpacer(24.dp)
        
        // Videos and Media
        HeroesSectionHeader("Videos & Media")
        HeroesVerticalSpacer(8.dp)
        
        lesson.videos.forEach { video ->
            HeroesCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    HeroesHorizontalSpacer(12.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${video.duration} mins",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { /* TODO: Play video */ }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                    }
                }
            }
            HeroesVerticalSpacer(8.dp)
        }
    }
}

@Composable
private fun LessonFacilitatorGuideTab(lesson: com.lifechurch.heroesinwaiting.data.model.Lesson) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Teaching Notes
        HeroesSectionHeader("Teaching Notes")
        HeroesVerticalSpacer(8.dp)
        
        Text(
            text = lesson.facilitatorNotes,
            style = MaterialTheme.typography.bodyLarge
        )
        
        HeroesVerticalSpacer(24.dp)
        
        // Discussion Questions
        HeroesSectionHeader("Discussion Questions")
        HeroesVerticalSpacer(8.dp)
        
        lesson.discussionQuestions.forEachIndexed { index, question ->
            HeroesCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Question ${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    HeroesVerticalSpacer(4.dp)
                    Text(
                        text = question,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            HeroesVerticalSpacer(8.dp)
        }
        
        HeroesVerticalSpacer(24.dp)
        
        // Tips and Best Practices
        HeroesSectionHeader("Tips & Best Practices")
        HeroesVerticalSpacer(8.dp)
        
        lesson.facilitationTips.forEach { tip ->
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = tip,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private val LessonDetailTab.displayName: String
    get() = when (this) {
        LessonDetailTab.OVERVIEW -> "Overview"
        LessonDetailTab.MATERIALS -> "Materials"
        LessonDetailTab.FACILITATOR_GUIDE -> "Guide"
    } 