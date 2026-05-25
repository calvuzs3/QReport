package net.calvuz.qreport.sync.domain.model

/**
 * Snapshot of the current sync state, used by the Settings UI.
 *
 * @param mode whether remote sync is enabled
 * @param lastSyncTimestamp epoch millis of the last successful sync, null if never synced
 * @param pendingChangesCount number of local records not yet pushed to the server
 * @param deviceId stable UUID identifying this device on the server
 */
data class SyncStatus(
    val mode: SyncMode,
    val lastSyncTimestamp: Long?,
    val pendingChangesCount: Int,
    val deviceId: String
)