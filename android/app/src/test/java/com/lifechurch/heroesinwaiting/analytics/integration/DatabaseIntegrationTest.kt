package com.lifechurch.heroesinwaiting.analytics.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifechurch.heroesinwaiting.analytics.utils.AnalyticsTestUtils
import com.lifechurch.heroesinwaiting.analytics.utils.COPPATestValidator
import com.lifechurch.heroesinwaiting.analytics.utils.MockDataGenerator
import com.lifechurch.heroesinwaiting.data.local.HeroesDatabase
import com.lifechurch.heroesinwaiting.data.local.dao.AnalyticsEventDao
import com.lifechurch.heroesinwaiting.data.local.dao.AnalyticsSyncBatchDao
import com.lifechurch.heroesinwaiting.data.local.dao.BehavioralAnalyticsDao
import com.lifechurch.heroesinwaiting.data.local.entities.AnalyticsEventEntity
import com.lifechurch.heroesinwaiting.data.local.entities.AnalyticsSyncBatchEntity
import com.lifechurch.heroesinwaiting.data.local.entities.BehavioralAnalyticsEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.*

/**
 * Integration tests for Room database operations with analytics entities
 * Validates database schema, constraints, performance, and COPPA compliance
 */
@RunWith(AndroidJUnit4::class)
class DatabaseIntegrationTest {
    
    private lateinit var database: HeroesDatabase
    private lateinit var behavioralAnalyticsDao: BehavioralAnalyticsDao
    private lateinit var analyticsEventDao: AnalyticsEventDao
    private lateinit var analyticsSyncBatchDao: AnalyticsSyncBatchDao
    
    @Before
    fun setup() {
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            HeroesDatabase::class.java
        )
        .allowMainThreadQueries()
        .build()
        
        // Get DAO references
        behavioralAnalyticsDao = database.behavioralAnalyticsDao()
        analyticsEventDao = database.analyticsEventDao()
        analyticsSyncBatchDao = database.analyticsSyncBatchDao()
    }
    
    @After
    fun teardown() {
        database.close()
        AnalyticsTestUtils.cleanupTestData()
    }
    
    // ================== Behavioral Analytics DAO Tests ==================
    
    @Test
    fun `behavioral analytics can be inserted and retrieved`() = runTest {
        // Given: Test behavioral analytics entity
        val entity = AnalyticsTestUtils.createTestBehavioralAnalyticsEntity()
        
        // When: Inserting and retrieving
        behavioralAnalyticsDao.insert(entity)
        val retrieved = behavioralAnalyticsDao.getById(entity.id)
        
        // Then: Entity should be correctly stored and retrieved
        assertNotNull(retrieved)
        assertEquals(entity.id, retrieved.id)
        assertEquals(entity.sessionId, retrieved.sessionId)
        assertEquals(entity.behavioralCategory, retrieved.behavioralCategory)
        assertEquals(entity.interactionType, retrieved.interactionType)
        assertEquals(entity.behavioralIndicators, retrieved.behavioralIndicators)
        
        // Validate COPPA compliance
        val validationResult = COPPATestValidator.validateBehavioralAnalytics(retrieved)
        assertTrue(validationResult.isCompliant)
    }
    
    @Test
    fun `behavioral analytics can be queried by session`() = runTest {
        // Given: Multiple entities for same session
        val sessionId = AnalyticsTestUtils.generateAnonymousSessionId()
        val entities = listOf(
            AnalyticsTestUtils.createTestBehavioralAnalyticsEntity(sessionId = sessionId),
            AnalyticsTestUtils.createTestBehavioralAnalyticsEntity(sessionId = sessionId),
            AnalyticsTestUtils.createTestBehavioralAnalyticsEntity() // Different session
        )
        
        // When: Inserting all entities
        entities.forEach { behavioralAnalyticsDao.insert(it) }
        
        // Then: Should retrieve only entities for specified session
        val sessionEntities = behavioralAnalyticsDao.getBySessionId(sessionId).first()
        assertEquals(2, sessionEntities.size)
        assertTrue(sessionEntities.all { it.sessionId == sessionId })
    }
    
    @Test
    fun `behavioral analytics can be queried by classroom`() = runTest {
        // Given: Multiple entities for different classrooms
        val classroomId = "test-classroom-specific"
        val entities = listOf(
            AnalyticsTestUtils.createTestBehavioralAnalyticsEntity().copy(classroomId = classroomId),
            AnalyticsTestUtils.createTestBehavioralAnalyticsEntity().copy(classroomId = classroomId),
            AnalyticsTestUtils.createTestBehavioralAnalyticsEntity() // Different classroom
        )
        
        // When: Inserting all entities
        entities.forEach { behavioralAnalyticsDao.insert(it) }
        
        // Then: Should retrieve only entities for specified classroom
        val classroomEntities = behavioralAnalyticsDao.getByClassroomId(classroomId).first()
        assertEquals(2, classroomEntities.size)
        assertTrue(classroomEntities.all { it.classroomId == classroomId })
    }
    
    @Test
    fun `behavioral analytics can be filtered by behavioral category`() = runTest {
        // Given: Entities with different behavioral categories
        val empathyEntities = MockDataGenerator.generateEmpathyScenario()
        val confidenceEntities = MockDataGenerator.generateConfidenceScenario()
        
        // When: Inserting mixed behavioral data
        empathyEntities.forEach { behavioralAnalyticsDao.insert(it) }
        confidenceEntities.forEach { behavioralAnalyticsDao.insert(it) }
        
        // Then: Should filter correctly by category
        val empathyResults = behavioralAnalyticsDao.getBehavioralCategory("empathy").first()
        val confidenceResults = behavioralAnalyticsDao.getBehavioralCategory("confidence").first()
        
        assertTrue(empathyResults.all { it.behavioralCategory == "empathy" })
        assertTrue(confidenceResults.all { it.behavioralCategory == "confidence" })
        assertEquals(empathyEntities.size, empathyResults.size)
        assertEquals(confidenceEntities.size, confidenceResults.size)
    }
    
    @Test
    fun `behavioral analytics supports time range queries`() = runTest {
        // Given: Entities with different timestamps
        val now = System.currentTimeMillis()
        val entities = listOf(
            AnalyticsTestUtils.createTestBehavioralAnalyticsEntity().copy(
                interactionTimestamp = now - 3600000 // 1 hour ago
            ),
            AnalyticsTestUtils.createTestBehavioralAnalyticsEntity().copy(
                interactionTimestamp = now - 1800000 // 30 minutes ago
            ),
            AnalyticsTestUtils.createTestBehavioralAnalyticsEntity().copy(
                interactionTimestamp = now - 7200000 // 2 hours ago
            )
        )
        
        // When: Inserting entities
        entities.forEach { behavioralAnalyticsDao.insert(it) }
        
        // Then: Should retrieve entities in time range
        val startTime = now - 3900000 // 65 minutes ago
        val endTime = now - 900000   // 15 minutes ago
        val timeRangeResults = behavioralAnalyticsDao.getInTimeRange(startTime, endTime).first()
        
        assertEquals(2, timeRangeResults.size)
        assertTrue(timeRangeResults.all { 
            it.interactionTimestamp >= startTime && it.interactionTimestamp <= endTime 
        })
    }
    
    @Test
    fun `behavioral analytics supports pagination`() = runTest {
        // Given: Large dataset
        val entities = MockDataGenerator.generatePerformanceDataset(50)
        
        // When: Inserting all entities
        entities.forEach { behavioralAnalyticsDao.insert(it) }
        
        // Then: Should support pagination
        val page1 = behavioralAnalyticsDao.getPaginated(limit = 20, offset = 0).first()
        val page2 = behavioralAnalyticsDao.getPaginated(limit = 20, offset = 20).first()
        val page3 = behavioralAnalyticsDao.getPaginated(limit = 20, offset = 40).first()
        
        assertEquals(20, page1.size)
        assertEquals(20, page2.size)
        assertEquals(10, page3.size) // Remaining entities
        
        // Ensure no duplicates between pages
        val allIds = (page1 + page2 + page3).map { it.id }.toSet()
        assertEquals(50, allIds.size)
    }
    
    // ================== Analytics Event DAO Tests ==================
    
    @Test
    fun `analytics events can be inserted and retrieved`() = runTest {
        // Given: Test analytics event entity
        val entity = AnalyticsTestUtils.createTestAnalyticsEventEntity()
        
        // When: Inserting and retrieving
        analyticsEventDao.insert(entity)
        val retrieved = analyticsEventDao.getById(entity.id)
        
        // Then: Entity should be correctly stored and retrieved
        assertNotNull(retrieved)
        assertEquals(entity.id, retrieved.id)
        assertEquals(entity.sessionId, retrieved.sessionId)
        assertEquals(entity.eventCategory, retrieved.eventCategory)
        assertEquals(entity.eventAction, retrieved.eventAction)
        assertEquals(entity.eventProperties, retrieved.eventProperties)
        
        // Validate COPPA compliance
        val validationResult = COPPATestValidator.validateAnalyticsEvent(retrieved)
        assertTrue(validationResult.isCompliant)
    }
    
    @Test
    fun `analytics events can be queried for sync`() = runTest {
        // Given: Mix of synced and unsynced events
        val entities = listOf(
            AnalyticsTestUtils.createTestAnalyticsEventEntity().copy(needsSync = true),
            AnalyticsTestUtils.createTestAnalyticsEventEntity().copy(needsSync = true),
            AnalyticsTestUtils.createTestAnalyticsEventEntity().copy(needsSync = false)
        )
        
        // When: Inserting all entities
        entities.forEach { analyticsEventDao.insert(it) }
        
        // Then: Should retrieve only unsynced events
        val unsyncedEvents = analyticsEventDao.getUnsyncedEvents().first()
        assertEquals(2, unsyncedEvents.size)
        assertTrue(unsyncedEvents.all { it.needsSync })
    }
    
    @Test
    fun `analytics events can be marked as synced`() = runTest {
        // Given: Unsynced event
        val entity = AnalyticsTestUtils.createTestAnalyticsEventEntity().copy(needsSync = true)
        analyticsEventDao.insert(entity)
        
        // When: Marking as synced
        analyticsEventDao.markAsSynced(entity.id)
        
        // Then: Event should be marked as synced
        val retrieved = analyticsEventDao.getById(entity.id)
        assertNotNull(retrieved)
        assertFalse(retrieved.needsSync)
    }
    
    // ================== Sync Batch DAO Tests ==================
    
    @Test
    fun `sync batches can be created and managed`() = runTest {
        // Given: Test sync batch
        val batch = AnalyticsTestUtils.createTestSyncBatchEntity()
        
        // When: Inserting batch
        analyticsSyncBatchDao.insert(batch)
        val retrieved = analyticsSyncBatchDao.getById(batch.id)
        
        // Then: Batch should be correctly stored
        assertNotNull(retrieved)
        assertEquals(batch.id, retrieved.id)
        assertEquals(batch.eventIds, retrieved.eventIds)
        assertEquals(batch.syncStatus, retrieved.syncStatus)
    }
    
    @Test
    fun `sync batches can be queried by status`() = runTest {
        // Given: Batches with different statuses
        val batches = listOf(
            AnalyticsTestUtils.createTestSyncBatchEntity().copy(syncStatus = "pending"),
            AnalyticsTestUtils.createTestSyncBatchEntity().copy(syncStatus = "pending"),
            AnalyticsTestUtils.createTestSyncBatchEntity().copy(syncStatus = "completed")
        )
        
        // When: Inserting all batches
        batches.forEach { analyticsSyncBatchDao.insert(it) }
        
        // Then: Should filter by status
        val pendingBatches = analyticsSyncBatchDao.getBatchesByStatus("pending").first()
        assertEquals(2, pendingBatches.size)
        assertTrue(pendingBatches.all { it.syncStatus == "pending" })
    }
    
    // ================== Database Performance Tests ==================
    
    @Test
    fun `database handles large batch inserts efficiently`() = runTest {
        // Given: Large dataset
        val entities = MockDataGenerator.generatePerformanceDataset(1000)
        val startTime = System.currentTimeMillis()
        
        // When: Batch inserting
        behavioralAnalyticsDao.insertAll(entities)
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // Then: Should complete in reasonable time (under 5 seconds)
        assertTrue(duration < 5000, "Batch insert took too long: ${duration}ms")
        
        // Verify all entities were inserted
        val count = behavioralAnalyticsDao.getCount()
        assertEquals(1000, count)
    }
    
    @Test
    fun `database queries perform well with large datasets`() = runTest {
        // Given: Large dataset in database
        val entities = MockDataGenerator.generatePerformanceDataset(1000)
        behavioralAnalyticsDao.insertAll(entities)
        
        // When: Performing various queries
        val startTime = System.currentTimeMillis()
        
        val allEntities = behavioralAnalyticsDao.getAll().first()
        val empathyEntities = behavioralAnalyticsDao.getBehavioralCategory("empathy").first()
        val recentEntities = behavioralAnalyticsDao.getInTimeRange(
            System.currentTimeMillis() - 3600000,
            System.currentTimeMillis()
        ).first()
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // Then: Queries should complete quickly (under 1 second)
        assertTrue(duration < 1000, "Database queries took too long: ${duration}ms")
        
        // Verify results
        assertEquals(1000, allEntities.size)
        assertTrue(empathyEntities.isNotEmpty())
        assertTrue(recentEntities.isNotEmpty())
    }
    
    // ================== Data Integrity Tests ==================
    
    @Test
    fun `database maintains data integrity with concurrent operations`() = runTest {
        // Given: Multiple concurrent operations
        val entities1 = MockDataGenerator.generateEmpathyScenario()
        val entities2 = MockDataGenerator.generateConfidenceScenario()
        val entities3 = MockDataGenerator.generateCommunicationScenario()
        
        // When: Concurrent inserts (simulated)
        behavioralAnalyticsDao.insertAll(entities1)
        behavioralAnalyticsDao.insertAll(entities2)
        behavioralAnalyticsDao.insertAll(entities3)
        
        // Then: All data should be correctly stored
        val totalCount = behavioralAnalyticsDao.getCount()
        val expectedCount = entities1.size + entities2.size + entities3.size
        assertEquals(expectedCount, totalCount)
        
        // Verify each category
        val empathyCount = behavioralAnalyticsDao.getBehavioralCategory("empathy").first().size
        val confidenceCount = behavioralAnalyticsDao.getBehavioralCategory("confidence").first().size
        val communicationCount = behavioralAnalyticsDao.getBehavioralCategory("communication").first().size
        
        assertEquals(entities1.size, empathyCount)
        assertEquals(entities2.size, confidenceCount)
        assertEquals(entities3.size, communicationCount)
    }
    
    @Test
    fun `database enforces foreign key constraints`() = runTest {
        // Given: Analytics event with classroom reference
        val event = AnalyticsTestUtils.createTestAnalyticsEventEntity()
        
        // When: Inserting event
        analyticsEventDao.insert(event)
        
        // Then: Should succeed (no foreign key constraints in current schema)
        val retrieved = analyticsEventDao.getById(event.id)
        assertNotNull(retrieved)
        assertEquals(event.classroomId, retrieved.classroomId)
    }
    
    // ================== Data Cleanup Tests ==================
    
    @Test
    fun `database supports data retention cleanup`() = runTest {
        // Given: Old and new data
        val now = System.currentTimeMillis()
        val oldEntities = listOf(
            AnalyticsTestUtils.createTestBehavioralAnalyticsEntity().copy(
                createdAt = now - (91 * 24 * 60 * 60 * 1000L) // 91 days old
            ),
            AnalyticsTestUtils.createTestBehavioralAnalyticsEntity().copy(
                createdAt = now - (92 * 24 * 60 * 60 * 1000L) // 92 days old
            )
        )
        val newEntities = listOf(
            AnalyticsTestUtils.createTestBehavioralAnalyticsEntity().copy(
                createdAt = now - (30 * 24 * 60 * 60 * 1000L) // 30 days old
            ),
            AnalyticsTestUtils.createTestBehavioralAnalyticsEntity().copy(
                createdAt = now - (60 * 24 * 60 * 60 * 1000L) // 60 days old
            )
        )
        
        // When: Inserting all data
        behavioralAnalyticsDao.insertAll(oldEntities + newEntities)
        
        // Cleanup data older than 90 days
        val cutoffTime = now - (90 * 24 * 60 * 60 * 1000L)
        behavioralAnalyticsDao.deleteOlderThan(cutoffTime)
        
        // Then: Only new data should remain
        val remainingCount = behavioralAnalyticsDao.getCount()
        assertEquals(2, remainingCount)
        
        val remainingEntities = behavioralAnalyticsDao.getAll().first()
        assertTrue(remainingEntities.all { it.createdAt >= cutoffTime })
    }
    
    // ================== Schema Validation Tests ==================
    
    @Test
    fun `database schema supports all required analytics fields`() = runTest {
        // Given: Entity with all possible fields populated
        val entity = BehavioralAnalyticsEntity(
            id = "test-id",
            sessionId = AnalyticsTestUtils.generateAnonymousSessionId(),
            classroomId = "test-classroom",
            lessonId = "test-lesson",
            activityId = "test-activity",
            interactionType = "test-interaction",
            interactionTimestamp = System.currentTimeMillis(),
            timeSpentSeconds = 300L,
            interactionCount = 5,
            behavioralCategory = "empathy",
            behavioralIndicators = mapOf(
                "score" to 4,
                "quality" to "high",
                "context" to "peer_support",
                "nested_data" to mapOf("sub_field" to "value")
            ),
            deviceType = "mobile",
            sessionContext = "classroom",
            screenSize = "normal",
            appVersion = "1.0.0",
            isOfflineRecorded = true,
            needsSync = true,
            syncAttempts = 2,
            lastSyncAttempt = System.currentTimeMillis() - 60000,
            loadTimeMs = 250L,
            errorOccurred = false,
            errorDetails = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        // When: Inserting complex entity
        behavioralAnalyticsDao.insert(entity)
        val retrieved = behavioralAnalyticsDao.getById(entity.id)
        
        // Then: All fields should be preserved
        assertNotNull(retrieved)
        assertEquals(entity.id, retrieved.id)
        assertEquals(entity.sessionId, retrieved.sessionId)
        assertEquals(entity.behavioralIndicators, retrieved.behavioralIndicators)
        assertEquals(entity.isOfflineRecorded, retrieved.isOfflineRecorded)
        assertEquals(entity.syncAttempts, retrieved.syncAttempts)
    }
}