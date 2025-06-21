package com.lifechurch.heroesinwaiting.data.database.dao

import androidx.room.*
import com.lifechurch.heroesinwaiting.data.database.entity.LessonEntity
import com.lifechurch.heroesinwaiting.data.model.Grade
import com.lifechurch.heroesinwaiting.data.model.LessonCategory
import com.lifechurch.heroesinwaiting.data.model.DifficultyLevel
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Lesson operations
 */
@Dao
interface LessonDao {
    
    @Query("SELECT * FROM lessons WHERE id = :id")
    suspend fun getLessonById(id: String): LessonEntity?
    
    @Query("SELECT * FROM lessons WHERE id = :id")
    fun getLessonByIdFlow(id: String): Flow<LessonEntity?>
    
    @Query("SELECT * FROM lessons WHERE isActive = 1 ORDER BY lessonNumber")
    fun getAllActiveLessonsFlow(): Flow<List<LessonEntity>>
    
    @Query("SELECT * FROM lessons WHERE isActive = 1 ORDER BY lessonNumber")
    suspend fun getAllActiveLessons(): List<LessonEntity>
    
    @Query("SELECT * FROM lessons WHERE grade = :grade AND isActive = 1 ORDER BY lessonNumber")
    suspend fun getLessonsByGrade(grade: Grade): List<LessonEntity>
    
    @Query("SELECT * FROM lessons WHERE grade = :grade AND isActive = 1 ORDER BY lessonNumber")
    fun getLessonsByGradeFlow(grade: Grade): Flow<List<LessonEntity>>
    
    @Query("SELECT * FROM lessons WHERE category = :category AND isActive = 1 ORDER BY lessonNumber")
    suspend fun getLessonsByCategory(category: LessonCategory): List<LessonEntity>
    
    @Query("SELECT * FROM lessons WHERE category = :category AND isActive = 1 ORDER BY lessonNumber")
    fun getLessonsByCategoryFlow(category: LessonCategory): Flow<List<LessonEntity>>
    
    @Query("SELECT * FROM lessons WHERE difficultyLevel = :difficulty AND isActive = 1 ORDER BY lessonNumber")
    suspend fun getLessonsByDifficulty(difficulty: DifficultyLevel): List<LessonEntity>
    
    @Query("SELECT * FROM lessons WHERE grade = :grade AND category = :category AND isActive = 1 ORDER BY lessonNumber")
    suspend fun getLessonsByGradeAndCategory(grade: Grade, category: LessonCategory): List<LessonEntity>
    
    @Query("SELECT * FROM lessons WHERE lessonNumber BETWEEN :start AND :end AND isActive = 1 ORDER BY lessonNumber")
    suspend fun getLessonsInRange(start: Int, end: Int): List<LessonEntity>
    
    @Query("SELECT * FROM lessons WHERE isActive = 1 AND isDownloaded = 1 ORDER BY lessonNumber")
    suspend fun getDownloadedLessons(): List<LessonEntity>
    
    @Query("SELECT * FROM lessons WHERE isActive = 1 AND isDownloaded = 1 ORDER BY lessonNumber")
    fun getDownloadedLessonsFlow(): Flow<List<LessonEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: LessonEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessons(lessons: List<LessonEntity>)
    
    @Update
    suspend fun updateLesson(lesson: LessonEntity)
    
    @Query("UPDATE lessons SET isDownloaded = :isDownloaded WHERE id = :id")
    suspend fun updateDownloadStatus(id: String, isDownloaded: Boolean)
    
    @Query("UPDATE lessons SET lastSyncAt = :timestamp WHERE id = :id")
    suspend fun updateLastSyncTime(id: String, timestamp: Long)
    
    @Delete
    suspend fun deleteLesson(lesson: LessonEntity)
    
    @Query("DELETE FROM lessons WHERE id = :id")
    suspend fun deleteLessonById(id: String)
    
    @Query("DELETE FROM lessons")
    suspend fun deleteAllLessons()
    
    @Query("SELECT * FROM lessons WHERE lastSyncAt < :timestamp")
    suspend fun getLessonsNeedingSync(timestamp: Long): List<LessonEntity>
    
    @Query("SELECT COUNT(*) FROM lessons WHERE isActive = 1")
    suspend fun getTotalLessonsCount(): Int
    
    @Query("SELECT COUNT(*) FROM lessons WHERE isActive = 1 AND isDownloaded = 1")
    suspend fun getDownloadedLessonsCount(): Int
    
    @Query("SELECT * FROM lessons WHERE title LIKE '%' || :query || '%' OR keyTerms LIKE '%' || :query || '%' AND isActive = 1 ORDER BY lessonNumber")
    suspend fun searchLessons(query: String): List<LessonEntity>
    
    @Query("SELECT * FROM lessons WHERE title LIKE '%' || :query || '%' OR keyTerms LIKE '%' || :query || '%' AND isActive = 1 ORDER BY lessonNumber")
    fun searchLessonsFlow(query: String): Flow<List<LessonEntity>>
}