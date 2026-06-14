package net.calvuz.qreport.backup.domain.model.backup

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * DocumentBackup - Backup of document metadata
 *
 * Mirrors DocumentEntity. The actual file referenced by [filePath] is archived
 * separately in documents.zip (see DocumentArchiveRepository).
 */
@Serializable
data class DocumentBackup(
    val id: String,
    val scope: String,
    val islandId: String?,
    val facilityId: String?,
    val clientId: String?,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val mimeType: String,
    val fileHash: String?,
    val title: String,
    val category: String,
    val notes: String?,
    @Contextual val createdAt: Instant,
    @Contextual val updatedAt: Instant,
    val isActive: Boolean = true,
    val isDeleted: Boolean = false,
    val syncedAt: Long? = null
)
