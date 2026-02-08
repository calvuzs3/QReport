package net.calvuz.qreport.settings.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.settings.domain.model.ListViewMode

/**
 * Repository for general app UI preferences.
 *
 * Provides per-list view mode persistence.
 */
interface AppSettingsRepository {

    /**
     * Observe the view mode for a specific list screen.
     *
     * @param listKey identifier for the list screen
     */
    fun getListViewMode(listKey: String): Flow<ListViewMode>

    /**
     * Update the view mode for a specific list screen.
     *
     * @param listKey identifier for the list screen
     * @param mode the new view mode
     */
    suspend fun setListViewMode(listKey: String, mode: ListViewMode)
}