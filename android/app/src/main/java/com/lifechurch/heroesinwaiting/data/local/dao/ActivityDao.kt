package com.lifechurch.heroesinwaiting.data.local.dao

import androidx.room.*
import com.lifechurch.heroesinwaiting.data.local.entities.ActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    
    @Query("SELECT * FROM activities WHERE isActive = 1")
    fun getAllActivities(): Flow<List<ActivityEntity>>
    
    @Query("SELECT * FROM activities WHERE id = :activityId")
    suspend fun getActivityById(activityId: String): ActivityEntity?
    
    @Query("SELECT * FROM activities WHERE id = :activityId")
    fun getActivityByIdFlow(activityId: String): Flow<ActivityEntity?>
    
    @Query("SELECT * FROM activities WHERE lessonId = :lessonId AND isActive = 1")
    fun getActivitiesByLesson(lessonId: String): Flow<List<ActivityEntity>>
    
    @Query("SELECT * FROM activities WHERE type = :type AND isActive = 1")
    fun getActivitiesByType(type: String): Flow<List<ActivityEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<ActivityEntity>)
    
    @Update
    suspend fun updateActivity(activity: ActivityEntity)
    
    @Delete
    suspend fun deleteActivity(activity: ActivityEntity)
    
    @Query("DELETE FROM activities WHERE id = :activityId")
    suspend fun deleteActivityById(activityId: String)
    
    @Query("DELETE FROM activities")
    suspend fun deleteAllActivities()
    
    @Query("SELECT COUNT(*) FROM activities WHERE isActive = 1")
    suspend fun getActiveActivityCount(): Int
    
    @Query("SELECT * FROM activities WHERE cachedAt < :cutoffTime")
    suspend fun getStaleContent(cutoffTime: Long): List<ActivityEntity>
    
    @Query("UPDATE activities SET lastUpdated = :timestamp WHERE id = :activityId")
    suspend fun updateLastUpdated(activityId: String, timestamp: Long = System.currentTimeMillis())
}