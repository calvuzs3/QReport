package net.calvuz.qreport.backup.domain.usecase

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import net.calvuz.qreport.backup.domain.repository.BackupRepository
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

/**
 * Use case for exporting a backup to a user-selected location
 *
 * Creates a single ZIP file containing all backup contents and writes it
 * to the Uri provided by the Storage Access Framework (SAF)
 */
class ExportBackupUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupRepository: BackupRepository
) {
    companion object {
        private const val BUFFER_SIZE = 8192
    }

    /**
     * Export backup to the specified Uri
     *
     * @param backupId ID of the backup to export
     * @param destinationUri Uri selected by user via SAF
     * @return Flow emitting export progress
     */
    suspend operator fun invoke(
        backupId: String,
        destinationUri: Uri
    ): Flow<ExportProgress> = flow {

        emit(ExportProgress.InProgress("Preparazione export...", 0f))

        try {
            // 1. Find backup directory
            val backups = backupRepository.getAvailableBackups()
            val backup = backups.find { it.id == backupId }

            if (backup == null) {
                emit(ExportProgress.Error("Backup non trovato"))
                return@flow
            }

            val backupDir = File(backup.dirPath)
            if (!backupDir.exists() || !backupDir.isDirectory) {
                emit(ExportProgress.Error("Directory backup non trovata"))
                return@flow
            }

            // 2. Collect all files to zip
            emit(ExportProgress.InProgress("Analisi file...", 0.1f))
            val filesToZip = collectFilesRecursively(backupDir)

            if (filesToZip.isEmpty()) {
                emit(ExportProgress.Error("Nessun file da esportare"))
                return@flow
            }

            val totalSize = filesToZip.sumOf { it.length() }
            Timber.d("Exporting ${filesToZip.size} files, total size: $totalSize bytes")

            // 3. Create ZIP and write to destination Uri
            emit(ExportProgress.InProgress("Creazione archivio...", 0.2f))

            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                    ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->

                        var processedBytes = 0L
                        val basePath = backupDir.absolutePath

                        for ((index, file) in filesToZip.withIndex()) {
                            // Calculate relative path for zip entry
                            val relativePath = file.absolutePath
                                .removePrefix(basePath)
                                .removePrefix(File.separator)

                            // Progress update
                            val fileProgress = index.toFloat() / filesToZip.size
                            val overallProgress = 0.2f + (fileProgress * 0.75f)

                            // Emit progress on main thread context
                            // Note: We're inside withContext(Dispatchers.IO),
                            // but flow emission happens on the flow's context

                            // Add file to zip
                            addFileToZip(zipOut, file, relativePath)
                            processedBytes += file.length()

                            Timber.v("Added to zip: $relativePath")
                        }
                    }
                } ?: throw IllegalStateException("Cannot open output stream for Uri")
            }

            emit(ExportProgress.InProgress("Finalizzazione...", 0.95f))
            emit(ExportProgress.Completed(destinationUri.toString()))

            Timber.d("Backup exported successfully to: $destinationUri")

        } catch (e: Exception) {
            Timber.e(e, "Export failed")
            emit(ExportProgress.Error("Errore durante export: ${e.message}"))
        }

    }.flowOn(Dispatchers.IO)

    /**
     * Collect all files in directory recursively
     */
    private fun collectFilesRecursively(directory: File): List<File> {
        val files = mutableListOf<File>()

        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                files.addAll(collectFilesRecursively(file))
            } else {
                files.add(file)
            }
        }

        return files
    }

    /**
     * Add single file to ZIP archive
     */
    private fun addFileToZip(
        zipOut: ZipOutputStream,
        file: File,
        entryPath: String
    ) {
        val entry = ZipEntry(entryPath)
        entry.time = file.lastModified()
        entry.size = file.length()

        zipOut.putNextEntry(entry)

        BufferedInputStream(FileInputStream(file)).use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                zipOut.write(buffer, 0, bytesRead)
            }
        }

        zipOut.closeEntry()
    }
}

/**
 * Export progress states
 */
sealed class ExportProgress {
    data object Idle : ExportProgress()

    data class InProgress(
        val message: String,
        val progress: Float
    ) : ExportProgress()

    data class Completed(
        val exportedPath: String
    ) : ExportProgress()

    data class Error(
        val message: String
    ) : ExportProgress()
}