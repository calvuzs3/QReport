package net.calvuz.qreport.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.domain.model.*

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

    suspend fun getCheckUpStatistics(id: String): CheckUpStatistics

    suspend fun getCheckUpProgress(id: String): CheckUpProgress
}

