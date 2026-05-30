package net.calvuz.qreport.sync.app

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.calvuz.qreport.sync.data.local.SyncSettingsDataStore
import net.calvuz.qreport.sync.data.local.TokenStorage
import net.calvuz.qreport.sync.domain.model.SyncMode
import net.calvuz.qreport.sync.domain.usecase.SyncUseCase
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Triggers a sync session when the app comes to foreground, provided:
 *  - Sync mode is [SyncMode.REMOTE_ENABLED]
 *  - The user is logged in (token present)
 *  - At least [MIN_SYNC_INTERVAL_MS] have passed since the last sync
 *
 * Registered on [ProcessLifecycleOwner] in [QReportApplication].
 */
@Singleton
class SyncForegroundObserver @Inject constructor(
    private val syncUseCase: SyncUseCase,
    private val syncSettingsDataStore: SyncSettingsDataStore,
    private val tokenStorage: TokenStorage
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStart(owner: LifecycleOwner) {
        scope.launch {
            triggerSyncIfNeeded()
        }
    }

    private suspend fun triggerSyncIfNeeded() {
        // Check sync is enabled
        val mode = syncSettingsDataStore.getSyncMode().first()
        if (mode != SyncMode.REMOTE_ENABLED) {
            Timber.d("SyncForegroundObserver: sync disabled, skipping")
            return
        }

        // Check user is logged in
        if (!tokenStorage.isLoggedIn()) {
            Timber.d("SyncForegroundObserver: not logged in, skipping")
            return
        }

        // Check minimum interval since last sync
        val lastSync = syncSettingsDataStore.getLastSyncTimestampOnce()
        val elapsed = System.currentTimeMillis() - lastSync
        if (elapsed < MIN_SYNC_INTERVAL_MS) {
            Timber.d("SyncForegroundObserver: last sync ${elapsed / 1000}s ago, skipping (min: ${MIN_SYNC_INTERVAL_MS / 1000}s)")
            return
        }

        Timber.d("SyncForegroundObserver: triggering foreground sync")
        syncUseCase()
        Timber.d("SyncForegroundObserver: foreground sync completed")
    }

    companion object {
        // Minimum time between automatic syncs: 30 minutes
        private const val MIN_SYNC_INTERVAL_MS = 30 * 60 * 1000L
    }
}