package com.lifechurch.heroesinwaiting.di

import android.content.Context
import android.content.SharedPreferences
import androidx.work.WorkManager
import com.lifechurch.heroesinwaiting.data.analytics.AnalyticsService
import com.lifechurch.heroesinwaiting.data.analytics.AnalyticsSyncManager
import com.lifechurch.heroesinwaiting.data.analytics.AdaptiveSyncScheduler
import com.lifechurch.heroesinwaiting.data.analytics.AnalyticsBatchManager
import com.lifechurch.heroesinwaiting.data.analytics.NetworkAwareSyncCoordinator
import com.lifechurch.heroesinwaiting.data.repository.AnalyticsRepository
import com.lifechurch.heroesinwaiting.data.privacy.COPPAComplianceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for analytics-related dependencies
 * Provides analytics service, repository, and sync management components
 */
@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {
    
    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("heroes_analytics_prefs", Context.MODE_PRIVATE)
    }
    
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
    
    @Provides
    @Singleton
    fun provideAnalyticsSyncManager(
        @ApplicationContext context: Context,
        workManager: WorkManager
    ): AnalyticsSyncManager {
        return AnalyticsSyncManager(context, workManager)
    }
    
    @Provides
    @Singleton
    fun provideAdaptiveSyncScheduler(
        syncManager: AnalyticsSyncManager,
        @ApplicationContext context: Context
    ): AdaptiveSyncScheduler {
        return AdaptiveSyncScheduler(syncManager, context)
    }
    
    @Provides
    @Singleton
    fun provideCOPPAComplianceManager(
        @ApplicationContext context: Context,
        sharedPreferences: SharedPreferences,
        analyticsEventDao: com.lifechurch.heroesinwaiting.data.local.dao.AnalyticsEventDao,
        behavioralAnalyticsDao: com.lifechurch.heroesinwaiting.data.local.dao.BehavioralAnalyticsDao
    ): COPPAComplianceManager {
        return COPPAComplianceManager(context, sharedPreferences, analyticsEventDao, behavioralAnalyticsDao)
    }
    
    @Provides
    @Singleton
    fun provideAnalyticsBatchManager(
        analyticsEventDao: com.lifechurch.heroesinwaiting.data.local.dao.AnalyticsEventDao,
        behavioralAnalyticsDao: com.lifechurch.heroesinwaiting.data.local.dao.BehavioralAnalyticsDao,
        syncBatchDao: com.lifechurch.heroesinwaiting.data.local.dao.AnalyticsSyncBatchDao
    ): AnalyticsBatchManager {
        return AnalyticsBatchManager(analyticsEventDao, behavioralAnalyticsDao, syncBatchDao)
    }
    
    @Provides
    @Singleton
    fun provideNetworkAwareSyncCoordinator(
        @ApplicationContext context: Context,
        analyticsRepository: AnalyticsRepository,
        batchManager: AnalyticsBatchManager,
        syncManager: AnalyticsSyncManager
    ): NetworkAwareSyncCoordinator {
        return NetworkAwareSyncCoordinator(context, analyticsRepository, batchManager, syncManager)
    }
    
    @Provides
    @Singleton
    fun provideAnalyticsService(
        @ApplicationContext context: Context,
        analyticsRepository: AnalyticsRepository,
        sharedPreferences: SharedPreferences,
        coppaComplianceManager: COPPAComplianceManager
    ): AnalyticsService {
        return AnalyticsService(context, analyticsRepository, sharedPreferences, coppaComplianceManager)
    }
}