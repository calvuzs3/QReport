package net.calvuz.qreport.domain.model.backup

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * ClientBackup - Backup dei clienti
 */
@Serializable
data class ClientBackup(
    val id: String,
    val companyName: String,
    val vatNumber: String?,
    val fiscalCode: String?,
    val website: String?,
    val industry: String?,
    val notes: String?,
    val headquartersJson: String?,
    val isActive: Boolean,
    @Contextual val createdAt: Instant,
    @Contextual val updatedAt: Instant
)