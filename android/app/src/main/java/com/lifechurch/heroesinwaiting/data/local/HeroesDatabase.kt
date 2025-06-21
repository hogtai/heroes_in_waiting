package com.lifechurch.heroesinwaiting.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.lifechurch.heroesinwaiting.data.local.converters.StringListConverter
import com.lifechurch.heroesinwaiting.data.local.dao.*
import com.lifechurch.heroesinwaiting.data.local.entities.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(
    entities = [
        LessonEntity::class,
        ActivityEntity::class,
        ScenarioEntity::class,
        ProgressEntity::class,
        ClassroomEntity::class,
        EmotionalCheckinEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(StringListConverter::class)
abstract class HeroesDatabase : RoomDatabase() {
    
    abstract fun lessonDao(): LessonDao
    abstract fun activityDao(): ActivityDao
    abstract fun scenarioDao(): ScenarioDao
    abstract fun progressDao(): ProgressDao
    abstract fun classroomDao(): ClassroomDao
    abstract fun emotionalCheckinDao(): EmotionalCheckinDao
    
    companion object {
        const val DATABASE_NAME = "heroes_database"
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideHeroesDatabase(@ApplicationContext context: Context): HeroesDatabase {
        return Room.databaseBuilder(
            context,
            HeroesDatabase::class.java,
            HeroesDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration() // For development - remove in production
        .build()
    }
    
    @Provides
    fun provideLessonDao(database: HeroesDatabase): LessonDao = database.lessonDao()
    
    @Provides
    fun provideActivityDao(database: HeroesDatabase): ActivityDao = database.activityDao()
    
    @Provides
    fun provideScenarioDao(database: HeroesDatabase): ScenarioDao = database.scenarioDao()
    
    @Provides
    fun provideProgressDao(database: HeroesDatabase): ProgressDao = database.progressDao()
    
    @Provides
    fun provideClassroomDao(database: HeroesDatabase): ClassroomDao = database.classroomDao()
    
    @Provides
    fun provideEmotionalCheckinDao(database: HeroesDatabase): EmotionalCheckinDao = database.emotionalCheckinDao()
}