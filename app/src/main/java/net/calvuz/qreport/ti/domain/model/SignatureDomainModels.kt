package net.calvuz.qreport.ti.domain.model

import net.calvuz.qreport.app.file.domain.model.CoreFileInfo

/**
 * Information about a signature file with business context
 */
data class SignatureFileInfo(
    val coreFileInfo: CoreFileInfo,
    val signatureType: SignatureType,
    val interventionId: String,
    val timestamp: Long
) {
    val name: String get() = coreFileInfo.name
    val path: String get() = coreFileInfo.path
    val size: Long get() = coreFileInfo.size
    val lastModified: Long get() = coreFileInfo.lastModified
}

/**
 * Type of signature
 */
enum class SignatureType(val prefix: String, val displayName: String) {
    TECHNICIAN("tech_sig", "Firma Tecnico"),
    CUSTOMER("cust_sig", "Firma Cliente");

    companion object {
        fun fromFilename(filename: String): SignatureType? {
            return values().find { filename.startsWith(it.prefix) }
        }
    }
}

/**
 * Statistics for signature storage
 */
data class SignatureStorageStats(
    val totalFiles: Int = 0,
    val totalSizeMB: Double = 0.0,
    val technicianSignatures: Int = 0,
    val customerSignatures: Int = 0,
    val averageFileSizeKB: Double = 0.0,
    val oldestSignatureDate: Long? = null,
    val newestSignatureDate: Long? = null,
    val directoryPath: String = ""
)

/**
 * Signature file naming utility
 */
object SignatureFileNaming {

    private const val FILE_EXTENSION = ".png"

    /**
     * Generate filename for signature
     */
    fun generateFilename(
        type: SignatureType,
        interventionId: String,
        timestamp: Long = System.currentTimeMillis()
    ): String {
        return "${type.prefix}_${interventionId}_${timestamp}${FILE_EXTENSION}"
    }

    /**
     * Parse intervention ID from filename
     */
    fun parseInterventionId(filename: String): String? {
        return try {
            val parts = filename.removeSuffix(FILE_EXTENSION).split("_")
            if (parts.size >= 3) parts[2] else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse timestamp from filename
     */
    fun parseTimestamp(filename: String): Long? {
        return try {
            val parts = filename.removeSuffix(FILE_EXTENSION).split("_")
            if (parts.size >= 4) parts[3].toLongOrNull() else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Validate signature filename format
     */
    fun isValidSignatureFilename(filename: String): Boolean {
        if (!filename.endsWith(FILE_EXTENSION)) return false

        val type = SignatureType.fromFilename(filename) ?: return false
        val interventionId = parseInterventionId(filename) ?: return false
        val timestamp = parseTimestamp(filename) ?: return false

        return interventionId.isNotBlank() && timestamp > 0
    }
}