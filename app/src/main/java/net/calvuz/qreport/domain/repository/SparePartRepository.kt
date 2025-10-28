package net.calvuz.qreport.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.domain.model.SparePart
import net.calvuz.qreport.domain.model.SparePartCategory
import net.calvuz.qreport.domain.model.SparePartUrgency

interface SparePartRepository {

    fun getSparePartsByCheckUpId(checkUpId: String): Flow<List<SparePart>>

    suspend fun getSparePartById(id: String): SparePart?

    fun getSparePartsByCategory(checkUpId: String, category: SparePartCategory): Flow<List<SparePart>>

    fun getSparePartsByUrgency(checkUpId: String, urgency: SparePartUrgency): Flow<List<SparePart>>

    suspend fun addSparePart(sparePart: SparePart): String

    suspend fun updateSparePart(sparePart: SparePart)

    suspend fun deleteSparePart(id: String)

    suspend fun getTotalValue(checkUpId: String): Double
}