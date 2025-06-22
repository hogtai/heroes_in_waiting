# Heroes in Waiting - Project Restart Prompt

Use this prompt when starting a new Claude Code conversation to continue the Heroes in Waiting project:

---

## Project Continuation Prompt

I'm continuing work on the **Heroes in Waiting** educational mobile app project. This is an anti-bullying curriculum app for elementary students (grades 4-6) with dual interfaces for facilitators and students.

You are the project manager in charge of coordinating a team of agents each with their own expertise. Each member of the team has its own instructions in a seperate .md file within the /agents directory. It's your job to work the projecplan.md and coordinator their efforts.

Code changes should be periodically pushed to github with commit messages. **IMPORTANT**: Do not include any references to Claude within all commit messages. The agents should inform each others work through collaboration to ensure the project is completed to a high amount of quality.

**Project Status**: Checkpoint 3 is 100% complete with all 4 critical issues resolved and QA certified. Ready to begin Checkpoint 4 - Facilitator Interface Development.

**Please help me with the following:**

1. **Read the project plan**: Review `/projectplan.md` to understand the current status with checkmarks showing completed work through Checkpoint 3, update as needed.

2. **Check current progress**: Examine `/tasks/todo.md` for the detailed completion plan and any remaining tasks, update as needed.

3. **Review the codebase**: The Android app foundation is complete with:
   - Kotlin/Jetpack Compose architecture
   - Clean Architecture with MVVM
   - Authentication flows (facilitator JWT + student classroom codes)
   - Core UI components and navigation
   - Room database and API integration

4. **Begin Checkpoint 4**: Help me coordinate the team to start Facilitator Interface Development:
   
   **ALL CHECKPOINT 3 ISSUES COMPLETED**:
   - ✅ **Issue 1**: Database consolidation (duplicate implementations resolved)
   - ✅ **Issue 2**: UI components verification (all components implemented and working)
   - ✅ **Issue 3**: Dashboard screen implementations (complete with ViewModels, data loading, error handling)
   - ✅ **Issue 4**: Network module configuration (API services consolidated, dependency injection fixed)
   - ✅ **QA Certification**: End-to-end authentication flows tested and production-ready
   
   Ready to begin Checkpoint 4 tasks: classroom management, lesson delivery, analytics dashboard, and content management.

**Technical Stack**:
- Frontend: Android (Kotlin/Jetpack Compose)
- Backend: Node.js/Express (already functional)
- Database: PostgreSQL (already implemented)
- Architecture: Clean Architecture with MVVM

**Development Team** (AI Agents in `/agents/` folder):

1. **Kotlin Mobile Developer** (`kotlin_mobile_dev.md`)
   - **Role**: Senior Android developer specializing in Kotlin/Jetpack Compose
   - **Focus**: Mobile app development, Clean Architecture, UI/UX implementation
   - **Current Status**: Completed all 4 critical issues. Dashboard screens with ViewModels and network module consolidated. Ready for Checkpoint 4.

2. **iOS Mobile Developer** (`ios_mobile_dev.md`)
   - **Role**: Senior iOS developer specializing in Swift/SwiftUI/UIKit
   - **Focus**: iOS app development, Apple HIG compliance, App Store deployment
   - **Current Status**: On standby for potential iOS version in future phases

3. **Product Designer** (`product_designer.md`)
   - **Role**: Expert UX/UI designer for mobile applications
   - **Focus**: User research, design systems, wireframes, platform-specific guidelines
   - **Current Status**: Design system foundation complete, supporting feature development

4. **QA Testers** (`qa_testers.md`)
   - **Role**: Automated and manual testing specialists
   - **Focus**: Cross-platform testing, test automation, regression testing
   - **Current Status**: Certified Checkpoint 3 completion. Ready to validate Checkpoint 4 implementations

5. **Database Administrator** (`dba.md`)
   - **Role**: PostgreSQL expert for performance and security
   - **Focus**: Schema optimization, security, backup/recovery, performance tuning
   - **Current Status**: Database foundation complete, monitoring ongoing

6. **Site Reliability Engineer** (`sre.md`)
   - **Role**: Infrastructure reliability and performance expert
   - **Focus**: SLO/SLI monitoring, incident response, cost optimization
   - **Current Status**: Backend infrastructure stable, monitoring production readiness

7. **Cybersecurity Engineer** (`cybersecurity.md`)
   - **Role**: Application security specialist
   - **Focus**: COPPA compliance, secure coding, vulnerability scanning, CI/CD security
   - **Current Status**: Security framework implemented, ongoing security reviews

**Key Files to Reference**:
- `/projectplan.md` - Master project plan with progress tracking
- `/tasks/todo.md` - Detailed task management and completion status
- `/agents/kotlin_mobile_dev.md` - Team member instructions
- `/android/` - Android app codebase
- `/CLAUDE.md` - Project workflow instructions

Please follow the standard workflow outlined in CLAUDE.md: read relevant files, create a plan in tasks/todo.md, check with me for approval, then coordinate the team to execute Checkpoint 4 development.

---

**Copy and paste this prompt when starting a new Claude Code conversation to seamlessly continue the Heroes in Waiting project.**