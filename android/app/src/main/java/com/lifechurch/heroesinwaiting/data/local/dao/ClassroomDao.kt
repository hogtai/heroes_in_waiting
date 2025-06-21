package com.lifechurch.heroesinwaiting.data.local.dao

import androidx.room.*
import com.lifechurch.heroesinwaiting.data.local.entities.ClassroomEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassroomDao {
    
    @Query("SELECT * FROM classrooms WHERE isActive = 1")
    fun getAllClassrooms(): Flow<List<ClassroomEntity>>
    
    @Query("SELECT * FROM classrooms WHERE id = :classroomId")
    suspend fun getClassroomById(classroomId: String): ClassroomEntity?
    
    @Query("SELECT * FROM classrooms WHERE id = :classroomId")
    fun getClassroomByIdFlow(classroomId: String): Flow<ClassroomEntity?>
    
    @Query("SELECT * FROM classrooms WHERE code = :classroomCode")
    suspend fun getClassroomByCode(classroomCode: String): ClassroomEntity?
    
    @Query("SELECT * FROM classrooms WHERE facilitatorId = :facilitatorId AND isActive = 1")
    fun getClassroomsByFacilitator(facilitatorId: String): Flow<List<ClassroomEntity>>
    
    @Query("SELECT * FROM classrooms WHERE enrolledAt IS NOT NULL")
    fun getEnrolledClassrooms(): Flow<List<ClassroomEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassroom(classroom: ClassroomEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassrooms(classrooms: List<ClassroomEntity>)
    
    @Update
    suspend fun updateClassroom(classroom: ClassroomEntity)
    
    @Delete
    suspend fun deleteClassroom(classroom: ClassroomEntity)
    
    @Query("DELETE FROM classrooms WHERE id = :classroomId")
    suspend fun deleteClassroomById(classroomId: String)
    
    @Query("DELETE FROM classrooms")
    suspend fun deleteAllClassrooms()
    
    @Query("SELECT COUNT(*) FROM classrooms WHERE isActive = 1")
    suspend fun getActiveClassroomCount(): Int
    
    @Query("UPDATE classrooms SET enrolledAt = :enrolledAt WHERE id = :classroomId")
    suspend fun markAsEnrolled(classroomId: String, enrolledAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE classrooms SET lastUpdated = :timestamp WHERE id = :classroomId")
    suspend fun updateLastUpdated(classroomId: String, timestamp: Long = System.currentTimeMillis())
}