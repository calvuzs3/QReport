package net.calvuz.qreport.checkup.checkup.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.checkup.checkup.domain.model.CheckUp
import net.calvuz.qreport.checkup.checkup.domain.model.CheckUpProgress
import net.calvuz.qreport.checkup.checkup.domain.model.CheckUpSingleStatistics

interface CheckUpRepository {

    fun getAllCheckUps(): Flow<List<CheckUp>>

    suspend fun getCheckUpById(id: String): CheckUp?

    suspend fun getCheckUpWithDetails(id: String): CheckUp?

    fun getCheckUpsByStatus(status: String): Flow<List<CheckUp>>

    fun getCheckUpsByIslandType(islandType: String): Flow<List<CheckUp>>

    suspend fun createCheckUp(checkUp: CheckUp): String

    suspend fun updateCheckUp(checkUp: CheckUp)

    suspend fun deleteCheckUp(id: String)

    suspend fun updateCheckUpStatus(id: String, status: String)

    suspend fun completeCheckUp(id: String, status: String)

    suspend fun getCheckUpStatistics(id: String): CheckUpSingleStatistics

    suspend fun getCheckUpProgress(id: String): CheckUpProgress
}

