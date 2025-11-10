package net.calvuz.qreport.domain.model.file

import net.calvuz.qreport.domain.model.export.ExportResult

/**
 * Interface per gestione file e storage
 */
interface FileManager {
    fun getPhotosDirectory(): String
    fun getExportsDirectory(): String
    fun createPhotoFile(checkItemId: String): String
    fun deletePhotoFile(filePath: String): Boolean
    fun getFileSize(filePath: String): Long

    // ✅ NUOVO: Metodi per export file management
    fun openExportedFile(exportResult: ExportResult.Success): Result<Unit>
    fun shareExportedFile(exportResult: ExportResult.Success): Result<Unit>
    fun getAppVersion(): String
}