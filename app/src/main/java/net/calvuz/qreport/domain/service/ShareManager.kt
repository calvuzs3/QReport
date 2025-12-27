package net.calvuz.qreport.domain.service

import android.content.Intent
import net.calvuz.qreport.domain.model.share.ShareAppInfo

interface ShareManager {

    // Single file share
    fun shareBackupFile(
        filePath: String,
        shareTitle: String = "Condividi Backup QReport"
    ): Result<Intent>

    // Directory share
    fun shareBackupDirectory(
        backupPath: String,
        shareTitle: String = "Condividi Backup Completo QReport"
    ): Result<Intent>

    // Backup share
    fun shareBackupWithApp(
        filePath: String,
        targetPackage: String,
        shareTitle: String = "Backup QReport"
    ): Result<Intent>

    fun getAvailableShareApps(filePath: String): List<ShareAppInfo>

}