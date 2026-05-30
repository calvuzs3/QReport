// ===== SyncResult.kt =====
package net.calvuz.qreport.sync.domain.model

/**
 * Result of a completed sync session.
 */
data class SyncResult(
    val syncedAt: Long,
    val pushedCount: Int,
    val pulledCount: Int
)