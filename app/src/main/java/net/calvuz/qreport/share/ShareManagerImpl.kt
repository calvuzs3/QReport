package net.calvuz.qreport.share

/**
 * Gestisce la condivisione di file tramite Android Share Intent
 *
 * Features:
 * - FileProvider per accesso sicuro ai file app
 * - Multiple sharing options (Email, Drive, Bluetooth, etc.)
 * - MIME type detection automatica
 * - Error handling robusto
 */
//@Singleton
//class ShareManagerImpl @Inject constructor(
//    @ApplicationContext private val context: Context
//): ShareManager {
//
//    companion object {
//        private const val FILE_PROVIDER_AUTHORITY = "${BuildConfig.APPLICATION_ID}.fileprovider"
//
//        // MIME types per diversi formati backup
//        private const val MIME_TYPE_JSON = "application/json"
//        private const val MIME_TYPE_ZIP = "application/zip"
//        private const val MIME_TYPE_BACKUP = "application/octet-stream"
//    }
//
//    /**
//     * Condividi singolo file di backup
//     */
//    override fun shareBackupFile(
//        filePath: String,
//        shareTitle: String
//    ): Result<Intent> {
//        return try {
//            val file = File(filePath)
//
//            if (!file.exists()) {
//                return Result.failure(
//                    IllegalArgumentException("File backup non trovato: $filePath")
//                )
//            }
//
//            if (!file.canRead()) {
//                return Result.failure(
//                    SecurityException("File backup non leggibile: $filePath")
//                )
//            }
//
//            val fileUri = createFileUri(file)
//            val mimeType = getMimeType(file)
//
//            val shareIntent = createShareIntent(
//                fileUri = fileUri,
//                mimeType = mimeType,
//                shareTitle = shareTitle,
//                fileDisplayName = file.name
//            )
//
//            Timber.d("Share intent created for file: ${file.name}, size: ${file.length()} bytes")
//            Result.success(shareIntent)
//
//        } catch (e: SecurityException) {
//            Timber.e(e, "Security error sharing backup file")
//            Result.failure(e)
//        } catch (e: Exception) {
//            Timber.e(e, "Error creating share intent for: $filePath")
//            Result.failure(e)
//        }
//    }
//
//    /**
//     * Condividi backup completo (multiple files)
//     */
//    override fun shareBackupDirectory(
//        backupPath: String,
//        shareTitle: String
//    ): Result<Intent> {
//        return try {
//            val backupDir = File(backupPath)
//
//            if (!backupDir.exists() || !backupDir.isDirectory) {
//                return Result.failure(
//                    IllegalArgumentException("Directory backup non trovata: $backupPath")
//                )
//            }
//
//            val backupFiles = backupDir.listFiles()?.filter { it.isFile }
//            if (backupFiles.isNullOrEmpty()) {
//                return Result.failure(
//                    IllegalStateException("Directory backup vuota: $backupPath")
//                )
//            }
//
//            // Se c'è solo un file, usa share singolo
//            if (backupFiles.size == 1) {
//                return shareBackupFile(
//                    backupFiles.first().absolutePath,
//                    shareTitle
//                )
//            }
//
//            // Multiple files - crea share con attachment multipli
//            val fileUris = backupFiles.map { createFileUri(it) }
//
//            val shareIntent = createMultipleFilesShareIntent(
//                fileUris = fileUris,
//                shareTitle = shareTitle,
//                backupDirName = backupDir.name
//            )
//
//            Timber.d("Share intent created for ${backupFiles.size} files from: ${backupDir.name}")
//            Result.success(shareIntent)
//
//        } catch (e: Exception) {
//            Timber.e(e, "Error sharing backup directory: $backupPath")
//            Result.failure(e)
//        }
//    }
//
//    /**
//     * Condividi con app specifica (es. Email, Drive)
//     */
//    override fun shareBackupWithApp(
//        filePath: String,
//        targetPackage: String,
//        shareTitle: String
//    ): Result<Intent> {
//        return try {
//            val shareResult = shareBackupFile(filePath, shareTitle)
//
//            if (shareResult.isFailure) {
//                return shareResult
//            }
//
//            val shareIntent = shareResult.getOrThrow()
//            shareIntent.setPackage(targetPackage)
//
//            // Verifica se l'app target è installata
//            val packageManager = context.packageManager
//            val resolvedActivities = packageManager.queryIntentActivities(shareIntent, 0)
//
//            if (resolvedActivities.isEmpty()) {
//                return Result.failure(
//                    IllegalStateException("App non installata: $targetPackage")
//                )
//            }
//
//            Timber.d("Share intent created for specific app: $targetPackage")
//            Result.success(shareIntent)
//
//        } catch (e: Exception) {
//            Timber.e(e, "Error sharing backup with app: $targetPackage")
//            Result.failure(e)
//        }
//    }
//
//    /**
//     * Ottieni apps disponibili per sharing
//     */
//    override fun getAvailableShareApps(filePath: String): List<ShareAppInfo> {
//        return try {
//            val shareResult = shareBackupFile(filePath)
//            if (shareResult.isFailure) {
//                return emptyList()
//            }
//
//            val shareIntent = shareResult.getOrThrow()
//            val packageManager = context.packageManager
//            val resolvedActivities = packageManager.queryIntentActivities(shareIntent, 0)
//
//            resolvedActivities.map { resolveInfo ->
//                ShareAppInfo(
//                    packageName = resolveInfo.activityInfo.packageName,
//                    appName = resolveInfo.loadLabel(packageManager).toString(),
//                    activityName = resolveInfo.activityInfo.name,
//                    icon = resolveInfo.loadIcon(packageManager)
//                )
//            }
//
//        } catch (e: Exception) {
//            Timber.e(e, "Error getting available share apps")
//            emptyList()
//        }
//    }
//
//    // ===== PRIVATE HELPER METHODS =====
//
//    /**
//     * Crea URI sicuro tramite FileProvider
//     */
//    private fun createFileUri(file: File): Uri {
//        return FileProvider.getUriForFile(
//            context,
//            FILE_PROVIDER_AUTHORITY,
//            file
//        )
//    }
//
//    /**
//     * Determina MIME type dal file
//     */
//    private fun getMimeType(file: File): String {
//        return when (file.extension.lowercase()) {
//            "json" -> MIME_TYPE_JSON
//            "zip" -> MIME_TYPE_ZIP
//            "qrbkp", "backup" -> MIME_TYPE_BACKUP
//            else -> MIME_TYPE_BACKUP
//        }
//    }
//
//    /**
//     * Crea Intent per sharing singolo file
//     */
//    private fun createShareIntent(
//        fileUri: Uri,
//        mimeType: String,
//        shareTitle: String,
//        fileDisplayName: String
//    ): Intent {
//        val intent = Intent(Intent.ACTION_SEND).apply {
//            type = mimeType
//            putExtra(Intent.EXTRA_STREAM, fileUri)
//            putExtra(Intent.EXTRA_SUBJECT, shareTitle)
//            putExtra(Intent.EXTRA_TEXT, "Backup QReport: $fileDisplayName")
//            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//        }
//
//        return Intent.createChooser(intent, shareTitle)
//    }
//
//    /**
//     * Crea Intent per sharing multiple files
//     */
//    private fun createMultipleFilesShareIntent(
//        fileUris: List<Uri>,
//        shareTitle: String,
//        backupDirName: String
//    ): Intent {
//        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
//            type = "*/*" // Mixed content types
//            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(fileUris))
//            putExtra(Intent.EXTRA_SUBJECT, shareTitle)
//            putExtra(Intent.EXTRA_TEXT, "Backup completo QReport: $backupDirName")
//            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//        }
//
//        return Intent.createChooser(intent, shareTitle)
//    }
//}
//
//
///**
// * Enum per app di sharing comuni
// */
//enum class CommonShareApps(val packageName: String, val displayName: String) {
//    GMAIL("com.google.android.gm", "Gmail"),
//    GOOGLE_DRIVE("com.google.android.apps.docs", "Google Drive"),
//    WHATSAPP("com.whatsapp", "WhatsApp"),
//    TELEGRAM("org.telegram.messenger", "Telegram"),
//    DROPBOX("com.dropbox.android", "Dropbox"),
//    BLUETOOTH("com.android.bluetooth", "Bluetooth"),
//    NEARBY_SHARE("com.google.android.gms", "Nearby Share")
//}