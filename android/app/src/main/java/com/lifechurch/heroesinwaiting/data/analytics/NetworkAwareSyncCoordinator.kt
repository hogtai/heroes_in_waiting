package com.lifechurch.heroesinwaiting.data.analytics

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.BatteryManager
import com.lifechurch.heroesinwaiting.data.repository.AnalyticsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Network-aware coordinator for analytics sync operations
 * Monitors network conditions and optimizes sync strategies accordingly
 */
@Singleton
class NetworkAwareSyncCoordinator @Inject constructor(
    private val context: Context,
    private val analyticsRepository: AnalyticsRepository,
    private val batchManager: AnalyticsBatchManager,
    private val syncManager: AnalyticsSyncManager
) {
    
    private val coordinatorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    // Network state tracking
    private val _networkState = MutableStateFlow(NetworkState())
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()
    
    // Sync strategy tracking
    private val _currentStrategy = MutableStateFlow(SyncStrategy.CONSERVATIVE)
    val currentStrategy: StateFlow<SyncStrategy> = _currentStrategy.asStateFlow()
    
    // Network callback for monitoring connectivity changes
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            coordinatorScope.launch {
                updateNetworkState()
                adaptSyncStrategy()
            }
        }
        
        override fun onLost(network: Network) {
            coordinatorScope.launch {
                updateNetworkState()
                adaptSyncStrategy()
            }
        }
        
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            coordinatorScope.launch {
                updateNetworkState()
                adaptSyncStrategy()
            }
        }
    }
    
    init {
        // Register network monitoring
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        
        // Initial network state check
        coordinatorScope.launch {
            updateNetworkState()
            adaptSyncStrategy()
            startSyncCoordination()
        }
    }
    
    /**
     * Starts the main sync coordination loop
     */
    private suspend fun startSyncCoordination() {
        while (true) {
            try {
                when (_currentStrategy.value) {
                    SyncStrategy.AGGRESSIVE -> {
                        performAggressiveSync()
                        delay(30000) // 30 seconds
                    }
                    SyncStrategy.MODERATE -> {
                        performModerateSync()
                        delay(60000) // 1 minute
                    }
                    SyncStrategy.CONSERVATIVE -> {
                        performConservativeSync()
                        delay(300000) // 5 minutes
                    }
                    SyncStrategy.MINIMAL -> {
                        performMinimalSync()
                        delay(900000) // 15 minutes
                    }
                    SyncStrategy.DISABLED -> {
                        delay(1800000) // 30 minutes - check if conditions improved
                    }
                }
            } catch (e: Exception) {
                // Handle errors gracefully
                delay(60000) // Wait 1 minute before retry
            }
        }
    }
    
    /**
     * Updates current network state
     */
    private suspend fun updateNetworkState() {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
        
        val networkQuality = determineNetworkQuality(capabilities)
        val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isMetered = connectivityManager.isActiveNetworkMetered
        val batteryLevel = getBatteryLevel()
        
        _networkState.value = NetworkState(
            isConnected = activeNetwork != null,
            networkQuality = networkQuality,
            isWifi = isWifi,
            isMetered = isMetered,
            batteryLevel = batteryLevel,
            lastUpdate = System.currentTimeMillis()
        )
    }
    
    /**
     * Adapts sync strategy based on current conditions
     */
    private suspend fun adaptSyncStrategy() {
        val state = _networkState.value
        
        _currentStrategy.value = when {
            !state.isConnected -> SyncStrategy.DISABLED
            
            state.batteryLevel == BatteryLevel.CRITICAL -> SyncStrategy.DISABLED
            
            state.batteryLevel == BatteryLevel.LOW && state.isMetered -> SyncStrategy.MINIMAL
            
            state.networkQuality == NetworkQuality.HIGH && 
            state.isWifi && 
            state.batteryLevel >= BatteryLevel.NORMAL -> SyncStrategy.AGGRESSIVE
            
            state.networkQuality >= NetworkQuality.MEDIUM && 
            !state.isMetered -> SyncStrategy.MODERATE
            
            state.networkQuality >= NetworkQuality.LOW -> SyncStrategy.CONSERVATIVE
            
            else -> SyncStrategy.MINIMAL
        }
        
        // Update sync manager with new strategy
        updateSyncManagerStrategy(_currentStrategy.value)
    }
    
    /**
     * Performs aggressive sync for optimal conditions
     */
    private suspend fun performAggressiveSync() {
        val state = _networkState.value
        
        // Create large batches for efficient transmission
        val batchIds = batchManager.createOptimizedBatches(
            networkQuality = state.networkQuality,
            batteryLevel = state.batteryLevel,
            wifiOnly = false
        )
        
        // Process all pending analytics
        analyticsRepository.syncPendingAnalytics()
        
        // Process retry batches
        batchManager.processRetryableBatches()
        
        // Schedule regular sync
        syncManager.scheduleRegularSync()
        syncManager.scheduleBatchSync(requiresWifi = false)
    }
    
    /**
     * Performs moderate sync for good conditions
     */
    private suspend fun performModerateSync() {
        val state = _networkState.value
        
        // Create medium-sized batches
        val batchIds = batchManager.createOptimizedBatches(
            networkQuality = state.networkQuality,
            batteryLevel = state.batteryLevel,
            wifiOnly = state.isMetered
        )
        
        // Sync behavioral analytics first (higher priority)
        analyticsRepository.syncPendingAnalytics()
        
        // Process some retry batches
        batchManager.processRetryableBatches()
        
        // Schedule regular sync
        syncManager.scheduleRegularSync()
    }
    
    /**
     * Performs conservative sync for limited conditions
     */
    private suspend fun performConservativeSync() {
        val state = _networkState.value
        
        // Create small batches to minimize data usage
        val batchIds = batchManager.createOptimizedBatches(
            networkQuality = state.networkQuality,
            batteryLevel = state.batteryLevel,
            wifiOnly = true
        )
        
        // Only sync high-priority behavioral analytics
        if (state.isWifi) {
            analyticsRepository.syncPendingAnalytics()
        }
        
        // Schedule batch sync with WiFi requirement
        syncManager.scheduleBatchSync(requiresWifi = true)
    }
    
    /**
     * Performs minimal sync for poor conditions
     */
    private suspend fun performMinimalSync() {
        // Only sync critical behavioral analytics
        if (_networkState.value.isWifi) {
            // Very small batches, WiFi only
            val batchIds = batchManager.createOptimizedBatches(
                networkQuality = NetworkQuality.LOW,
                batteryLevel = _networkState.value.batteryLevel,
                wifiOnly = true
            )
            
            // Sync only priority data
            syncManager.schedulePrioritySync()
        }
    }
    
    /**
     * Updates sync manager with current strategy
     */
    private fun updateSyncManagerStrategy(strategy: SyncStrategy) {
        when (strategy) {
            SyncStrategy.AGGRESSIVE -> {
                syncManager.scheduleRegularSync()
                syncManager.scheduleBatchSync(requiresWifi = false)
            }
            SyncStrategy.MODERATE -> {
                syncManager.scheduleRegularSync()
                syncManager.scheduleBatchSync(requiresWifi = true)
            }
            SyncStrategy.CONSERVATIVE -> {
                syncManager.scheduleBatchSync(requiresWifi = true)
            }
            SyncStrategy.MINIMAL -> {
                syncManager.schedulePrioritySync()
            }
            SyncStrategy.DISABLED -> {
                syncManager.cancelAllSyncWork()
            }
        }
    }
    
    /**
     * Determines network quality based on capabilities
     */
    private fun determineNetworkQuality(capabilities: NetworkCapabilities?): NetworkQuality {
        if (capabilities == null) return NetworkQuality.NONE
        
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) -> NetworkQuality.HIGH
            
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkQuality.MEDIUM
            
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                // Could be enhanced to check cellular signal strength
                NetworkQuality.MEDIUM
            }
            
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> NetworkQuality.LOW
            
            else -> NetworkQuality.NONE
        }
    }
    
    /**
     * Gets current battery level
     */
    private fun getBatteryLevel(): BatteryLevel {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        
        return when {
            batteryLevel >= 80 -> BatteryLevel.HIGH
            batteryLevel >= 50 -> BatteryLevel.NORMAL
            batteryLevel >= 20 -> BatteryLevel.LOW
            else -> BatteryLevel.CRITICAL
        }
    }
    
    /**
     * Forces immediate sync (user-triggered)
     */
    suspend fun forceImmediateSync(): Result<Unit> {
        return try {
            val result = analyticsRepository.syncPendingAnalytics()
            if (result.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Sync failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Gets current sync health status
     */
    suspend fun getSyncHealthStatus(): SyncHealthStatus {
        val batchHealth = batchManager.generateBatchHealthReport()
        val networkState = _networkState.value
        val syncStrategy = _currentStrategy.value
        
        return SyncHealthStatus(
            batchHealth = batchHealth,
            networkState = networkState,
            currentStrategy = syncStrategy,
            lastSync = System.currentTimeMillis() // Would track actual last sync
        )
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}

// Supporting data classes and enums

data class NetworkState(
    val isConnected: Boolean = false,
    val networkQuality: NetworkQuality = NetworkQuality.NONE,
    val isWifi: Boolean = false,
    val isMetered: Boolean = true,
    val batteryLevel: BatteryLevel = BatteryLevel.NORMAL,
    val lastUpdate: Long = 0L
)

enum class SyncStrategy {
    AGGRESSIVE,   // Best conditions - frequent, large batches
    MODERATE,     // Good conditions - regular, medium batches  
    CONSERVATIVE, // Limited conditions - infrequent, small batches
    MINIMAL,      // Poor conditions - critical data only
    DISABLED      // No network or critical battery
}

data class SyncHealthStatus(
    val batchHealth: BatchHealthReport,
    val networkState: NetworkState,
    val currentStrategy: SyncStrategy,
    val lastSync: Long
)