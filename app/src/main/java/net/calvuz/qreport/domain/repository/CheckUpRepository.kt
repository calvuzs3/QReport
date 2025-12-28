package net.calvuz.qreport.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.domain.model.checkup.CheckUp
import net.calvuz.qreport.domain.model.checkup.CheckUpProgress
import net.calvuz.qreport.domain.model.checkup.CheckUpSingleStatistics
import net.calvuz.qreport.domain.model.checkup.CheckUpStatus
import net.calvuz.qreport.domain.model.island.IslandType
import net.calvuz.qreport.domain.model.spare.SparePart

interface CheckUpRepository {

    fun getAllCheckUps(): Flow<List<CheckUp>>

    suspend fun getCheckUpById(id: String): CheckUp?

    suspend fun getCheckUpWithDetails(id: String): CheckUp?

    fun getCheckUpsByStatus(status: CheckUpStatus): Flow<List<CheckUp>>

    fun getCheckUpsByIslandType(islandType: IslandType): Flow<List<CheckUp>>

    suspend fun createCheckUp(checkUp: CheckUp): String

    suspend fun updateCheckUp(checkUp: CheckUp)

    suspend fun deleteCheckUp(id: String)

    suspend fun updateCheckUpStatus(id: String, status: CheckUpStatus)

    suspend fun completeCheckUp(id: String)

    suspend fun getCheckUpStatistics(id: String): CheckUpSingleStatistics

    suspend fun getCheckUpProgress(id: String): CheckUpProgress

    // SPARE PARTS
    suspend fun addSparePart(sparePart: SparePart): String
    suspend fun updateSparePart(sparePart: SparePart)
    suspend fun deleteSparePart(id: String)
    suspend fun getSparePartsByCheckUp(checkUpId: String): List<SparePart>
}

