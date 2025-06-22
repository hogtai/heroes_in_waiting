# Checkpoint 4: Classroom Management System Implementation Plan

## Project Context
**Current Phase**: Checkpoint 4 - Facilitator Interface Development (Task Group 1)
**Focus Area**: Classroom Management System
**Foundation**: Building on Checkpoint 3's complete authentication flows and dashboard foundations

## Architecture Understanding
Based on the existing codebase analysis:
- **Pattern**: MVVM with Clean Architecture using Hilt DI
- **UI Framework**: Jetpack Compose with Material3 design system
- **State Management**: StateFlow and Compose State (following `FacilitatorDashboardViewModel` pattern)
- **Navigation**: Type-safe routing with Jetpack Navigation
- **Database**: Room for local caching (`ClassroomDao`, `ClassroomEntity`)
- **Network**: Retrofit with proper error handling (`ApiService`, `ClassroomRepository`)
- **Components**: Established UI component library (`HeroesComponents.kt`)

## Implementation Strategy
Follow the established patterns from:
- `FacilitatorDashboardScreen.kt` - UI composition patterns
- `FacilitatorDashboardViewModel.kt` - State management and events
- `ClassroomRepository.kt` - Data layer with offline-first approach
- `HeroesComponents.kt` - Consistent UI component usage

## TODO List

### ✅ Task 1.1: Create Classroom Creation Screen
- [ ] **Task 1.1.1**: Create `CreateClassroomScreen.kt` 
  - Form for classroom name, description, grade level (4-6)
  - Student capacity settings (default 30)
  - Heroes in Waiting curriculum selection
  - Follow Material3 design patterns from existing screens

- [ ] **Task 1.1.2**: Implement `CreateClassroomViewModel.kt`
  - StateFlow-based state management (follow `FacilitatorDashboardViewModel` pattern)
  - Form validation and error handling
  - Integration with existing `ClassroomRepository.createClassroom()`
  - Navigation events for success/failure

- [ ] **Task 1.1.3**: Update Navigation Routes
  - Add route handling to existing navigation system
  - Connect to FAB in `FacilitatorDashboardScreen.kt`
  - Handle navigation events from dashboard

### ✅ Task 1.2: Create Classroom Management Overview Screen
- [ ] **Task 1.2.1**: Create `ClassroomManagementScreen.kt`
  - List of facilitator's active classrooms (use existing `getFacilitatorClassrooms()`)
  - Edit/archive classroom functionality
  - Quick stats per classroom using `ClassroomStats`

- [ ] **Task 1.2.2**: Implement `ClassroomManagementViewModel.kt`
  - Classroom list state management
  - Edit/archive operations
  - Navigation to classroom details

- [ ] **Task 1.2.3**: Update Navigation
  - Add classroom management route
  - Connect from dashboard quick actions

### ✅ Task 1.3: Create Student Roster Management
- [ ] **Task 1.3.1**: Create `StudentRosterScreen.kt`
  - View enrolled students (demographic data only, no PII)
  - Student progress overview
  - Manual enrollment/removal controls
  - Follow COPPA compliance patterns from `Student` model

- [ ] **Task 1.3.2**: Implement `StudentRosterViewModel.kt`
  - Student list management
  - Progress data aggregation
  - Roster modification operations

### ✅ Task 1.4: Create Classroom Code Sharing Component
- [ ] **Task 1.4.1**: Create `ClassroomCodeComponent.kt`
  - Display current classroom code
  - Share functionality (text sharing)
  - Code regeneration controls
  - Use `ClassroomUtils.generateClassroomCode()`

- [ ] **Task 1.4.2**: Enhance Repository Methods
  - Add code regeneration to `ClassroomRepository.kt`
  - Code sharing utilities
  - Update local cache with new codes

### ✅ Task 1.5: Integration and Navigation Updates
- [ ] **Task 1.5.1**: Update `Screen.kt` Navigation Routes
  - Add new screen routes for all new screens
  - Maintain type-safe routing patterns

- [ ] **Task 1.5.2**: Update `MainActivity.kt` Navigation Handling
  - Wire up new routes to screen composables
  - Handle navigation arguments properly

- [ ] **Task 1.5.3**: Connect Dashboard Integration
  - Update `FacilitatorDashboardScreen.kt` navigation handlers
  - Ensure proper data flow between screens

### ✅ Task 1.6: Testing and Quality Assurance
- [ ] **Task 1.6.1**: Test Classroom Creation Flow
  - Form validation works correctly
  - API integration creates classrooms
  - Navigation flow is smooth

- [ ] **Task 1.6.2**: Test Classroom Management
  - List displays correctly
  - Edit/archive operations work
  - Student roster management functions

- [ ] **Task 1.6.3**: Test Integration Points
  - Dashboard navigation works
  - State persistence across screens
  - Error handling and offline scenarios

## Implementation Details

### Form Validation Rules
- **Classroom Name**: 1-100 characters, required
- **Description**: Optional, max 500 characters
- **Grade Level**: Must be GRADE_4, GRADE_5, or GRADE_6
- **Max Students**: 5-50 students, default 30

### UI Component Reuse
- Use `HeroesCard` for main content containers
- Use `HeroesTextField` for form inputs
- Use `HeroesLargeButton` for primary actions
- Use `HeroesSecondaryButton` for secondary actions
- Use `HeroesLoadingIndicator` for loading states
- Use `HeroesErrorDisplay` for error states

### State Management Pattern
```kotlin
// Follow existing ViewModel pattern
data class CreateClassroomUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val validationErrors: Map<String, String> = emptyMap(),
    val isFormValid: Boolean = false
)

sealed class CreateClassroomEvent {
    object NavigateBack : CreateClassroomEvent()
    data class ClassroomCreated(val classroom: Classroom) : CreateClassroomEvent()
    data class ShowError(val message: String) : CreateClassroomEvent()
}
```

### Error Handling Strategy
- Use `Result<T>` pattern for repository methods
- Display user-friendly error messages
- Maintain offline-first approach with cached data
- Graceful degradation when network unavailable

### COPPA Compliance
- Student data displays demographic info only
- No personally identifiable information shown
- Use `Student.sessionId` for identification
- Follow patterns from existing `DemographicInfo` model

## File Locations

### New Files to Create
1. `/android/app/src/main/java/com/lifechurch/heroesinwaiting/presentation/screens/facilitator/CreateClassroomScreen.kt`
2. `/android/app/src/main/java/com/lifechurch/heroesinwaiting/presentation/screens/facilitator/ClassroomManagementScreen.kt`
3. `/android/app/src/main/java/com/lifechurch/heroesinwaiting/presentation/screens/facilitator/StudentRosterScreen.kt`
4. `/android/app/src/main/java/com/lifechurch/heroesinwaiting/presentation/components/ClassroomCodeComponent.kt`
5. `/android/app/src/main/java/com/lifechurch/heroesinwaiting/presentation/viewmodel/CreateClassroomViewModel.kt`
6. `/android/app/src/main/java/com/lifechurch/heroesinwaiting/presentation/viewmodel/ClassroomManagementViewModel.kt`
7. `/android/app/src/main/java/com/lifechurch/heroesinwaiting/presentation/viewmodel/StudentRosterViewModel.kt`

### Files to Update
1. `/android/app/src/main/java/com/lifechurch/heroesinwaiting/presentation/navigation/Screen.kt`
2. `/android/app/src/main/java/com/lifechurch/heroesinwaiting/presentation/MainActivity.kt`
3. `/android/app/src/main/java/com/lifechurch/heroesinwaiting/data/repository/ClassroomRepository.kt` (if needed)

## Success Criteria
- [ ] Facilitators can create new classrooms with proper validation
- [ ] Classroom management screen shows all facilitator classrooms
- [ ] Student roster displays students with demographic data only
- [ ] Classroom codes can be shared and regenerated
- [ ] All screens follow existing design patterns and architecture
- [ ] Navigation flows work seamlessly with existing dashboard
- [ ] Error handling and loading states work properly
- [ ] COPPA compliance maintained throughout

## Next Steps After Implementation
1. Conduct thorough testing of all new screens
2. Verify integration with existing dashboard
3. Review for COPPA compliance
4. Prepare for Task Group 2 (Lesson Management System)
5. Document any architectural decisions or patterns established

---

**Implementation Approach**: Start with the simplest screen (CreateClassroomScreen) to establish patterns, then build more complex screens following the same approach. Each screen should be fully functional before moving to the next to ensure quality and maintainability.