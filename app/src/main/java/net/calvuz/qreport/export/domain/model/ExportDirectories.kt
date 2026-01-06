package net.calvuz.qreport.export.domain.model

import net.calvuz.qreport.app.file.domain.model.DirectorySpec

/**
 * ExportDirectories - Export feature directory specifications
 *
 * Example of how the export feature extends DirectorySpec
 * for organizing different export formats.
 */
object ExportDirectories {
    // Main
    val EXPORTS = DirectorySpec("exports")

    // Export format-specific directories
    val WORD_EXPORTS = DirectorySpec("word")
    val PDF_EXPORTS = DirectorySpec("pdf")
    val TEXT_EXPORTS = DirectorySpec("text")
    val COMBINED_EXPORTS = DirectorySpec("combined")

    // Temporary export operations
    val TEMP_WORD = DirectorySpec("temp/export_word")
    val TEMP_PROCESSING = DirectorySpec("temp/export_processing")

    // Archive organization
    val ARCHIVED_EXPORTS = DirectorySpec("exports/archive")
}