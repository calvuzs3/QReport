package net.calvuz.qreport.checkup.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.checkup.domain.model.CheckItem
import net.calvuz.qreport.checkup.domain.model.CheckItemStatus
import net.calvuz.qreport.client.island.domain.model.IslandType
import net.calvuz.qreport.checkup.domain.model.module.ModuleProgress

interface CheckItemRepository {

    fun getCheckItemsByCheckUpId(checkUpId: String): Flow<List<CheckItem>>

    suspend fun getCheckItemById(id: String): CheckItem?

    fun getCheckItemsByModule(checkUpId: String, moduleType: String): Flow<List<CheckItem>>

    suspend fun createCheckItem(checkItem: CheckItem)

    suspend fun createCheckItemsFromTemplates(checkUpId: String, islandType: IslandType)

    suspend fun updateCheckItem(checkItem: CheckItem)

    suspend fun updateCheckItemStatus(id: String, status: CheckItemStatus)

    suspend fun updateCheckItemNotes(id: String, notes: String)

    suspend fun deleteCheckItem(id: String)

    suspend fun getModuleProgress(checkUpId: String, moduleType: String): ModuleProgress
}