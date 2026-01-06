package net.calvuz.qreport.backup.domain.model.backup

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * FacilityBackup - Backup degli stabilimenti
 */
@Serializable
data class FacilityBackup(
    val id: String,
    val clientId: String,
    val name: String,
    val code: String?,
    val description: String?,
    val facilityType: String,
    val addressJson: String,
    val isPrimary: Boolean,
    val isActive: Boolean,
    @Contextual val createdAt: Instant,
    @Contextual val updatedAt: Instant
)