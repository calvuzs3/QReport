package net.calvuz.qreport.app.file.domain.model

/**
 * DirectorySpec - Extensible directory specification system
 *
 * Replaces DirectoryType enum with a flexible, feature-extensible approach.
 * Features can add their own directories without modifying core code.
 */
@JvmInline
value class DirectorySpec(val name: String) {

    companion object Core {
        // Core application directories
        val PHOTOS = DirectorySpec("photos")
        val TEMP = DirectorySpec("temp")
        val CACHE = DirectorySpec("cache")
    }

    override fun toString(): String = name
}

/**
 * Example usage for feature-specific directories:
 *
 * object BackupDirectories {
 *     val DATABASE = DirectorySpec("backups")
 *     val DATABASE_BACKUPS = DirectorySpec("backups/database")
 *     val SETTINGS_BACKUPS = DirectorySpec("backups/settings")
 *     val PHOTO_ARCHIVES = DirectorySpec("backups/photos")
 * }
 *
 * object ExportDirectories {
 *     val EXPORTS = DirectorySpec("exports")
 *     val WORD_EXPORTS = DirectorySpec("exports/word")
 *     val PDF_EXPORTS = DirectorySpec("exports/pdf")
 *     val COMBINED_EXPORTS = DirectorySpec("exports/combined")
 * }
 *
 * object PhotoDirectories {
 *     val PHOTOS = DirectorySpec("photos")
 *     val THUMBNAILS = DirectorySpec("photos/thumbnails")
 *     ..val PROCESSED = DirectorySpec("photos/processed")
 * }
 */