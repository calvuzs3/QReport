package net.calvuz.qreport.data.local.file

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import net.calvuz.qreport.domain.model.file.FileManager
import net.calvuz.qreport.domain.model.export.ExportFormat
import net.calvuz.qreport.domain.model.export.ExportResult
import timber.log.Timber
import java.io.File

/**
 * Implementazione FileManager
 */
class FileManagerImpl(
    private val context: Context
) : FileManager {

    private val photosDir = "photos"
    private val exportsDir = "exports"

    override fun getPhotosDirectory(): String {
        val dir = context.filesDir.resolve(photosDir)
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }

    override fun getExportsDirectory(): String {
        val dir = context.filesDir.resolve(exportsDir)
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }

    override fun createPhotoFile(checkItemId: String): String {
        val photosDir = getPhotosDirectory()
        val timestamp = System.currentTimeMillis()
        return "$photosDir/${checkItemId}_$timestamp.jpg"
    }

    override fun deletePhotoFile(filePath: String): Boolean {
        return try {
            File(filePath).delete()
        } catch (e: Exception) {
            false
        }
    }

    override fun getFileSize(filePath: String): Long {
        return try {
            File(filePath).length()
        } catch (e: Exception) {
            0L
        }
    }

    // ✅ NUOVO: Implementazione metodi export file management
    override fun openExportedFile(exportResult: ExportResult.Success): Result<Unit> {
        return try {
            val file = File(exportResult.filePath)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, getMimeType(exportResult.format))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(intent, "Apri con"))
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to open exported file")
            Result.failure(e)
        }
    }

    override fun shareExportedFile(exportResult: ExportResult.Success): Result<Unit> {
        return try {
            val file = File(exportResult.filePath)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = getMimeType(exportResult.format)
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Report QReport - ${exportResult.fileName}")
                putExtra(Intent.EXTRA_TEXT, "Report generato dall'app QReport")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(intent, "Condividi"))
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to share exported file")
            Result.failure(e)
        }
    }

    override fun getAppVersion(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.toString()
        } catch (e: Exception) {
            Timber.w(e, "Failed to get app version")
            "unknown"
        }
    }

    private fun getMimeType(format: ExportFormat): String {
        return when (format) {
            ExportFormat.WORD -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            ExportFormat.TEXT -> "text/plain"
            ExportFormat.PHOTO_FOLDER -> "application/zip"
            ExportFormat.COMBINED_PACKAGE -> "application/zip"
        }
    }
}