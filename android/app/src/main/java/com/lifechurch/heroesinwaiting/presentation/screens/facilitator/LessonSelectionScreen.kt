package com.lifechurch.heroesinwaiting.presentation.screens.facilitator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifechurch.heroesinwaiting.data.model.*
import com.lifechurch.heroesinwaiting.presentation.components.*
import com.lifechurch.heroesinwaiting.presentation.theme.*
import com.lifechurch.heroesinwaiting.presentation.viewmodel.LessonSelectionViewModel
import com.lifechurch.heroesinwaiting.presentation.viewmodel.LessonSelectionEvent

/**
 * Lesson Selection Screen for facilitators
 * Displays the 12 Heroes in Waiting lessons with search and filter capabilities
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonSelectionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLessonDetail: (String) -> Unit,
    viewModel: LessonSelectionViewModel = hiltViewModel()
) {
    // Collect state from ViewModel
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lessons by viewModel.lessons.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedGrade by viewModel.selectedGrade.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
    val isRetrying by viewModel.isRetrying.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()
    
    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LessonSelectionEvent.NavigateToLessonDetail -> {
                    onNavigateToLessonDetail(event.lessonId)
                }
                is LessonSelectionEvent.AuthenticationRequired -> {
                    // TODO: Navigate to auth screen
                }
                is LessonSelectionEvent.NetworkError -> {
                    // TODO: Show network error snackbar
                }
                is LessonSelectionEvent.SyncCompleted -> {
                    // TODO: Show sync success snackbar
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Heroes in Waiting Lessons",
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Offline indicator
                    if (isOffline) {
                        IconButton(
                            onClick = { viewModel.forceRefresh() },
                            enabled = !isRetrying
                        ) {
                            Icon(
                                Icons.Default.CloudOff,
                                contentDescription = "Offline mode - tap to refresh",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    
                    // Refresh button
                    IconButton(
                        onClick = { viewModel.forceRefresh() },
                        enabled = !isRetrying
                    ) {
                        if (isRetrying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Offline status banner
            if (isOffline && uiState.hasCachedData) {
                OfflineStatusBanner(
                    lastSyncTime = lastSyncTime,
                    onRefresh = { viewModel.forceRefresh() }
                )
            }
            
            // Search and filter section
            SearchAndFilterSection(
                searchQuery = searchQuery,
                selectedGrade = selectedGrade,
                selectedCategory = selectedCategory,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onGradeSelected = viewModel::updateGradeFilter,
                onCategorySelected = viewModel::updateCategoryFilter,
                onClearFilters = viewModel::clearAllFilters,
                hasActiveFilters = viewModel.hasActiveFilters(),
                filterSummary = viewModel.getFilterSummary(),
                gradeOptions = viewModel.getGradeOptions(),
                categoryOptions = viewModel.getCategoryOptions()
            )
            
            // Content section
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    uiState.showLoadingState -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            HeroesLoadingIndicator(message = "Loading your hero lessons...")
                        }
                    }
                    
                    uiState.showErrorState -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            EnhancedErrorDisplay(
                                message = uiState.error ?: "Failed to load lessons",
                                hasCachedData = uiState.hasCachedData,
                                isOffline = isOffline,
                                onRetry = { viewModel.retryLoadLessons() },
                                onUseCachedData = { /* Already using cached data */ }
                            )
                        }
                    }
                    
                    uiState.showNoDataState -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyLessonsState(
                                hasActiveFilters = viewModel.hasActiveFilters(),
                                onClearFilters = viewModel::clearAllFilters
                            )
                        }
                    }
                    
                    else -> {
                        LessonsGrid(
                            lessons = lessons,
                            onLessonClick = viewModel::navigateToLessonDetail
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineStatusBanner(
    lastSyncTime: Long?,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Offline Mode",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
                
                lastSyncTime?.let { timestamp ->
                    val minutesAgo = (System.currentTimeMillis() - timestamp) / (1000 * 60)
                    val timeText = when {
                        minutesAgo < 1 -> "Just now"
                        minutesAgo < 60 -> "$minutesAgo minutes ago"
                        minutesAgo < 1440 -> "${minutesAgo / 60} hours ago"
                        else -> "${minutesAgo / 1440} days ago"
                    }
                    
                    Text(
                        text = "Last updated: $timeText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            TextButton(
                onClick = onRefresh,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Refresh")
            }
        }
    }
}

@Composable
private fun EnhancedErrorDisplay(
    message: String,
    hasCachedData: Boolean,
    isOffline: Boolean,
    onRetry: () -> Unit,
    onUseCachedData: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = when {
                isOffline -> Icons.Default.CloudOff
                else -> Icons.Default.Error
            },
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (hasCachedData) {
                OutlinedButton(
                    onClick = onUseCachedData,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.OfflineStorage,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use Cached Data")
                }
            }
            
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Try Again")
            }
        }
    }
}

@Composable
private fun SearchAndFilterSection(
    searchQuery: String,
    selectedGrade: Grade?,
    selectedCategory: LessonCategory?,
    onSearchQueryChange: (String) -> Unit,
    onGradeSelected: (Grade?) -> Unit,
    onCategorySelected: (LessonCategory?) -> Unit,
    onClearFilters: () -> Unit,
    hasActiveFilters: Boolean,
    filterSummary: String,
    gradeOptions: List<Grade>,
    categoryOptions: List<LessonCategory>
) {
    HeroesCard {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Find Your Perfect Lesson",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Search Field
            HeroesTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = "Search lessons",
                placeholder = "Search by title, description, or key terms",
                leadingIcon = Icons.Default.Search
            )
            
            // Filter Chips Row
            LessonFilterChips(
                selectedGrade = selectedGrade,
                selectedCategory = selectedCategory,
                onGradeSelected = onGradeSelected,
                onCategorySelected = onCategorySelected,
                gradeOptions = gradeOptions,
                categoryOptions = categoryOptions
            )
            
            // Active Filters Summary
            if (hasActiveFilters) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Showing: $filterSummary",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    
                    TextButton(onClick = onClearFilters) {
                        Text(
                            text = "Clear All",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonFilterChips(
    selectedGrade: Grade?,
    selectedCategory: LessonCategory?,
    onGradeSelected: (Grade?) -> Unit,
    onCategorySelected: (LessonCategory?) -> Unit,
    gradeOptions: List<Grade>,
    categoryOptions: List<LessonCategory>
) {
    var showGradeMenu by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Grade Filter Chip
        FilterChip(
            onClick = { showGradeMenu = true },
            label = { 
                Text(
                    text = selectedGrade?.displayName ?: "All Grades",
                    style = MaterialTheme.typography.bodyMedium
                ) 
            },
            selected = selectedGrade != null,
            leadingIcon = if (selectedGrade != null) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
            } else null,
            trailingIcon = {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        )
        
        // Category Filter Chip
        FilterChip(
            onClick = { showCategoryMenu = true },
            label = { 
                Text(
                    text = selectedCategory?.displayName ?: "All Categories",
                    style = MaterialTheme.typography.bodyMedium
                ) 
            },
            selected = selectedCategory != null,
            leadingIcon = if (selectedCategory != null) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
            } else null,
            trailingIcon = {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        )
    }
    
    // Grade Menu
    DropdownMenu(
        expanded = showGradeMenu,
        onDismissRequest = { showGradeMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text("All Grades") },
            onClick = {
                onGradeSelected(null)
                showGradeMenu = false
            }
        )
        gradeOptions.forEach { grade ->
            DropdownMenuItem(
                text = { Text(grade.displayName) },
                onClick = {
                    onGradeSelected(grade)
                    showGradeMenu = false
                }
            )
        }
    }
    
    // Category Menu
    DropdownMenu(
        expanded = showCategoryMenu,
        onDismissRequest = { showCategoryMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text("All Categories") },
            onClick = {
                onCategorySelected(null)
                showCategoryMenu = false
            }
        )
        categoryOptions.forEach { category ->
            DropdownMenuItem(
                text = { Text(category.displayName) },
                onClick = {
                    onCategorySelected(category)
                    showCategoryMenu = false
                }
            )
        }
    }
}

@Composable
private fun LessonsGrid(
    lessons: List<Lesson>,
    onLessonClick: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(300.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(lessons) { lesson ->
            LessonCard(
                lesson = lesson,
                onClick = { onLessonClick(lesson.id) }
            )
        }
    }
}

@Composable
private fun LessonCard(
    lesson: Lesson,
    onClick: () -> Unit
) {
    HeroCard(
        title = lesson.title,
        subtitle = lesson.description,
        onClick = onClick,
        icon = when (lesson.category) {
            LessonCategory.BULLYING_PREVENTION -> Icons.Default.Shield
            LessonCategory.EMPATHY_BUILDING -> Icons.Default.Favorite
            LessonCategory.CONFLICT_RESOLUTION -> Icons.Default.Handshake
            LessonCategory.SELF_CONFIDENCE -> Icons.Default.EmojiPeople
            LessonCategory.COMMUNICATION_SKILLS -> Icons.Default.Chat
            LessonCategory.COMMUNITY_BUILDING -> Icons.Default.Groups
            LessonCategory.LEADERSHIP -> Icons.Default.Star
            LessonCategory.RESPECT_AND_KINDNESS -> Icons.Default.VolunteerActivism
        }
    )
}

@Composable
private fun EmptyLessonsState(
    hasActiveFilters: Boolean,
    onClearFilters: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📚",
            fontSize = 64.sp
        )
        
        HeroesVerticalSpacer(16.dp)
        
        Text(
            text = if (hasActiveFilters) "No lessons match your filters" else "No lessons available",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        HeroesVerticalSpacer(8.dp)
        
        Text(
            text = if (hasActiveFilters) {
                "Try adjusting your search terms or filters to find more lessons."
            } else {
                "Lessons will appear here when they become available."
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (hasActiveFilters) {
            HeroesVerticalSpacer(24.dp)
            
            HeroesSecondaryButton(
                text = "Clear All Filters",
                onClick = onClearFilters
            )
        }
    }
}