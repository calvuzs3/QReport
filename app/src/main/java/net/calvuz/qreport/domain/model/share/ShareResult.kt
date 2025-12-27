package net.calvuz.qreport.domain.model.share

import android.content.Intent

/**
 * Risultato operazione sharing
 */
data class ShareResult(
    val intent: Intent,
    val shareMode: ShareMode,
    val backupPath: String,
    val targetApp: String?,
    val shareTitle: String,
    val availableApps: List<ShareAppInfo>
)

