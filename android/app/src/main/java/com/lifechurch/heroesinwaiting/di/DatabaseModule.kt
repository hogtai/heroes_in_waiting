package com.lifechurch.heroesinwaiting.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.lifechurch.heroesinwaiting.data.local.HeroesDatabase
import com.lifechurch.heroesinwaiting.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for database and data store dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "heroes_preferences")
    
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
    
    @Provides
    @Singleton
    fun provideHeroesDatabase(@ApplicationContext context: Context): HeroesDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            HeroesDatabase::class.java,
            HeroesDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration() // For development only
        .build()
    }
    
    @Provides
    fun provideLessonDao(database: HeroesDatabase): LessonDao {
        return database.lessonDao()
    }
    
    @Provides
    fun provideActivityDao(database: HeroesDatabase): ActivityDao {
        return database.activityDao()
    }
    
    @Provides
    fun provideScenarioDao(database: HeroesDatabase): ScenarioDao {
        return database.scenarioDao()
    }
    
    @Provides
    fun provideProgressDao(database: HeroesDatabase): ProgressDao {
        return database.progressDao()
    }
    
    @Provides
    fun provideClassroomDao(database: HeroesDatabase): ClassroomDao {
        return database.classroomDao()
    }
    
    @Provides
    fun provideEmotionalCheckinDao(database: HeroesDatabase): EmotionalCheckinDao {
        return database.emotionalCheckinDao()
    }
    
    @Provides
    fun provideBehavioralAnalyticsDao(database: HeroesDatabase): BehavioralAnalyticsDao {
        return database.behavioralAnalyticsDao()
    }
    
    @Provides
    fun provideAnalyticsEventDao(database: HeroesDatabase): AnalyticsEventDao {
        return database.analyticsEventDao()
    }
    
    @Provides
    fun provideAnalyticsSyncBatchDao(database: HeroesDatabase): AnalyticsSyncBatchDao {
        return database.analyticsSyncBatchDao()
    }
}