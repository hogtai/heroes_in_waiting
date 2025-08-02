package com.lifechurch.heroesinwaiting.analytics.integration

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.*
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.lifechurch.heroesinwaiting.analytics.utils.EspressoAnalyticsTestUtils
import com.lifechurch.heroesinwaiting.data.analytics.AnalyticsSyncWorker
import com.lifechurch.heroesinwaiting.data.analytics.AnalyticsService
import com.lifechurch.heroesinwaiting.data.local.HeroesDatabase
import com.lifechurch.heroesinwaiting.data.privacy.COPPAComplianceManager
import com.lifechurch.heroesinwaiting.data.repository.AnalyticsRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Instrumentation tests for WorkManager background sync functionality
 * Tests real WorkManager execution with analytics sync operations
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WorkManagerInstrumentationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var analyticsRepository: AnalyticsRepository

    @Inject
    lateinit var analyticsService: AnalyticsService

    @Inject
    lateinit var coppaComplianceManager: COPPAComplianceManager

    @Inject
    lateinit var database: HeroesDatabase

    private lateinit var context: Context
    private lateinit var workManager: WorkManager
    private lateinit var testUtils: EspressoAnalyticsTestUtils

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Initialize WorkManagerTestInitHelper for testing
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()
        
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)
        
        testUtils = EspressoAnalyticsTestUtils(database, analyticsService, coppaComplianceManager)
        
        runTest {
            testUtils.setupCOPPACompliantEnvironment()
            testUtils.clearAnalyticsData()
        }
    }

    @After
    fun teardown() {
        runTest {
            testUtils.cleanupTestData()
        }
    }

    // ================== Basic WorkManager Tests ==================

    @Test
    fun testAnalyticsSyncWorkerExecution() = runTest {
        // Given: Analytics data that needs syncing
        val testAnalytics = testUtils.createTestBehavioralAnalytics(
            interactionType = "test_sync",
            behavioralCategory = "engagement"
        )
        database.behavioralAnalyticsDao().insertBehavioralAnalytics(testAnalytics)
        
        // When: Analytics sync worker is executed
        val inputData = workDataOf(
            "sync_type" to "regular",
            "requires_wifi" to false,
            "max_retries" to 3
        )
        
        val syncRequest = OneTimeWorkRequestBuilder<AnalyticsSyncWorker>()
            .setInputData(inputData)
            .build()
        
        workManager.enqueue(syncRequest).result.get()
        
        // Then: Work should complete successfully
        val workInfo = workManager.getWorkInfoById(syncRequest.id).get()
        assertEquals(WorkInfo.State.SUCCEEDED, workInfo.state)
        
        // Verify output data
        val outputData = workInfo.outputData
        assertTrue(outputData.getInt("sync_success_count", 0) > 0)
    }

    @Test
    fun testBatchSyncWorkerExecution() = runTest {
        // Given: Multiple analytics events for batch sync
        repeat(5) { index ->
            val testAnalytics = testUtils.createTestBehavioralAnalytics(
                interactionType = "batch_test_$index",
                behavioralCategory = "engagement"
            )
            database.behavioralAnalyticsDao().insertBehavioralAnalytics(testAnalytics)
        }
        
        // When: Batch sync worker is executed
        val inputData = workDataOf(
            "sync_type" to "batch",
            "batch_size" to 5,
            "requires_wifi" to false
        )
        
        val batchSyncRequest = OneTimeWorkRequestBuilder<AnalyticsSyncWorker>()
            .setInputData(inputData)
            .build()
        
        workManager.enqueue(batchSyncRequest).result.get()
        
        // Then: Batch sync should succeed
        val workInfo = workManager.getWorkInfoById(batchSyncRequest.id).get()
        assertEquals(WorkInfo.State.SUCCEEDED, workInfo.state)
        
        val outputData = workInfo.outputData
        assertEquals(5, outputData.getInt("sync_success_count", 0))
    }

    @Test
    fun testPrioritySyncWorkerExecution() = runTest {
        // Given: High priority analytics event
        val priorityAnalytics = testUtils.createTestBehavioralAnalytics(
            interactionType = "priority_sync",
            behavioralCategory = "emotional_wellbeing"
        )
        database.behavioralAnalyticsDao().insertBehavioralAnalytics(priorityAnalytics)
        
        // When: Priority sync worker is executed
        val inputData = workDataOf(
            "sync_type" to "priority",
            "requires_wifi" to false,
            "immediate" to true
        )
        
        val prioritySyncRequest = OneTimeWorkRequestBuilder<AnalyticsSyncWorker>()
            .setInputData(inputData)
            .addTag("priority_sync")
            .build()
        
        workManager.enqueue(prioritySyncRequest).result.get()
        
        // Then: Priority sync should complete
        val workInfo = workManager.getWorkInfoById(prioritySyncRequest.id).get()
        assertEquals(WorkInfo.State.SUCCEEDED, workInfo.state)
        assertTrue(workInfo.tags.contains("priority_sync"))
    }

    // ================== Network Condition Tests ==================

    @Test
    fun testWifiRequiredSyncBehavior() = runTest {
        // Given: Analytics data and WiFi requirement
        val testAnalytics = testUtils.createTestBehavioralAnalytics()
        database.behavioralAnalyticsDao().insertBehavioralAnalytics(testAnalytics)
        
        // When: Sync worker requires WiFi
        val inputData = workDataOf(
            "sync_type" to "regular",
            "requires_wifi" to true,
            "max_retries" to 1
        )
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .build()
        
        val wifiSyncRequest = OneTimeWorkRequestBuilder<AnalyticsSyncWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .build()
        
        workManager.enqueue(wifiSyncRequest).result.get()
        
        // Then: Work should respect WiFi constraints
        val workInfo = workManager.getWorkInfoById(wifiSyncRequest.id).get()
        // Note: In test environment, this may succeed or be blocked based on test network setup
        assertTrue(workInfo.state in listOf(WorkInfo.State.SUCCEEDED, WorkInfo.State.BLOCKED))
    }

    @Test
    fun testNetworkUnavailableRetryBehavior() = runTest {
        // Given: Analytics data with network retry configuration
        val testAnalytics = testUtils.createTestBehavioralAnalytics()
        database.behavioralAnalyticsDao().insertBehavioralAnalytics(testAnalytics)
        
        // When: Sync worker configured with retry policy
        val inputData = workDataOf(
            "sync_type" to "regular",
            "max_retries" to 3,
            "simulate_network_failure" to true // Test-only parameter
        )
        
        val retryPolicy = BackoffPolicy.EXPONENTIAL
        val retrySyncRequest = OneTimeWorkRequestBuilder<AnalyticsSyncWorker>()
            .setInputData(inputData)
            .setBackoffCriteria(retryPolicy, 30, TimeUnit.SECONDS)
            .build()
        
        workManager.enqueue(retrySyncRequest).result.get()
        
        // Then: Work should handle retries appropriately
        val workInfo = workManager.getWorkInfoById(retrySyncRequest.id).get()
        assertTrue(workInfo.state in listOf(WorkInfo.State.SUCCEEDED, WorkInfo.State.FAILED))
        assertTrue(workInfo.runAttemptCount <= 4) // Initial + 3 retries
    }

    // ================== Periodic Sync Tests ==================

    @Test
    fun testPeriodicSyncScheduling() = runTest {
        // Given: Periodic sync configuration
        val periodicSyncRequest = PeriodicWorkRequestBuilder<AnalyticsSyncWorker>(
            15, TimeUnit.MINUTES
        ).setInputData(
            workDataOf(
                "sync_type" to "periodic",
                "requires_wifi" to false
            )
        ).addTag("periodic_analytics_sync")
        .build()
        
        // When: Periodic work is enqueued
        workManager.enqueueUniquePeriodicWork(
            "analytics_periodic_sync",
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicSyncRequest
        ).result.get()
        
        // Then: Periodic work should be scheduled
        val workInfos = workManager.getWorkInfosByTag("periodic_analytics_sync").get()
        assertTrue(workInfos.isNotEmpty())
        assertEquals(WorkInfo.State.ENQUEUED, workInfos.first().state)
    }

    @Test
    fun testPeriodicSyncCancellation() = runTest {
        // Given: Active periodic sync
        val periodicSyncRequest = PeriodicWorkRequestBuilder<AnalyticsSyncWorker>(
            15, TimeUnit.MINUTES
        ).addTag("cancellable_periodic_sync")
        .build()
        
        workManager.enqueueUniquePeriodicWork(
            "cancellable_analytics_sync",
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicSyncRequest
        ).result.get()
        
        // When: Periodic work is cancelled
        workManager.cancelUniqueWork("cancellable_analytics_sync").result.get()
        
        // Then: Work should be cancelled
        val workInfos = workManager.getWorkInfosByTag("cancellable_periodic_sync").get()
        assertTrue(workInfos.isEmpty() || workInfos.first().state == WorkInfo.State.CANCELLED)
    }

    // ================== COPPA Compliance Tests ==================

    @Test
    fun testCOPPACompliantSyncWorker() = runTest {
        // Given: COPPA compliant analytics data
        val compliantAnalytics = testUtils.createTestBehavioralAnalytics(
            interactionType = "coppa_test",
            behavioralCategory = "engagement",
            behavioralIndicators = mapOf(
                "engagement_level" to "high",
                "interaction_count" to 5
            )
        )
        database.behavioralAnalyticsDao().insertBehavioralAnalytics(compliantAnalytics)
        
        // When: Sync worker processes COPPA data
        val inputData = workDataOf(
            "sync_type" to "regular",
            "coppa_validation" to true
        )
        
        val complianceSyncRequest = OneTimeWorkRequestBuilder<AnalyticsSyncWorker>()
            .setInputData(inputData)
            .addTag("coppa_compliant_sync")
            .build()
        
        workManager.enqueue(complianceSyncRequest).result.get()
        
        // Then: Sync should maintain COPPA compliance
        val workInfo = workManager.getWorkInfoById(complianceSyncRequest.id).get()
        assertEquals(WorkInfo.State.SUCCEEDED, workInfo.state)
        
        // Verify data remains compliant after sync
        val storedAnalytics = testUtils.getStoredAnalytics()
        val complianceResult = testUtils.validateAnalyticsCOPPACompliance(storedAnalytics)
        assertTrue(complianceResult.isCompliant, "Synced data must remain COPPA compliant")
    }

    @Test
    fun testDataRetentionCleanupWorker() = runTest {
        // Given: Analytics data older than retention period
        val oldAnalytics = testUtils.createTestBehavioralAnalytics(
            interactionType = "old_data",
            behavioralCategory = "engagement"
        ).copy(
            createdAt = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(91) // 91 days old
        )
        database.behavioralAnalyticsDao().insertBehavioralAnalytics(oldAnalytics)
        
        val recentAnalytics = testUtils.createTestBehavioralAnalytics(
            interactionType = "recent_data",
            behavioralCategory = "engagement"
        )
        database.behavioralAnalyticsDao().insertBehavioralAnalytics(recentAnalytics)
        
        // When: Cleanup sync worker is executed
        val inputData = workDataOf(
            "sync_type" to "cleanup",
            "retention_days" to 90
        )
        
        val cleanupSyncRequest = OneTimeWorkRequestBuilder<AnalyticsSyncWorker>()
            .setInputData(inputData)
            .build()
        
        workManager.enqueue(cleanupSyncRequest).result.get()
        
        // Then: Old data should be cleaned up
        val workInfo = workManager.getWorkInfoById(cleanupSyncRequest.id).get()
        assertEquals(WorkInfo.State.SUCCEEDED, workInfo.state)
        
        val remainingAnalytics = testUtils.getStoredAnalytics()
        assertTrue(remainingAnalytics.none { it.interactionType == "old_data" })
        assertTrue(remainingAnalytics.any { it.interactionType == "recent_data" })
    }

    // ================== Performance Tests ==================

    @Test
    fun testLargeDatasetSyncPerformance() = runTest {
        // Given: Large dataset for performance testing
        repeat(100) { index ->
            val testAnalytics = testUtils.createTestBehavioralAnalytics(
                interactionType = "performance_test_$index",
                behavioralCategory = "engagement",
                behavioralIndicators = testUtils.generateRealisticBehavioralData("engagement")
            )
            database.behavioralAnalyticsDao().insertBehavioralAnalytics(testAnalytics)
        }
        
        // When: Large dataset sync is performed
        val startTime = System.currentTimeMillis()
        
        val inputData = workDataOf(
            "sync_type" to "batch",
            "batch_size" to 100,
            "performance_test" to true
        )
        
        val performanceSyncRequest = OneTimeWorkRequestBuilder<AnalyticsSyncWorker>()
            .setInputData(inputData)
            .build()
        
        workManager.enqueue(performanceSyncRequest).result.get()
        
        val endTime = System.currentTimeMillis()
        val syncDuration = endTime - startTime
        
        // Then: Sync should complete within performance threshold
        val workInfo = workManager.getWorkInfoById(performanceSyncRequest.id).get()
        assertEquals(WorkInfo.State.SUCCEEDED, workInfo.state)
        
        assertTrue(syncDuration < 30000, "Large dataset sync should complete under 30 seconds")
        assertEquals(100, workInfo.outputData.getInt("sync_success_count", 0))
    }

    @Test
    fun testConcurrentSyncWorkerExecution() = runTest {
        // Given: Multiple analytics datasets for concurrent sync
        val syncRequests = mutableListOf<WorkRequest>()
        
        repeat(3) { workerIndex ->
            repeat(10) { dataIndex ->
                val testAnalytics = testUtils.createTestBehavioralAnalytics(
                    interactionType = "concurrent_test_${workerIndex}_$dataIndex",
                    behavioralCategory = "engagement"
                )
                database.behavioralAnalyticsDao().insertBehavioralAnalytics(testAnalytics)
            }
            
            val syncRequest = OneTimeWorkRequestBuilder<AnalyticsSyncWorker>()
                .setInputData(workDataOf(
                    "sync_type" to "batch",
                    "batch_size" to 10,
                    "worker_id" to workerIndex
                ))
                .addTag("concurrent_worker_$workerIndex")
                .build()
            
            syncRequests.add(syncRequest)
        }
        
        // When: Multiple sync workers execute concurrently
        workManager.enqueue(syncRequests).result.get()
        
        // Then: All workers should complete successfully
        syncRequests.forEach { request ->
            val workInfo = workManager.getWorkInfoById(request.id).get()
            assertEquals(WorkInfo.State.SUCCEEDED, workInfo.state)
        }
        
        // Verify all data was synced
        val totalSyncedCount = syncRequests.sumOf { request ->
            workManager.getWorkInfoById(request.id).get()
                .outputData.getInt("sync_success_count", 0)
        }
        assertEquals(30, totalSyncedCount)
    }

    // ================== Error Handling Tests ==================

    @Test
    fun testSyncWorkerErrorRecovery() = runTest {
        // Given: Analytics data with potential sync issues
        val problematicAnalytics = testUtils.createTestBehavioralAnalytics(
            interactionType = "error_test",
            behavioralCategory = "engagement",
            behavioralIndicators = mapOf(
                "test_error" to "simulate_failure"
            )
        )
        database.behavioralAnalyticsDao().insertBehavioralAnalytics(problematicAnalytics)
        
        // When: Sync worker encounters errors
        val inputData = workDataOf(
            "sync_type" to "regular",
            "simulate_error" to true,
            "max_retries" to 2
        )
        
        val errorSyncRequest = OneTimeWorkRequestBuilder<AnalyticsSyncWorker>()
            .setInputData(inputData)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.SECONDS)
            .build()
        
        workManager.enqueue(errorSyncRequest).result.get()
        
        // Then: Worker should handle errors gracefully
        val workInfo = workManager.getWorkInfoById(errorSyncRequest.id).get()
        assertTrue(workInfo.state in listOf(WorkInfo.State.SUCCEEDED, WorkInfo.State.FAILED))
        
        if (workInfo.state == WorkInfo.State.FAILED) {
            assertTrue(workInfo.runAttemptCount > 1, "Should have attempted retries")
        }
    }
}