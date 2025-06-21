package com.lifechurch.heroesinwaiting.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor() : Interceptor {
    
    // TODO: Inject TokenManager when implemented
    private var authToken: String? = null
    
    fun setToken(token: String?) {
        authToken = token
    }
    
    fun getToken(): String? = authToken
    
    fun clearToken() {
        authToken = null
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Skip auth for certain endpoints
        val skipAuthPaths = listOf(
            "/auth/facilitator/register",
            "/auth/facilitator/login",
            "/auth/student/enroll",
            "/classrooms/",
            "/content/"
        )
        
        val shouldSkipAuth = skipAuthPaths.any { path ->
            originalRequest.url.encodedPath.contains(path)
        }
        
        if (shouldSkipAuth || authToken == null) {
            return chain.proceed(originalRequest)
        }
        
        // Add Authorization header for authenticated requests
        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $authToken")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .build()
        
        val response = chain.proceed(authenticatedRequest)
        
        // Handle 401 Unauthorized responses
        if (response.code == 401) {
            // Clear invalid token
            authToken = null
            // TODO: Trigger re-authentication flow
        }
        
        return response
    }
}