package net.calvuz.qreport.settings.data.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.settings.data.local.AppSettingsDataStore
import net.calvuz.qreport.settings.domain.model.ListViewMode
import net.calvuz.qreport.settings.domain.repository.AppSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [AppSettingsRepository].
 *
 * Delegates to [AppSettingsDataStore] for persistence.
 */
@Singleton
class AppSettingsRepositoryImpl @Inject constructor(
    private val appSettingsDataStore: AppSettingsDataStore
) : AppSettingsRepository {

    override fun getListViewMode(listKey: String): Flow<ListViewMode> {
        return appSettingsDataStore.getListViewMode(listKey)
    }

    override suspend fun setListViewMode(listKey: String, mode: ListViewMode) {
        appSettingsDataStore.setListViewMode(listKey, mode)
    }
}