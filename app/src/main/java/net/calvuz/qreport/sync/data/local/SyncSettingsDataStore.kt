package net.calvuz.qreport.sync.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import net.calvuz.qreport.sync.domain.model.SyncMode
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.syncSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "sync_settings"
)

/**
 * DataStore for remote sync preferences.
 *
 * Stores:
 *  - [SyncMode]: whether remote sync is enabled for this device
 *  - lastSyncTimestamp: epoch millis of the last successful sync (null = never)
 *  - deviceId: stable UUID generated on first access, identifies this device on the server
 *
 * Note: auth token (JWT) is NOT stored here — it belongs in EncryptedSharedPreferences
 * and will be added in Phase 3 when the network layer is implemented.
 */
@Singleton
class SyncSettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object PreferencesKeys {
        val SYNC_MODE = stringPreferencesKey("sync_mode")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val SERVER_URL = stringPreferencesKey("server_url")
    }

    // ===== SYNC MODE =====

    /**
     * Observe the current [SyncMode].
     * Defaults to [SyncMode.LOCAL_ONLY] if never set.
     */
    fun getSyncMode(): Flow<SyncMode> =
        context.syncSettingsDataStore.data
            .catch { exception ->
                Timber.e(exception, "Error reading sync mode")
                emit(emptyPreferences())
            }
            .map { preferences ->
                SyncMode.fromString(preferences[PreferencesKeys.SYNC_MODE])
            }

    /**
     * Update the [SyncMode].
     */
    suspend fun setSyncMode(mode: SyncMode) {
        context.syncSettingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.SYNC_MODE] = mode.name
        }
        Timber.d("Sync mode updated to ${mode.name}")
    }

    // ===== LAST SYNC TIMESTAMP =====

    /**
     * Observe the epoch millis of the last successful sync.
     * Emits null if sync has never been performed.
     */
    fun getLastSyncTimestamp(): Flow<Long?> =
        context.syncSettingsDataStore.data
            .catch { exception ->
                Timber.e(exception, "Error reading last sync timestamp")
                emit(emptyPreferences())
            }
            .map { preferences ->
                preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP]
            }

    /**
     * Persist the timestamp of a successful sync.
     */
    suspend fun setLastSyncTimestamp(timestamp: Long) {
        context.syncSettingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] = timestamp
        }
        Timber.d("Last sync timestamp updated: $timestamp")
    }

    /**
     * One-shot read of the last sync timestamp (for use in suspend functions).
     * Returns 0L if sync has never been performed, so delta queries return everything.
     */
    suspend fun getLastSyncTimestampOnce(): Long =
        context.syncSettingsDataStore.data
            .catch { exception ->
                Timber.e(exception, "Error reading last sync timestamp (once)")
                emit(emptyPreferences())
            }
            .map { preferences ->
                preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] ?: 0L
            }
            .first()

    // ===== DEVICE ID =====

    /**
     * Returns the stable device UUID, generating and persisting one on first call.
     */
    suspend fun getDeviceId(): String {
        val stored = context.syncSettingsDataStore.data
            .map { preferences -> preferences[PreferencesKeys.DEVICE_ID] }
            .first()

        if (!stored.isNullOrBlank()) return stored

        // First access: generate and persist a new UUID
        val newId = UUID.randomUUID().toString()
        context.syncSettingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.DEVICE_ID] = newId
        }
        Timber.d("Device ID generated: $newId")
        return newId
    }

    // ===== SERVER URL =====

    fun getServerUrl(): Flow<String> =
        context.syncSettingsDataStore.data
            .catch { exception ->
                Timber.e(exception, "Error reading server URL")
                emit(emptyPreferences())
            }
            .map { preferences ->
                preferences[PreferencesKeys.SERVER_URL] ?: ""
            }

    suspend fun setServerUrl(url: String) {
        context.syncSettingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.SERVER_URL] = url.trim()
        }
        Timber.d("Server URL updated: $url")
    }

    // ===== RESET =====

    /**
     * Clears sync state (timestamp and mode) without touching the device ID.
     * Useful when the user logs out from the remote server.
     */
    suspend fun clearSyncState() {
        context.syncSettingsDataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.SYNC_MODE)
            preferences.remove(PreferencesKeys.LAST_SYNC_TIMESTAMP)
        }
        Timber.d("Sync state cleared")
    }
}