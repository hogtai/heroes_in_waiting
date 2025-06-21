package com.lifechurch.heroesinwaiting.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.lifechurch.heroesinwaiting.data.database.HeroesDatabase
import com.lifechurch.heroesinwaiting.data.database.dao.*
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
            "heroes_in_waiting_database"
        )
        .fallbackToDestructiveMigration() // For development only
        .build()
    }
    
    @Provides
    fun provideFacilitatorDao(database: HeroesDatabase): FacilitatorDao {
        return database.facilitatorDao()
    }
    
    @Provides
    fun provideStudentDao(database: HeroesDatabase): StudentDao {
        return database.studentDao()
    }
    
    @Provides
    fun provideLessonDao(database: HeroesDatabase): LessonDao {
        return database.lessonDao()
    }
    
    @Provides
    fun provideClassroomDao(database: HeroesDatabase): ClassroomDao {
        return database.classroomDao()
    }
}