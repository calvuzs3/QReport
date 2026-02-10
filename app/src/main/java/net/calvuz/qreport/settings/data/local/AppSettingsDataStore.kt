package net.calvuz.qreport.settings.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import net.calvuz.qreport.settings.domain.model.ListViewMode
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings"
)

/**
 * DataStore for general app UI preferences.
 *
 * Stores per-list view mode preferences using a key pattern:
 * "view_mode_{listKey}" -> ListViewMode name (e.g. "FULL", "COMPACT", "MINIMAL")
 *
 * List keys are defined as constants for type safety.
 */
@Singleton
class AppSettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        // List screen keys
        const val LIST_KEY_CLIENTS = "clients"
        const val LIST_KEY_FACILITIES = "facilities"
        const val LIST_KEY_CONTACTS = "contacts"
        const val LIST_KEY_CONTRACTS = "contracts"
        const val LIST_KEY_ISLANDS = "islands"
        const val LIST_KEY_CHECKUPS = "checkups"
        const val LIST_KEY_TI = "technical_intervention"

        /** Builds the DataStore preference key for a given list. */
        private fun viewModeKey(listKey: String): Preferences.Key<String> {
            return stringPreferencesKey("view_mode_$listKey")
        }
    }

    /**
     * Observe the [ListViewMode] for a specific list screen.
     *
     * @param listKey one of the LIST_KEY_* constants
     * @return Flow emitting the current view mode, defaults to [ListViewMode.DEFAULT]
     */
    fun getListViewMode(listKey: String): Flow<ListViewMode> {
        return context.appSettingsDataStore.data
            .catch { exception ->
                Timber.e(exception, "Error reading view mode for list: $listKey")
                emit(androidx.datastore.preferences.core.emptyPreferences())
            }
            .map { preferences ->
                val stored = preferences[viewModeKey(listKey)]
                ListViewMode.fromString(stored)
            }
    }

    /**
     * Update the [ListViewMode] for a specific list screen.
     *
     * @param listKey one of the LIST_KEY_* constants
     * @param mode the new view mode to persist
     */
    suspend fun setListViewMode(listKey: String, mode: ListViewMode) {
        context.appSettingsDataStore.edit { preferences ->
            preferences[viewModeKey(listKey)] = mode.name
        }
        Timber.d("View mode for '$listKey' updated to ${mode.name}")
    }
}