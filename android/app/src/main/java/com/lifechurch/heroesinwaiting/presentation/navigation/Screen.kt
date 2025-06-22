package com.lifechurch.heroesinwaiting.presentation.navigation

/**
 * Navigation destinations for the app
 */
sealed class Screen(val route: String) {
    // Authentication flow
    object Auth : Screen("auth")
    object FacilitatorLogin : Screen("facilitator_login")
    object FacilitatorRegister : Screen("facilitator_register")
    object StudentEnrollment : Screen("student_enrollment")
    object ClassroomPreview : Screen("classroom_preview/{classroomCode}") {
        fun createRoute(classroomCode: String) = "classroom_preview/$classroomCode"
    }
    
    // Facilitator flow
    object FacilitatorDashboard : Screen("facilitator_dashboard")
    object ClassroomManagement : Screen("classroom_management")
    object CreateClassroom : Screen("create_classroom")
    object ClassroomDetails : Screen("classroom_details/{classroomId}") {
        fun createRoute(classroomId: String) = "classroom_details/$classroomId"
    }
    object LessonManagement : Screen("lesson_management")
    object LessonSelection : Screen("lesson_selection")
    object LessonDetails : Screen("lesson_details/{lessonId}") {
        fun createRoute(lessonId: String) = "lesson_details/$lessonId"
    }
    object StartSession : Screen("start_session/{classroomId}") {
        fun createRoute(classroomId: String) = "start_session/$classroomId"
    }
    object LiveSession : Screen("live_session/{sessionId}") {
        fun createRoute(sessionId: String) = "live_session/$sessionId"
    }
    object Analytics : Screen("analytics")
    object FacilitatorProfile : Screen("facilitator_profile")
    
    // Student flow
    object StudentDashboard : Screen("student_dashboard")
    object StudentLesson : Screen("student_lesson/{lessonId}") {
        fun createRoute(lessonId: String) = "student_lesson/$lessonId"
    }
    object StudentActivity : Screen("student_activity/{activityId}") {
        fun createRoute(activityId: String) = "student_activity/$activityId"
    }
    object EmotionalCheckin : Screen("emotional_checkin")
    object StudentProgress : Screen("student_progress")
    object StudentHelp : Screen("student_help")
    
    // Shared screens
    object Settings : Screen("settings")
    object Help : Screen("help")
    object About : Screen("about")
}