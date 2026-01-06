package net.calvuz.qreport.checkup.data.local.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import net.calvuz.qreport.checkup.data.local.dao.CheckItemDao
import net.calvuz.qreport.checkup.domain.model.CheckItem
import net.calvuz.qreport.checkup.domain.model.CheckItemStatus
import net.calvuz.qreport.client.island.domain.model.IslandType
import net.calvuz.qreport.checkup.domain.model.module.ModuleProgress
import net.calvuz.qreport.checkup.domain.repository.CheckItemRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckItemRepositoryImpl @Inject constructor(
    private val checkItemDao: CheckItemDao
): CheckItemRepository {

    // In CheckItemRepositoryImpl.kt
    override suspend fun updateCheckItemStatus(id: String, status: CheckItemStatus) {
        val now = if (status != CheckItemStatus.PENDING) Clock.System.now() else null
        checkItemDao.updateCheckItemStatus(id, status.name, now)
    }

    override fun getCheckItemsByCheckUpId(checkUpId: String): Flow<List<CheckItem>> {
        TODO("Not yet implemented")
    }

    override suspend fun getCheckItemById(id: String): CheckItem? {
        TODO("Not yet implemented")
    }

    override fun getCheckItemsByModule(
        checkUpId: String,
        moduleType: String
    ): Flow<List<CheckItem>> {
        TODO("Not yet implemented")
    }

    override suspend fun createCheckItem(checkItem: CheckItem) {
        TODO("Not yet implemented")
    }

    override suspend fun createCheckItemsFromTemplates(
        checkUpId: String,
        islandType: IslandType
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun updateCheckItem(checkItem: CheckItem) {
        TODO("Not yet implemented")
    }

    override suspend fun updateCheckItemNotes(id: String, notes: String) {
        checkItemDao.updateCheckItemNotes(id, notes)
    }

    override suspend fun deleteCheckItem(id: String) {
        TODO("Not yet implemented")
    }

    override suspend fun getModuleProgress(
        checkUpId: String,
        moduleType: String
    ): ModuleProgress {
        TODO("Not yet implemented")
    }
}