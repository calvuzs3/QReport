package net.calvuz.qreport.backup.domain.model

import kotlinx.serialization.Serializable

/**
 * SignatureBackupInfo - Information for single signature backup
 *
 * Follows PhotoBackupInfo pattern for consistency
 */
@Serializable
data class SignatureBackupInfo(
    val interventionId: String,
    val signatureType: String,      // "TECHNICIAN" or "CUSTOMER"
    val fileName: String,
    val relativePath: String,
    val sizeBytes: Long,
    val sha256Hash: String
)