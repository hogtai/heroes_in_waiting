package com.lifechurch.heroesinwaiting.data.database.dao

import androidx.room.*
import com.lifechurch.heroesinwaiting.data.database.entity.FacilitatorEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Facilitator operations
 */
@Dao
interface FacilitatorDao {
    
    @Query("SELECT * FROM facilitators WHERE id = :id")
    suspend fun getFacilitatorById(id: String): FacilitatorEntity?
    
    @Query("SELECT * FROM facilitators WHERE email = :email")
    suspend fun getFacilitatorByEmail(email: String): FacilitatorEntity?
    
    @Query("SELECT * FROM facilitators WHERE id = :id")
    fun getFacilitatorByIdFlow(id: String): Flow<FacilitatorEntity?>
    
    @Query("SELECT * FROM facilitators WHERE isActive = 1")
    fun getActiveFacilitatorsFlow(): Flow<List<FacilitatorEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFacilitator(facilitator: FacilitatorEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFacilitators(facilitators: List<FacilitatorEntity>)
    
    @Update
    suspend fun updateFacilitator(facilitator: FacilitatorEntity)
    
    @Delete
    suspend fun deleteFacilitator(facilitator: FacilitatorEntity)
    
    @Query("DELETE FROM facilitators WHERE id = :id")
    suspend fun deleteFacilitatorById(id: String)
    
    @Query("DELETE FROM facilitators")
    suspend fun deleteAllFacilitators()
    
    @Query("UPDATE facilitators SET lastSyncAt = :timestamp WHERE id = :id")
    suspend fun updateLastSyncTime(id: String, timestamp: Long)
    
    @Query("SELECT * FROM facilitators WHERE lastSyncAt < :timestamp")
    suspend fun getFacilitatorsNeedingSync(timestamp: Long): List<FacilitatorEntity>
}