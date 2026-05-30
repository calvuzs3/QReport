package net.calvuz.qreport.sync.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import net.calvuz.qreport.sync.data.local.SyncSettingsDataStore
import net.calvuz.qreport.sync.data.local.dao.SyncDao
import net.calvuz.qreport.sync.domain.model.SyncMode
import net.calvuz.qreport.sync.domain.model.SyncStatus
import net.calvuz.qreport.sync.domain.repository.SyncRepository
import javax.inject.Inject

/**
 * Implementation of [SyncRepository].
 *
 * Phase 1: manages sync settings and exposes pending-change diagnostics.
 * Phase 3: push/pull orchestration will be added here when the network layer
 *           (RemoteDataSource, Retrofit) is implemented.
 */
class SyncRepositoryImpl @Inject constructor(
    private val syncDao: SyncDao,
    private val syncSettingsDataStore: SyncSettingsDataStore
) : SyncRepository {

    // ===== SYNC MODE =====

    override fun getSyncMode(): Flow<SyncMode> =
        syncSettingsDataStore.getSyncMode()

    override suspend fun setSyncMode(mode: SyncMode): Result<Unit> {
        return try {
            syncSettingsDataStore.setSyncMode(mode)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== SYNC STATUS =====

    override fun getLastSyncTimestamp(): Flow<Long?> =
        syncSettingsDataStore.getLastSyncTimestamp()

    override suspend fun countPendingChanges(): Result<Int> {
        return try {
            val count = syncDao.countPendingSync()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSyncStatus(): Result<SyncStatus> {
        return try {
            val mode = syncSettingsDataStore.getSyncMode()
                .first()
            val lastSync = syncSettingsDataStore.getLastSyncTimestampOnce()
                .let { if (it == 0L) null else it }
            val pending = syncDao.countPendingSync()
            val deviceId = syncSettingsDataStore.getDeviceId()

            Result.success(
                SyncStatus(
                    mode = mode,
                    lastSyncTimestamp = lastSync,
                    pendingChangesCount = pending,
                    deviceId = deviceId
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== DEVICE =====

    override suspend fun getDeviceId(): Result<String> {
        return try {
            Result.success(syncSettingsDataStore.getDeviceId())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== SERVER URL =====

    override fun getServerUrl(): Flow<String> =
        syncSettingsDataStore.getServerUrl()

    override suspend fun setServerUrl(url: String): Result<Unit> {
        return try {
            syncSettingsDataStore.setServerUrl(url)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== RESET =====

    override suspend fun clearSyncState(): Result<Unit> {
        return try {
            syncSettingsDataStore.clearSyncState()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resetLastSyncTimestamp(): Result<Unit> {
        return try {
            syncSettingsDataStore.setLastSyncTimestamp(0L)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}