package net.calvuz.qreport.sync.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.sync.domain.model.SyncMode
import net.calvuz.qreport.sync.domain.model.SyncStatus

/**
 * Repository interface for sync state and orchestration.
 *
 * Phase 1: settings management and pending-change diagnostics (local only).
 * Phase 3: push/pull methods will be added when the network layer is implemented.
 */
interface SyncRepository {

    // ===== SYNC MODE =====

    fun getSyncMode(): Flow<SyncMode>
    suspend fun setSyncMode(mode: SyncMode): Result<Unit>

    // ===== SYNC STATUS =====

    /** Flow of the last successful sync timestamp, null if never synced. */
    fun getLastSyncTimestamp(): Flow<Long?>

    /** Total number of local records not yet pushed to the server. */
    suspend fun countPendingChanges(): Result<Int>

    /** Snapshot of the current sync state, useful for the Settings UI. */
    suspend fun getSyncStatus(): Result<SyncStatus>

    // ===== DEVICE =====

    suspend fun getDeviceId(): Result<String>

    // ===== SERVER URL =====

    fun getServerUrl(): Flow<String>
    suspend fun setServerUrl(url: String): Result<Unit>

    // ===== RESET =====

    /** Clears sync timestamps and mode. Called on server logout. */
    suspend fun clearSyncState(): Result<Unit>
}