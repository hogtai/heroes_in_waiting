package com.lifechurch.heroesinwaiting.data.local.dao

import androidx.room.*
import com.lifechurch.heroesinwaiting.data.local.entities.ProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    
    @Query("SELECT * FROM student_progress WHERE studentId = :studentId ORDER BY completedAt DESC")
    fun getProgressByStudent(studentId: String): Flow<List<ProgressEntity>>
    
    @Query("SELECT * FROM student_progress WHERE studentId = :studentId AND itemType = :itemType ORDER BY completedAt DESC")
    fun getProgressByStudentAndType(studentId: String, itemType: String): Flow<List<ProgressEntity>>
    
    @Query("SELECT * FROM student_progress WHERE studentId = :studentId AND itemId = :itemId AND itemType = :itemType")
    suspend fun getProgressByStudentAndItem(studentId: String, itemId: String, itemType: String): ProgressEntity?
    
    @Query("SELECT * FROM student_progress WHERE syncedToServer = 0 ORDER BY createdAt ASC")
    suspend fun getUnsyncedProgress(): List<ProgressEntity>
    
    @Query("SELECT COUNT(*) FROM student_progress WHERE studentId = :studentId AND itemType = :itemType")
    suspend fun getCompletedCount(studentId: String, itemType: String): Int
    
    @Query("SELECT SUM(timeSpent) FROM student_progress WHERE studentId = :studentId AND timeSpent IS NOT NULL")
    suspend fun getTotalTimeSpent(studentId: String): Int?
    
    @Query("SELECT AVG(score) FROM student_progress WHERE studentId = :studentId AND score IS NOT NULL")
    suspend fun getAverageScore(studentId: String): Double?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: ProgressEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressList(progressList: List<ProgressEntity>)
    
    @Update
    suspend fun updateProgress(progress: ProgressEntity)
    
    @Query("UPDATE student_progress SET syncedToServer = 1 WHERE id = :progressId")
    suspend fun markAsSynced(progressId: String)
    
    @Query("UPDATE student_progress SET syncedToServer = 1 WHERE id IN (:progressIds)")
    suspend fun markAsSynced(progressIds: List<String>)
    
    @Delete
    suspend fun deleteProgress(progress: ProgressEntity)
    
    @Query("DELETE FROM student_progress WHERE studentId = :studentId")
    suspend fun deleteProgressByStudent(studentId: String)
    
    @Query("DELETE FROM student_progress WHERE createdAt < :cutoffTime AND syncedToServer = 1")
    suspend fun deleteOldSyncedProgress(cutoffTime: Long)
    
    // Analytics queries
    @Query("""
        SELECT itemId, COUNT(*) as completionCount 
        FROM student_progress 
        WHERE itemType = :itemType 
        GROUP BY itemId 
        ORDER BY completionCount DESC
    """)
    suspend fun getMostCompletedItems(itemType: String): List<ItemCompletionCount>
    
    @Query("""
        SELECT DATE(completedAt/1000, 'unixepoch') as date, COUNT(*) as count
        FROM student_progress 
        WHERE studentId = :studentId 
        GROUP BY DATE(completedAt/1000, 'unixepoch')
        ORDER BY date DESC
        LIMIT 30
    """)
    suspend fun getDailyActivityCounts(studentId: String): List<DailyActivityCount>
}

data class ItemCompletionCount(
    val itemId: String,
    val completionCount: Int
)

data class DailyActivityCount(
    val date: String,
    val count: Int
)