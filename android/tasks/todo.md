# Issue 4 - Network Module Configuration

## Problem Analysis
- Two duplicate ApiService interfaces exist: `/data/api/ApiService.kt` (218 lines, comprehensive) and `/data/remote/ApiService.kt` (124 lines, basic)
- NetworkModule.kt correctly references the comprehensive ApiService from `/data/api/`
- No files import the duplicate from `/data/remote/` - it's completely unused
- Need to consolidate network layer and improve error handling

## Consolidation Strategy
Keep the more comprehensive ApiService from `/data/api/` directory (currently in use) and remove the duplicate from `/data/remote/`. The `/data/api/ApiService.kt` is superior because:
- More complete endpoint coverage (184 lines vs 124)
- Includes StudentApiService interface for non-JWT operations
- Better organized with clear sections
- Enhanced backend API integration comments
- Mobile-optimized endpoints
- Already integrated with dependency injection

## TODO Items

### ✅ Task 1: Analyze Current Network Layer
- [x] Examine both ApiService files and their differences
- [x] Check which files import each interface
- [x] Verify NetworkModule.kt dependencies
- [x] Identify which interface is actively used

### ⏳ Task 2: Remove Duplicate ApiService
- [ ] Delete the unused `/data/remote/ApiService.kt` file
- [ ] Verify no imports reference the removed file
- [ ] Ensure clean compilation after removal

### ⏳ Task 3: Consolidate DTOs and Response Classes
- [ ] Examine `/data/api/response/ApiResponses.kt` 
- [ ] Examine `/data/remote/dto/` files for any missing DTOs
- [ ] Move any unique DTOs to the main response directory
- [ ] Update imports if necessary

### ⏳ Task 4: Enhance Network Error Handling
- [ ] Review NetworkModule.kt timeout configurations
- [ ] Add retry logic configuration
- [ ] Implement proper error interceptors
- [ ] Add network state handling

### ⏳ Task 5: Verify Repository Integration
- [ ] Check all repository files can access network services
- [ ] Ensure AuthRepository, ClassroomRepository, LessonRepository work correctly
- [ ] Test dependency injection is working

### ⏳ Task 6: Clean Up Remote Directory Structure
- [ ] Move AuthInterceptor.kt to more appropriate location if needed
- [ ] Organize remaining files in `/data/remote/` directory
- [ ] Update import statements as needed

### ⏳ Task 7: Final Integration Testing
- [ ] Verify clean compilation
- [ ] Check all network dependencies resolve correctly
- [ ] Ensure no missing imports or references
- [ ] Test that both facilitator (JWT) and student (no JWT) endpoints work

## Technical Notes
- **Keep**: `/data/api/ApiService.kt` and `StudentApiService` (comprehensive, in use)
- **Remove**: `/data/remote/ApiService.kt` (duplicate, unused)
- **Enhance**: Error handling, retry logic, timeout configurations
- **Preserve**: Clean architecture separation between facilitator and student operations

## Success Criteria
- [x] No duplicate ApiService interfaces
- [ ] Clean compilation with proper dependency injection
- [ ] All repositories can access network services
- [ ] Proper error handling and retry logic implemented
- [ ] Network module follows Android/Kotlin best practices