package com.lifechurch.heroesinwaiting.data.database.dao

import androidx.room.*
import com.lifechurch.heroesinwaiting.data.database.entity.StudentEntity
import com.lifechurch.heroesinwaiting.data.model.Grade
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Student operations
 * Handles COPPA-compliant student data with session-based anonymity
 */
@Dao
interface StudentDao {
    
    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun getStudentById(id: String): StudentEntity?
    
    @Query("SELECT * FROM students WHERE sessionId = :sessionId")
    suspend fun getStudentBySessionId(sessionId: String): StudentEntity?
    
    @Query("SELECT * FROM students WHERE sessionId = :sessionId")
    fun getStudentBySessionIdFlow(sessionId: String): Flow<StudentEntity?>
    
    @Query("SELECT * FROM students WHERE classroomId = :classroomId AND isActive = 1")
    suspend fun getStudentsByClassroom(classroomId: String): List<StudentEntity>
    
    @Query("SELECT * FROM students WHERE classroomId = :classroomId AND isActive = 1")
    fun getStudentsByClassroomFlow(classroomId: String): Flow<List<StudentEntity>>
    
    @Query("SELECT * FROM students WHERE grade = :grade AND isActive = 1")
    suspend fun getStudentsByGrade(grade: Grade): List<StudentEntity>
    
    @Query("SELECT * FROM students WHERE currentLessonId = :lessonId AND isActive = 1")
    suspend fun getStudentsInLesson(lessonId: String): List<StudentEntity>
    
    @Query("SELECT * FROM students WHERE isActive = 1 ORDER BY lastActiveAt DESC")
    suspend fun getAllActiveStudents(): List<StudentEntity>
    
    @Query("SELECT * FROM students WHERE isActive = 1 ORDER BY lastActiveAt DESC")
    fun getAllActiveStudentsFlow(): Flow<List<StudentEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<StudentEntity>)
    
    @Update
    suspend fun updateStudent(student: StudentEntity)
    
    @Query("UPDATE students SET currentLessonId = :lessonId WHERE sessionId = :sessionId")
    suspend fun updateCurrentLesson(sessionId: String, lessonId: String?)
    
    @Query("UPDATE students SET totalProgressPercentage = :progress WHERE sessionId = :sessionId")
    suspend fun updateProgress(sessionId: String, progress: Float)
    
    @Query("UPDATE students SET completedLessons = :completedLessons WHERE sessionId = :sessionId")
    suspend fun updateCompletedLessons(sessionId: String, completedLessons: String)
    
    @Query("UPDATE students SET lastActiveAt = :timestamp WHERE sessionId = :sessionId")
    suspend fun updateLastActiveTime(sessionId: String, timestamp: String)
    
    @Query("UPDATE students SET lastSyncAt = :timestamp WHERE sessionId = :sessionId")
    suspend fun updateLastSyncTime(sessionId: String, timestamp: Long)
    
    @Delete
    suspend fun deleteStudent(student: StudentEntity)
    
    @Query("DELETE FROM students WHERE id = :id")
    suspend fun deleteStudentById(id: String)
    
    @Query("DELETE FROM students WHERE sessionId = :sessionId")
    suspend fun deleteStudentBySessionId(sessionId: String)
    
    @Query("DELETE FROM students WHERE classroomId = :classroomId")
    suspend fun deleteStudentsByClassroom(classroomId: String)
    
    @Query("DELETE FROM students")
    suspend fun deleteAllStudents()
    
    @Query("SELECT * FROM students WHERE lastSyncAt < :timestamp")
    suspend fun getStudentsNeedingSync(timestamp: Long): List<StudentEntity>
    
    @Query("SELECT COUNT(*) FROM students WHERE classroomId = :classroomId AND isActive = 1")
    suspend fun getStudentCountByClassroom(classroomId: String): Int
    
    @Query("SELECT COUNT(*) FROM students WHERE grade = :grade AND isActive = 1")
    suspend fun getStudentCountByGrade(grade: Grade): Int
    
    @Query("SELECT * FROM students WHERE totalProgressPercentage >= :minProgress AND isActive = 1 ORDER BY totalProgressPercentage DESC")
    suspend fun getStudentsByMinProgress(minProgress: Float): List<StudentEntity>
    
    // Analytics queries (anonymized)
    @Query("SELECT COUNT(*) FROM students WHERE isActive = 1")
    suspend fun getTotalActiveStudentsCount(): Int
    
    @Query("SELECT AVG(totalProgressPercentage) FROM students WHERE isActive = 1")
    suspend fun getAverageProgressPercentage(): Float?
    
    @Query("SELECT grade, COUNT(*) as count FROM students WHERE isActive = 1 GROUP BY grade")
    suspend fun getStudentCountsByGrade(): Map<Grade, Int>
}