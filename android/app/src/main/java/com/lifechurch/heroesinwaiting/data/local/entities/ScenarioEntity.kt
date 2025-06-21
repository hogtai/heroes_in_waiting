package com.lifechurch.heroesinwaiting.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.lifechurch.heroesinwaiting.data.local.converters.StringListConverter

@Entity(tableName = "scenarios")
@TypeConverters(StringListConverter::class)
data class ScenarioEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val situation: String,
    val characters: List<String>,
    val lessonId: String?,
    val difficulty: String, // "easy", "medium", "hard"
    val fullSituation: String? = null,
    val discussionQuestions: List<String> = emptyList(),
    val learningObjectives: List<String> = emptyList(),
    val isActive: Boolean,
    val cachedAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
)