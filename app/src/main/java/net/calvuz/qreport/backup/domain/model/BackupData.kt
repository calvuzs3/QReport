package net.calvuz.qreport.backup.domain.model

import kotlinx.serialization.Serializable
import net.calvuz.qreport.backup.domain.model.backup.DatabaseBackup
import net.calvuz.qreport.backup.domain.model.backup.SettingsBackup

/**
 * BackupData - Container principale per tutto il backup
 */
@Serializable
data class BackupData(
    val metadata: BackupMetadata,
    val database: DatabaseBackup,
    val settings: SettingsBackup,
    val photoManifest: PhotoManifest,
    val signatureManifest: SignatureManifest
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

    /**
     * Check if backup includes signatures
     */
    fun includesSignatures(): Boolean {
        return signatureManifest.totalSignatures > 0
    }
}