# Lesson Management System Implementation Plan

## Overview
Implement LessonSelectionScreen and LessonDetailScreen following the established patterns from CreateClassroomScreen, integrating with the existing Lesson data model and LessonRepository.

## Architecture Analysis
Based on codebase review:
- **Lesson Model**: Comprehensive with 12 curriculum lessons, content sections, activities, assessments
- **LessonRepository**: Full MVVM support with offline-first approach, search/filter capabilities
- **Navigation**: Screen.kt already has LessonManagement and LessonDetails routes
- **Components**: HeroesComponents.kt provides consistent UI library
- **Patterns**: CreateClassroomScreen shows established MVVM + StateFlow patterns

## Implementation Tasks

### ✅ Task 1: Analyze Existing Codebase
- [x] Review Lesson data model structure (comprehensive with 12 lessons, content, activities)
- [x] Examine LessonRepository methods (getAllLessons, search, filters, offline support)
- [x] Study navigation patterns in Screen.kt (routes already defined)
- [x] Review HeroesComponents UI library (HeroCard, HeroButton, grid layouts available)
- [x] Understand CreateClassroomScreen MVVM patterns (StateFlow, validation, events)

### ⏳ Task 2: Update Navigation Routes
- [ ] Add LessonSelection route to Screen.kt for lesson browsing
- [ ] Verify LessonDetails route handles lessonId parameter correctly
- [ ] Update FacilitatorDashboardScreen to navigate to lesson selection
- [ ] Ensure proper back navigation flow

### ⏳ Task 3: Create LessonSelectionViewModel
- [ ] Implement StateFlow-based reactive state management
- [ ] Add lesson loading, search, and filter logic
- [ ] Create error handling and loading states
- [ ] Implement grade-based filtering using LessonRepository
- [ ] Add search functionality with LessonRepository.searchLessons()
- [ ] Create lesson selection event handling

### ⏳ Task 4: Create LessonDetailViewModel  
- [ ] Implement lesson loading by ID using LessonRepository.getLessonById()
- [ ] Add download/bookmark functionality with repository methods
- [ ] Create tabbed content state management (Overview, Materials, Guide)
- [ ] Implement lesson start functionality
- [ ] Add error handling and loading states

### ⏳ Task 5: Implement LessonSelectionScreen
- [ ] Create grid layout using LazyVerticalGrid for 12 lessons
- [ ] Design lesson cards with title, description, grade level, duration
- [ ] Add progress indicators showing completion status
- [ ] Implement search bar using HeroTextField component
- [ ] Add filter chips for grade level and category
- [ ] Create navigation to LessonDetailScreen
- [ ] Use HeroesComponents for consistent styling

### ⏳ Task 6: Implement LessonDetailScreen
- [ ] Create comprehensive lesson information display
- [ ] Implement tabbed interface (Overview, Materials, Facilitator Guide)
- [ ] Add lesson overview section with objectives and key terms
- [ ] Create materials tab with resources and handouts
- [ ] Design facilitator guide tab with teaching notes
- [ ] Add start lesson button with proper navigation
- [ ] Implement download/bookmark functionality
- [ ] Add back navigation with proper state management

### ⏳ Task 7: Create Lesson UI Components
- [ ] Design LessonCard component for grid display
- [ ] Create LessonProgressIndicator for completion status
- [ ] Build LessonFilterChips for category/grade filtering
- [ ] Design LessonTabLayout for detail screen sections
- [ ] Create LessonActionButtons (start, download, bookmark)
- [ ] Ensure all components follow Heroes design system

### ⏳ Task 8: Integration Testing
- [ ] Test lesson selection grid loads all 12 lessons correctly
- [ ] Verify search functionality works with lesson titles/descriptions
- [ ] Test filter functionality by grade and category
- [ ] Ensure lesson detail screen shows comprehensive information
- [ ] Test tabbed navigation within lesson details
- [ ] Verify download and bookmark functionality
- [ ] Test navigation flow from dashboard → selection → detail → back

### ⏳ Task 9: COPPA Compliance & Professional Interface
- [ ] Ensure educator-focused interface design
- [ ] Verify no student data collection in lesson browsing
- [ ] Review accessibility compliance (48dp+ touch targets)
- [ ] Test with Material3 and Heroes design system
- [ ] Validate professional educator experience

### ⏳ Task 10: Error Handling & Edge Cases
- [ ] Test offline functionality with cached lessons
- [ ] Handle network errors gracefully
- [ ] Test empty states (no lessons, no search results)
- [ ] Verify loading states throughout the flow
- [ ] Test lesson detail loading failures
- [ ] Ensure proper error messages and retry options

## Technical Implementation Details

### LessonSelectionViewModel Structure
```kotlin
@HiltViewModel
class LessonSelectionViewModel @Inject constructor(
    private val lessonRepository: LessonRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LessonSelectionUiState())
    val uiState: StateFlow<LessonSelectionUiState> = _uiState.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _selectedGrade = MutableStateFlow<Grade?>(null)
    val selectedGrade: StateFlow<Grade?> = _selectedGrade.asStateFlow()
    
    private val _selectedCategory = MutableStateFlow<LessonCategory?>(null)
    val selectedCategory: StateFlow<LessonCategory?> = _selectedCategory.asStateFlow()
    
    val lessons: StateFlow<List<Lesson>> = combine(
        searchQuery,
        selectedGrade,
        selectedCategory
    ) { query, grade, category ->
        // Filter and search logic
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
```

### LessonDetailViewModel Structure
```kotlin
@HiltViewModel  
class LessonDetailViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val lessonId: String = checkNotNull(savedStateHandle["lessonId"])
    
    private val _uiState = MutableStateFlow(LessonDetailUiState())
    val uiState: StateFlow<LessonDetailUiState> = _uiState.asStateFlow()
    
    private val _selectedTab = MutableStateFlow(LessonDetailTab.OVERVIEW)
    val selectedTab: StateFlow<LessonDetailTab> = _selectedTab.asStateFlow()
    
    val lesson: StateFlow<Lesson?> = lessonRepository.getLessonById(lessonId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
```

### UI Component Requirements
- **LessonCard**: Display lesson info in grid, navigate to detail
- **LessonProgressIndicator**: Show completion status with visual feedback  
- **LessonFilterChips**: Grade level and category filtering
- **LessonDetailTabs**: Overview, Materials, Facilitator Guide sections
- **LessonActionButtons**: Start lesson, download, bookmark actions

## Success Criteria
- [ ] 12 Heroes in Waiting lessons display in organized grid layout
- [ ] Search and filter functionality works correctly
- [ ] Lesson detail screen shows comprehensive information
- [ ] Tabbed content navigation works smoothly
- [ ] Professional educator interface maintained
- [ ] COPPA compliance preserved
- [ ] Offline functionality supports lesson browsing
- [ ] Integration with dashboard navigation complete

## Dependencies
- ✅ Lesson.kt data model (comprehensive structure available)
- ✅ LessonRepository.kt (full MVVM support with offline-first)
- ✅ HeroesComponents.kt (UI library with cards, buttons, grids)
- ✅ Navigation Screen.kt (routes already defined)
- ✅ CreateClassroomScreen pattern (MVVM + StateFlow example)

## Estimated Implementation
- **ViewModels**: 2-3 hours (following CreateClassroomViewModel pattern)
- **LessonSelectionScreen**: 3-4 hours (grid layout, search, filters)
- **LessonDetailScreen**: 4-5 hours (tabs, comprehensive display)
- **UI Components**: 2-3 hours (lesson-specific components)
- **Integration & Testing**: 2-3 hours
- **Total**: 13-18 hours

This implementation will provide a complete lesson browsing and detail system that integrates seamlessly with the existing Heroes in Waiting Android app architecture.