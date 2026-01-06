package net.calvuz.qreport.backup.data.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Health status of backup system
 */
data class BackupHealthResult(
    val isHealthy: Boolean,
    val issues: List<String>,
    val warnings: List<String>,
    val lastCheckTimestamp: Instant
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