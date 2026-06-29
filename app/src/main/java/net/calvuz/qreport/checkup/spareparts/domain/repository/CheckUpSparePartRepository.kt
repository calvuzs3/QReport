package net.calvuz.qreport.checkup.spareparts.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.checkup.spareparts.domain.model.CheckUpSparePart

interface CheckUpSparePartRepository {
    fun observeByCheckup(checkupId: String): Flow<List<CheckUpSparePart>>
    suspend fun addParts(parts: List<CheckUpSparePart>): Result<Unit>
    suspend fun removePart(id: String): Result<Unit>
    suspend fun updateQuantity(id: String, quantity: Double?): Result<Unit>
    suspend fun updateNotes(id: String, notes: String): Result<Unit>
    suspend fun clearAllForCheckup(checkupId: String): Result<Unit>
}
