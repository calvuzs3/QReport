package net.calvuz.qreport.backup.domain.model

import kotlinx.serialization.Serializable

/**
 * SignatureManifest - Manifest for signature files in backup
 *
 * Follows PhotoManifest pattern for consistency.
 * Used to track signatures included in signatures.zip
 */
@Serializable
data class SignatureManifest(
    val totalSignatures: Int,
    val totalSizeMB: Double,
    val signatures: List<SignatureBackupInfo>,
    val technicianCount: Int,
    val customerCount: Int
) {
    companion object {
        /**
         * Create an empty SignatureManifest
         */
        fun empty(): SignatureManifest {
            return SignatureManifest(
                totalSignatures = 0,
                totalSizeMB = 0.0,
                signatures = emptyList(),
                technicianCount = 0,
                customerCount = 0
            )
        }
    }
}