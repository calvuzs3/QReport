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
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Core file repository implementation
 *
 * CONTIENE SOLO:
 * - File system operations generiche
 * - Directory management base
 * - Operazioni CRUD su file
 *
 * NON CONTIENE:
 * - Business logic di Backup
 * - Business logic di Export
 * - Logiche specifiche di feature
 */
@Singleton
class CoreFileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : CoreFileRepository {

    /** Definitions */
    private val fileProviderAuthority = "${context.packageName}.fileprovider"

    // ===== DIRECTORY MANAGEMENT =====

    override suspend fun getOrCreateDirectory(spec: DirectorySpec): QrResult<String, QrError.FileError> {
        return try {
            val dir = when (spec) {
                DirectorySpec.Core.PHOTOS -> context.filesDir.resolve("photos")
                DirectorySpec.Core.TEMP -> context.filesDir.resolve("temp")
                DirectorySpec.Core.CACHE -> context.cacheDir
                else -> {
                    // Handle custom DirectorySpec - create path from name
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
                    return QrResult.Error(QrError.FileError.DIRECTORY_CREATE)
                }
            }

            Timber.d("Directory ready: ${dir.absolutePath}")
            QrResult.Success(dir.absolutePath)

        } catch (e: Exception) {
            Timber.e(e, "${QrError.FileError.DIRECTORY_CREATE}: $spec")
            QrResult.Error(QrError.FileError.DIRECTORY_CREATE)
        }
    }

    override suspend fun createSubDirectory(
        baseDirectorySpec: DirectorySpec,
        subDirName: String
    ): QrResult<String, QrError.FileError> {
        return try {
            val baseResult = getOrCreateDirectory(baseDirectorySpec)

            when (baseResult) {
                is QrResult.Error -> baseResult
                is QrResult.Success -> {
                    val subDir = File(baseResult.data, subDirName)

                    if (!subDir.exists()) {
                        val created = subDir.mkdirs()
                        if (!created) {
                            Timber.e("Failed to create subdirectory: ${subDir.absolutePath}")
                            return QrResult.Error(QrError.FileError.DIRECTORY_CREATE)
                        }
                    }

                    Timber.d("Subdirectory ready: ${subDir.absolutePath}")
                    QrResult.Success(subDir.absolutePath)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "${QrError.FileError.DIRECTORY_CREATE}: $subDirName")
            QrResult.Error(QrError.FileError.DIRECTORY_CREATE)
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
                return QrResult.Error(QrError.FileError.FILE_NOT_FOUND)
            }

            // Create destination directory if needed
            destFile.parentFile?.let { parentDir ->
                if (!parentDir.exists()) {
                    parentDir.mkdirs()
                }
            }

            sourceFile.copyTo(destFile, overwrite = true)
            Timber.d("File copied: $sourcePath → $destinationPath")

            QrResult.Success(Unit)

        } catch (e: IOException) {
            Timber.e(e, "${QrError.FileError.FILE_READ}: $sourcePath")
            QrResult.Error(QrError.FileError.FILE_READ)
        } catch (e: Exception) {
            Timber.e(e, "${QrError.FileError.FILE_READ}: $sourcePath → $destinationPath")
            QrResult.Error(QrError.FileError.FILE_COPY)
        }
    }

    override suspend fun moveFile(
        sourcePath: String,
        destinationPath: String
    ): QrResult<Unit, QrError.FileError> {
        return try {
            // Copy first
            val copyResult = copyFile(sourcePath, destinationPath)

            when (copyResult) {
                is QrResult.Error -> copyResult
                is QrResult.Success -> {
                    // Delete source after successful copy
                    deleteFile(sourcePath)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "${QrError.FileError.FILE_MOVE}: $sourcePath → $destinationPath")
            QrResult.Error(QrError.FileError.FILE_MOVE)
        }
    }

    override suspend fun deleteFile(filePath: String): QrResult<Unit, QrError.FileError> {
        return try {
            val file = File(filePath)

            if (!file.exists()) {
                Timber.w("File to delete not found: $filePath")
                return QrResult.Success(Unit) // Not an error - already deleted
            }

            val deleted = file.delete()
            if (!deleted) {
                Timber.e("Failed to delete file: $filePath")
                return QrResult.Error(QrError.FileError.FILE_DELETE)
            }

            Timber.d("File deleted: $filePath")
            QrResult.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "${QrError.FileError.FILE_DELETE}: $filePath")
            QrResult.Error(QrError.FileError.FILE_DELETE)
        }
    }

    override suspend fun deleteDirectory(dirPath: String): QrResult<Unit, QrError.FileError> {
        return try {
            val dir = File(dirPath)

            if (!dir.exists()) {
                Timber.w("Directory to delete not found: $dirPath")
                return QrResult.Success(Unit) // Not an error - already deleted
            }

            val deleted = dir.deleteRecursively()
            if (!deleted) {
                Timber.e("Failed to delete directory: $dirPath")
                return QrResult.Error(QrError.FileError.FILE_DELETE)
            }

            Timber.d("Directory deleted: $dirPath")
            QrResult.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "${QrError.FileError.FILE_DELETE}: $dirPath")
            QrResult.Error(QrError.FileError.FILE_DELETE)
        }
    }

    override suspend fun fileExists(filePath: String): Boolean {
        return try {
            File(filePath).exists()
        } catch (e: Exception) {
            Timber.w(e, "${QrError.File.FILE_NOT_EXISTS}: $filePath")
            false
        }
    }

    override suspend fun getFileSize(filePath: String): QrResult<Long, QrError.FileError> {
        return try {
            val file = File(filePath)

            if (!file.exists()) {
                return QrResult.Error(QrError.FileError.FILE_NOT_FOUND)
            }

            QrResult.Success(file.length())

        } catch (e: Exception) {
            Timber.e(e, "${QrError.FileError.IO_ERROR}: $filePath")
            QrResult.Error(QrError.FileError.IO_ERROR)
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
                return QrResult.Error(QrError.FileError.FILE_NOT_FOUND)
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

                    // Apply filter if provided
                    if (filter?.matches(fileInfo) != false) {
                        fileInfo
                    } else {
                        null
                    }

                } catch (e: Exception) {
                    Timber.w(e, "${QrError.File.PROCESSING}: ${file.name}")
                    null
                }
            } ?: emptyList()

            QrResult.Success(files)

        } catch (e: Exception) {
            Timber.e(e, "${QrError.FileError.FILE_READ}: $dirPath")
            QrResult.Error(QrError.FileError.FILE_READ)
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

            val listResult = listFiles(dirPath)
            when (listResult) {
                is QrResult.Error -> QrResult.Error(listResult.error)
                is QrResult.Success -> {
                    for (fileInfo in listResult.data) {
                        if (fileInfo.lastModified < cutoffTime) {
                            when (deleteFile(fileInfo.path)) {
                                is QrResult.Success -> deletedCount++
                                is QrResult.Error -> Timber.w("${QrError.FileError.FILE_DELETE}: ${fileInfo.name}")
                            }
                        }
                    }

                    Timber.d("Cleaned up $deletedCount old files from: $dirPath")
                    QrResult.Success(deletedCount)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "${QrError.FileError.FILE_DELETE}: $dirPath")
            QrResult.Error(QrError.FileError.FILE_DELETE)
        }
    }

    override suspend fun getDirectorySize(dirPath: String): QrResult<Long, QrError.FileError> {
        return try {
            val dir = File(dirPath)

            if (!dir.exists() || !dir.isDirectory) {
                return QrResult.Error(QrError.FileError.FILE_NOT_FOUND)
            }

            val totalSize = dir.walkTopDown()
                .filter { it.isFile }
                .map { it.length() }
                .sum()

            QrResult.Success(totalSize)

        } catch (e: Exception) {
            Timber.e(e, "${QrError.FileError.IO_ERROR}: $dirPath")
            QrResult.Error(QrError.FileError.IO_ERROR)
        }
    }

    // ===== FILEPROVIDER SUPPORT =====

    override suspend fun getFileProviderAuthority(): String {
        return fileProviderAuthority
    }

    override suspend fun createFileProviderUri(filePath: String): QrResult<Uri, QrError.FileError> {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                Timber.e("File not found for FileProvider URI: $filePath")
                return QrResult.Error(QrError.FileError.FILE_NOT_FOUND)
            }

            val uri = FileProvider.getUriForFile(context, getFileProviderAuthority(), file)
            Timber.d("Created FileProvider URI: $uri for file: $filePath")
            QrResult.Success(uri)

        } catch (e: Exception) {
            Timber.e(e, "Failed to create FileProvider URI for: $filePath")
            QrResult.Error(QrError.FileError.FILE_READ)
        }
    }

    override suspend fun isFileProviderConfigured(): Boolean {
        return try {
            // Test if FileProvider is properly configured by creating a temporary file
            val tempDir = getOrCreateDirectory(DirectorySpec.Core.TEMP)
            when (tempDir) {
                is QrResult.Error -> false
                is QrResult.Success -> {
                    val testFile = File(tempDir.data, "test_fileprovider.tmp")
                    testFile.createNewFile()

                    val uriResult = createFileProviderUri(testFile.absolutePath)
                    testFile.delete()

                    when (uriResult) {
                        is QrResult.Success -> {
                            Timber.d("FileProvider is properly configured")
                            true
                        }
                        is QrResult.Error -> {
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