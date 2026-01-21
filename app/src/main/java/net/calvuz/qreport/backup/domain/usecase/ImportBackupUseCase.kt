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
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.backup.domain.repository.BackupFileRepository
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipInputStream
import javax.inject.Inject

/**
 * Use case for importing a backup from a user-selected ZIP file
 *
 * Extracts the ZIP file to the internal backup directory and validates it
 */
class ImportBackupUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupRepository: BackupRepository,
    private val backupFileRepository: BackupFileRepository,
    private val validateBackupUseCase: ValidateBackupUseCase
) {
    companion object {
        private const val BUFFER_SIZE = 8192
        private const val MAX_ENTRY_SIZE = 500 * 1024 * 1024L // 500MB max per entry
        private const val MAX_ENTRIES = 10000 // Max number of entries in ZIP
    }

    /**
     * Import backup from the specified Uri
     *
     * @param sourceUri Uri of the ZIP file selected by user via SAF
     * @return Flow emitting import progress
     */
    suspend operator fun invoke(sourceUri: Uri): Flow<ImportProgress> = flow {

        emit(ImportProgress.InProgress("Preparazione import...", 0f))

        try {
            // 1. Generate unique backup ID for import
            val importedBackupId = "imported_${UUID.randomUUID()}"

            // 2. Get backup directory path
            emit(ImportProgress.InProgress("Creazione directory...", 0.1f))

            val backupDirPath = when (val result = backupFileRepository.generateBackupPath(importedBackupId)) {
                is QrResult.Success -> result.data
                is QrResult.Error -> {
                    emit(ImportProgress.Error("Impossibile creare directory backup"))
                    return@flow
                }
            }

            val backupDir = File(backupDirPath)
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            // 3. Extract ZIP to backup directory
            emit(ImportProgress.InProgress("Estrazione archivio...", 0.2f))

            val extractedFiles = withContext(Dispatchers.IO) {
                extractZipToDirectory(sourceUri, backupDir)
            }

            if (extractedFiles == 0) {
                // Cleanup empty directory
                backupDir.deleteRecursively()
                emit(ImportProgress.Error("Archivio vuoto o non valido"))
                return@flow
            }

            Timber.d("Extracted $extractedFiles files to ${backupDir.absolutePath}")

            // 4. Find and validate database.json
            emit(ImportProgress.InProgress("Validazione backup...", 0.8f))

            val databaseFile = File(backupDir, "database.json")
            if (!databaseFile.exists()) {
                // Cleanup invalid backup
                backupDir.deleteRecursively()
                emit(ImportProgress.Error("Backup non valido: database.json mancante"))
                return@flow
            }

            // 5. Validate backup structure
            val validation = validateBackupUseCase(databaseFile.absolutePath)
            if (!validation.isValid) {
                // Cleanup invalid backup
                backupDir.deleteRecursively()
                emit(ImportProgress.Error("Backup non valido: ${validation.issues.firstOrNull() ?: "struttura errata"}"))
                return@flow
            }

            // 6. Validation warnings (non-blocking)
            if (validation.warnings.isNotEmpty()) {
                Timber.w("Import warnings: ${validation.warnings.joinToString()}")
            }

            emit(ImportProgress.InProgress("Finalizzazione...", 0.95f))
            emit(ImportProgress.Completed(importedBackupId, extractedFiles))

            Timber.d("Backup imported successfully: $importedBackupId")

        } catch (e: SecurityException) {
            Timber.e(e, "Security exception during import - permission denied")
            emit(ImportProgress.Error("Permesso negato per accedere al file"))
        } catch (e: Exception) {
            Timber.e(e, "Import failed")
            emit(ImportProgress.Error("Errore durante import: ${e.message}"))
        }

    }.flowOn(Dispatchers.IO)

    /**
     * Extract ZIP contents to destination directory
     *
     * @return Number of files extracted
     */
    private fun extractZipToDirectory(sourceUri: Uri, destDir: File): Int {
        var extractedCount = 0
        var entryCount = 0

        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->

                var entry = zipIn.nextEntry

                while (entry != null) {
                    entryCount++

                    // Security: limit number of entries (zip bomb protection)
                    if (entryCount > MAX_ENTRIES) {
                        Timber.w("Too many entries in ZIP, stopping at $MAX_ENTRIES")
                        break
                    }

                    // Security: validate entry name (path traversal protection)
                    val entryName = entry.name
                    if (entryName.contains("..") || entryName.startsWith("/")) {
                        Timber.w("Skipping suspicious entry: $entryName")
                        entry = zipIn.nextEntry
                        continue
                    }

                    // Security: check entry size
                    if (entry.size > MAX_ENTRY_SIZE) {
                        Timber.w("Skipping oversized entry: $entryName (${entry.size} bytes)")
                        entry = zipIn.nextEntry
                        continue
                    }

                    val outputFile = File(destDir, entryName)

                    // Security: ensure output file is within destination directory
                    if (!outputFile.canonicalPath.startsWith(destDir.canonicalPath)) {
                        Timber.w("Skipping entry outside destination: $entryName")
                        entry = zipIn.nextEntry
                        continue
                    }

                    if (entry.isDirectory) {
                        outputFile.mkdirs()
                    } else {
                        // Ensure parent directories exist
                        outputFile.parentFile?.mkdirs()

                        // Extract file
                        BufferedOutputStream(FileOutputStream(outputFile)).use { output ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            var bytesRead: Int
                            var totalRead = 0L

                            while (zipIn.read(buffer).also { bytesRead = it } != -1) {
                                totalRead += bytesRead

                                // Security: double-check size during extraction
                                if (totalRead > MAX_ENTRY_SIZE) {
                                    Timber.w("Entry exceeded max size during extraction: $entryName")
                                    break
                                }

                                output.write(buffer, 0, bytesRead)
                            }
                        }

                        extractedCount++
                        Timber.v("Extracted: $entryName")
                    }

                    entry = zipIn.nextEntry
                }
            }
        } ?: throw IllegalStateException("Cannot open input stream for Uri")

        return extractedCount
    }
}

/**
 * Import progress states
 */
sealed class ImportProgress {
    data object Idle : ImportProgress()

    data class InProgress(
        val message: String,
        val progress: Float
    ) : ImportProgress()

    data class Completed(
        val backupId: String,
        val filesImported: Int
    ) : ImportProgress()

    data class Error(
        val message: String
    ) : ImportProgress()
}