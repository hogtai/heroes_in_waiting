package com.lifechurch.heroesinwaiting.data.local.dao

import androidx.room.*
import com.lifechurch.heroesinwaiting.data.local.entities.ScenarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScenarioDao {
    
    @Query("SELECT * FROM scenarios WHERE isActive = 1")
    fun getAllScenarios(): Flow<List<ScenarioEntity>>
    
    @Query("SELECT * FROM scenarios WHERE id = :scenarioId")
    suspend fun getScenarioById(scenarioId: String): ScenarioEntity?
    
    @Query("SELECT * FROM scenarios WHERE id = :scenarioId")
    fun getScenarioByIdFlow(scenarioId: String): Flow<ScenarioEntity?>
    
    @Query("SELECT * FROM scenarios WHERE lessonId = :lessonId AND isActive = 1")
    fun getScenariosByLesson(lessonId: String): Flow<List<ScenarioEntity>>
    
    @Query("SELECT * FROM scenarios WHERE difficulty = :difficulty AND isActive = 1")
    fun getScenariosByDifficulty(difficulty: String): Flow<List<ScenarioEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenario(scenario: ScenarioEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenarios(scenarios: List<ScenarioEntity>)
    
    @Update
    suspend fun updateScenario(scenario: ScenarioEntity)
    
    @Delete
    suspend fun deleteScenario(scenario: ScenarioEntity)
    
    @Query("DELETE FROM scenarios WHERE id = :scenarioId")
    suspend fun deleteScenarioById(scenarioId: String)
    
    @Query("DELETE FROM scenarios")
    suspend fun deleteAllScenarios()
    
    @Query("SELECT COUNT(*) FROM scenarios WHERE isActive = 1")
    suspend fun getActiveScenarioCount(): Int
    
    @Query("SELECT * FROM scenarios WHERE cachedAt < :cutoffTime")
    suspend fun getStaleContent(cutoffTime: Long): List<ScenarioEntity>
    
    @Query("UPDATE scenarios SET lastUpdated = :timestamp WHERE id = :scenarioId")
    suspend fun updateLastUpdated(scenarioId: String, timestamp: Long = System.currentTimeMillis())
}