package com.lifechurch.heroesinwaiting.data.database.dao

import androidx.room.*
import com.lifechurch.heroesinwaiting.data.database.entity.ClassroomEntity
import com.lifechurch.heroesinwaiting.data.model.Grade
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Classroom operations
 */
@Dao
interface ClassroomDao {
    
    @Query("SELECT * FROM classrooms WHERE id = :id")
    suspend fun getClassroomById(id: String): ClassroomEntity?
    
    @Query("SELECT * FROM classrooms WHERE classroomCode = :code")
    suspend fun getClassroomByCode(code: String): ClassroomEntity?
    
    @Query("SELECT * FROM classrooms WHERE classroomCode = :code")
    fun getClassroomByCodeFlow(code: String): Flow<ClassroomEntity?>
    
    @Query("SELECT * FROM classrooms WHERE facilitatorId = :facilitatorId AND isActive = 1")
    suspend fun getClassroomsByFacilitator(facilitatorId: String): List<ClassroomEntity>
    
    @Query("SELECT * FROM classrooms WHERE facilitatorId = :facilitatorId AND isActive = 1")
    fun getClassroomsByFacilitatorFlow(facilitatorId: String): Flow<List<ClassroomEntity>>
    
    @Query("SELECT * FROM classrooms WHERE grade = :grade AND isActive = 1")
    suspend fun getClassroomsByGrade(grade: Grade): List<ClassroomEntity>
    
    @Query("SELECT * FROM classrooms WHERE isActive = 1 AND isPublic = 1")
    suspend fun getPublicClassrooms(): List<ClassroomEntity>
    
    @Query("SELECT * FROM classrooms WHERE isActive = 1 AND isPublic = 1")
    fun getPublicClassroomsFlow(): Flow<List<ClassroomEntity>>
    
    @Query("SELECT * FROM classrooms WHERE hasActiveSession = 1")
    suspend fun getActiveSessionClassrooms(): List<ClassroomEntity>
    
    @Query("SELECT * FROM classrooms WHERE hasActiveSession = 1")
    fun getActiveSessionClassroomsFlow(): Flow<List<ClassroomEntity>>
    
    @Query("SELECT * FROM classrooms WHERE currentLessonId = :lessonId")
    suspend fun getClassroomsWithLesson(lessonId: String): List<ClassroomEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassroom(classroom: ClassroomEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassrooms(classrooms: List<ClassroomEntity>)
    
    @Update
    suspend fun updateClassroom(classroom: ClassroomEntity)
    
    @Query("UPDATE classrooms SET currentStudentCount = :count WHERE id = :id")
    suspend fun updateStudentCount(id: String, count: Int)
    
    @Query("UPDATE classrooms SET hasActiveSession = :hasActiveSession, currentSessionId = :sessionId WHERE id = :id")
    suspend fun updateSessionStatus(id: String, hasActiveSession: Boolean, sessionId: String?)
    
    @Query("UPDATE classrooms SET currentLessonId = :lessonId WHERE id = :id")
    suspend fun updateCurrentLesson(id: String, lessonId: String?)
    
    @Query("UPDATE classrooms SET lastSyncAt = :timestamp WHERE id = :id")
    suspend fun updateLastSyncTime(id: String, timestamp: Long)
    
    @Delete
    suspend fun deleteClassroom(classroom: ClassroomEntity)
    
    @Query("DELETE FROM classrooms WHERE id = :id")
    suspend fun deleteClassroomById(id: String)
    
    @Query("DELETE FROM classrooms WHERE facilitatorId = :facilitatorId")
    suspend fun deleteClassroomsByFacilitator(facilitatorId: String)
    
    @Query("DELETE FROM classrooms")
    suspend fun deleteAllClassrooms()
    
    @Query("SELECT * FROM classrooms WHERE lastSyncAt < :timestamp")
    suspend fun getClassroomsNeedingSync(timestamp: Long): List<ClassroomEntity>
    
    @Query("SELECT COUNT(*) FROM classrooms WHERE facilitatorId = :facilitatorId AND isActive = 1")
    suspend fun getClassroomCountByFacilitator(facilitatorId: String): Int
    
    @Query("SELECT COUNT(*) FROM classrooms WHERE isActive = 1")
    suspend fun getTotalActiveClassroomsCount(): Int
    
    @Query("SELECT SUM(currentStudentCount) FROM classrooms WHERE isActive = 1")
    suspend fun getTotalActiveStudentsCount(): Int
    
    @Query("SELECT * FROM classrooms WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' AND isActive = 1")
    suspend fun searchClassrooms(query: String): List<ClassroomEntity>
}