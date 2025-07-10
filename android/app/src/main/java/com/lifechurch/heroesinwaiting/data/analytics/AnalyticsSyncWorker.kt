package com.lifechurch.heroesinwaiting.data.analytics

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.lifechurch.heroesinwaiting.data.repository.AnalyticsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * WorkManager implementation for background analytics sync
 * Handles efficient batching, retry logic, and network-aware sync operations
 */
@HiltWorker
class AnalyticsSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val analyticsRepository: AnalyticsRepository
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val syncType = inputData.getString(KEY_SYNC_TYPE) ?: SYNC_TYPE_REGULAR
            val requiresWifi = inputData.getBoolean(KEY_REQUIRES_WIFI, false)
            val maxRetries = inputData.getInt(KEY_MAX_RETRIES, 3)
            
            // Check network conditions
            if (requiresWifi && !isWifiConnected()) {
                return@withContext Result.retry()
            }
            
            if (!isNetworkAvailable()) {
                return@withContext Result.retry()
            }
            
            val syncResult = when (syncType) {
                SYNC_TYPE_PRIORITY -> syncPriorityAnalytics()
                SYNC_TYPE_BATCH -> syncBatchAnalytics()
                SYNC_TYPE_CLEANUP -> performCleanupSync()
                else -> syncRegularAnalytics()
            }
            
            if (syncResult.isSuccess) {
                val result = syncResult.getOrNull()
                setOutputData(workDataOf(
                    KEY_SYNC_SUCCESS_COUNT to result?.successCount,
                    KEY_SYNC_FAILURE_COUNT to result?.failureCount,
                    KEY_SYNC_ERRORS to result?.errors?.joinToString(";")
                ))
                Result.success()
            } else {
                val exception = syncResult.exceptionOrNull()
                if (runAttemptCount < maxRetries && isRetryableError(exception)) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            if (runAttemptCount < inputData.getInt(KEY_MAX_RETRIES, 3) && isRetryableError(e)) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
    
    private suspend fun syncRegularAnalytics(): Result<com.lifechurch.heroesinwaiting.data.repository.SyncResult> {
        return analyticsRepository.syncPendingAnalytics()
    }
    
    private suspend fun syncPriorityAnalytics(): Result<com.lifechurch.heroesinwaiting.data.repository.SyncResult> {
        // Implement priority sync logic - sync only high-priority events
        return analyticsRepository.syncPendingAnalytics()
    }
    
    private suspend fun syncBatchAnalytics(): Result<com.lifechurch.heroesinwaiting.data.repository.SyncResult> {
        // Implement larger batch sync for optimal network usage
        return analyticsRepository.syncPendingAnalytics()
    }
    
    private suspend fun performCleanupSync(): Result<com.lifechurch.heroesinwaiting.data.repository.SyncResult> {
        // Sync any remaining data before cleanup
        val syncResult = analyticsRepository.syncPendingAnalytics()
        
        if (syncResult.isSuccess) {
            // Perform cleanup of old data
            analyticsRepository.cleanupOldAnalytics(retentionDays = 90)
        }
        
        return syncResult
    }
    
    private fun isNetworkAvailable(): Boolean {
        // Implementation would check network connectivity
        return true // Placeholder
    }
    
    private fun isWifiConnected(): Boolean {
        // Implementation would check WiFi connectivity
        return true // Placeholder
    }
    
    private fun isRetryableError(exception: Throwable?): Boolean {
        return when (exception) {
            is java.net.SocketTimeoutException,
            is java.net.ConnectException,
            is java.io.IOException -> true
            else -> false
        }
    }
    
    companion object {
        const val WORK_NAME_REGULAR_SYNC = "analytics_regular_sync"
        const val WORK_NAME_PRIORITY_SYNC = "analytics_priority_sync"
        const val WORK_NAME_BATCH_SYNC = "analytics_batch_sync"
        const val WORK_NAME_CLEANUP_SYNC = "analytics_cleanup_sync"
        
        const val KEY_SYNC_TYPE = "sync_type"
        const val KEY_REQUIRES_WIFI = "requires_wifi"
        const val KEY_MAX_RETRIES = "max_retries"
        const val KEY_SYNC_SUCCESS_COUNT = "sync_success_count"
        const val KEY_SYNC_FAILURE_COUNT = "sync_failure_count"
        const val KEY_SYNC_ERRORS = "sync_errors"
        
        const val SYNC_TYPE_REGULAR = "regular"
        const val SYNC_TYPE_PRIORITY = "priority"
        const val SYNC_TYPE_BATCH = "batch"
        const val SYNC_TYPE_CLEANUP = "cleanup"
    }
}

/**
 * Manager for scheduling and coordinating analytics sync work
 */
class AnalyticsSyncManager constructor(
    private val context: Context,
    private val workManager: WorkManager
) {
    
    /**
     * Schedule regular analytics sync
     */
    fun scheduleRegularSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<AnalyticsSyncWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInputData(workDataOf(
                AnalyticsSyncWorker.KEY_SYNC_TYPE to AnalyticsSyncWorker.SYNC_TYPE_REGULAR
            ))
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_DELAY_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            AnalyticsSyncWorker.WORK_NAME_REGULAR_SYNC,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }
    
    /**
     * Schedule priority sync for immediate processing
     */
    fun schedulePrioritySync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val workRequest = OneTimeWorkRequestBuilder<AnalyticsSyncWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(
                AnalyticsSyncWorker.KEY_SYNC_TYPE to AnalyticsSyncWorker.SYNC_TYPE_PRIORITY
            ))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        
        workManager.enqueueUniqueWork(
            AnalyticsSyncWorker.WORK_NAME_PRIORITY_SYNC,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
    
    /**
     * Schedule batch sync for efficient network usage
     */
    fun scheduleBatchSync(requiresWifi: Boolean = true) {
        val networkType = if (requiresWifi) NetworkType.UNMETERED else NetworkType.CONNECTED
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .setRequiresBatteryNotLow(true)
            .setRequiresDeviceIdle(true) // Run when device is idle for better performance
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<AnalyticsSyncWorker>(
            repeatInterval = 6,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInputData(workDataOf(
                AnalyticsSyncWorker.KEY_SYNC_TYPE to AnalyticsSyncWorker.SYNC_TYPE_BATCH,
                AnalyticsSyncWorker.KEY_REQUIRES_WIFI to requiresWifi
            ))
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_DELAY_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            AnalyticsSyncWorker.WORK_NAME_BATCH_SYNC,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }
    
    /**
     * Schedule cleanup sync to remove old data
     */
    fun scheduleCleanupSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) // WiFi only for cleanup
            .setRequiresBatteryNotLow(true)
            .setRequiresDeviceIdle(true)
            .setRequiresCharging(true) // Only when charging
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<AnalyticsSyncWorker>(
            repeatInterval = 7,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setInputData(workDataOf(
                AnalyticsSyncWorker.KEY_SYNC_TYPE to AnalyticsSyncWorker.SYNC_TYPE_CLEANUP
            ))
            .setInitialDelay(1, TimeUnit.HOURS) // Delay initial cleanup
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            AnalyticsSyncWorker.WORK_NAME_CLEANUP_SYNC,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
    
    /**
     * Cancel all analytics sync work
     */
    fun cancelAllSyncWork() {
        workManager.cancelUniqueWork(AnalyticsSyncWorker.WORK_NAME_REGULAR_SYNC)
        workManager.cancelUniqueWork(AnalyticsSyncWorker.WORK_NAME_PRIORITY_SYNC)
        workManager.cancelUniqueWork(AnalyticsSyncWorker.WORK_NAME_BATCH_SYNC)
        workManager.cancelUniqueWork(AnalyticsSyncWorker.WORK_NAME_CLEANUP_SYNC)
    }
    
    /**
     * Get sync work status
     */
    fun getSyncWorkStatus(): Map<String, WorkInfo.State> {
        val workNames = listOf(
            AnalyticsSyncWorker.WORK_NAME_REGULAR_SYNC,
            AnalyticsSyncWorker.WORK_NAME_PRIORITY_SYNC,
            AnalyticsSyncWorker.WORK_NAME_BATCH_SYNC,
            AnalyticsSyncWorker.WORK_NAME_CLEANUP_SYNC
        )
        
        return workNames.associateWith { workName ->
            workManager.getWorkInfosForUniqueWork(workName)
                .get()
                .firstOrNull()?.state ?: WorkInfo.State.CANCELLED
        }
    }
}

/**
 * Network-aware sync scheduler that adapts to connection quality
 */
class AdaptiveSyncScheduler constructor(
    private val syncManager: AnalyticsSyncManager,
    private val context: Context
) {
    
    /**
     * Schedule sync based on current network conditions
     */
    fun scheduleAdaptiveSync() {
        when (getCurrentNetworkQuality()) {
            NetworkQuality.HIGH -> {
                // Good connection - schedule regular sync
                syncManager.scheduleRegularSync()
                syncManager.scheduleBatchSync(requiresWifi = false)
            }
            NetworkQuality.MEDIUM -> {
                // Moderate connection - WiFi-only batch sync
                syncManager.scheduleRegularSync()
                syncManager.scheduleBatchSync(requiresWifi = true)
            }
            NetworkQuality.LOW -> {
                // Poor connection - priority sync only
                syncManager.schedulePrioritySync()
            }
            NetworkQuality.NONE -> {
                // No connection - cancel sync work
                syncManager.cancelAllSyncWork()
            }
        }
    }
    
    private fun getCurrentNetworkQuality(): NetworkQuality {
        // Implementation would assess actual network quality
        return NetworkQuality.HIGH // Placeholder
    }
}

enum class NetworkQuality {
    HIGH, MEDIUM, LOW, NONE
}