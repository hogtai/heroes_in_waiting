package com.lifechurch.heroesinwaiting.data.local.dao

import androidx.room.*
import com.lifechurch.heroesinwaiting.data.local.entities.EmotionalCheckinEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmotionalCheckinDao {
    
    @Query("SELECT * FROM emotional_checkins WHERE studentId = :studentId ORDER BY timestamp DESC")
    fun getCheckinsByStudent(studentId: String): Flow<List<EmotionalCheckinEntity>>
    
    @Query("SELECT * FROM emotional_checkins WHERE studentId = :studentId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestCheckin(studentId: String): EmotionalCheckinEntity?
    
    @Query("SELECT * FROM emotional_checkins WHERE syncedToServer = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedCheckins(): List<EmotionalCheckinEntity>
    
    @Query("SELECT * FROM emotional_checkins WHERE studentId = :studentId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getCheckinsInRange(studentId: String, startTime: Long, endTime: Long): Flow<List<EmotionalCheckinEntity>>
    
    @Query("""
        SELECT emotion, COUNT(*) as count 
        FROM emotional_checkins 
        WHERE studentId = :studentId 
        GROUP BY emotion 
        ORDER BY count DESC
    """)
    suspend fun getEmotionFrequency(studentId: String): List<EmotionFrequency>
    
    @Query("""
        SELECT AVG(intensity) as averageIntensity
        FROM emotional_checkins 
        WHERE studentId = :studentId AND emotion = :emotion
    """)
    suspend fun getAverageIntensityForEmotion(studentId: String, emotion: String): Double?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckin(checkin: EmotionalCheckinEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckins(checkins: List<EmotionalCheckinEntity>)
    
    @Update
    suspend fun updateCheckin(checkin: EmotionalCheckinEntity)
    
    @Query("UPDATE emotional_checkins SET syncedToServer = 1 WHERE id = :checkinId")
    suspend fun markAsSynced(checkinId: String)
    
    @Query("UPDATE emotional_checkins SET syncedToServer = 1 WHERE id IN (:checkinIds)")
    suspend fun markAsSynced(checkinIds: List<String>)
    
    @Delete
    suspend fun deleteCheckin(checkin: EmotionalCheckinEntity)
    
    @Query("DELETE FROM emotional_checkins WHERE studentId = :studentId")
    suspend fun deleteCheckinsByStudent(studentId: String)
    
    @Query("DELETE FROM emotional_checkins WHERE timestamp < :cutoffTime AND syncedToServer = 1")
    suspend fun deleteOldSyncedCheckins(cutoffTime: Long)
    
    @Query("SELECT COUNT(*) FROM emotional_checkins WHERE studentId = :studentId")
    suspend fun getCheckinCount(studentId: String): Int
}

data class EmotionFrequency(
    val emotion: String,
    val count: Int
)