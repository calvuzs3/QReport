package net.calvuz.qreport.domain.usecase.backup

import net.calvuz.qreport.domain.model.file.FileManager
import net.calvuz.qreport.domain.service.ShareManager
import net.calvuz.qreport.domain.repository.backup.BackupRepository
import net.calvuz.qreport.domain.model.share.*
import net.calvuz.qreport.util.SizeUtils.getFormattedSize
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * ✅ CORRECTED: Use case con enum usage corretti
 *
 * ShareMode = Modalità condivisione (SINGLE_FILE, COMPLETE_BACKUP, COMPRESSED)
 * ShareOptionType = Tipo UI opzione (FILE_OPTION, APP_SPECIFIC, etc)
 */
class ShareBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
    private val shareManager: ShareManager,
    private val fileManager: FileManager
) {

    /**
     * ✅ MAIN: Condividi backup con ShareMode esistente
     */
    suspend operator fun invoke(
        backupPath: String,
        shareMode: ShareMode = ShareMode.SINGLE_FILE,  // ✅ Use existing enum
        targetApp: String? = null
    ): Result<ShareResult> {
        return try {
            Timber.d("Sharing backup: $backupPath with mode: $shareMode")

            // 1. Analyze backup structure
            val backupStructure = analyzeBackupStructure(backupPath)
            Timber.d("Backup structure: ${backupStructure.summary}")

            // 2. Validate backup based on structure
            val validationPath = if (backupStructure.isDirectory) {
                backupStructure.mainFilePath
            } else {
                backupPath
            }

            val validation = backupRepository.validateBackup(validationPath)
            if (!validation.isValid) {
                return Result.failure(
                    IllegalStateException("Cannot share invalid backup: ${validation.errors.firstOrNull()}")
                )
            }

            // 3. Create share intent based on ShareMode
            val shareTitle = createShareTitle(backupStructure, shareMode)

            val shareIntentResult = when (shareMode) {
                ShareMode.SINGLE_FILE -> {
                    Timber.d("Sharing single file: ${backupStructure.mainFilePath}")
                    shareManager.shareBackupFile(backupStructure.mainFilePath, shareTitle)
                }

                ShareMode.COMPLETE_BACKUP -> {
                    if (backupStructure.isDirectory) {
                        Timber.d("Sharing complete directory: ${backupStructure.backupPath}")
                        shareManager.shareBackupDirectory(backupStructure.backupPath, shareTitle)
                    } else {
                        Timber.d("Fallback to single file (no directory available)")
                        shareManager.shareBackupFile(backupStructure.mainFilePath, shareTitle)
                    }
                }

                ShareMode.COMPRESSED -> {
                    Timber.d("Creating compressed backup for sharing")

                    try {
                        // 1. Create compressed backup using FileManager (data layer)
                        val compressedResult =
                            fileManager.createCompressedBackup(
                                backupPath = backupStructure.backupPath,
                                includeAllFiles = true  // All files if directory, single if file
                            )


                        if (compressedResult.isFailure) {
                            throw compressedResult.exceptionOrNull()
                                ?: Exception("Failed to create compressed backup")
                        }

                        val zipFile = compressedResult.getOrThrow()

                        // 2. Create share title for ZIP
                        val compressedTitle = createCompressedShareTitle(backupStructure)

                        // 3. Share the ZIP file
                        val zipShareResult = shareManager.shareBackupFile(
                            filePath = zipFile.absolutePath,
                            shareTitle = compressedTitle
                        )

                        if (zipShareResult.isFailure) {
                            throw zipShareResult.exceptionOrNull()
                                ?: Exception("Failed to share ZIP backup")
                        }

                        Timber.d("Compressed backup shared successfully:")
                        Timber.d("  - ZIP file: ${zipFile.name}")
                        Timber.d("  - Size: ${zipFile.length()} bytes")

                        zipShareResult

                    } catch (e: Exception) {
                        Timber.e(e, "Compressed sharing failed, falling back to main file")

                        // Fallback: Share main file without compression
                        val fallbackTitle = "$shareTitle (ZIP non disponibile)"
                        shareManager.shareBackupFile(backupStructure.mainFilePath, fallbackTitle)
                    }
                }
            }

            if (shareIntentResult.isFailure) {
                return Result.failure(
                    shareIntentResult.exceptionOrNull()
                        ?: Exception("Failed to create share intent")
                )
            }

            val shareIntent = shareIntentResult.getOrThrow()

            // 4. Handle specific app targeting if requested
            val finalIntent = if (targetApp != null) {
                val targetResult = shareManager.shareBackupWithApp(
                    backupStructure.mainFilePath,
                    targetApp,
                    shareTitle
                )
                if (targetResult.isFailure) {
                    return Result.failure(
                        targetResult.exceptionOrNull()
                            ?: Exception("Failed to target specific app: $targetApp")
                    )
                }
                targetResult.getOrThrow()
            } else {
                shareIntent
            }

            // 5. Create successful result
            val shareResult = ShareResult(
                intent = finalIntent,
                shareMode = shareMode,  // ✅ Use existing ShareMode
                backupPath = backupPath,
                targetApp = targetApp,
                shareTitle = shareTitle,
                availableApps = shareManager.getAvailableShareApps(backupStructure.mainFilePath)
            )

            Timber.d("Backup sharing prepared successfully:")
            Timber.d("  - Title: ${shareResult.shareTitle}")
            Timber.d("  - Mode: ${shareResult.shareMode}")
            Timber.d("  - Structure: ${backupStructure.summary}")

            Result.success(shareResult)

        } catch (e: Exception) {
            Timber.e(e, "Error sharing backup $backupPath")
            Result.failure(e)
        }
    }

    /**
     * ✅ CORRECTED: Generate options usando enum corretti
     */
    suspend fun getAvailableShareOptions(backupPath: String): Result<List<ShareOption>> {
        return try {
            Timber.d("Analyzing share options for: $backupPath")

            // Analyze backup structure
            val backupStructure = analyzeBackupStructure(backupPath)
            Timber.d("Detected structure: ${backupStructure.summary}")

            // Validate main file
            val validation = backupRepository.validateBackup(backupStructure.mainFilePath)
            if (!validation.isValid) {
                return Result.failure(
                    IllegalStateException("Cannot analyze invalid backup")
                )
            }

            val shareOptions = mutableListOf<ShareOption>()
            val availableApps = shareManager.getAvailableShareApps(backupStructure.mainFilePath)

            // ✅ GROUP 1: File Options (usando ShareMode esistente)

            // Add complete backup option if directory with multiple files
            if (backupStructure.isDirectory && backupStructure.totalFiles > 1) {
                shareOptions.add(
                    ShareOption(
                        type = ShareOptionType.FILE_OPTION,        // ✅ UI categorization
                        shareMode = ShareMode.COMPLETE_BACKUP,     // ✅ Actual behavior
                        title = "Backup Completo",
                        subtitle = "Tutti i ${backupStructure.totalFiles} file (${
                            backupStructure.totalSize.getFormattedSize()
                        })"
                    )
                )
            }

            // Add single file option
            shareOptions.add(
                ShareOption(
                    type = ShareOptionType.FILE_OPTION,        // ✅ UI categorization
                    shareMode = ShareMode.SINGLE_FILE,         // ✅ Actual behavior
                    title = "Solo File Principale",
                    subtitle = "Condividi ${backupStructure.mainFileName} (${
                        (
                                backupStructure.mainFileSize
                                ).getFormattedSize()
                    })"
                )
            )

            // Add compressed backup option
            if (backupStructure.isDirectory && backupStructure.totalFiles > 1) {
                shareOptions.add(
                    ShareOption(
                        type = ShareOptionType.FILE_OPTION,        // ✅ UI categorization
                        shareMode = ShareMode.COMPRESSED,          // ✅ Actual behavior
                        title = "Backup Compresso",
                        subtitle = "Zippa tutti i ${backupStructure.totalFiles} file (${
                            (
                                    backupStructure.totalSize
                                    ).getFormattedSize()
                        })"
                    )
                )
            }

//            // ✅ GROUP 2: App Options
//
//            // Add general share option
//            shareOptions.add(
//                ShareOption(
//                    type = ShareOptionType.APP_GENERIC,        // ✅ UI categorization
//                    shareMode = ShareMode.SINGLE_FILE,         // ✅ Default behavior
//                    title = "Condividi con...",
//                    subtitle = "Scegli app per condivisione"
//                )
//            )
//
//            // Add specific apps (top 3 most common)
//            availableApps.take(3).forEach { appInfo ->
//                shareOptions.add(
//                    ShareOption(
//                        type = ShareOptionType.APP_SPECIFIC,      // ✅ UI categorization
//                        shareMode = ShareMode.SINGLE_FILE,        // ✅ Default behavior
//                        title = appInfo.appName,
//                        subtitle = "Condividi tramite ${appInfo.appName}",
//                        targetPackage = appInfo.packageName
//                    )
//                )
//            }

            Timber.d("Generated ${shareOptions.size} share options:")
            shareOptions.forEach { option ->
                Timber.d("  - ${option.title} (UI:${option.type}, Mode:${option.shareMode})")
            }

            Result.success(shareOptions)

        } catch (e: Exception) {
            Timber.e(e, "Error getting share options for: $backupPath")
            Result.failure(e)
        }
    }

    // ===== PRIVATE HELPER METHODS =====

    /**
     * Create appropriate share title for compressed backup
     */
    private fun createCompressedShareTitle(structure: BackupStructure): String {
        return if (structure.isDirectory) {
            "QReport Backup ZIP - ${structure.totalFiles} file compressi"
        } else {
            "QReport Backup ZIP - ${structure.mainFileName}"
        }
    }

    /**
     * Analyze backup structure
     */
    private fun analyzeBackupStructure(backupPath: String): BackupStructure {
        val backupFile = File(backupPath)

        Timber.d("Analyzing backup structure:")
        Timber.d("  - Input path: $backupPath")
        Timber.d("  - Is directory: ${backupFile.isDirectory}")
        Timber.d("  - Exists: ${backupFile.exists()}")

        return if (backupFile.isDirectory && backupFile.exists()) {
            // Directory backup - analyze contents
            val files = backupFile.listFiles()?.filter { it.isFile } ?: emptyList()
            val mainFile = findMainBackupFile(files)

            Timber.d("  - Directory files: ${files.map { it.name }}")
            Timber.d("  - Main file: ${mainFile?.name}")

            BackupStructure(
                isDirectory = true,
                backupPath = backupPath,
                mainFilePath = mainFile?.absolutePath ?: backupPath,
                mainFileName = mainFile?.name ?: "backup.json",
                mainFileSize = mainFile?.length() ?: 0L,
                totalFiles = files.size,
                totalSize = files.sumOf { it.length() },
                additionalFiles = files.filterNot { it == mainFile }.map { it.name }
            )
        } else {
            // Single file backup
            Timber.d("  - Single file backup: ${backupFile.name}")

            BackupStructure(
                isDirectory = false,
                backupPath = backupPath,
                mainFilePath = backupPath,
                mainFileName = backupFile.name,
                mainFileSize = backupFile.length(),
                totalFiles = 1,
                totalSize = backupFile.length(),
                additionalFiles = emptyList()
            )
        }
    }

    /**
     * Find main backup file (JSON) in directory
     */
    private fun findMainBackupFile(files: List<File>): File? {
        val candidates = listOf(
            "qreport_backup_full.json",
            "database.json",
            "backup.json"
        )

        return candidates.firstNotNullOfOrNull { fileName ->
            files.find { it.name == fileName }
        } ?: files.find { it.extension == "json" }
    }

    private fun createShareTitle(structure: BackupStructure, shareMode: ShareMode): String {
        return when (shareMode) {
            ShareMode.SINGLE_FILE -> "QReport Backup - ${structure.mainFileName}"
            ShareMode.COMPLETE_BACKUP -> {
                if (structure.isDirectory) {
                    "QReport Backup Completo - ${structure.totalFiles} file"
                } else {
                    "QReport Backup - ${structure.mainFileName}"
                }
            }

            ShareMode.COMPRESSED -> createCompressedShareTitle(structure)
        }
    }
}

/**
 * ✅ Backup structure analysis result (unchanged)
 */
data class BackupStructure(
    val isDirectory: Boolean,
    val backupPath: String,
    val mainFilePath: String,
    val mainFileName: String,
    val mainFileSize: Long,
    val totalFiles: Int,
    val totalSize: Long,
    val additionalFiles: List<String>
) {
    val summary: String
        get() = if (isDirectory) {
            "Directory with $totalFiles files: $mainFileName + [${additionalFiles.joinToString(", ")}]"
        } else {
            "Single file: $mainFileName"
        }
}