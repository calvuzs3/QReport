package net.calvuz.qreport.backup.domain.model.backup

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * ContactBackup - Backup dei contatti
 */
@Serializable
data class ContactBackup(
    val id: String,
    val clientId: String,
    val firstName: String,
    val lastName: String?,
    val title: String?,
    val role: String?,
    val department: String?,
    val phone: String?,
    val mobilePhone: String?,
    val email: String?,
    val alternativeEmail: String?,
    val isPrimary: Boolean,
    val isActive: Boolean,
    val preferredContactMethod: String?,
    val notes: String?,
    @Contextual val createdAt: Instant,
    @Contextual val updatedAt: Instant
)