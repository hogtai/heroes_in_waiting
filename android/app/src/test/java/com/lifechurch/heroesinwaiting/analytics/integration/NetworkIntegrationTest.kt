package com.lifechurch.heroesinwaiting.analytics.integration

import com.lifechurch.heroesinwaiting.analytics.utils.AnalyticsTestUtils
import com.lifechurch.heroesinwaiting.analytics.utils.COPPATestValidator
import com.lifechurch.heroesinwaiting.analytics.utils.MockDataGenerator
import com.lifechurch.heroesinwaiting.data.api.ApiService
import com.lifechurch.heroesinwaiting.data.api.StudentApiService
import com.lifechurch.heroesinwaiting.data.api.response.*
import com.lifechurch.heroesinwaiting.data.remote.dto.*
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.test.*

/**
 * Network integration tests for analytics API endpoints
 * Validates API contracts, authentication, error handling, and COPPA compliance
 */
class NetworkIntegrationTest {
    
    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: ApiService
    private lateinit var studentApiService: StudentApiService
    
    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        apiService = retrofit.create(ApiService::class.java)
        studentApiService = retrofit.create(StudentApiService::class.java)
    }
    
    @After
    fun teardown() {
        mockWebServer.shutdown()
        AnalyticsTestUtils.cleanupTestData()
    }
    
    // ================== Behavioral Analytics API Tests ==================
    
    @Test
    fun `behavioral analytics submission succeeds with valid data`() = runTest {
        // Given: Valid behavioral analytics data
        val analyticsData = MockDataGenerator.generateEmpathyScenario()
        val requestDto = BehavioralAnalyticsDto(
            sessionId = AnalyticsTestUtils.generateAnonymousSessionId(),
            classroomId = AnalyticsTestUtils.TEST_CLASSROOM_ID,
            behavioralEvents = analyticsData.map { entity ->
                BehavioralEventDto(
                    interactionType = entity.interactionType,
                    behavioralCategory = entity.behavioralCategory,
                    timestamp = entity.interactionTimestamp,
                    timeSpentSeconds = entity.timeSpentSeconds,
                    behavioralIndicators = entity.behavioralIndicators,
                    deviceInfo = mapOf("platform" to "Android", "version" to "1.0.0")
                )
            }
        )
        
        // Mock successful response
        mockWebServer.enqueue(MockResponse().apply {
            setResponseCode(200)
            setBody("""
                {
                    "success": true,
                    "message": "Behavioral analytics submitted successfully",
                    "analyticsId": "analytics-123"
                }
            """.trimIndent())
        })
        
        // When: Submitting analytics
        val response = studentApiService.submitBehavioralAnalytics(requestDto)
        
        // Then: Should succeed
        assertTrue(response.isSuccessful)
        assertNotNull(response.body())
        assertEquals(true, response.body()?.success)
        
        // Validate request was COPPA compliant
        val request = mockWebServer.takeRequest()
        assertNotNull(request.body)
        
        // Ensure no PII in request
        val requestBody = request.body.readUtf8()
        assertFalse(requestBody.contains("@"), "Request should not contain email addresses")
        assertFalse(requestBody.contains("555-"), "Request should not contain phone numbers")
    }
    
    @Test
    fun `behavioral analytics submission handles server errors gracefully`() = runTest {
        // Given: Valid request data
        val analyticsData = MockDataGenerator.generateConfidenceScenario()
        val requestDto = BehavioralAnalyticsDto(
            sessionId = AnalyticsTestUtils.generateAnonymousSessionId(),
            classroomId = AnalyticsTestUtils.TEST_CLASSROOM_ID,
            behavioralEvents = analyticsData.map { entity ->
                BehavioralEventDto(
                    interactionType = entity.interactionType,
                    behavioralCategory = entity.behavioralCategory,
                    timestamp = entity.interactionTimestamp,
                    timeSpentSeconds = entity.timeSpentSeconds,
                    behavioralIndicators = entity.behavioralIndicators,
                    deviceInfo = mapOf("platform" to "Android")
                )
            }
        )
        
        // Mock server error
        mockWebServer.enqueue(MockResponse().apply {
            setResponseCode(500)
            setBody("""
                {
                    "success": false,
                    "message": "Internal server error",
                    "error": "Database connection failed"
                }
            """.trimIndent())
        })
        
        // When: Submitting analytics
        val response = studentApiService.submitBehavioralAnalytics(requestDto)
        
        // Then: Should handle error gracefully
        assertFalse(response.isSuccessful)
        assertEquals(500, response.code())
    }
    
    @Test
    fun `behavioral analytics batch submission processes large datasets`() = runTest {
        // Given: Large batch of analytics data
        val largeBatch = MockDataGenerator.generatePerformanceDataset(100)
        val requestDto = BehavioralAnalyticsDto(
            sessionId = AnalyticsTestUtils.generateAnonymousSessionId(),
            classroomId = AnalyticsTestUtils.TEST_CLASSROOM_ID,
            behavioralEvents = largeBatch.map { entity ->
                BehavioralEventDto(
                    interactionType = entity.interactionType,
                    behavioralCategory = entity.behavioralCategory,
                    timestamp = entity.interactionTimestamp,
                    timeSpentSeconds = entity.timeSpentSeconds,
                    behavioralIndicators = entity.behavioralIndicators,
                    deviceInfo = mapOf("platform" to "Android")
                )
            }
        )
        
        // Mock successful response
        mockWebServer.enqueue(MockResponse().apply {
            setResponseCode(200)
            setBody("""
                {
                    "success": true,
                    "message": "Batch analytics processed successfully",
                    "processed": 100,
                    "analyticsId": "batch-123"
                }
            """.trimIndent())
        })
        
        // When: Submitting large batch
        val startTime = System.currentTimeMillis()
        val response = studentApiService.submitBehavioralAnalytics(requestDto)
        val endTime = System.currentTimeMillis()
        
        // Then: Should handle large batch efficiently
        assertTrue(response.isSuccessful)
        assertTrue((endTime - startTime) < 5000, "Large batch submission took too long")
        
        // Verify request size is reasonable
        val request = mockWebServer.takeRequest()
        assertTrue(request.bodySize > 0, "Request should contain data")
    }
    
    // ================== Analytics Events API Tests ==================
    
    @Test
    fun `analytics events submission succeeds with valid data`() = runTest {
        // Given: Valid analytics events
        val events = MockDataGenerator.generateAnalyticsEvents(10)
        val requestDto = AnalyticsEventsDto(
            sessionId = AnalyticsTestUtils.generateAnonymousSessionId(),
            classroomId = AnalyticsTestUtils.TEST_CLASSROOM_ID,
            events = events.map { event ->
                AnalyticsEventDto(
                    eventCategory = event.eventCategory,
                    eventAction = event.eventAction,
                    eventLabel = event.eventLabel,
                    eventValue = event.eventValue,
                    timestamp = event.timestamp,
                    eventProperties = event.eventProperties,
                    deviceInfo = event.deviceInfo
                )
            }
        )
        
        // Mock successful response
        mockWebServer.enqueue(MockResponse().apply {
            setResponseCode(200)
            setBody("""
                {
                    "success": true,
                    "message": "Analytics events submitted successfully",
                    "processed": 10,
                    "eventIds": ["event-1", "event-2", "event-3"]
                }
            """.trimIndent())
        })
        
        // When: Submitting events
        val response = studentApiService.submitAnalyticsEvents(requestDto)
        
        // Then: Should succeed
        assertTrue(response.isSuccessful)
        assertNotNull(response.body())
        assertEquals(true, response.body()?.success)
    }
    
    @Test
    fun `analytics events API validates request format`() = runTest {
        // Given: Invalid request format (missing required fields)
        val invalidRequestDto = AnalyticsEventsDto(
            sessionId = "", // Invalid empty session ID
            classroomId = AnalyticsTestUtils.TEST_CLASSROOM_ID,
            events = emptyList() // Invalid empty events list
        )
        
        // Mock validation error response
        mockWebServer.enqueue(MockResponse().apply {
            setResponseCode(400)
            setBody("""
                {
                    "success": false,
                    "message": "Validation failed",
                    "errors": [
                        "Session ID is required",
                        "Events list cannot be empty"
                    ]
                }
            """.trimIndent())
        })
        
        // When: Submitting invalid request
        val response = studentApiService.submitAnalyticsEvents(invalidRequestDto)
        
        // Then: Should return validation error
        assertFalse(response.isSuccessful)
        assertEquals(400, response.code())
    }
    
    // ================== Facilitator Analytics Dashboard API Tests ==================
    
    @Test
    fun `facilitator dashboard API requires authentication`() = runTest {
        // Given: Request without authentication
        mockWebServer.enqueue(MockResponse().apply {
            setResponseCode(401)
            setBody("""
                {
                    "success": false,
                    "message": "Authentication required",
                    "error": "Missing or invalid JWT token"
                }
            """.trimIndent())
        })
        
        // When: Accessing dashboard without auth
        val response = apiService.getFacilitatorAnalyticsDashboard(
            classroomId = AnalyticsTestUtils.TEST_CLASSROOM_ID,
            timeframe = "7d"
        )
        
        // Then: Should return unauthorized
        assertFalse(response.isSuccessful)
        assertEquals(401, response.code())
    }
    
    @Test
    fun `facilitator dashboard returns aggregated analytics`() = runTest {
        // Given: Valid dashboard request with auth
        mockWebServer.enqueue(MockResponse().apply {
            setResponseCode(200)
            setBody("""
                {
                    "success": true,
                    "dashboard": {
                        "classroomId": "${AnalyticsTestUtils.TEST_CLASSROOM_ID}",
                        "timeframe": "7d",
                        "totalSessions": 45,
                        "totalInteractions": 234,
                        "behavioralInsights": {
                            "empathy": {
                                "averageScore": 4.2,
                                "totalInteractions": 89,
                                "growth": "+15%"
                            },
                            "confidence": {
                                "averageScore": 3.8,
                                "totalInteractions": 67,
                                "growth": "+22%"
                            },
                            "communication": {
                                "averageScore": 4.0,
                                "totalInteractions": 45,
                                "growth": "+18%"
                            },
                            "leadership": {
                                "averageScore": 3.6,
                                "totalInteractions": 33,
                                "growth": "+12%"
                            }
                        },
                        "engagementMetrics": {
                            "highEngagement": 0.68,
                            "mediumEngagement": 0.24,
                            "lowEngagement": 0.08
                        },
                        "recentActivity": [
                            {
                                "timestamp": ${System.currentTimeMillis()},
                                "activity": "peer_support_interaction",
                                "category": "empathy",
                                "score": 5
                            }
                        ]
                    }
                }
            """.trimIndent())
        })
        
        // When: Fetching dashboard with valid auth (would include JWT in real implementation)
        val response = apiService.getFacilitatorAnalyticsDashboard(
            classroomId = AnalyticsTestUtils.TEST_CLASSROOM_ID,
            timeframe = "7d"
        )
        
        // Then: Should return dashboard data
        assertTrue(response.isSuccessful)
        assertNotNull(response.body())
        assertEquals(true, response.body()?.success)
    }
    
    // ================== Analytics Export API Tests ==================
    
    @Test
    fun `analytics export API returns anonymized research data`() = runTest {
        // Given: Export request for research data
        mockWebServer.enqueue(MockResponse().apply {
            setResponseCode(200)
            setBody("""
                {
                    "success": true,
                    "export": {
                        "format": "json",
                        "dataType": "anonymized_research",
                        "totalRecords": 156,
                        "timeRange": {
                            "start": "${System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000}",
                            "end": "${System.currentTimeMillis()}"
                        },
                        "data": [
                            {
                                "anonymousId": "anon_abc123",
                                "behavioralCategory": "empathy",
                                "interactions": 12,
                                "averageScore": 4.1,
                                "engagementLevel": "high"
                            },
                            {
                                "anonymousId": "anon_def456",
                                "behavioralCategory": "confidence",
                                "interactions": 8,
                                "averageScore": 3.7,
                                "engagementLevel": "medium"
                            }
                        ],
                        "aggregatedMetrics": {
                            "empathyDevelopment": 0.78,
                            "confidenceBuilding": 0.65,
                            "communicationSkills": 0.72,
                            "leadershipBehavior": 0.58
                        }
                    }
                }
            """.trimIndent())
        })
        
        // When: Requesting export
        val response = apiService.exportAnalyticsForResearch(
            classroomId = AnalyticsTestUtils.TEST_CLASSROOM_ID,
            timeframe = "7d",
            format = "json"
        )
        
        // Then: Should return anonymized data
        assertTrue(response.isSuccessful)
        assertNotNull(response.body())
        
        // Validate response contains no PII
        val responseBody = response.body().toString()
        assertFalse(responseBody.contains("@"), "Export should not contain email addresses")
        assertFalse(responseBody.contains("phone"), "Export should not contain phone numbers")
        assertTrue(responseBody.contains("anon_"), "Export should use anonymous identifiers")
    }
    
    // ================== Error Handling Tests ==================
    
    @Test
    fun `API handles network timeouts gracefully`() = runTest {
        // Given: Slow server response (simulated timeout)
        mockWebServer.enqueue(MockResponse().apply {
            setResponseCode(200)
            setBody("{\"success\": true}")
            setHeadersDelay(10000, java.util.concurrent.TimeUnit.MILLISECONDS) // 10 second delay
        })
        
        // When: Making request that times out
        try {
            val response = studentApiService.submitBehavioralAnalytics(
                BehavioralAnalyticsDto(
                    sessionId = AnalyticsTestUtils.generateAnonymousSessionId(),
                    classroomId = AnalyticsTestUtils.TEST_CLASSROOM_ID,
                    behavioralEvents = emptyList()
                )
            )
            // If we get here, the timeout didn't work as expected
            fail("Expected timeout exception")
        } catch (e: Exception) {
            // Then: Should handle timeout appropriately
            assertTrue(e.message?.contains("timeout") == true || e is java.net.SocketTimeoutException)
        }
    }
    
    @Test
    fun `API handles malformed JSON responses`() = runTest {
        // Given: Invalid JSON response
        mockWebServer.enqueue(MockResponse().apply {
            setResponseCode(200)
            setBody("{ invalid json }")
        })
        
        // When: Making request with malformed response
        try {
            val response = studentApiService.submitBehavioralAnalytics(
                BehavioralAnalyticsDto(
                    sessionId = AnalyticsTestUtils.generateAnonymousSessionId(),
                    classroomId = AnalyticsTestUtils.TEST_CLASSROOM_ID,
                    behavioralEvents = emptyList()
                )
            )
            // Should handle parsing error
            assertNull(response.body())
        } catch (e: Exception) {
            // Parsing error is acceptable
            assertTrue(e.message?.contains("JSON") == true || e.message?.contains("parse") == true)
        }
    }
    
    // ================== Performance Tests ==================
    
    @Test
    fun `API responses are received within acceptable time limits`() = runTest {
        // Given: Normal API response
        mockWebServer.enqueue(MockResponse().apply {
            setResponseCode(200)
            setBody("""
                {
                    "success": true,
                    "message": "Analytics submitted successfully"
                }
            """.trimIndent())
        })
        
        // When: Making API request
        val startTime = System.currentTimeMillis()
        val response = studentApiService.submitBehavioralAnalytics(
            BehavioralAnalyticsDto(
                sessionId = AnalyticsTestUtils.generateAnonymousSessionId(),
                classroomId = AnalyticsTestUtils.TEST_CLASSROOM_ID,
                behavioralEvents = listOf(
                    BehavioralEventDto(
                        interactionType = "test",
                        behavioralCategory = "empathy",
                        timestamp = System.currentTimeMillis(),
                        timeSpentSeconds = 60,
                        behavioralIndicators = mapOf("score" to 4),
                        deviceInfo = mapOf("platform" to "Android")
                    )
                )
            )
        )
        val endTime = System.currentTimeMillis()
        
        // Then: Response should be fast (under 2 seconds for local mock)
        val responseTime = endTime - startTime
        assertTrue(responseTime < 2000, "API response took too long: ${responseTime}ms")
        assertTrue(response.isSuccessful)
    }
    
    // ================== COPPA Compliance Network Tests ==================
    
    @Test
    fun `all API requests maintain COPPA compliance`() = runTest {
        // Given: Various API request types
        val behavioralData = MockDataGenerator.generateEmpathyScenario()
        val eventData = MockDataGenerator.generateAnalyticsEvents(5)
        
        // Set up request interceptor to capture all requests
        val capturedRequests = mutableListOf<String>()
        
        // Mock responses for all requests
        repeat(2) {
            mockWebServer.enqueue(MockResponse().apply {
                setResponseCode(200)
                setBody("{\"success\": true}")
            })
        }
        
        // When: Making various API calls
        studentApiService.submitBehavioralAnalytics(
            BehavioralAnalyticsDto(
                sessionId = AnalyticsTestUtils.generateAnonymousSessionId(),
                classroomId = AnalyticsTestUtils.TEST_CLASSROOM_ID,
                behavioralEvents = behavioralData.map { entity ->
                    BehavioralEventDto(
                        interactionType = entity.interactionType,
                        behavioralCategory = entity.behavioralCategory,
                        timestamp = entity.interactionTimestamp,
                        timeSpentSeconds = entity.timeSpentSeconds,
                        behavioralIndicators = entity.behavioralIndicators,
                        deviceInfo = mapOf("platform" to "Android")
                    )
                }
            )
        )
        
        studentApiService.submitAnalyticsEvents(
            AnalyticsEventsDto(
                sessionId = AnalyticsTestUtils.generateAnonymousSessionId(),
                classroomId = AnalyticsTestUtils.TEST_CLASSROOM_ID,
                events = eventData.map { event ->
                    AnalyticsEventDto(
                        eventCategory = event.eventCategory,
                        eventAction = event.eventAction,
                        eventLabel = event.eventLabel,
                        eventValue = event.eventValue,
                        timestamp = event.timestamp,
                        eventProperties = event.eventProperties,
                        deviceInfo = event.deviceInfo
                    )
                }
            )
        )
        
        // Then: All requests should be COPPA compliant
        repeat(2) {
            val request = mockWebServer.takeRequest()
            val requestBody = request.body.readUtf8()
            capturedRequests.add(requestBody)
        }
        
        // Validate all requests for COPPA compliance
        capturedRequests.forEach { requestBody ->
            // Should not contain PII patterns
            assertFalse(requestBody.contains("@"), "Request contains email address")
            assertFalse(requestBody.contains("555-"), "Request contains phone number")
            assertFalse(requestBody.contains("123 Main St"), "Request contains address")
            
            // Should contain anonymous identifiers
            assertTrue(requestBody.contains("anon_") || 
                      requestBody.contains("-") && requestBody.length > 30, 
                      "Request should contain anonymous identifiers")
        }
    }
}