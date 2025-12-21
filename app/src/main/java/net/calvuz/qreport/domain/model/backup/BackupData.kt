package net.calvuz.qreport.domain.model.backup

import kotlinx.serialization.Serializable

/**
 * BackupData - Container principale per tutto il backup
 */
@Serializable
data class BackupData(
    val metadata: BackupMetadata,
    val database: DatabaseBackup,
    val settings: SettingsBackup,
    val photoManifest: PhotoManifest
) {
    /**
     * Calcola dimensione totale stimata in bytes
     */
    fun getTotalEstimatedSize(): Long {
        return metadata.totalSize
    }

    /**
     * Verifica se il backup include foto
     */
    fun includesPhotos(): Boolean {
        return photoManifest.totalPhotos > 0
    }
}