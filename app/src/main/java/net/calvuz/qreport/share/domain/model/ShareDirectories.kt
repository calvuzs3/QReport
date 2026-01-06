package net.calvuz.qreport.share.domain.model

import net.calvuz.qreport.app.file.domain.model.DirectorySpec

/**
 * ShareDirectories - Share feature directory specifications
 *
 * Defines all directories used by the Share feature for organizing
 * temporary files, archives, and shared content.
 */
object ShareDirectories {

    // Main shares directory
    val SHARES = DirectorySpec("shares")

    // Share-specific subdirectories
    val TEMP_SHARES = DirectorySpec("shares/temp")
    val ZIP_ARCHIVES = DirectorySpec("shares/archives")
    val PREPARED_EXPORTS = DirectorySpec("shares/exports")

    // Temporary processing
    val TEMP_ZIP_CREATION = DirectorySpec("temp/share_zip")
    val TEMP_FILE_PREPARATION = DirectorySpec("temp/share_prep")

    // Share method-specific directories
    val EMAIL_ATTACHMENTS = DirectorySpec("shares/email")
    val CLOUD_UPLOADS = DirectorySpec("shares/cloud")
    val DIRECT_SHARES = DirectorySpec("shares/direct")

    // Archive and cleanup
    val SHARED_HISTORY = DirectorySpec("shares/history")
}