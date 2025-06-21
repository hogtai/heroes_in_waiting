package com.lifechurch.heroesinwaiting.data.local.dao

import androidx.room.*
import com.lifechurch.heroesinwaiting.data.local.entities.LessonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonDao {
    
    @Query("SELECT * FROM lessons WHERE isActive = 1 ORDER BY orderIndex ASC")
    fun getAllLessons(): Flow<List<LessonEntity>>
    
    @Query("SELECT * FROM lessons WHERE id = :lessonId")
    suspend fun getLessonById(lessonId: String): LessonEntity?
    
    @Query("SELECT * FROM lessons WHERE id = :lessonId")
    fun getLessonByIdFlow(lessonId: String): Flow<LessonEntity?>
    
    @Query("SELECT * FROM lessons WHERE category = :category AND isActive = 1 ORDER BY orderIndex ASC")
    fun getLessonsByCategory(category: String): Flow<List<LessonEntity>>
    
    @Query("SELECT * FROM lessons WHERE isActive = 1 AND mainContent IS NOT NULL ORDER BY orderIndex ASC")
    fun getLessonsWithContent(): Flow<List<LessonEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: LessonEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessons(lessons: List<LessonEntity>)
    
    @Update
    suspend fun updateLesson(lesson: LessonEntity)
    
    @Delete
    suspend fun deleteLesson(lesson: LessonEntity)
    
    @Query("DELETE FROM lessons WHERE id = :lessonId")
    suspend fun deleteLessonById(lessonId: String)
    
    @Query("DELETE FROM lessons")
    suspend fun deleteAllLessons()
    
    @Query("SELECT COUNT(*) FROM lessons WHERE isActive = 1")
    suspend fun getActiveLessonCount(): Int
    
    @Query("SELECT * FROM lessons WHERE cachedAt < :cutoffTime")
    suspend fun getStaleContent(cutoffTime: Long): List<LessonEntity>
    
    @Query("UPDATE lessons SET lastUpdated = :timestamp WHERE id = :lessonId")
    suspend fun updateLastUpdated(lessonId: String, timestamp: Long = System.currentTimeMillis())
}