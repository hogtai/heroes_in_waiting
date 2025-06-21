package com.lifechurch.heroesinwaiting.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.lifechurch.heroesinwaiting.data.api.ApiService
import com.lifechurch.heroesinwaiting.data.api.StudentApiService
import com.lifechurch.heroesinwaiting.data.database.dao.FacilitatorDao
import com.lifechurch.heroesinwaiting.data.database.dao.StudentDao
import com.lifechurch.heroesinwaiting.data.database.entity.FacilitatorEntity
import com.lifechurch.heroesinwaiting.data.database.entity.StudentEntity
import com.lifechurch.heroesinwaiting.data.model.*
import com.lifechurch.heroesinwaiting.data.api.response.ClassroomPreviewResponse
import com.lifechurch.heroesinwaiting.data.api.response.StudentEnrollmentResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for authentication with dual auth flows:
 * 1. Facilitator authentication with JWT tokens
 * 2. Student authentication with classroom codes (COPPA-compliant)
 */
@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val studentApiService: StudentApiService,
    private val facilitatorDao: FacilitatorDao,
    private val studentDao: StudentDao,
    private val dataStore: DataStore<Preferences>
) {
    
    companion object {
        private val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val USER_TYPE_KEY = stringPreferencesKey("user_type")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val SESSION_ID_KEY = stringPreferencesKey("session_id")
        private val IS_LOGGED_IN_KEY = booleanPreferencesKey("is_logged_in")
        private val CLASSROOM_CODE_KEY = stringPreferencesKey("classroom_code")
    }
    
    /**
     * Facilitator login with email and password
     */
    suspend fun loginFacilitator(email: String, password: String): Result<AuthResponse> {
        return try {
            val request = FacilitatorLoginRequest(email, password)
            val response = apiService.loginFacilitator(request)
            
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                
                // Save auth data
                saveAuthData(
                    token = authResponse.token.token,
                    refreshToken = authResponse.token.refreshToken,
                    userType = UserType.FACILITATOR,
                    userId = authResponse.user.id
                )
                
                // Cache facilitator data
                if (authResponse.user is Facilitator) {
                    val facilitatorEntity = FacilitatorEntity.fromDomainModel(authResponse.user)
                    facilitatorDao.insertFacilitator(facilitatorEntity)
                }
                
                Result.success(authResponse)
            } else {
                Result.failure(Exception("Login failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Facilitator registration
     */
    suspend fun registerFacilitator(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        organization: String? = null,
        role: String? = null
    ): Result<AuthResponse> {
        return try {
            val request = FacilitatorRegistrationRequest(
                email, password, firstName, lastName, organization, role
            )
            val response = apiService.registerFacilitator(request)
            
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                
                // Save auth data
                saveAuthData(
                    token = authResponse.token.token,
                    refreshToken = authResponse.token.refreshToken,
                    userType = UserType.FACILITATOR,
                    userId = authResponse.user.id
                )
                
                // Cache facilitator data
                if (authResponse.user is Facilitator) {
                    val facilitatorEntity = FacilitatorEntity.fromDomainModel(authResponse.user)
                    facilitatorDao.insertFacilitator(facilitatorEntity)
                }
                
                Result.success(authResponse)
            } else {
                Result.failure(Exception("Registration failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Student enrollment using classroom code (no authentication required)
     */
    suspend fun enrollStudent(
        classroomCode: String,
        grade: Grade,
        demographicInfo: DemographicInfo
    ): Result<StudentEnrollmentResponse> {
        return try {
            val request = StudentEnrollmentRequest(classroomCode, grade, demographicInfo)
            val response = studentApiService.joinClassroom(request)
            
            if (response.isSuccessful && response.body() != null) {
                val enrollmentResponse = response.body()!!
                
                // Save student session data (no JWT, just session-based)
                saveStudentSessionData(
                    sessionId = enrollmentResponse.sessionId,
                    classroomCode = classroomCode,
                    userId = enrollmentResponse.student.id
                )
                
                // Cache student data locally
                val studentEntity = StudentEntity.fromDomainModel(enrollmentResponse.student)
                studentDao.insertStudent(studentEntity)
                
                Result.success(enrollmentResponse)
            } else {
                Result.failure(Exception("Enrollment failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Preview classroom before enrollment
     */
    suspend fun previewClassroom(classroomCode: String): Result<ClassroomPreviewResponse> {
        return try {
            val response = studentApiService.getClassroomPreview(classroomCode)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Classroom not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Refresh JWT token for facilitators
     */
    suspend fun refreshToken(): Result<AuthToken> {
        return try {
            val currentToken = getAuthToken()
            if (currentToken.isNullOrEmpty()) {
                return Result.failure(Exception("No token to refresh"))
            }
            
            val response = apiService.refreshToken("Bearer $currentToken")
            if (response.isSuccessful && response.body() != null) {
                val newToken = response.body()!!
                
                // Update stored token
                dataStore.edit { preferences ->
                    preferences[AUTH_TOKEN_KEY] = newToken.token
                    preferences[REFRESH_TOKEN_KEY] = newToken.refreshToken ?: ""
                }
                
                Result.success(newToken)
            } else {
                Result.failure(Exception("Token refresh failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Logout user (clears all auth data)
     */
    suspend fun logout(): Result<Unit> {
        return try {
            val token = getAuthToken()
            if (!token.isNullOrEmpty()) {
                // Call logout endpoint for facilitators
                try {
                    apiService.logout("Bearer $token")
                } catch (e: Exception) {
                    // Continue with local logout even if API call fails
                }
            }
            
            // Clear all auth data
            clearAuthData()
            
            Result.success(Unit)
        } catch (e: Exception) {
            // Still clear local data even if API call fails
            clearAuthData()
            Result.success(Unit)
        }
    }
    
    /**
     * Check if user is currently logged in
     */
    fun isLoggedIn(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[IS_LOGGED_IN_KEY] ?: false
        }
    }
    
    /**
     * Get current user type
     */
    fun getUserType(): Flow<UserType?> {
        return dataStore.data.map { preferences ->
            preferences[USER_TYPE_KEY]?.let { 
                try {
                    UserType.valueOf(it)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
    
    /**
     * Get current user ID
     */
    suspend fun getCurrentUserId(): String? {
        return dataStore.data.first()[USER_ID_KEY]
    }
    
    /**
     * Get current session ID (for students)
     */
    suspend fun getCurrentSessionId(): String? {
        return dataStore.data.first()[SESSION_ID_KEY]
    }
    
    /**
     * Get current auth token (for facilitators)
     */
    suspend fun getAuthToken(): String? {
        return dataStore.data.first()[AUTH_TOKEN_KEY]
    }
    
    /**
     * Get current classroom code (for students)
     */
    suspend fun getCurrentClassroomCode(): String? {
        return dataStore.data.first()[CLASSROOM_CODE_KEY]
    }
    
    /**
     * Get formatted auth header for API calls
     */
    suspend fun getAuthHeader(): String? {
        val token = getAuthToken()
        return if (!token.isNullOrEmpty()) "Bearer $token" else null
    }
    
    /**
     * Check if current session is a student session
     */
    suspend fun isStudentSession(): Boolean {
        val userType = dataStore.data.first()[USER_TYPE_KEY]
        return userType == UserType.STUDENT.name
    }
    
    /**
     * Check if current session is a facilitator session
     */
    suspend fun isFacilitatorSession(): Boolean {
        val userType = dataStore.data.first()[USER_TYPE_KEY]
        return userType == UserType.FACILITATOR.name
    }
    
    /**
     * Get current facilitator from local database
     */
    suspend fun getCurrentFacilitator(): Facilitator? {
        val userId = getCurrentUserId()
        return if (userId != null) {
            facilitatorDao.getFacilitatorById(userId)?.toDomainModel()
        } else null
    }
    
    /**
     * Get current student from local database
     */
    suspend fun getCurrentStudent(): Student? {
        val sessionId = getCurrentSessionId()
        return if (sessionId != null) {
            studentDao.getStudentBySessionId(sessionId)?.toDomainModel()
        } else null
    }
    
    // Private helper methods
    
    private suspend fun saveAuthData(
        token: String,
        refreshToken: String?,
        userType: UserType,
        userId: String
    ) {
        dataStore.edit { preferences ->
            preferences[AUTH_TOKEN_KEY] = token
            preferences[REFRESH_TOKEN_KEY] = refreshToken ?: ""
            preferences[USER_TYPE_KEY] = userType.name
            preferences[USER_ID_KEY] = userId
            preferences[IS_LOGGED_IN_KEY] = true
        }
    }
    
    private suspend fun saveStudentSessionData(
        sessionId: String,
        classroomCode: String,
        userId: String
    ) {
        dataStore.edit { preferences ->
            preferences[SESSION_ID_KEY] = sessionId
            preferences[CLASSROOM_CODE_KEY] = classroomCode
            preferences[USER_TYPE_KEY] = UserType.STUDENT.name
            preferences[USER_ID_KEY] = userId
            preferences[IS_LOGGED_IN_KEY] = true
        }
    }
    
    private suspend fun clearAuthData() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}