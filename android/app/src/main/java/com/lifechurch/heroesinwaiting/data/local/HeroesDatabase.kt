package com.lifechurch.heroesinwaiting.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.lifechurch.heroesinwaiting.data.local.converters.StringListConverter
import com.lifechurch.heroesinwaiting.data.local.converters.MapConverter
import com.lifechurch.heroesinwaiting.data.local.dao.*
import com.lifechurch.heroesinwaiting.data.local.entities.*

@Database(
    entities = [
        LessonEntity::class,
        ActivityEntity::class,
        ScenarioEntity::class,
        ProgressEntity::class,
        ClassroomEntity::class,
        EmotionalCheckinEntity::class,
        BehavioralAnalyticsEntity::class,
        AnalyticsEventEntity::class,
        AnalyticsSyncBatchEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(StringListConverter::class, MapConverter::class)
abstract class HeroesDatabase : RoomDatabase() {
    
    abstract fun lessonDao(): LessonDao
    abstract fun activityDao(): ActivityDao
    abstract fun scenarioDao(): ScenarioDao
    abstract fun progressDao(): ProgressDao
    abstract fun classroomDao(): ClassroomDao
    abstract fun emotionalCheckinDao(): EmotionalCheckinDao
    abstract fun behavioralAnalyticsDao(): BehavioralAnalyticsDao
    abstract fun analyticsEventDao(): AnalyticsEventDao
    abstract fun analyticsSyncBatchDao(): AnalyticsSyncBatchDao
    
    companion object {
        const val DATABASE_NAME = "heroes_database"
    }
}