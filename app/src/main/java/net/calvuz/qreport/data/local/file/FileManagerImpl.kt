package net.calvuz.qreport.data.local.file

import android.content.Context
import net.calvuz.qreport.domain.model.file.FileManager
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
}