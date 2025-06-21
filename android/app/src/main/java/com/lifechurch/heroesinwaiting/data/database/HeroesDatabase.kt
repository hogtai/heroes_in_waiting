package com.lifechurch.heroesinwaiting.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.lifechurch.heroesinwaiting.data.database.dao.*
import com.lifechurch.heroesinwaiting.data.database.entity.*
import com.lifechurch.heroesinwaiting.data.database.converter.DatabaseConverters

/**
 * Main Room database for Heroes in Waiting app
 * Provides offline-first data persistence with automatic synchronization
 */
@Database(
    entities = [
        FacilitatorEntity::class,
        StudentEntity::class,
        LessonEntity::class,
        ClassroomEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(DatabaseConverters::class)
abstract class HeroesDatabase : RoomDatabase() {
    
    // Data Access Objects
    abstract fun facilitatorDao(): FacilitatorDao
    abstract fun studentDao(): StudentDao
    abstract fun lessonDao(): LessonDao
    abstract fun classroomDao(): ClassroomDao
    
    companion object {
        @Volatile
        private var INSTANCE: HeroesDatabase? = null
        
        private const val DATABASE_NAME = "heroes_in_waiting_database"
        
        fun getDatabase(context: Context): HeroesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HeroesDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations()
                .fallbackToDestructiveMigration() // For development - remove in production
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * Creates an in-memory database for testing
         */
        fun getInMemoryDatabase(context: Context): HeroesDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                HeroesDatabase::class.java
            )
            .allowMainThreadQueries() // Only for testing
            .build()
        }
    }
}