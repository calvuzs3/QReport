package net.calvuz.qreport.app.file.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import net.calvuz.qreport.app.file.data.extension.matches
import net.calvuz.qreport.app.file.domain.model.CoreFileInfo
import net.calvuz.qreport.app.file.domain.model.FileFilter
import net.calvuz.qreport.app.file.domain.repository.*
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.file.domain.model.DirectorySpec
import net.calvuz.qreport.app.result.domain.QrResult.*
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Core file repository implementation.
 *
 * Contains ONLY:
 * - Generic file system operations
 * - Base directory management
 * - File CRUD operations
 *
 * Does NOT contain:
 * - Backup business logic
 * - Export business logic
 * - Feature-specific logic
 */
@Singleton
class CoreFileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : CoreFileRepository {

    private val fileProviderAuthority = "${context.packageName}.fileprovider"

    // ===== DIRECTORY MANAGEMENT =====

    override suspend fun getOrCreateDirectory(spec: DirectorySpec): QrResult<String, QrError.FileError> {
        return try {
            val dir = when (spec) {
                DirectorySpec.PHOTOS -> context.filesDir.resolve("photos")
                DirectorySpec.TEMP -> context.filesDir.resolve("temp")
                DirectorySpec.CACHE -> context.cacheDir
                else -> {
                    // Custom DirectorySpec: build path from name.
                    // Names starting with "cache/" resolve under cacheDir.
                    val dirPath = spec.name
                    if (dirPath.startsWith("cache/")) {
                        context.cacheDir.resolve(dirPath.removePrefix("cache/"))
                    } else {
                        context.filesDir.resolve(dirPath)
                    }
                }
            }

            if (!dir.exists()) {
                val created = dir.mkdirs()
                if (!created) {
                    Timber.e("Failed to create directory: ${dir.absolutePath}")
                    return Error(QrError.FileError.DirectoryCreateError(dir.absolutePath))
                }
            }

            Timber.d("Directory ready: ${dir.absolutePath}")
            Success(dir.absolutePath)

        } catch (e: Exception) {
            Timber.e(e, "DirectoryCreateError: $spec")
            Error(QrError.FileError.DirectoryCreateError(spec.name))
        }
    }

    override suspend fun createSubDirectory(
        spec: DirectorySpec,
        subDirName: String
    ): QrResult<String, QrError.FileError> {
        return try {
            when (val baseResult = getOrCreateDirectory(spec)) {
                is Error -> baseResult
                is Success -> {
                    val subDir = File(baseResult.data, subDirName)

                    if (!subDir.exists()) {
                        val created = subDir.mkdirs()
                        if (!created) {
                            Timber.e("Failed to create subdirectory: ${subDir.absolutePath}")
                            return Error(QrError.FileError.DirectoryCreateError(subDir.absolutePath))
                        }
                    }

                    Timber.d("Subdirectory ready: ${subDir.absolutePath}")
                    Success(subDir.absolutePath)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "DirectoryCreateError: $subDirName")
            Error(QrError.FileError.DirectoryCreateError(subDirName))
        }
    }

    // ===== FILE OPERATIONS =====

    override suspend fun copyFile(
        sourcePath: String,
        destinationPath: String
    ): QrResult<Unit, QrError.FileError> {
        return try {
            val sourceFile = File(sourcePath)
            val destFile = File(destinationPath)

            if (!sourceFile.exists()) {
                Timber.e("Source file not found: $sourcePath")
                return Error(QrError.FileError.FileNotFound(sourcePath))
            }

            // Create destination directory if needed
            destFile.parentFile?.let { if (!it.exists()) it.mkdirs() }

            sourceFile.copyTo(destFile, overwrite = true)
            Timber.d("File copied: $sourcePath → $destinationPath")
            Success(Unit)

        } catch (e: IOException) {
            Timber.e(e, "FileCopyError: $sourcePath → $destinationPath")
            Error(QrError.FileError.FileCopyError(sourcePath, destinationPath))
        } catch (e: Exception) {
            Timber.e(e, "FileCopyError: $sourcePath → $destinationPath")
            Error(QrError.FileError.FileCopyError(sourcePath, destinationPath))
        }
    }

    override suspend fun moveFile(
        sourcePath: String,
        destinationPath: String
    ): QrResult<Unit, QrError.FileError> {
        return try {
            when (val copyResult = copyFile(sourcePath, destinationPath)) {
                is Error -> copyResult
                is Success -> deleteFile(sourcePath)
            }

        } catch (e: Exception) {
            Timber.e(e, "FileMoveError: $sourcePath → $destinationPath")
            Error(QrError.FileError.FileMoveError(sourcePath, destinationPath))
        }
    }

    override suspend fun deleteFile(filePath: String): QrResult<Unit, QrError.FileError> {
        return try {
            val file = File(filePath)

            if (!file.exists()) {
                Timber.w("File to delete not found (already deleted): $filePath")
                return Success(Unit) // Idempotent — not an error
            }

            val deleted = file.delete()
            if (!deleted) {
                Timber.e("Failed to delete file: $filePath")
                return Error(QrError.FileError.FileDeleteError(filePath))
            }

            Timber.d("File deleted: $filePath")
            Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "FileDeleteError: $filePath")
            Error(QrError.FileError.FileDeleteError(filePath))
        }
    }

    override suspend fun deleteDirectory(dirPath: String): QrResult<Unit, QrError.FileError> {
        return try {
            val dir = File(dirPath)

            if (!dir.exists()) {
                Timber.w("Directory to delete not found (already deleted): $dirPath")
                return Success(Unit) // Idempotent — not an error
            }

            val deleted = dir.deleteRecursively()
            if (!deleted) {
                Timber.e("Failed to delete directory: $dirPath")
                return Error(QrError.FileError.DirectoryDeleteError(dirPath))
            }

            Timber.d("Directory deleted: $dirPath")
            Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "DirectoryDeleteError: $dirPath")
            Error(QrError.FileError.DirectoryDeleteError(dirPath))
        }
    }

    override suspend fun fileExists(filePath: String): Boolean {
        return try {
            File(filePath).exists()
        } catch (e: Exception) {
            // Never propagate — callers depend on a safe boolean return
            Timber.w(e, "FileAccessError: $filePath")
            false
        }
    }

    override suspend fun getFileSize(filePath: String): QrResult<Long, QrError.FileError> {
        return try {
            val file = File(filePath)

            if (!file.exists()) {
                return Error(QrError.FileError.FileNotFound(filePath))
            }

            Success(file.length())

        } catch (e: Exception) {
            Timber.e(e, "IoError: $filePath")
            Error(QrError.FileError.IoError(e))
        }
    }

    override suspend fun listFiles(
        dirPath: String,
        filter: FileFilter?
    ): QrResult<List<CoreFileInfo>, QrError.FileError> {
        return try {
            val dir = File(dirPath)

            if (!dir.exists() || !dir.isDirectory) {
                Timber.e("Directory not found: $dirPath")
                return Error(QrError.FileError.DirectoryNotFound(dirPath))
            }

            val files = dir.listFiles()?.mapNotNull { file ->
                try {
                    val fileInfo = CoreFileInfo(
                        name = file.name,
                        path = file.absolutePath,
                        size = file.length(),
                        lastModified = file.lastModified(),
                        isDirectory = file.isDirectory,
                        extension = file.extension.takeIf { it.isNotEmpty() }
                    )
                    // Apply filter — null filter means include all
                    if (filter?.matches(fileInfo) != false) fileInfo else null

                } catch (e: Exception) {
                    Timber.w(e, "IoError on entry: ${file.name}")
                    null // Skip bad entries; do not fail the whole list
                }
            } ?: emptyList()

            Success(files)

        } catch (e: Exception) {
            Timber.e(e, "FileReadError: $dirPath")
            Error(QrError.FileError.FileReadError(dirPath))
        }
    }

    // ===== CLEANUP OPERATIONS =====

    override suspend fun cleanupOldFiles(
        dirPath: String,
        olderThanDays: Int
    ): QrResult<Int, QrError.FileError> {
        return try {
            val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
            var deletedCount = 0

            when (val listResult = listFiles(dirPath)) {
                is Error -> Error(listResult.error)
                is Success -> {
                    for (fileInfo in listResult.data) {
                        if (!fileInfo.isDirectory && fileInfo.lastModified < cutoffTime) {
                            when (deleteFile(fileInfo.path)) {
                                is Success -> deletedCount++
                                is Error -> Timber.w("FileDeleteError: ${fileInfo.name}")
                            }
                        }
                    }

                    Timber.d("Cleaned up $deletedCount old files from: $dirPath")
                    Success(deletedCount)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "CleanupFailed: $dirPath")
            Error(QrError.FileError.CleanupFailed)
        }
    }

    override suspend fun getDirectorySize(dirPath: String): QrResult<Long, QrError.FileError> {
        return try {
            val dir = File(dirPath)

            if (!dir.exists() || !dir.isDirectory) {
                return Error(QrError.FileError.DirectoryNotFound(dirPath))
            }

            val totalSize = dir.walkTopDown()
                .filter { it.isFile }
                .sumOf { it.length() }

            Success(totalSize)

        } catch (e: Exception) {
            Timber.e(e, "IoError: $dirPath")
            Error(QrError.FileError.IoError(e))
        }
    }

    // ===== FILEPROVIDER SUPPORT =====

    override suspend fun getFileProviderAuthority(): String = fileProviderAuthority

    override suspend fun createFileProviderUri(filePath: String): QrResult<Uri, QrError.FileError> {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                Timber.e("File not found for FileProvider URI: $filePath")
                return Error(QrError.FileError.FileNotFound(filePath))
            }

            val uri = FileProvider.getUriForFile(context, fileProviderAuthority, file)
            Timber.d("Created FileProvider URI: $uri for file: $filePath")
            Success(uri)

        } catch (e: Exception) {
            Timber.e(e, "Failed to create FileProvider URI for: $filePath")
            Error(QrError.FileError.FileAccessError(filePath))
        }
    }

    override suspend fun isFileProviderConfigured(): Boolean {
        return try {
            when (val tempDir = getOrCreateDirectory(DirectorySpec.TEMP)) {
                is Error -> false
                is Success -> {
                    val testFile = File(tempDir.data, "test_fileprovider.tmp")
                    testFile.createNewFile()
                    val uriResult = createFileProviderUri(testFile.absolutePath)
                    testFile.delete()

                    when (uriResult) {
                        is Success -> {
                            Timber.d("FileProvider is properly configured")
                            true
                        }
                        is Error -> {
                            Timber.w("FileProvider not properly configured")
                            false
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking FileProvider configuration")
            false
        }
    }
}