package net.calvuz.qreport.util

import net.calvuz.qreport.data.backup.model.BackupInfo

object SizeUtils {
    /**
     * Formato display-friendly della dimensione
     */
    fun BackupInfo.getFormattedSize(): String {
        return when {
            totalSizeMB < 1 -> "${(totalSizeMB * 1024).toInt()} KB"
            totalSizeMB < 1024 -> "${totalSizeMB.toInt()} MB"
            else -> "${(totalSizeMB / 1024).toInt()} GB"
        }
    }

    fun getFormattedSize(totalSizeMB: Double): String {
        return when {
            totalSizeMB < 1 -> "${(totalSizeMB * 1024).toInt()} KB"
            totalSizeMB < 1024 -> "${totalSizeMB.toInt()} MB"
            else -> "${(totalSizeMB / 1024).toInt()} GB"
        }
    }

    fun Long.getFormattedSize(): String {
        val value: Long = this
        return when {
            value < 1 -> "${(value * 1024).toLong()} KB"
            value < 1024 -> "${value.toLong()} MB"
            else -> "${(value / 1024).toLong()} GB"
        }
    }
}