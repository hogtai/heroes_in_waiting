package com.lifechurch.heroesinwaiting.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lifechurch.heroesinwaiting.data.model.Facilitator

/**
 * Room entity for storing facilitator data locally
 */
@Entity(tableName = "facilitators")
data class FacilitatorEntity(
    @PrimaryKey
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val organization: String?,
    val role: String?,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String?,
    val emailVerified: Boolean,
    val lastLoginAt: String?,
    val classroomCount: Int,
    // Local-only fields
    val lastSyncAt: Long = System.currentTimeMillis(),
    val isOfflineMode: Boolean = false
) {
    
    /**
     * Converts entity to domain model
     */
    fun toDomainModel(): Facilitator {
        return Facilitator(
            id = id,
            email = email,
            firstName = firstName,
            lastName = lastName,
            organization = organization,
            role = role,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt,
            emailVerified = emailVerified,
            lastLoginAt = lastLoginAt,
            classroomCount = classroomCount
        )
    }
    
    companion object {
        /**
         * Creates entity from domain model
         */
        fun fromDomainModel(facilitator: Facilitator): FacilitatorEntity {
            return FacilitatorEntity(
                id = facilitator.id,
                email = facilitator.email,
                firstName = facilitator.firstName,
                lastName = facilitator.lastName,
                organization = facilitator.organization,
                role = facilitator.role,
                isActive = facilitator.isActive,
                createdAt = facilitator.createdAt,
                updatedAt = facilitator.updatedAt,
                emailVerified = facilitator.emailVerified,
                lastLoginAt = facilitator.lastLoginAt,
                classroomCount = facilitator.classroomCount
            )
        }
    }
}