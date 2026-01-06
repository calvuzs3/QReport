package net.calvuz.qreport.backup.domain.model

import net.calvuz.qreport.app.file.domain.model.DirectorySpec


/**
 * BackupDirectories - Feature-specific directory specifications
 *
 * Example of how the backup feature extends DirectorySpec
 * without modifying core code.
 */
object BackupDirectories {
    // Main backup directory
    val BACKUPS = DirectorySpec("backups")

    // Backup specific directories
    val DATABASE_BACKUPS = DirectorySpec("backups/database")
    val SETTINGS_BACKUPS = DirectorySpec("backups/settings")
    val PHOTO_ARCHIVES = DirectorySpec("backups/photos")
    val FULL_BACKUPS = DirectorySpec("backups/complete")

    // Temporary backup operations
    val TEMP_DATABASE = DirectorySpec("temp/backup_db")
    val TEMP_ZIP = DirectorySpec("temp/backup_zip")
}