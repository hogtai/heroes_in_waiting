package com.lifechurch.heroesinwaiting.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "classrooms")
data class ClassroomEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val code: String,
    val gradeLevel: String,
    val schoolName: String,
    val facilitatorId: String,
    val facilitatorName: String,
    val studentCount: Int,
    val isActive: Boolean,
    val enrolledAt: Long? = null, // When student enrolled, null for facilitator classrooms
    val cachedAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
)