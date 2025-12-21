package net.calvuz.qreport.data.backup.model

import kotlinx.datetime.Clock

/**
 * Health status of backup system
 */
data class BackupHealthResult(
    val isHealthy: Boolean,
    val issues: List<String>,
    val warnings: List<String>,
    val lastCheckTimestamp: kotlinx.datetime.Instant
) {
    companion object {
        fun unhealthy(issues: List<String>) = BackupHealthResult(
            isHealthy = false,
            issues = issues,
            warnings = emptyList(),
            lastCheckTimestamp = Clock.System.now()
        )
    }
}